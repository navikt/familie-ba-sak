package no.nav.familie.ba.sak.oppgave

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.FagsakRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.oppgave.OppgaveService.Behandlingstema
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
    lateinit var integrasjonTjeneste: IntegrasjonTjeneste

    @MockK
    lateinit var fagsakRepository: FagsakRepository

    @InjectMockKs
    lateinit var oppgaveService: OppgaveService

    @Test
    fun `Opprett oppgave skal bruke aktør fra fagsak, kalle arbeidsfordeling og opprettOppgave`() {
        every { fagsakRepository.finnFagsak(FAGSAK_ID) } returns Fagsak(personIdent = PersonIdent(ident = FNR),
                                                                        aktørId = AktørId(id = AKTØR_ID_FAGSAK))
        every { integrasjonTjeneste.hentBehandlendeEnhetForPersonident(FNR) } returns listOf(
                mockk {
                    every { enhetId } returns ENHETSNUMMER
                }
        )
        every { integrasjonTjeneste.hentAktørId(FNR) } returns AktørId(AKTØR_ID_INTEGRASJONER)
        val slot = slot<OpprettOppgave>()
        every { integrasjonTjeneste.opprettOppgave(capture(slot)) } returns OPPGAVE_ID

        oppgaveService.opprettOppgaveForNyBehandling(FAGSAK_ID)

        assertThat(slot.captured.enhetsnummer).isEqualTo(ENHETSNUMMER)
        assertThat(slot.captured.saksId).isEqualTo(FAGSAK_ID.toString())
        assertThat(slot.captured.ident).isEqualTo(OppgaveIdent(ident = AKTØR_ID_INTEGRASJONER, type = IdentType.Aktør))
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.ORDINÆR_BARNETRYGD.kode)
        assertThat(slot.captured.fristFerdigstillelse).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.BAR)
        assertThat(slot.captured.beskrivelse).contains("https://barnetrygd.nais.adeo.no/fagsak/${FAGSAK_ID}/behandle")
    }

    @Test
    fun `Opprett oppgave skal kalle oppretteOppgave selv om den ikke finner en enhetsnummer, men da med uten tildeltEnhetsnummer`() {
        every { fagsakRepository.finnFagsak(FAGSAK_ID) } returns Fagsak(personIdent = PersonIdent(ident = FNR))
        every { integrasjonTjeneste.hentAktørId(FNR) } returns AktørId(AKTØR_ID_INTEGRASJONER)
        every { integrasjonTjeneste.hentBehandlendeEnhetForPersonident(FNR) } returns emptyList()
        val slot = slot<OpprettOppgave>()
        every { integrasjonTjeneste.opprettOppgave(capture(slot)) } returns OPPGAVE_ID

        oppgaveService.opprettOppgaveForNyBehandling(FAGSAK_ID)

        assertThat(slot.captured.enhetsnummer).isNull()
        assertThat(slot.captured.saksId).isEqualTo(FAGSAK_ID.toString())
        assertThat(slot.captured.ident).isEqualTo(OppgaveIdent(ident = AKTØR_ID_INTEGRASJONER, type = IdentType.Aktør))
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.ORDINÆR_BARNETRYGD.kode)
        assertThat(slot.captured.fristFerdigstillelse).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.BAR)
        assertThat(slot.captured.beskrivelse).contains("https://barnetrygd.nais.adeo.no/fagsak/${FAGSAK_ID}/behandle")
    }

    @Test
    fun `Opprett oppgave skal kaste IllegalStateException hvis det ikke finnes en fagsak`() {
        every { fagsakRepository.finnFagsak(FAGSAK_ID) } returns null
        assertThatThrownBy { oppgaveService.opprettOppgaveForNyBehandling(FAGSAK_ID) }
                .hasMessageContaining("Kan ikke finne fagsak med id $FAGSAK_ID")
                .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `Opprett oppgave skal kaste IllegalStateException hvis det ikke finner en aktør`() {
        every { fagsakRepository.finnFagsak(FAGSAK_ID) } returns Fagsak(personIdent = PersonIdent(ident = FNR))
        every { integrasjonTjeneste.hentAktørId(FNR) } throws RuntimeException("aktør")
        assertThatThrownBy { oppgaveService.opprettOppgaveForNyBehandling(FAGSAK_ID) }
                .hasMessage("aktør")
                .isInstanceOf(java.lang.RuntimeException::class.java)
    }

    companion object {
        private const val FAGSAK_ID = 10000000L
        private const val OPPGAVE_ID = "42"
        private const val FNR = "fnr"
        private const val ENHETSNUMMER = "enhet"
        private const val AKTØR_ID_FAGSAK = "0123456789"
        private const val AKTØR_ID_INTEGRASJONER = "987654321"
    }
}