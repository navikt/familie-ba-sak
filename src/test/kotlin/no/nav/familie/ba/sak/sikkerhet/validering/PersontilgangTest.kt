package no.nav.familie.ba.sak.sikkerhet.validering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.ekstern.restDomene.RestForelderBarnRelasjon
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonInfo
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import java.time.LocalDate

class PersontilgangTest {

    private lateinit var client: IntegrasjonClient
    private lateinit var persontilgang: Persontilgang

    @BeforeEach
    fun setUp() {
        client = mockk()
        persontilgang = Persontilgang(client)
    }

    @Test
    fun `isValid returnerer true hvis sjekkTilgangTilPersoner returnerer true for alle personer`() {
        every { client.sjekkTilgangTilPersoner(any<List<String>>()) }
            .returns(
                listOf(
                    Tilgang(true),
                    Tilgang(true),
                    Tilgang(true)
                )
            )
        val harTilgang = persontilgang.isValid(ResponseEntity.ok(Ressurs.success(restPersonInfo())), mockk())
        assertTrue(harTilgang)
    }

    @Test
    fun `isValid returnerer false hvis sjekkTilgangTilPersoner returnerer false for minst en person`() {
        every { client.sjekkTilgangTilPersoner(any<List<String>>()) }
            .returns(
                listOf(
                    Tilgang(true),
                    Tilgang(false),
                    Tilgang(true)
                )
            )
        val harTilgang = persontilgang.isValid(ResponseEntity.ok(Ressurs.success(restPersonInfo())), mockk())
        assertFalse(harTilgang)
    }

    @Test
    fun `isValid returnerer true hvis noe har feilet og det ikke ligger persondata på responsen`() {
        val harTilgang = persontilgang.isValid(ResponseEntity.ok(Ressurs.failure("Dette gikk dårlig")), mockk())
        assertTrue(harTilgang)
    }

    private fun restPersonInfo(): RestPersonInfo {
        val forelderBarnRelasjon = listOf(
            RestForelderBarnRelasjon(
                personIdent = "123",
                navn = "",
                relasjonRolle = FORELDERBARNRELASJONROLLE.BARN,
                fødselsdato = null
            ),
            RestForelderBarnRelasjon(
                personIdent = "456",
                navn = "",
                relasjonRolle = FORELDERBARNRELASJONROLLE.BARN,
                fødselsdato = null
            )
        )
        return RestPersonInfo(
            personIdent = "789",
            navn = "",
            kjønn = null,
            fødselsdato = LocalDate.now(),
            forelderBarnRelasjon = forelderBarnRelasjon
        )
    }
}
