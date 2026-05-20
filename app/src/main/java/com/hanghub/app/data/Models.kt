package com.hanghub.app.data

import com.hanghub.app.ui.theme.HangVibe
import com.hanghub.app.ui.theme.UserStatus

// ═══════════════════════════════════════════════════════════════════════════
// MARK: Data Models
// ═══════════════════════════════════════════════════════════════════════════

data class HHUser(
    val id: String,
    val name: String,
    val avatar: String,       // emoji
    val status: UserStatus,
    val aura: Int,
    val streak: Int = 0,
    val distance: Double? = null,
)

data class HHPlace(
    val id: String,
    val name: String,
    val category: String,
    val emoji: String,
    val vibe: HangVibe,
    val rating: Double,
    val price: String,
    val dist: Double,
    val match: Int,
    val friends: Int,
    val hours: String,
)

enum class PlanState { VOTING, LOCKED }

data class PlanCost(
    val total: Int,
    val perPerson: Int,
    val settled: Int,
    val owed: Int,
)

data class HHPlan(
    val id: String,
    val title: String,
    val vibe: HangVibe,
    val place: HHPlace?,
    val time: String,
    val inHrs: Double,
    val state: PlanState,
    val participantIDs: List<String>,
    val rsvp: Map<String, String>,
    val options: List<HHPlace> = emptyList(),
    val votes: Map<String, List<String>> = emptyMap(),
    val cost: PlanCost? = null,
)

data class HHChat(
    val id: String,
    val userID: String,
    val lastMessage: String,
    val time: String,
    val unread: Int,
    val isTyping: Boolean = false,
)

data class ChatMessage(
    val from: String,
    val text: String,
    val time: String,
)

// ═══════════════════════════════════════════════════════════════════════════
// MARK: Sample Data — mirrors data.jsx 1-to-1
// ═══════════════════════════════════════════════════════════════════════════

object SampleData {

    val users = listOf(
        HHUser("me",    "You",        "🦊", UserStatus.FREE,    78, streak = 9),
        HHUser("sara",  "Sara Patel", "🌸", UserStatus.FREE,    84, distance = 0.3),
        HHUser("kai",   "Kai Wu",     "🐻", UserStatus.BUSY,    62, distance = 0.8),
        HHUser("lena",  "Lena Rios",  "🦋", UserStatus.FREE,    91, distance = 1.2),
        HHUser("theo",  "Theo Park",  "🐸", UserStatus.OFFLINE, 55, distance = 2.4),
        HHUser("amara", "Amara J.",   "🌻", UserStatus.FREE,    73, distance = 0.5),
        HHUser("finn",  "Finn Rook",  "🦉", UserStatus.BUSY,    68, distance = 1.9),
        HHUser("mila",  "Mila Cho",   "🍄", UserStatus.FREE,    80, distance = 0.7),
    )

    val places = listOf(
        HHPlace("p1","Otium Coffee Bar",  "Coffee · Plant-based","☕", HangVibe.COZY,        4.7,"$$",  0.3,96,3,"open · closes 9pm"),
        HHPlace("p2","The Lantern Room",  "Wine · Small plates", "🥂", HangVibe.ROMANTIC,    4.8,"$$$", 0.8,88,1,"open · closes 11pm"),
        HHPlace("p3","Sunset Bluff",      "Outdoor · Trail",     "🌄", HangVibe.ADVENTUROUS, 4.6,"$",   2.1,72,0,"always open"),
        HHPlace("p4","Moonshot Arcade",   "Games · Bar",         "🕹️",HangVibe.PARTY,       4.5,"$$",  1.4,84,5,"open · closes 2am"),
        HHPlace("p5","Folio Books & Tea", "Bookstore · Café",    "📚", HangVibe.CHILL,       4.9,"$",   0.6,91,2,"open · closes 8pm"),
    )

    val plans = listOf(
        HHPlan(
            id = "pl1", title = "Post-work coffee?", vibe = HangVibe.COZY,
            place = places[0], time = "Today · 6:30 PM", inHrs = 1.5, state = PlanState.VOTING,
            participantIDs = listOf("me","sara","kai","lena"),
            rsvp = mapOf("me" to "yes","sara" to "yes","kai" to "maybe","lena" to "yes"),
            options = listOf(places[0], places[4], places[1]),
            votes = mapOf("p1" to listOf("me","sara","lena"), "p5" to listOf("kai"), "p2" to emptyList()),
        ),
        HHPlan(
            id = "pl2", title = "Sunday hike + brunch", vibe = HangVibe.ADVENTUROUS,
            place = places[2], time = "Sun · 9:00 AM", inHrs = 62.0, state = PlanState.LOCKED,
            participantIDs = listOf("me","amara","finn","mila","theo"),
            rsvp = mapOf("me" to "yes","amara" to "yes","finn" to "maybe","mila" to "yes","theo" to "no"),
            cost = PlanCost(84, 21, 2, 2),
        ),
        HHPlan(
            id = "pl3", title = "Arcade night", vibe = HangVibe.PARTY,
            place = places[3], time = "Fri · 9:00 PM", inHrs = 28.0, state = PlanState.VOTING,
            participantIDs = listOf("me","kai","finn","mila","lena","sara"),
            rsvp = mapOf("me" to "yes","kai" to "yes","finn" to "yes","mila" to "yes","lena" to "maybe","sara" to "yes"),
            options = listOf(places[3]),
        ),
    )

    val chats = listOf(
        HHChat("c1","sara",  "on my way — grabbing a window seat ☕","2m",  2, isTyping = true),
        HHChat("c2","lena",  "omg YES let's do the arcade thing",     "12m", 0),
        HHChat("c3","kai",   "can't make coffee, next time?",          "1h",  0),
        HHChat("c4","amara", "I'll bring snacks for the hike",         "3h",  1),
        HHChat("c5","mila",  "reservation confirmed 🎉",               "yesterday", 0),
    )

    val chatThread = listOf(
        ChatMessage("sara", "you around after 6?",                   "5:42 PM"),
        ChatMessage("me",   "yesss — coffee?",                       "5:43 PM"),
        ChatMessage("sara", "cozy vibes only, please",               "5:43 PM"),
        ChatMessage("sara", "Otium or Folio?",                        "5:44 PM"),
        ChatMessage("me",   "Otium. meet you there in 20",           "5:45 PM"),
        ChatMessage("sara", "on my way — grabbing a window seat ☕", "5:48 PM"),
    )

    fun user(id: String) = users.firstOrNull { it.id == id }
    fun place(id: String) = places.firstOrNull { it.id == id }
}
