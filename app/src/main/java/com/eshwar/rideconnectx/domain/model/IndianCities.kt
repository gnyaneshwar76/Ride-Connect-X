package com.eshwar.rideconnectx.domain.model

/**
 * The City picker's list (N15): bundled, so it works offline and costs
 * nothing. State capitals, and cities of about a lakh or more people in each
 * state. A city missing here can still be kept if it was saved earlier.
 */
object IndianCities {
    val all: List<String> = listOf(
        "Agartala", "Agra", "Ahmedabad", "Ahmednagar", "Aizawl", "Ajmer", "Akola", "Aligarh",
        "Allahabad", "Alwar", "Ambala", "Amravati", "Amritsar", "Anand", "Anantapur", "Asansol",
        "Aurangabad", "Bareilly", "Belgaum", "Bengaluru", "Bhagalpur", "Bharatpur", "Bhavnagar",
        "Bhilai", "Bhiwandi", "Bhopal", "Bhubaneswar", "Bikaner", "Bilaspur", "Bokaro",
        "Chandigarh", "Chennai", "Coimbatore", "Cuttack", "Darbhanga", "Davanagere", "Dehradun",
        "Delhi", "Dhanbad", "Dharwad", "Dibrugarh", "Durg", "Durgapur", "Eluru", "Erode",
        "Faridabad", "Firozabad", "Gandhinagar", "Gangtok", "Gaya", "Ghaziabad", "Gorakhpur",
        "Gulbarga", "Guntur", "Gurugram", "Guwahati", "Gwalior", "Haldwani", "Hisar", "Hubli",
        "Hyderabad", "Imphal", "Indore", "Itanagar", "Jabalpur", "Jaipur", "Jalandhar", "Jalgaon",
        "Jammu", "Jamnagar", "Jamshedpur", "Jhansi", "Jodhpur", "Junagadh", "Kadapa", "Kakinada",
        "Kalyan", "Kanpur", "Karimnagar", "Karnal", "Kochi", "Kohima", "Kolhapur", "Kolkata",
        "Kollam", "Korba", "Kota", "Kozhikode", "Kurnool", "Latur", "Lucknow", "Ludhiana",
        "Madurai", "Malegaon", "Mangaluru", "Mathura", "Meerut", "Moradabad", "Mumbai", "Muzaffarpur",
        "Mysuru", "Nagpur", "Nanded", "Nashik", "Navi Mumbai", "Nellore", "Nizamabad", "Noida",
        "Ongole", "Panaji", "Panipat", "Patiala", "Patna", "Puducherry", "Pune", "Raipur",
        "Rajahmundry", "Rajkot", "Ranchi", "Rohtak", "Rourkela", "Saharanpur", "Salem", "Sangli",
        "Secunderabad", "Shillong", "Shimla", "Siliguri", "Solapur", "Srinagar", "Surat",
        "Thane", "Thanjavur", "Thiruvananthapuram", "Thrissur", "Tiruchirappalli", "Tirunelveli",
        "Tirupati", "Tiruppur", "Udaipur", "Ujjain", "Vadodara", "Varanasi", "Vasai-Virar",
        "Vellore", "Vijayawada", "Visakhapatnam", "Warangal",
    )

    /** Names starting with [query] first, then names containing it. */
    fun search(query: String, limit: Int = 8): List<String> {
        val q = query.trim()
        if (q.isEmpty()) return all.take(limit)
        val starts = all.filter { it.startsWith(q, ignoreCase = true) }
        val contains = all.filter { it.contains(q, ignoreCase = true) && it !in starts }
        return (starts + contains).take(limit)
    }

    /** Case-insensitive match against the list, giving its spelling back. */
    fun find(name: String): String? = all.firstOrNull { it.equals(name.trim(), ignoreCase = true) }
}
