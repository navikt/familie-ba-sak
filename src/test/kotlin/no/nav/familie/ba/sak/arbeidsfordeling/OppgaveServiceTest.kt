package no.nav.familie.ba.sak.arbeidsfordeling

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.arbeidsfordeling.OppgaveService.Behandlingstema
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.oppgave.IdentType
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdent
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgave
import no.nav.familie.kontrakter.felles.oppgave.Tema
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OppgaveServiceTest {

    @MockK
    lateinit var integrasjonClient: IntegrasjonClient

    @MockK
    lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockK
    lateinit var behandlingRepository: BehandlingRepository

    @InjectMockKs
    lateinit var oppgaveService: OppgaveService

    @Test
    fun `Opprett oppgave skal lage oppgave med enhetsnummer fra norg2`() {
        every { behandlingRepository.finnBehandling(BEHANDLING_ID) } returns lagTestBehandling()
        every { behandlingRepository.save(any<Behandling>()) } returns lagTestBehandling()
        every { arbeidsfordelingService.hentBehandlendeEnhet(any()) } returns listOf(
                mockk {
                    every { enhetId } returns ENHETSNUMMER
                }
        )
        every { integrasjonClient.hentAktørId(FNR) } returns AktørId(
                AKTØR_ID_INTEGRASJONER)
        val slot = slot<OpprettOppgave>()
        every { integrasjonClient.opprettOppgave(capture(slot)) } returns OPPGAVE_ID

        oppgaveService.opprettOppgaveForNyBehandling(BEHANDLING_ID)

        assertThat(slot.captured.enhetsnummer).isEqualTo(ENHETSNUMMER)
        assertThat(slot.captured.saksId).isEqualTo(FAGSAK_ID.toString())
        assertThat(slot.captured.ident).isEqualTo(OppgaveIdent(ident = AKTØR_ID_INTEGRASJONER, type = IdentType.Aktør))
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.ORDINÆR_BARNETRYGD.kode)
        assertThat(slot.captured.fristFerdigstillelse).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.BAR)
        assertThat(slot.captured.beskrivelse).contains("https://barnetrygd.nais.adeo.no/fagsak/$FAGSAK_ID")
    }

    @Test
    fun `Opprett oppgave skal kalle oppretteOppgave selv om den ikke finner en enhetsnummer, men da med uten tildeltEnhetsnummer`() {
        every { behandlingRepository.finnBehandling(BEHANDLING_ID) } returns lagTestBehandling()
        every { behandlingRepository.save(any<Behandling>()) } returns lagTestBehandling()
        every { integrasjonClient.hentAktørId(FNR) } returns AktørId(
                AKTØR_ID_INTEGRASJONER)
        every { arbeidsfordelingService.hentBehandlendeEnhet(any()) } returns emptyList()
        val slot = slot<OpprettOppgave>()
        every { integrasjonClient.opprettOppgave(capture(slot)) } returns OPPGAVE_ID

        oppgaveService.opprettOppgaveForNyBehandling(BEHANDLING_ID)

        assertThat(slot.captured.enhetsnummer).isNull()
        assertThat(slot.captured.saksId).isEqualTo(FAGSAK_ID.toString())
        assertThat(slot.captured.ident).isEqualTo(OppgaveIdent(ident = AKTØR_ID_INTEGRASJONER, type = IdentType.Aktør))
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.ORDINÆR_BARNETRYGD.kode)
        assertThat(slot.captured.fristFerdigstillelse).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.BAR)
        assertThat(slot.captured.beskrivelse).contains("https://barnetrygd.nais.adeo.no/fagsak/$FAGSAK_ID")
    }


    @Test
    fun `Opprett oppgave skal kaste Exception hvis det ikke finner en aktør`() {
        every { behandlingRepository.finnBehandling(BEHANDLING_ID) } returns lagTestBehandling()
        every { integrasjonClient.hentAktørId(FNR) } throws RuntimeException("aktør")
        assertThatThrownBy { oppgaveService.opprettOppgaveForNyBehandling(BEHANDLING_ID) }
                .hasMessage("aktør")
                .isInstanceOf(java.lang.RuntimeException::class.java)
    }

    @Test
    fun `Ferdigstill oppgave`() {
        every { behandlingRepository.finnBehandling(BEHANDLING_ID) } returns mockk {
            every { oppgaveId } returns OPPGAVE_ID
        }
        val slot = slot<Long>()
        every { integrasjonClient.ferdigstillOppgave(capture(slot)) } just runs

        oppgaveService.ferdigstillOppgave(BEHANDLING_ID)
        assertThat(slot.captured).isEqualTo(OPPGAVE_ID.toLong())
    }

    @Test
    fun `Ferdigstill oppgave feiler fordi den ikke finner oppgave på behandlingen`() {
        every { behandlingRepository.finnBehandling(BEHANDLING_ID) } returns mockk {
            every { oppgaveId } returns null
        }

        assertThatThrownBy { oppgaveService.ferdigstillOppgave(BEHANDLING_ID) }
                .hasMessage("Kan ikke finne oppgave for behandlingId $BEHANDLING_ID")
                .isInstanceOf(java.lang.IllegalStateException::class.java)
    }

    private fun lagTestBehandling(): Behandling {
        return Behandling(
                fagsak = Fagsak(
                        id = FAGSAK_ID,
                        personIdent = PersonIdent(ident = FNR),
                        aktørId = AktørId(id = AKTØR_ID_FAGSAK)
                ),
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR)
    }

    companion object {
        private const val FAGSAK_ID = 10000000L
        private const val BEHANDLING_ID = 20000000L
        private const val OPPGAVE_ID = "42"
        private const val FNR = "fnr"
        private const val ENHETSNUMMER = "enhet"
        private const val AKTØR_ID_FAGSAK = "0123456789"
        private const val AKTØR_ID_INTEGRASJONER = "987654321"
    }
}