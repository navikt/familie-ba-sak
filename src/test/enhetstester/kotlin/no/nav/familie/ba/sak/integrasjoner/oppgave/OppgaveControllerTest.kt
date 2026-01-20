package no.nav.familie.ba.sak.integrasjoner.oppgave

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.journalføring.InnkommendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.FinnOppgaveRequestDto
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
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

    @BeforeAll
    fun init() {
        every { tilgangService.verifiserHarTilgangTilHandling(any(), any()) } just runs
    }

    @Test
    fun `Tildeling av oppgave til saksbehandler skal returnere OK og sende med OppgaveId i respons`() {
        val oppgaveId = "1234"
        val saksbehandlerId = "Z999999"
        every { oppgaveService.fordelOppgave(any(), any()) } returns oppgaveId

        val respons = oppgaveController.fordelOppgave(oppgaveId.toLong(), saksbehandlerId)

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(oppgaveId, respons.body?.data)
    }

    @Test
    fun `Tilbakestilling av tildeling på oppgave skal returnere OK og sende med Oppgave i respons`() {
        val oppgave =
            Oppgave(
                id = 1234,
            )
        every { oppgaveService.tilbakestillFordelingPåOppgave(oppgave.id!!) } returns oppgave

        val respons = oppgaveController.tilbakestillFordelingPåOppgave(oppgave.id!!)

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(oppgave, respons.body?.data)
    }

    @Test
    fun `Tildeling av oppgave skal returnere feil ved feil fra integrasjonsklienten`() {
        val oppgaveId = "1234"
        val saksbehandlerId = "Z999998"
        every {
            oppgaveService.fordelOppgave(
                any(),
                any(),
            )
        } throws IntegrasjonException("Kall mot integrasjon feilet ved fordel oppgave")

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
        every {
            oppgaveService.hentOppgaver(any())
        } returns FinnOppgaveResponseDto(1, listOf(Oppgave(tema = Tema.BAR)))
        val response = oppgaveController.hentOppgaver(FinnOppgaveRequestDto())
        val oppgaverOgAntall = response.body?.data as FinnOppgaveResponseDto
        Assertions.assertEquals(1, oppgaverOgAntall.antallTreffTotalt)
        Assertions.assertEquals(Tema.BAR, oppgaverOgAntall.oppgaver.first().tema)
    }
}
