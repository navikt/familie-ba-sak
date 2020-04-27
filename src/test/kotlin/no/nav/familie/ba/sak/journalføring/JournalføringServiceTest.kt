package no.nav.familie.ba.sak.journalføring

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.journalføring.domene.*
import no.nav.familie.ba.sak.oppgave.OppgaveService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

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
        every { oppgaveService.opprettOppgaveForNyBehandling(any()) } returns ""
        every { stegService.håndterNyBehandling(any()) } returns lagBehandling()

        val request = OppdaterJournalpostRequest(knyttTilFagsak = true,
                                                 bruker = Bruker(id = "12345678910"),
                                                 sak = Sak(fagsakId = fagsakId))
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
        every { oppgaveService.opprettOppgaveForNyBehandling(any()) } returns ""
        every { stegService.håndterNyBehandling(any()) } returns lagBehandling()

        val request = OppdaterJournalpostRequest(knyttTilFagsak = false,
            bruker = Bruker(id = "12345678910"))
        journalføringService.ferdigstill(request, journalpostId, "9999", "1")

        assertThat(slot.captured.sak?.fagsakId).isEqualTo(null)
        assertThat(slot.captured.sak?.fagsaksystem).isEqualTo(null)
        assertThat(slot.captured.sak?.sakstype).isEqualTo(Sakstype.GENERELL_SAK.type)

    }

}