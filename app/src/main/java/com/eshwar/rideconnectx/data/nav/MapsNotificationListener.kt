package com.eshwar.rideconnectx.data.nav

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.telecom.TelecomManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.eshwar.rideconnectx.core.di.ServiceEntryPoint
import com.eshwar.rideconnectx.data.repository.NavigationRelay
import com.eshwar.rideconnectx.domain.ProtocolEngine
import com.eshwar.rideconnectx.domain.model.NavManeuver
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Reads Google Maps' ongoing navigation notification and feeds each instruction
 * into [NavigationRelay].
 *
 * Maps has no public API for live turn-by-turn, and its notification is the only
 * supported surface that carries the instruction, the distance to the turn and
 * the remaining ETA. That is what the rider already sees on their lock screen,
 * so nothing here is scraped that they are not already being shown.
 *
 * The notification's wording is not a contract — Google changes it — so parsing
 * is deliberately forgiving: an unrecognised phrase yields a maneuver with a
 * generic icon rather than dropping the update.
 */
class MapsNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "RCX-Nav"

        /**
         * The running listener, so a debug broadcast can reach it.
         *
         * A notification listener is started by the system and cannot be bound
         * to from a receiver, and the arrow catalogue dump has to run inside
         * this service because that is where the rendering code lives.
         */
        @Volatile
        var instance: MapsNotificationListener? = null
            private set

        /** Maps and Maps Go both post the same shaped navigation notification. */
        val MAPS_PACKAGES = setOf(
            "com.google.android.apps.maps",
            "com.google.android.apps.mapslite",
        )

        /**
         * Apps whose notifications light the cluster's message lamp.
         *
         * The cluster has one indicator, not a per-app inbox, so this only has
         * to answer "is there something waiting". SMS is included because the
         * official app treats WhatsApp and SMS the same way.
         */
        val MESSAGE_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.google.android.apps.messaging",
        )

        /** Notification categories that mean a call rather than a message. */
        private val CALL_CATEGORIES = setOf(
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_MISSED_CALL,
        )

        /**
         * Telephony packages whose call category is believed even when they are
         * not the current default dialer — the in-call UI and the telecom
         * service post under their own names on several OEM builds.
         *
         * A name in this set is **not** sufficient on its own. `com.android.*`
         * is a plain application id that nothing reserves: on a retail phone
         * that ships `com.google.android.dialer`, any app can be built with
         * `applicationId "com.android.dialer"`, install without conflict, and
         * inherit this trust. Membership here is only checked together with the
         * system flag — see [isTrustedCallSource].
         */
        private val TELECOM_PACKAGES = setOf(
            "com.android.server.telecom",
            "com.android.dialer",
            "com.android.incallui",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
        )

        /**
         * Apps that place calls the rider cares about and are already trusted
         * for messages. Without these, restricting calls to telephony silently
         * drops every VoIP call — which is most calls, for many riders.
         */
        private val VOIP_CALL_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.thoughtcrime.securesms",
            "com.google.android.apps.tachyon",
        )
    }

    /** Fetched rather than field-injected — see [ServiceEntryPoint]. */
    private val relay: NavigationRelay
        get() = EntryPointAccessors
            .fromApplication(applicationContext, ServiceEntryPoint::class.java)
            .navigationRelay()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Identifies the manoeuvre arrow. Built lazily on first navigation. */
    private val matcher by lazy { ArrowMatcher(applicationContext) }

    init { instance = this }

    /**
     * Maps' own resources, held open so the icon name lookup is not a package
     * manager round-trip several times a second for a whole ride.
     */
    private var mapsResources: android.content.res.Resources? = null

    /**
     * Turns a drawable id from another app into its resource name.
     *
     * The id itself is meaningless across Maps versions - it is an index into
     * their resource table and shifts on every release. The *name* does not, so
     * that is what gets recorded and what any future mapping is keyed on.
     *
     * Best-effort: an app can ship stripped resource names, and a lookup that
     * fails must never interrupt navigation.
     */
    /**
     * Everything the notification carries, dumped once per distinct value.
     *
     * The 19 August evening ride ruled out the two fields we had been reading:
     * the text has no direction on 88% of frames, and the small icon is one
     * generic `nav_notification_icon` for every manoeuvre. But the rider's
     * screenshot of the shade shows an arrow drawn on the right-hand side of
     * the notification, which is the **large icon** slot - a field we have
     * never looked at.
     *
     * So this lists every extras key with its type and value, and describes the
     * large icon properly: a resource id resolves to a drawable name, and a
     * bitmap is reduced to a hash. A hash is enough - the same arrow produces
     * the same hash every time, so ~15 hashes map to ~15 manoeuvres by
     * observation, exactly how the cluster codes were established.
     */
    private var lastDump = ""

    private fun dumpDiagnostics(sbn: StatusBarNotification, extras: android.os.Bundle) {
        val log = runCatching {
            EntryPointAccessors
                .fromApplication(applicationContext, ServiceEntryPoint::class.java)
                .rideLog()
        }.getOrNull() ?: return

        val keys = extras.keySet().sorted()

        // The full dump is keyed on the SHAPE of the notification - which keys
        // exist - not on their values. Values change every second (the distance
        // counts down), and deduplicating on those would write a dump per
        // frame and bury the log. One block per structural change is enough.
        val structure = keys.joinToString(",")
        if (structure != lastDump) {
            lastDump = structure
            val summary = StringBuilder()
            for (k in keys) {
                val v = runCatching { extras.get(k) }.getOrNull() ?: continue
                val shown = when (v) {
                    is CharSequence -> "\"${v.toString().take(60)}\""
                    is Array<*> -> v.joinToString(" / ", limit = 4) { it.toString().take(30) }
                    else -> v.toString().take(60)
                }
                summary.append("      $k = [${v.javaClass.simpleName}] $shown\n")
            }
            log.diag("---- notification structure ----\n$summary")
        }
    }

    /**
     * Records each distinct arrow the first time it is seen, with whatever
     * Maps was saying at that moment. That pairing is the whole mapping table.
     */
    private fun recordArrow(sig: IconSig, title: String, text: String) {
        if (sig.shape.isBlank()) return
        if (!arrowsSeen.add(sig.shape)) return
        runCatching {
            EntryPointAccessors
                .fromApplication(applicationContext, ServiceEntryPoint::class.java)
                .rideLog()
                .diag(
                    "NEW ARROW #${arrowsSeen.size}  shape=${sig.shape}  " +
                        "exact=${sig.exact}  while Maps said: \"$text\"  ($title)",
                )
        }
    }

    /**
     * A picture reduced to two fingerprints.
     *
     * [exact] is every pixel's alpha, and [shape] is a coarse 16x16 on/off
     * grid. Two of them because an exact hash is only useful if Maps redraws
     * the arrow identically every time - and if it anti-aliases, or overlays a
     * countdown ring, the exact hash changes every frame and says nothing. The
     * coarse grid survives that, because a left arrow still covers the same
     * squares. Whichever proves stable across a route is the one we key on;
     * recording both means one run answers that rather than two.
     */
    private data class IconSig(val exact: String, val shape: String, val note: String)

    private fun iconSignature(icon: android.graphics.drawable.Icon?): IconSig {
        if (icon == null) return IconSig("", "", "none")

        val type = runCatching { icon.type }.getOrNull()
        if (type == android.graphics.drawable.Icon.TYPE_RESOURCE) {
            val id = runCatching { icon.resId }.getOrDefault(0)
            val pkg = runCatching { icon.resPackage }.getOrNull().orEmpty()
            val name = resolveIconName(pkg.ifBlank { MAPS_PACKAGES.first() }, id)
            return IconSig(name.ifBlank { "res$id" }, name.ifBlank { "res$id" }, "resource $name")
        }

        return runCatching {
            val drawable = icon.loadDrawable(this)
                ?: return@runCatching IconSig("", "", "unrenderable")
            val n = 32
            val bmp = android.graphics.Bitmap.createBitmap(
                n, n, android.graphics.Bitmap.Config.ARGB_8888,
            )
            android.graphics.Canvas(bmp).also {
                drawable.setBounds(0, 0, n, n)
                drawable.draw(it)
            }
            val px = IntArray(n * n)
            bmp.getPixels(px, 0, n, 0, 0, n, n)
            bmp.recycle()

            // Alpha only. Maps tints the arrow white on a coloured card, so the
            // shape lives in the alpha channel and the colour is noise.
            var fine = 17
            for (p in px) fine = fine * 31 + (p ushr 24)

            // Coarse: 16x16 cells, each on if its 2x2 block is mostly opaque.
            val bits = StringBuilder()
            for (cy in 0 until 16) for (cx in 0 until 16) {
                var sum = 0
                for (dy in 0..1) for (dx in 0..1) {
                    sum += px[(cy * 2 + dy) * n + (cx * 2 + dx)] ushr 24
                }
                bits.append(if (sum / 4 > 128) '1' else '0')
            }
            val shape = bits.toString().chunked(4)
                .joinToString("") { Integer.toHexString(it.toInt(2)) }

            IconSig(Integer.toHexString(fine), shape, "bitmap ${icon.type}")
        }.getOrDefault(IconSig("", "", "sig-failed"))
    }

    /**
     * Arrow shapes already written down, so each new one is recorded once.
     *
     * This is the mapping table being built: a shape, and the instruction that
     * was on screen the first time it appeared. Exactly how the cluster codes
     * were established - see it, write it down, do not guess.
     */
    private val arrowsSeen = mutableSetOf<String>()

    /**
     * Fingerprints every manoeuvre arrow in Maps' own catalogue, once.
     *
     * This is the step that removes simulation from the problem. A driven route
     * only yields the manoeuvres that route contains - the 20 August run gave
     * five out of sixty-seven - so waiting to meet a roundabout or a U-turn on
     * the road would take many more runs and still never guarantee coverage.
     *
     * Maps' resources are readable by any app, so each named drawable is loaded
     * and put through **the same [iconSignature] used on the live notification
     * icon**. Same renderer, same 32x32 alpha reduction, therefore directly
     * comparable fingerprints. Whether that holds is verifiable rather than
     * assumed: the five arrows already captured live must come back with the
     * names we expect, and if they do not, this whole approach is wrong and
     * the log will say so.
     */
    fun dumpArrowCatalog() {
        val log = runCatching {
            EntryPointAccessors
                .fromApplication(applicationContext, ServiceEntryPoint::class.java)
                .rideLog()
        }.getOrNull() ?: return

        val res = runCatching {
            packageManager.getResourcesForApplication(MapsArrowCatalog.MAPS_PACKAGE)
        }.getOrNull() ?: run {
            log.diag("ARROW CATALOG: cannot read Maps resources")
            return
        }

        log.diag("==== ARROW CATALOG (${MapsArrowCatalog.NAMES.size} names) ====")
        var found = 0
        for (name in MapsArrowCatalog.NAMES) {
            val id = runCatching {
                res.getIdentifier(name, "drawable", MapsArrowCatalog.MAPS_PACKAGE)
            }.getOrDefault(0)
            if (id == 0) {
                log.diag("CAT  $name  = NOT FOUND")
                continue
            }
            // Rendered from the drawable directly: a resource-backed Icon would
            // only report its name back, and the shape is the whole point.
            val shape = runCatching {
                val d = androidx.core.content.res.ResourcesCompat.getDrawable(res, id, null)
                    ?: return@runCatching ""
                renderShape(d)
            }.getOrDefault("")
            found++
            log.diag("CAT  $name  shape=$shape")
        }
        log.diag("==== ARROW CATALOG END: $found resolved ====")
    }

    /** The 16x16 alpha grid used for both live icons and catalogue drawables. */
    private fun renderShape(drawable: android.graphics.drawable.Drawable): String {
        val n = 32
        val bmp = android.graphics.Bitmap.createBitmap(
            n, n, android.graphics.Bitmap.Config.ARGB_8888,
        )
        android.graphics.Canvas(bmp).also {
            drawable.setBounds(0, 0, n, n)
            drawable.draw(it)
        }
        val px = IntArray(n * n)
        bmp.getPixels(px, 0, n, 0, 0, n, n)
        bmp.recycle()
        val bits = StringBuilder()
        for (cy in 0 until 16) for (cx in 0 until 16) {
            var sum = 0
            for (dy in 0..1) for (dx in 0..1) {
                sum += px[(cy * 2 + dy) * n + (cx * 2 + dx)] ushr 24
            }
            bits.append(if (sum / 4 > 128) '1' else '0')
        }
        return bits.toString().chunked(4)
            .joinToString("") { Integer.toHexString(it.toInt(2)) }
    }

    /**
     * Whether the display is on, for the stall investigation.
     *
     * `isInteractive` rather than the deprecated `isScreenOn`: it reports the
     * state that governs whether apps are allowed to keep updating, which is
     * the thing under suspicion.
     */
    private fun isScreenOn(): Boolean = runCatching {
        (getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager)
            .isInteractive
    }.getOrDefault(true)

    private fun resolveIconName(pkg: String, resId: Int): String {
        if (resId == 0) return ""
        return runCatching {
            val res = mapsResources
                ?: packageManager.getResourcesForApplication(pkg).also { mapsResources = it }
            res.getResourceEntryName(resId)
        }.getOrDefault("")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in MAPS_PACKAGES) {
            relayClusterAlert(sbn)
            return
        }

        val extras = sbn.notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()

        // One compact line rather than a dump of every field. The full dump was
        // used on 6 August to establish the layout — title is the distance, text
        // is the instruction, subText the journey — and it is recorded in the
        // knowledge base. Keeping the three fields that matter is enough to spot
        // a future Maps release moving them again, without flooding logcat
        // several times a second for a whole ride.
        // The maneuver ICON, which is where the direction actually lives.
        //
        // The 19 August ride proved the text does not carry it. Maps puts the
        // road you are turning ONTO in `text` and counts the distance down to
        // the turn - "Neredmet - Sainikpur X Rd  400 m" means "in 400 m turn
        // onto Neredmet Rd", with no left or right anywhere in the words. Only
        // 5 of ~40 turns that ride said "Turn left"/"Turn right" outright, so
        // 1,719 of 1,982 frames went out as STRAIGHT (code 40) - which the
        // rider saw on the dashboard, and which code 40 is photographed to draw.
        //
        // The arrow the rider sees in the notification shade is the small icon,
        // so its resource name is the direction. Recorded, not yet acted on:
        // the ids are Maps' own and have to be observed before they can be
        // mapped, the same rule the cluster codes were established under.
        // The LARGE icon is the manoeuvre arrow - the one the rider can see in
        // their own screenshot of the shade, drawn on the right of the card.
        // The small icon is Maps' logo and is the same on every frame; reading
        // only that is what made two rides report a single generic name.
        val arrow = runCatching {
            iconSignature(extras.getParcelable(Notification.EXTRA_LARGE_ICON))
        }.getOrDefault(IconSig("", "", "failed"))

        // Identify the arrow against Maps' own catalogue. This is what supplies
        // the direction on the ~88% of frames whose text carries none.
        val arrowMatch = runCatching {
            val icon = extras.getParcelable<android.graphics.drawable.Icon>(
                Notification.EXTRA_LARGE_ICON,
            )
            icon?.loadDrawable(this)?.let { matcher.match(it) }
        }.getOrNull()

        // The log shows the identified name where it used to show a bare
        // fingerprint, so a wrong arrow is readable without decoding hex.
        val iconName = arrowMatch?.let { "${it.name} (±${it.distance})" }
            ?: arrow.shape.ifBlank { "" }
        val iconId = runCatching { sbn.notification.smallIcon?.resId ?: 0 }.getOrDefault(0)

        runCatching { dumpDiagnostics(sbn, extras) }
        runCatching { recordArrow(arrow, title, text) }

        Log.d(TAG, "RAW title='$title' | text='$text' | subText='$subText' | icon=$iconName ($iconId)")
        // Also to the ride log - logcat does not survive a 90 minute ride, and
        // the 19 August ride showed the arrival case is only visible here.
        runCatching {
            EntryPointAccessors
                .fromApplication(applicationContext, ServiceEntryPoint::class.java)
                .rideLog()
                .raw(title, text, subText, iconName, iconId)
        }

        if (title.isBlank() && text.isBlank()) return

        val parsed = MapsNotificationParser.parse(
            title, text, subText, iconName, arrowMatch?.code,
        )
        if (parsed == null) {
            Log.v(TAG, "Maps notification ignored: title='$title' text='$text'")
            return
        }
        // Read here rather than in the relay: the rider's report that the screen
        // being off stalls navigation has to be measured at the moment Maps
        // posts, not whenever the packet happens to be built.
        val maneuver = parsed.copy(screenOn = isScreenOn())

        Log.d(TAG, "Maneuver: ${maneuver.instruction} in ${maneuver.distanceToTurn}")
        scope.launch { relay.onManeuver(maneuver) }
    }

    /**
     * Lights the cluster's message or missed-call lamp for a non-Maps
     * notification.
     *
     * Only WhatsApp/SMS and call notifications count. Ongoing notifications are
     * skipped — a music player or a running download is not something the rider
     * needs flagged mid-ride, and it would keep the lamp permanently lit.
     */
    private fun relayClusterAlert(sbn: StatusBarNotification) {
        val category = sbn.notification?.category
        val isCall = category in CALL_CATEGORIES

        // Every non-Maps notification is logged at verbose, because when a lamp
        // does not light the first question is always 'did the app even see it',
        // and without this there is no way to tell a missed notification from a
        // rejected one. The rider hit exactly that on 18 August.
        Log.v(
            TAG,
            "alert? pkg=${sbn.packageName} category=$category ongoing=${sbn.isOngoing} isCall=$isCall",
        )

        // Ongoing notifications are skipped for MESSAGES only. A music player or
        // a running download must not pin the lamp on.
        //
        // Calls are deliberately exempt: an *incoming* call posts an ongoing
        // notification while it rings, so the old blanket `if (sbn.isOngoing)
        // return` at the top of this function threw away every incoming call
        // before the category was even looked at. That is why the call lamp
        // never lit during the 18 August test.
        when {
            isCall && isTrustedCallSource(sbn.packageName) -> {
                val who = sbn.notification.extras
                    ?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                Log.d(TAG, "Call from ${sbn.packageName} who='$who' - flagging cluster")
                ClusterAlerts.onMissedCall(title = who)
            }
            sbn.isOngoing -> return
            sbn.packageName in MESSAGE_PACKAGES -> {
                val extras = sbn.notification.extras
                val who = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                val body = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
                // 'W' WhatsApp, 'N' SMS - the identifiers the official app uses
                // in the 0x06 packet.
                val app = if (sbn.packageName.startsWith("com.whatsapp")) 'W' else 'N'
                Log.d(TAG, "Message from ${sbn.packageName} who='$who' - flagging cluster")
                ClusterAlerts.onNotification(appIdentifier = app, title = who, text = body)
            }
        }
    }

    /**
     * Whether a call-category notification is believed.
     *
     * Android lets *any* app set [Notification.CATEGORY_CALL], and the call
     * branch above passes the notification's own title straight to the cluster
     * as a caller name. Without this check any installed app could print 26
     * characters of its choosing on the dashboard and light the missed-call
     * lamp — the message branch has always had an allowlist, the call branch
     * had none.
     *
     * The default dialer is read fresh each time rather than cached: the rider
     * can change it, and a stale answer here silently drops real calls.
     */
    private fun isTrustedCallSource(pkg: String): Boolean {
        // The current default dialer is the rider's own choice, recorded by the
        // OS. This is the primary rule.
        val dialer = runCatching {
            getSystemService(TelecomManager::class.java)?.defaultDialerPackage
        }.getOrNull()
        if (pkg == dialer) return true

        // A telephony name is believed only if the OS also says that package is
        // part of the system image. Checking the name alone would let any app
        // claim `com.android.dialer` on a phone that does not already ship it.
        if (pkg in TELECOM_PACKAGES && isSystemPackage(pkg)) return true

        if (pkg in VOIP_CALL_PACKAGES || pkg in MESSAGE_PACKAGES) return true

        Log.d(TAG, "Ignoring call-category notification from untrusted pkg=$pkg")
        return false
    }

    /** True only for a package on the system image or a system-signed update. */
    private fun isSystemPackage(pkg: String): Boolean = runCatching {
        val flags = packageManager.getApplicationInfo(pkg, 0).flags
        (flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
    }.getOrDefault(false)
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName !in MAPS_PACKAGES) return
        // Maps clearing its notification means the route ended or was cancelled.
        Log.d(TAG, "Maps navigation notification removed — stopping relay")
        relay.stop()
    }
}

/**
 * Turns the text of a Maps navigation notification into a [NavManeuver].
 *
 * Kept separate from the service so it can be reasoned about — and tested —
 * without an Android notification in hand.
 */
object MapsNotificationParser {

    /** "400 m", "1.2 km", "250 ft", "0.5 mi" — the distance to the next turn. */
    private val DISTANCE = Regex(
        """(\d+(?:[.,]\d+)?)\s*(km|m|mi|ft|yd)\b""",
        RegexOption.IGNORE_CASE,
    )

    /** "12 min", "1 hr 5 min" — remaining time. */
    private val ETA_MINUTES = Regex("""(\d+)\s*min""", RegexOption.IGNORE_CASE)
    private val ETA_HOURS = Regex("""(\d+)\s*(?:hr|hour)""", RegexOption.IGNORE_CASE)

    /**
     * Ordered longest-phrase-first: "slight left" has to be tested before
     * "left", or every slight turn would be reported as a plain one.
     */
    // `[\s-]+` rather than `\s+` throughout: the qualifier and the direction
    // are sometimes joined by a hyphen rather than a space, and losing the
    // qualifier turns a slight turn into a hard one on the cluster.
    private val MANEUVERS: List<Pair<Regex, Int>> = listOf(
        Regex("""u[- ]?turn""", RegexOption.IGNORE_CASE) to ProtocolEngine.Maneuver.U_TURN,
        Regex("""sharp[\s-]+left""", RegexOption.IGNORE_CASE) to ProtocolEngine.Maneuver.SHARP_LEFT,
        Regex("""sharp[\s-]+right""", RegexOption.IGNORE_CASE) to ProtocolEngine.Maneuver.SHARP_RIGHT,
        Regex("""slight[\s-]+left""", RegexOption.IGNORE_CASE) to ProtocolEngine.Maneuver.SLIGHT_LEFT,
        Regex("""slight[\s-]+right""", RegexOption.IGNORE_CASE) to ProtocolEngine.Maneuver.SLIGHT_RIGHT,
        Regex("""keep[\s-]+left""", RegexOption.IGNORE_CASE) to ProtocolEngine.Maneuver.KEEP_LEFT,
        Regex("""keep[\s-]+right""", RegexOption.IGNORE_CASE) to ProtocolEngine.Maneuver.KEEP_RIGHT,
        Regex("""roundabout|rotary""", RegexOption.IGNORE_CASE) to ProtocolEngine.Maneuver.ROUNDABOUT,
        Regex("""merge""", RegexOption.IGNORE_CASE) to ProtocolEngine.Maneuver.MERGE,
        Regex("""arriv|destination""", RegexOption.IGNORE_CASE) to ProtocolEngine.Maneuver.DESTINATION,
        Regex("""\bleft\b""", RegexOption.IGNORE_CASE) to ProtocolEngine.Maneuver.TURN_LEFT,
        Regex("""\bright\b""", RegexOption.IGNORE_CASE) to ProtocolEngine.Maneuver.TURN_RIGHT,
    )

    /**
     * Captured from Google Maps on the rider's phone, 6 August 2026:
     *
     * ```
     * title   = "0 m"                                 ← distance to the turn
     * text    = "Head north"                          ← the instruction
     * subText = "1 hr 12 min · 38 km · 1:38 am ETA"   ← duration, remaining, ETA
     * ```
     *
     * This is the **opposite** of what the code assumed for months: it read the
     * title as the instruction, so it searched "350 m" for the word "left" and
     * of course never found one. A whole ride relayed nothing but straight-ahead
     * because of it, and the ETA was read from the wrong field too.
     *
     * Field roles are now fixed to what the device actually sends. `bigText`,
     * `infoText`, `summaryText`, `titleBig` and `tickerText` were all null, so
     * there is nothing else to fall back on.
     */
    /**
     * Reads a maneuver out of an icon's resource name.
     *
     * Maps names its drawables after what they draw - `ic_maneuver_turn_left`,
     * `ic_maneuver_roundabout_right` and so on - so the same phrase table that
     * reads the text reads these too, once the underscores are spaces. That is
     * why this is a normalisation and not a second hand-written table: one
     * table cannot drift out of step with itself.
     *
     * Returns null when the name matches nothing, which is also the answer for
     * a Maps build that ships stripped resource names.
     */
    fun maneuverFromIconName(iconName: String): Int? {
        if (iconName.isBlank()) return null
        val words = iconName.replace('_', ' ').replace('-', ' ')
        return MANEUVERS.firstOrNull { (pattern, _) -> pattern.containsMatchIn(words) }?.second
    }

    /**
     * Metres left of the whole journey, read from Maps' subText.
     *
     * `"0 min · 40 m · 6:12 pm ETA"` -> 40. The remaining figure is always the
     * last distance in that line, which is what tells us the rider has arrived.
     */
    private fun remainingMetres(subText: String): Int? {
        val last = DISTANCE.findAll(subText).map { it.value }.lastOrNull() ?: return null
        val number = Regex("""[\d.]+""").find(last)?.value?.toFloatOrNull() ?: return null
        return if (last.contains("km", ignoreCase = true)) (number * 1000).toInt()
        else number.toInt()
    }

    /**
     * How close the destination has to be before a distance-less frame counts
     * as arrival rather than a notification we simply cannot read.
     */
    private const val ARRIVAL_METRES = 60

    fun parse(
        title: String,
        text: String,
        subText: String = "",
        iconName: String = "",
        iconCode: Int? = null,
    ): NavManeuver? {
        // The instruction lives in `text`. Keeping title in the haystack costs
        // nothing and covers a Maps version that swaps them back.
        val haystack = "$text $title"

        // Maps posts other notifications too (traffic, offers, "you're offline").
        // A navigation one always states a distance to the next turn.
        val distance = DISTANCE.find(title)?.value
            ?: DISTANCE.find(text)?.value
            // No distance anywhere, but the destination is metres away: this is
            // the arrival frame. Maps drops the next-turn distance because there
            // is no next turn, and puts the destination's NAME in the text -
            // "SRI DEVI RESIDENCY", "Ramani Nilayam". No regex can match an
            // arbitrary place name, which is why arrival went undetected for
            // every ride until 20 August; the remaining distance identifies it
            // without needing to.
            //
            // Maps ships `maneuver_destination` but does not switch the
            // notification to it - it keeps the previous arrow - so the picture
            // cannot be used here either.
            ?: run {
                val left = remainingMetres(subText)
                if (left != null && left <= ARRIVAL_METRES && text.isNotBlank()) {
                    return NavManeuver(
                        maneuverId = ProtocolEngine.Maneuver.DESTINATION,
                        instruction = text.trim(),
                        distanceToTurn = "$left m",
                        remainingDistance = "$left m",
                        phraseRecognised = true,
                        iconName = iconName,
                        codeSource = NavManeuver.Source.ICON,
                    )
                }
                null
            }
            ?: return null

        val matched = MANEUVERS.firstOrNull { (pattern, _) -> pattern.containsMatchIn(haystack) }
            ?.second

        // The ARROW wins when it was identified, and the text is the fallback.
        //
        // That order is the opposite of what it was, and it is deliberate. The
        // arrow is what Maps actually draws for the rider, and it distinguishes
        // slight from normal from sharp, keep from fork, and every roundabout -
        // distinctions the text usually omits. The text only ever carries a
        // direction on about 12% of frames, and on those two the arrow agreed.
        //
        // A match is only accepted inside ArrowMatcher.MAX_DISTANCE, so an arrow
        // Maps has newly introduced falls through to the text rather than being
        // forced onto the nearest old shape. If neither speaks, STRAIGHT - the
        // same behaviour as before, so this can never be worse than it was.
        val maneuverId = iconCode
            ?: matched
            ?: ProtocolEngine.Maneuver.STRAIGHT
        val codeSource = when {
            iconCode != null -> NavManeuver.Source.ICON
            matched != null -> NavManeuver.Source.TEXT
            else -> NavManeuver.Source.FALLBACK
        }

        // Never the distance: "350 m" told the rider nothing and made the ride
        // log unreadable.
        val instruction = text.trim().ifBlank { title.trim() }

        // "1 hr 12 min · 38 km · 1:38 am ETA" — everything about the rest of the
        // journey is in subText, so that is where both of these come from.
        val journey = subText.ifBlank { text }

        return NavManeuver(
            maneuverId = maneuverId,
            instruction = instruction,
            distanceToTurn = distance.trim(),
            etaMinutes = parseEtaMinutes(journey),
            remainingDistance = DISTANCE.findAll(journey).map { it.value }.lastOrNull()
                ?.trim().orEmpty(),
            phraseRecognised = matched != null,
            iconName = iconName,
            iconManeuverId = iconCode,
            codeSource = codeSource,
        )
    }

    private fun parseEtaMinutes(source: String): Int? {
        val hours = ETA_HOURS.find(source)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minutes = ETA_MINUTES.find(source)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val total = hours * 60 + minutes
        return total.takeIf { it > 0 }
    }
}
