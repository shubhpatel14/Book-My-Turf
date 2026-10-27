package com.bookmyturf.app.data

import java.io.Serializable

data class Tournament(
    val id: String,
    val name: String,
    val sport: Sport,
    val city: String,
    val venue: String,
    val date: String,
    val registrationDeadline: String,
    val teamSize: Int,
    val maxTeams: Int,
    val entryFee: Int,
    val prizePool: Int,
    val description: String,
    val rules: String,
    val active: Boolean = true,
    val imageUrls: List<String> = emptyList()
) : Serializable

fun sampleTournaments(): List<Tournament> {
    val today = java.time.LocalDate.now()
    fun event(offset: Long) = today.plusDays(offset).toString()
    fun deadline(offset: Long) = today.plusDays(offset - 3).toString()
    return listOf(
        Tournament("bmt-football-cup", "Vadodara Football Cup", Sport.FOOTBALL, "Vadodara", "Strike Arena, Gotri", event(21), deadline(21), 6, 16, 1200, 25000, "Fast-paced six-a-side football under the lights.", "Six players per team. Arrive 30 minutes early."),
        Tournament("bmt-cricket-clash", "Box Cricket Clash", Sport.CRICKET, "Vadodara", "TurfX Sports Arena, Akota", event(28), deadline(28), 7, 20, 1500, 35000, "A full day of box cricket and knockout matches.", "Seven players per team. Bring a valid photo ID."),
        Tournament("bmt-badminton-open", "Badminton Doubles Open", Sport.BADMINTON, "Vadodara", "Urban Sports Club", event(35), deadline(35), 2, 32, 600, 12000, "Doubles brackets for players of every level.", "Two players per team. Standard doubles rules apply."),
        Tournament("bmt-basketball-jam", "3x3 Basketball Jam", Sport.BASKETBALL, "Vadodara", "GoalZone Arena", event(42), deadline(42), 3, 16, 900, 18000, "Streetball energy and a one-day 3x3 bracket.", "Three players per team. Games follow 3x3 rules.")
    )
}

fun validateTournament(tournament: Tournament) {
    require(tournament.name.isNotBlank() && tournament.city.isNotBlank() && tournament.venue.isNotBlank()) { "Enter a name, city and venue." }
    require(tournament.teamSize in 1..30 && tournament.maxTeams in 2..128) { "Check team size and maximum teams." }
    require(tournament.entryFee in 0..100000 && tournament.prizePool in 0..10000000) { "Check entry fee and prize pool." }
    require(tournament.imageUrls.size <= 4 && tournament.imageUrls.all {
        it.startsWith("http://", ignoreCase = true) ||
        it.startsWith("https://", ignoreCase = true) ||
        it.startsWith("data:", ignoreCase = true) ||
        it.startsWith("content:", ignoreCase = true) ||
        it.startsWith("file:", ignoreCase = true)
    }) { "Add up to four valid image URLs." }
    val eventDate = runCatching { java.time.LocalDate.parse(tournament.date) }.getOrNull() ?: error("Use YYYY-MM-DD for the event date.")
    val deadline = runCatching { java.time.LocalDate.parse(tournament.registrationDeadline) }.getOrNull() ?: error("Use YYYY-MM-DD for the registration deadline.")
    require(deadline <= eventDate) { "Registration must close by the event date." }
    require(!eventDate.isBefore(java.time.LocalDate.now())) { "Event date must be in the future." }
}

fun validateTournamentRegistration(tournament: Tournament, teamName: String, captainName: String, captainPhone: String) {
    require(tournament.active && !java.time.LocalDate.parse(tournament.registrationDeadline).isBefore(java.time.LocalDate.now())) { "Registration is closed." }
    require(teamName.trim().length in 2..80 && captainName.trim().length in 2..80) { "Enter a team name and captain name." }
    require(captainPhone.filter(Char::isDigit).length in 10..13) { "Enter a valid captain phone number." }
}

data class TournamentRegistration(
    val id: String,
    val tournamentId: String,
    val tournamentName: String,
    val sport: Sport,
    val teamName: String,
    val captainName: String,
    val captainPhone: String,
    val amount: Int,
    val transactionId: String,
    val paymentStatus: String = "DEMO_PAID",
    val status: String = "CONFIRMED",
    val userId: String = ""
) : Serializable
