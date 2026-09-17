package com.bookmyturf.app.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TournamentRepositoryTest {
    @Test fun captainRegistrationsRespectTournamentCapacity() {
        runBlocking {
        val repository = DemoBookMyTurfRepository()
        val tournament = sampleTournaments().first().copy(maxTeams = 2)
        repository.upsertTournament(tournament)

        val first = repository.registerTournament(tournament, "The Strikers", "Alex Patel", "9876543210", "BMT-DEMO-12345678")
        repository.registerTournament(tournament, "The Runners", "Sam Shah", "9876543211", "BMT-DEMO-87654321")

        assertEquals("The Strikers", first.teamName)
        assertEquals(2, repository.tournamentRegistrations.first().size)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.registerTournament(tournament, "Third Team", "Dev Shah", "9876543212", "BMT-DEMO-ABCDEF12") }
        }
        }
    }
}
