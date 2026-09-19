package com.eshwar.rideconnectx.data.nav

import android.content.Context
import com.eshwar.rideconnectx.BuildConfig
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Appends every maneuver sent to the cluster to a file on the device.
 *
 * logcat only survives while a laptop is attached and its buffer is small, so a
 * real ride would leave nothing behind but the rider's memory of which arrows
 * looked wrong. This keeps the evidence on the phone until it can be collected:
 *
 * ```
 * adb pull /sdcard/Android/data/com.gnyaneshwar.rideconnectx/files/ride-log.txt
 * ```
 *
 * Deliberately plain text and append-only. Writes are best-effort — a logging
 * failure must never interrupt navigation.
 */
@Singleton
class RideLog @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val stamp = SimpleDateFormat("HH:mm:ss", Locale.US)

    /**
     * Debug builds only.
     *
     * This is a development aid, and it records where the rider went: every
     * Maps maneuver instruction plus route start and end with full date and
     * time. In `src/main` with no gate it shipped in release builds too, into
     * external storage, uncapped and never deleted — readable by any app
     * holding READ_EXTERNAL_STORAGE on Android 9, and by ADB or a backup on
     * later versions. The path is unchanged so the existing `adb pull` still
     * works on the debug build used for turn testing.
     */
    private val file: File?
        get() {
            if (!BuildConfig.DEBUG) return null
            return runCatching { File(context.getExternalFilesDir(null), FILE_NAME) }.getOrNull()
        }

    /**
     * One entry per maneuver, written so a mismatch is obvious without decoding
     * anything:
     *
     * ```
     * [08:12:44]  Maps said : "Turn left onto MG Road"  (300 m)
     *             Cluster should show : LEFT   (code 38)
     *             Reached the scooter : yes
     *             What it ACTUALLY showed : ______________________
     * ```
     *
     * The blank line is for the rider to fill in from memory afterwards, or for
     * whoever reads the log to match against what they were told.
     */
    /** Wall-clock of the previous entry, for the stall column. */
    private var lastManeuverAt: Long = 0L

    fun maneuver(
        instruction: String,
        code: Int,
        distanceMetres: Int,
        delivered: Boolean,
        phraseRecognised: Boolean = true,
        iconName: String = "",
        iconCode: Int? = null,
        codeSource: String = "TEXT",
        screenOn: Boolean = true,
    ) {
        val m = com.eshwar.rideconnectx.domain.ProtocolEngine.Maneuver
        val label = m.label(code)
        // Confidence matters when reading this back: a PREDICTED code drawing
        // nothing is expected, a PHOTOGRAPHED one drawing nothing is a fault.
        val confidence = when (m.confidence(code)) {
            com.eshwar.rideconnectx.domain.ProtocolEngine.Maneuver.Confidence.PHOTOGRAPHED -> "PHOTOGRAPHED - should definitely draw"
            com.eshwar.rideconnectx.domain.ProtocolEngine.Maneuver.Confidence.PREDICTED -> "PREDICTED ONLY - never verified"
            com.eshwar.rideconnectx.domain.ProtocolEngine.Maneuver.Confidence.BELIEVED_BLANK -> "believed BLANK - may draw nothing"
        }
        val now = System.currentTimeMillis()
        val gapSeconds = if (lastManeuverAt == 0L) 0 else ((now - lastManeuverAt) / 1000).toInt()
        lastManeuverAt = now

        append("[${stamp.format(Date())}]  Maps said : \"$instruction\"  (${distanceMetres} m)")

        // What Maps itself meant, from the icon - the only field that carries a
        // direction on most frames. Printed beside our code so the two can be
        // compared by eye without decoding anything.
        val iconLabel = iconCode?.let { "${m.label(it)} (code $it)" }
            ?: if (iconName.isBlank()) "no icon" else "UNRECOGNISED"
        append("            Maps icon   : ${iconName.ifBlank { "—" }}  ->  $iconLabel")
        append("            We sent     : $label   (code $code)   [from $codeSource]")

        // The comparison the rider asked for. A disagreement is the interesting
        // case and must never be something you have to notice for yourself.
        if (iconCode != null && iconCode != code) {
            append("            *** MISMATCH - icon says ${m.label(iconCode)}, we sent $label ***")
        }

        append("            Code status : $confidence")
        if (!phraseRecognised && codeSource == "FALLBACK") {
            append("            *** NO DIRECTION ANYWHERE - text and icon both silent, sent STRAIGHT ***")
        } else if (!phraseRecognised) {
            append("            (direction came from the icon, not the words)")
        }
        append("            Reached the scooter : ${if (delivered) "yes" else "NO — not delivered"}")
        append("            Screen : ${if (screenOn) "on" else "OFF"}   Gap since last : ${gapSeconds}s")
        if (gapSeconds >= 5) {
            append("            *** STALL - no Maps update for ${gapSeconds}s (cluster was showing stale data) ***")
        }
        append("            What it ACTUALLY showed : ______________________")
        append("")
    }

    /**
     * The raw Maps notification fields, exactly as posted.
     *
     * Added after the 19 August ride. That ride proved the relay works - 1,982
     * manoeuvres, 98% delivered over 91 minutes - but arrival was never
     * detected, because Maps announces it with the DESTINATION'S NAME
     * ("Kl Universiy Hyderabad / 88XR+76R") and not the word "arriving".
     * No regex can be written against an arbitrary place name.
     *
     * Only `text` was ever recorded, so there was no way to see what the other
     * fields said at that moment. Recording all three means the next ride
     * settles it with evidence instead of a guess.
     *
     * Logged only when it changes, so a 90 minute ride does not write the same
     * line 1,900 times.
     */
    private var lastRaw: String = ""

    fun raw(
        title: String,
        text: String,
        subText: String,
        iconName: String = "",
        iconId: Int = 0,
    ) {
        val line = "title='$title' | text='$text' | subText='$subText' | " +
            "icon='$iconName' ($iconId)"
        if (line == lastRaw) return
        lastRaw = line
        append("    RAW  $line")
    }

    /**
     * A one-off diagnostic line, for working out where a field lives.
     *
     * Kept separate from [raw] because these are wide dumps written once per
     * distinct value, not per frame - the point is to discover a field, not to
     * record a ride.
     */
    fun diag(line: String) {
        append("  DIAG $line")
    }

    /** Marks the start of a route so separate rides can be told apart. */
    fun sessionStart() {
        append("")
        append("==================================================")
        append(" ROUTE STARTED  ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        append("==================================================")
        append("")
    }

    fun sessionEnd() {
        append("----- route ended -----")
        append("")
    }

    private fun append(line: String) {
        val f = file ?: return
        runCatching { f.appendText(line + "\n") }
            .onFailure { Log.w(TAG, "Ride log write failed", it) }
    }

    private companion object {
        const val TAG = "RCX-Nav"
        const val FILE_NAME = "ride-log.txt"
    }
}
