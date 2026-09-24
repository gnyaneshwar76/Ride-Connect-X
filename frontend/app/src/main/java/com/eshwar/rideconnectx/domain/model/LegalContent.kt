package com.eshwar.rideconnectx.domain.model

/** One numbered section of a legal document. */
data class LegalSection(
    val title: String,
    val body: String,
    val bullets: List<String> = emptyList(),
    val tail: String = "",
)

/** Which document the legal screen is showing. */
enum class LegalDoc(val screenTitle: String) {
    TERMS("Terms & Conditions"),
    PRIVACY("Privacy Policy"),

    /**
     * Both documents on one page. This is what the profile step links to —
     * sending a rider to two separate pages to agree to one checkbox is
     * needless friction.
     */
    BOTH("Terms & Privacy Policy"),
}

/**
 * The real Terms of Service and Privacy Policy, supplied by the RideConnectX
 * team. Kept as data rather than strings.xml so the screen can render the
 * numbered / bulleted structure faithfully.
 */
object LegalContent {

    fun sections(doc: LegalDoc): List<LegalSection> = when (doc) {
        LegalDoc.TERMS -> TERMS
        LegalDoc.PRIVACY -> PRIVACY
        LegalDoc.BOTH -> TERMS + PRIVACY
    }

    val TERMS = listOf(
        LegalSection(
            "1. Acceptance of Terms",
            "Welcome to RideConnectX. By downloading, installing, or using the application, you agree to these Terms of Service. If you do not agree with these terms, please discontinue use of the application.",
        ),
        LegalSection(
            "2. About RideConnectX",
            "RideConnectX is a companion application designed to enhance compatible Suzuki Smart Connect vehicles by providing navigation, ride information, connectivity features, and personalization tools.",
            tail = "RideConnectX is an independent application and is not affiliated with, endorsed by, or sponsored by Suzuki Motor Corporation or any of its subsidiaries.",
        ),
        LegalSection(
            "3. Eligibility",
            "You must comply with all applicable laws while using RideConnectX.",
            tail = "You are responsible for ensuring that your use of the application is legal in your region.",
        ),
        LegalSection(
            "4. Safe Riding",
            "Your safety is your responsibility. RideConnectX is intended to assist your riding experience but must never distract you while operating a vehicle.\n\nAlways:",
            bullets = listOf(
                "Focus on the road.",
                "Follow traffic laws.",
                "Wear appropriate safety equipment.",
                "Obey local regulations.",
            ),
            tail = "Do not interact with the application while riding unless it is safe to do so.",
        ),
        LegalSection(
            "5. Navigation",
            "Navigation guidance is provided for convenience only. Routes, estimated arrival times, traffic conditions, and turn instructions may occasionally be inaccurate or unavailable.",
            tail = "Always prioritize road signs, traffic signals, and local laws over information displayed by the application.",
        ),
        LegalSection(
            "6. Bluetooth Connectivity",
            "Vehicle connectivity depends on device compatibility, Bluetooth availability, operating system restrictions, and supported vehicle features.",
            tail = "RideConnectX does not guarantee uninterrupted or continuous connectivity.",
        ),
        LegalSection(
            "7. User Accounts",
            "Some features may require signing in.",
            tail = "You are responsible for maintaining the security of your account and any activity performed using your account.",
        ),
        LegalSection(
            "8. Intellectual Property",
            "RideConnectX, its branding, interface, graphics, icons, and original software are protected by applicable intellectual property laws.",
            tail = "You may not copy, modify, reverse engineer, redistribute, or commercially exploit the application except where permitted by law.",
        ),
        LegalSection(
            "9. Acceptable Use",
            "You agree not to:",
            bullets = listOf(
                "Misuse the application.",
                "Attempt unauthorized access.",
                "Interfere with application functionality.",
                "Use RideConnectX for illegal activities.",
                "Modify communication with vehicles in ways that violate applicable laws.",
            ),
        ),
        LegalSection(
            "10. Updates",
            "RideConnectX may receive updates that improve functionality, security, compatibility, or performance.",
            tail = "Some features may change or be removed over time.",
        ),
        LegalSection(
            "11. Disclaimer",
            "RideConnectX is provided \"as is\" without warranties of any kind.",
            tail = "While every effort is made to provide reliable functionality, uninterrupted operation cannot be guaranteed.",
        ),
        LegalSection(
            "12. Limitation of Liability",
            "To the maximum extent permitted by law, RideConnectX and its developers shall not be liable for indirect, incidental, consequential, or special damages arising from the use or inability to use the application.",
        ),
        LegalSection(
            "13. Changes to These Terms",
            "These Terms may be updated periodically.",
            tail = "Continued use of RideConnectX after updates constitutes acceptance of the revised Terms.",
        ),
        LegalSection(
            "14. Contact",
            "Questions regarding these Terms may be submitted through the official RideConnectX support channels.",
        ),
    )

    val PRIVACY = listOf(
        LegalSection(
            "1. Introduction",
            "RideConnectX values your privacy.",
            tail = "This Privacy Policy explains what information the application collects, how it is used, and how it is protected.",
        ),
        LegalSection(
            "2. Information You Provide",
            "Depending on how you use RideConnectX, you may provide:",
            bullets = listOf(
                "Name",
                "Email address",
                "Profile information",
                "Vehicle selection",
                "Personal preferences",
            ),
        ),
        LegalSection(
            "3. Information Generated by the App",
            "RideConnectX may generate or store:",
            bullets = listOf(
                "Ride statistics",
                "Navigation preferences",
                "Vehicle settings",
                "Connected Bluetooth device information",
                "Application preferences",
                "Theme selection",
            ),
        ),
        LegalSection(
            "4. Location Information",
            "Location access is required to provide navigation and Bluetooth functionality.",
            tail = "Location information is used only for application features that require it.",
        ),
        LegalSection(
            "5. Bluetooth Information",
            "Bluetooth permission is required to discover and communicate with compatible vehicles.",
            tail = "RideConnectX accesses Bluetooth only when necessary for vehicle connectivity.",
        ),
        LegalSection(
            "6. Notifications",
            "Notification permission is used to deliver navigation guidance, ride alerts, and important application information.",
        ),
        LegalSection(
            "7. Data Storage",
            "Application data may be stored locally on your device.",
            tail = "If cloud-based features are introduced in future versions, this policy will be updated accordingly.",
        ),
        LegalSection(
            "8. Data Sharing",
            "RideConnectX does not sell your personal information.",
            tail = "Information is shared only when necessary to provide requested services or when required by applicable law.",
        ),
        LegalSection(
            "9. Security",
            "Reasonable measures are implemented to protect your information against unauthorized access, disclosure, or misuse.",
            tail = "However, no electronic storage method is completely secure.",
        ),
        LegalSection(
            "10. Your Choices",
            "You may:",
            bullets = listOf(
                "Change application permissions through device settings.",
                "Delete application data.",
                "Remove your account, where supported.",
                "Uninstall the application at any time.",
            ),
        ),
        LegalSection(
            "11. Children's Privacy",
            "RideConnectX is not intended for children under the age required by applicable law.",
        ),
        LegalSection(
            "12. Policy Updates",
            "This Privacy Policy may be updated periodically.",
            tail = "Material changes will be reflected by updating the \"Last Updated\" date.",
        ),
        LegalSection(
            "13. Contact",
            "Questions regarding privacy may be submitted through the official RideConnectX support channels.",
        ),
    )
}
