package com.eshwar.rideconnectx.domain.model

/** What sign-in does with the rider profile already on the phone. */
enum class HandoverAction {
    /** It is this account's own profile. */
    KEEP,

    /** Someone else's, or left behind: wipe it before this account's comes down. */
    CLEAR,

    /** The rider asked for it to follow them, and the account has none of its own. */
    CARRY,
}

/**
 * The local profile (name, nickname, city, vehicle, paint, photo) belongs to
 * one owner, and only ever moves to another when the rider asked for that.
 *
 * Before this, whatever profile was on the phone was merge-written into any
 * account that signed in without one of its own — a guest's name ("rocky bhai")
 * turned up in a different person's Google account setup (rider, 26 Sep).
 */
object ProfileHandover {
    /**
     * @param localOwner who the local profile belongs to; blank when unknown.
     * @param carry the rider chose "Save to an account" / "Move to another account".
     * @param accountHasProfile the account already holds a complete profile.
     */
    fun decide(localOwner: String, uid: String, carry: Boolean, accountHasProfile: Boolean): HandoverAction =
        when {
            localOwner == uid -> HandoverAction.KEEP
            // An account's own profile always wins over a carried one.
            carry && !accountHasProfile -> HandoverAction.CARRY
            else -> HandoverAction.CLEAR
        }
}
