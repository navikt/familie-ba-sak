package no.nav.familie.ba.sak.journalføring

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.journalføring.domene.Sakstype
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.BrukerIdType
import no.nav.familie.kontrakter.felles.journalpost.Sak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

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
        every { oppgaveService.opprettOppgave(any(), any(), any()) } returns ""
        every { stegService.håndterNyBehandling(any()) } returns lagBehandling()

        val request = OppdaterJournalpostRequest(knyttTilFagsak = true,
                                                 bruker = Bruker(id = "12345678910", type = BrukerIdType.FNR),
                                                 sak = Sak(fagsakId = fagsakId,
                                                           arkivsaksnummer = null,
                                                           arkivsaksystem = "GSAK",
                                                           fagsaksystem = FAGSYSTEM,
                                                           sakstype = Sakstype.FAGSAK.name),
                                                 mottattDato = LocalDateTime.now())
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
        every { oppgaveService.opprettOppgave(any(), any(), any()) } returns ""
        every { stegService.håndterNyBehandling(any()) } returns lagBehandling()

        val request = OppdaterJournalpostRequest(knyttTilFagsak = false,
                                                 bruker = Bruker(id = "12345678910", type = BrukerIdType.FNR),
                                                 mottattDato = LocalDateTime.now())
        journalføringService.ferdigstill(request, journalpostId, "9999", "1")

        assertThat(slot.captured.sak?.fagsakId).isEqualTo(null)
        assertThat(slot.captured.sak?.fagsaksystem).isEqualTo(null)
        assertThat(slot.captured.sak?.sakstype).isEqualTo(Sakstype.GENERELL_SAK.type)

    }

}