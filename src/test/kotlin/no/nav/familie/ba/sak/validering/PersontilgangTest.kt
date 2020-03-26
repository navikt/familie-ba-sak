package no.nav.familie.ba.sak.validering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.RestFamilierelasjon
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.RestPersonInfo
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import no.nav.familie.ba.sak.integrasjoner.domene.FAMILIERELASJONSROLLE
import no.nav.familie.ba.sak.integrasjoner.domene.Tilgang
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import java.time.LocalDate

class PersontilgangTest {
    private lateinit var onBehalfClient: IntegrasjonOnBehalfClient
    private lateinit var persontilgang: Persontilgang

    @BeforeEach
    fun setUp() {
        onBehalfClient = mockk()
        persontilgang = Persontilgang(onBehalfClient)
    }

    @Test
    fun `isValid returnerer true hvis sjekkTilgangTilPersoner returnerer true for alle personer`() {
        every { onBehalfClient.sjekkTilgangTilPersoner(any<List<String>>()) }
                .returns(listOf(Tilgang(true),
                                Tilgang(true),
                                Tilgang(true)))
        val harTilgang = persontilgang.isValid(ResponseEntity.ok(Ressurs.success(restPersonInfo())), mockk())
        assertTrue(harTilgang)
    }

    @Test
    fun `isValid returnerer false hvis sjekkTilgangTilPersoner returnerer false for minst en person`() {
        every { onBehalfClient.sjekkTilgangTilPersoner(any<List<String>>()) }
                .returns(listOf(Tilgang(true),
                                Tilgang(false),
                                Tilgang(true)))
        val harTilgang = persontilgang.isValid(ResponseEntity.ok(Ressurs.success(restPersonInfo())), mockk())
        assertFalse(harTilgang)
    }

    @Test
    fun `isValid returnerer true hvis noe har feilet og det ikke ligger persondata på responsen`() {
        val harTilgang = persontilgang.isValid(ResponseEntity.ok(Ressurs.failure("Dette gikk dårlig")), mockk())
        assertTrue(harTilgang)
    }

    private fun restPersonInfo(): RestPersonInfo {
        val familierelasjoner = listOf(
                RestFamilierelasjon(personIdent = "123", navn = "", relasjonRolle = FAMILIERELASJONSROLLE.BARN, fødselsdato = null),
                RestFamilierelasjon(personIdent = "456", navn = "", relasjonRolle = FAMILIERELASJONSROLLE.BARN, fødselsdato = null)
        )
        return RestPersonInfo(personIdent = "789", navn = "", kjønn = null, fødselsdato = LocalDate.now(), familierelasjoner = familierelasjoner)
    }
}