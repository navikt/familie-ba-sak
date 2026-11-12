package no.nav.familie.ba.sak.sikkerhet

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilgangControllerRollesjekkTest {
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val personidentService = mockk<PersonidentService>()
    private val personTilgangService = mockk<PersonTilgangService>()
    private val tilgangService = mockk<TilgangService>()
    private val tilgangController =
        TilgangController(
            personopplysningerService = personopplysningerService,
            personidentService = personidentService,
            personTilgangService = personTilgangService,
            tilgangService = tilgangService,
        )

    private val aktør = randomAktør()
    private val fødselsnummer = aktør.aktivFødselsnummer()

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
        every { personidentService.hentAktør(fødselsnummer) } returns aktør
        every { personopplysningerService.hentAdressebeskyttelseSomSystembruker(aktør) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT
        every { personTilgangService.sjekkTilgangTilPerson(fødselsnummer) } returns PersonTilgang.medTilgang(fødselsnummer)
    }

    @Test
    fun `skal kreve veilederrolle`() {
        // Act
        val respons = tilgangController.hentTilgangOgDiskresjonskode(TilgangRequestDTO(brukerIdent = fødselsnummer))

        // Assert
        verify(exactly = 1) { tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Sjekke tilgang til person") }
        assertThat(respons.body?.data?.saksbehandlerHarTilgang).isTrue()
    }

    @Test
    fun `skal ikke sjekke tilgang når kaller mangler veilederrolle`() {
        // Arrange
        every {
            tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Sjekke tilgang til person")
        } throws RolleTilgangskontrollFeil("Krever VEILEDER")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> { tilgangController.hentTilgangOgDiskresjonskode(TilgangRequestDTO(brukerIdent = fødselsnummer)) }

        verify(exactly = 0) { personopplysningerService.hentAdressebeskyttelseSomSystembruker(any()) }
    }
}
