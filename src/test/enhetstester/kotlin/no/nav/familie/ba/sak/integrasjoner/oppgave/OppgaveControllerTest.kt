package no.nav.familie.ba.sak.integrasjoner.oppgave

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.datagenerator.lagPersonInfo
import no.nav.familie.ba.sak.datagenerator.lagTestJournalpost
import no.nav.familie.ba.sak.datagenerator.lagTestOppgaveDTO
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.ekstern.restDomene.MinimalFagsakDto
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.journalføring.InnkommendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.FinnOppgaveRequestDto
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveControllerTest {
    private val oppgaveService = mockk<OppgaveService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val personidentService = mockk<PersonidentService>()
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val fagsakService = mockk<FagsakService>()
    private val innkommendeJournalføringService = mockk<InnkommendeJournalføringService>()
    private val tilgangService = mockk<TilgangService>()

    private val oppgaveController =
        OppgaveController(
            oppgaveService = oppgaveService,
            fagsakService = fagsakService,
            personidentService = personidentService,
            integrasjonKlient = integrasjonKlient,
            personopplysningerService = personopplysningerService,
            tilgangService = tilgangService,
            innkommendeJournalføringService = innkommendeJournalføringService,
        )

    @BeforeEach
    fun init() {
        clearMocks(oppgaveService, personopplysningerService, personidentService, integrasjonKlient, fagsakService, innkommendeJournalføringService, tilgangService)
        every { tilgangService.verifiserHarTilgangTilHandling(any(), any()) } just runs
    }

    @Test
    fun `Tildeling av oppgave til saksbehandler skal returnere OK og sende med OppgaveId i respons`() {
        // Arrange
        val oppgaveId = "1234"
        val saksbehandlerId = "Z999999"
        every { oppgaveService.fordelOppgave(any(), any()) } returns oppgaveId

        // Act
        val respons = oppgaveController.fordelOppgave(oppgaveId.toLong(), saksbehandlerId)

        // Assert
        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(oppgaveId, respons.body?.data)
    }

    @Test
    fun `Tilbakestilling av tildeling på oppgave skal returnere OK og sende med Oppgave i respons`() {
        // Arrange
        val oppgave =
            Oppgave(
                id = 1234,
            )
        every { oppgaveService.tilbakestillFordelingPåOppgave(oppgave.id!!) } returns oppgave

        // Act
        val respons = oppgaveController.tilbakestillFordelingPåOppgave(oppgave.id!!)

        // Assert
        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(oppgave, respons.body?.data)
    }

    @Test
    fun `Tildeling av oppgave skal returnere feil ved feil fra integrasjonsklienten`() {
        // Arrange
        val oppgaveId = "1234"
        val saksbehandlerId = "Z999998"
        every {
            oppgaveService.fordelOppgave(
                any(),
                any(),
            )
        } throws IntegrasjonException("Kall mot integrasjon feilet ved fordel oppgave")

        // Act & Assert
        val exception =
            assertThrows<IntegrasjonException> {
                oppgaveController.fordelOppgave(
                    oppgaveId.toLong(),
                    saksbehandlerId,
                )
            }

        Assertions.assertEquals("Kall mot integrasjon feilet ved fordel oppgave", exception.message)
    }

    @Test
    fun `hentOppgaver via OppgaveController skal fungere`() {
        // Arrange
        every {
            oppgaveService.hentOppgaver(any())
        } returns FinnOppgaveResponseDto(1, listOf(Oppgave(tema = Tema.BAR)))

        // Act
        val response = oppgaveController.hentOppgaver(FinnOppgaveRequestDto())

        // Assert
        val oppgaverOgAntall = response.body?.data as FinnOppgaveResponseDto
        Assertions.assertEquals(1, oppgaverOgAntall.antallTreffTotalt)
        Assertions.assertEquals(Tema.BAR, oppgaverOgAntall.oppgaver.first().tema)
    }

    @Test
    fun `skal kreve veilederrolle for å hente oppgaver`() {
        // Arrange
        every { oppgaveService.hentOppgaver(any()) } returns FinnOppgaveResponseDto(0, emptyList())

        // Act
        oppgaveController.hentOppgaver(FinnOppgaveRequestDto())

        // Assert
        verify(exactly = 1) { tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente oppgaver") }
    }

    @Test
    fun `skal gi tilgangsfeil og ikke hente oppgaver når kaller mangler veilederrolle`() {
        // Arrange
        every {
            tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente oppgaver")
        } throws RolleTilgangskontrollFeil("Krever VEILEDER")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> { oppgaveController.hentOppgaver(FinnOppgaveRequestDto()) }

        verify(exactly = 0) { oppgaveService.hentOppgaver(any()) }
    }

    @Test
    fun `skal kreve veilederrolle ved henting av data for manuell journalføring`() {
        // Arrange
        val aktør = randomAktør()
        val oppgave = lagTestOppgaveDTO(oppgaveId = 1L).copy(aktoerId = aktør.aktørId)
        every { oppgaveService.hentOppgave(1L) } returns oppgave
        every { personidentService.hentAktør(aktør.aktørId) } returns aktør
        every { integrasjonKlient.hentJournalpost(oppgave.journalpostId!!) } returns
            lagTestJournalpost(personIdent = aktør.aktivFødselsnummer(), journalpostId = oppgave.journalpostId!!, avsenderMottakerIdType = null, kanal = "NAV_NO")
        every { fagsakService.hentMinimalFagsakForPerson(aktør) } returns Ressurs.success(mockk<MinimalFagsakDto>())
        every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør) } returns
            lagPersonInfo(navn = "Navn Navnesen")

        // Act
        val respons = oppgaveController.hentDataForManuellJournalføring(1L)

        // Assert
        verify(exactly = 1) {
            tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente data for manuell journalføring")
        }
        val person = respons.body?.data?.person
        assertThat(person?.navn).isEqualTo("Navn Navnesen")
    }

    @Test
    fun `skal ikke hente data for manuell journalføring når kaller mangler veilederrolle`() {
        // Arrange
        every {
            tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente data for manuell journalføring")
        } throws RolleTilgangskontrollFeil("Krever VEILEDER")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> { oppgaveController.hentDataForManuellJournalføring(2L) }

        verify(exactly = 0) { oppgaveService.hentOppgave(any()) }
        verify(exactly = 0) { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(any(), any()) }
    }

    @Test
    fun `skal kreve forvalterrolle for å hente frister`() {
        // Arrange
        every { oppgaveService.hentFristerForÅpneUtvidetBarnetrygdBehandlinger() } returns "behandlingId;oppgaveId;frist"

        // Act
        val respons = oppgaveController.hentFristerForÅpneUtvidetBarnetrygdBehandlinger()

        // Assert
        verify(exactly = 1) {
            tilgangService.verifiserHarTilgangTilHandling(
                minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                handling = "Hente frister for åpne utvidet barnetrygd-behandlinger",
            )
        }
        assertThat(respons.body?.data).isEqualTo("behandlingId;oppgaveId;frist")
    }

    @Test
    fun `skal ikke hente frister når kaller ikke er forvalter`() {
        // Arrange
        every {
            tilgangService.verifiserHarTilgangTilHandling(
                minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                handling = "Hente frister for åpne utvidet barnetrygd-behandlinger",
            )
        } throws RolleTilgangskontrollFeil("Krever FORVALTER")

        // Act & Assert
        val exception =
            assertThrows<RolleTilgangskontrollFeil> {
                oppgaveController.hentFristerForÅpneUtvidetBarnetrygdBehandlinger()
            }

        assertThat(exception.message).isEqualTo("Krever FORVALTER")
        verify(exactly = 0) { oppgaveService.hentFristerForÅpneUtvidetBarnetrygdBehandlinger() }
    }
}
