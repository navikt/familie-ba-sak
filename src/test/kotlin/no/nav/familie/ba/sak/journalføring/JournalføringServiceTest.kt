package no.nav.familie.ba.sak.journalføring

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.behandling.restDomene.RestOppdaterJournalpost
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.lagTestJournalpost
import no.nav.familie.ba.sak.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.journalføring.domene.Sakstype
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class JournalføringServiceTest {

    @MockK
    lateinit var integrasjonClient: IntegrasjonClient

    @MockK
    lateinit var fagsakService: FagsakService

    @MockK
    lateinit var stegService: StegService

    @MockK
    lateinit var oppgaveService: OppgaveService

    @MockK
    lateinit var loggService: LoggService

    @InjectMockKs
    lateinit var journalføringService: JournalføringService

    @Test
    fun `ferdigstill skal oppdatere journalpost med fagsakId hvis knyttTilFagsak er true`() {
        val journalpostId = "1234567"
        val fagsakId = "1111111"

        val slot = slot<OppdaterJournalpostRequest>()
        every { integrasjonClient.oppdaterJournalpost(capture(slot), any()) } returns OppdaterJournalpostResponse(journalpostId)
        every { integrasjonClient.ferdigstillJournalpost(any(), any()) } just runs
        every { integrasjonClient.ferdigstillOppgave(any()) } just runs
        every { fagsakService.hentEllerOpprettFagsakForPersonIdent(any()) } returns Fagsak(id = fagsakId.toLong())
        every { integrasjonClient.hentJournalpost(any()) } returns Ressurs.Companion.success(lagTestJournalpost("1", "1234567"))
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) } returns ""
        every { stegService.håndterNyBehandling(any()) } returns lagBehandling()
        every { loggService.opprettMottattDokument(any(), any(), any()) } just runs

        val request =
                RestOppdaterJournalpost(knyttTilFagsak = true,
                                        avsender = NavnOgIdent(
                                                id = "09089121008",
                                                navn = "LUNKEN VEPS"),
                                        bruker = NavnOgIdent(
                                                id = "09089121008",
                                                navn = "LUNKEN VEPS"),
                                        dokumentTittel = "Søknad om ordinær barnetrygd",
                                        dokumentInfoId = "453883904",
                                        eksisterendeLogiskeVedlegg = listOf(
                                                LogiskVedlegg("318554361",
                                                              "Test")),
                                        logiskeVedlegg = listOf(
                                                LogiskVedlegg("318554361",
                                                              "Test")),
                                        datoMottatt = LocalDate.of(
                                                2020,
                                                5,
                                                4).atStartOfDay(),
                                        navIdent = "Z992691")

        journalføringService.ferdigstill(request, journalpostId, "9999", "1")

        assertThat(slot.captured.sak?.fagsakId).isEqualTo(fagsakId)
        assertThat(slot.captured.sak?.fagsaksystem).isEqualTo("BA")
        assertThat(slot.captured.sak?.sakstype).isEqualTo(Sakstype.FAGSAK.type)

    }

    @Test
    fun `ferdigstill skal oppdatere journalpost med GENERELL_SAKSTYPE hvis knyttTilFagsak er false`() {
        val journalpostId = "1234567"

        val slot = slot<OppdaterJournalpostRequest>()
        every { integrasjonClient.oppdaterJournalpost(capture(slot), any()) } returns OppdaterJournalpostResponse(journalpostId)
        every { integrasjonClient.ferdigstillJournalpost(any(), any()) } just runs
        every { integrasjonClient.ferdigstillOppgave(any()) } just runs
        every { integrasjonClient.hentJournalpost(any()) } returns Ressurs.Companion.success(lagTestJournalpost("1", "1234567"))
        every { oppgaveService.opprettOppgave(any(), any(), any()) } returns ""
        every { stegService.håndterNyBehandling(any()) } returns lagBehandling()

        val request =
                RestOppdaterJournalpost(knyttTilFagsak = false,
                                        avsender = NavnOgIdent(
                                                id = "12345678910",
                                                navn = "navn"),
                                        bruker = NavnOgIdent(
                                                id = "12345678910",
                                                navn = "navn"),
                                        dokumentTittel = "Søknad om ordinær barnetrygd",
                                        dokumentInfoId = "123",
                                        eksisterendeLogiskeVedlegg = listOf(
                                                LogiskVedlegg("1",
                                                              "tittel")),
                                        logiskeVedlegg = listOf(
                                                LogiskVedlegg("1",
                                                              "tittel")),
                                        datoMottatt = LocalDate.now()
                                                .atStartOfDay(),
                                        navIdent = "Z111111")

        journalføringService.ferdigstill(request, journalpostId, "9999", "1")

        assertThat(slot.captured.sak?.fagsakId).isEqualTo(null)
        assertThat(slot.captured.sak?.fagsaksystem).isEqualTo(null)
        assertThat(slot.captured.sak?.sakstype).isEqualTo(Sakstype.GENERELL_SAK.type)

    }

}