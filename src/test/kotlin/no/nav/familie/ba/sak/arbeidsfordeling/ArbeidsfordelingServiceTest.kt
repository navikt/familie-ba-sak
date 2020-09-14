package no.nav.familie.ba.sak.arbeidsfordeling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.lagTestOppgaveDTO
import no.nav.familie.ba.sak.oppgave.domene.DbOppgave
import no.nav.familie.ba.sak.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class ArbeidsfordelingServiceTest {

    @MockK
    lateinit var behandlingService: BehandlingService

    @MockK
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @MockK
    lateinit var integrasjonClient: IntegrasjonClient

    @MockK
    lateinit var personopplysningerService: PersonopplysningerService

    @MockK
    lateinit var oppgaveRepository: OppgaveRepository

    @InjectMockKs
    lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @Test
    fun `hentBehandlendeEnhet skal kjøre uten feil`() {
        val fagsak = defaultFagsak

        every { integrasjonClient.hentBehandlendeEnhet(any()) } returns listOf()
        every { personopplysningerService.hentPersoninfoMedRelasjoner(any()) } returns PersonInfo(LocalDate.now())
        every { behandlingService.hentAktivForFagsak(any()) } returns Behandling(
                fagsak = fagsak,
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                opprinnelse = BehandlingOpprinnelse.MANUELL
        )
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) }
                .returns(PersonopplysningGrunnlag(behandlingId = 0))

        arbeidsfordelingService.hentBehandlendeEnhet(fagsak)
    }

    @Test
    fun `Skal sjekke at enhet blir valgt basert på oppgave og ikke arbeidsfordeling`() {
        val behandling = lagBehandling()

        every {
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(any(),
                                                                               any())
        } returns DbOppgave(behandling = behandling, erFerdigstilt = false, gsakId = "1234", type = Oppgavetype.BehandleSak)

        every { integrasjonClient.finnOppgaveMedId(any()) } returns lagTestOppgaveDTO(oppgaveId = 123L, tildeltEnhetsnr = "4820")
        every { integrasjonClient.hentBehandlendeEnhet(any()) } returns listOf(Arbeidsfordelingsenhet("9999",
                                                                                                      "Ukjent"))
        every { personopplysningerService.hentPersoninfoMedRelasjoner(any()) } returns PersonInfo(LocalDate.now())
        every { behandlingService.hentAktivForFagsak(any()) } returns behandling
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) }
                .returns(PersonopplysningGrunnlag(behandlingId = behandling.id))

        val enhet = arbeidsfordelingService.bestemBehandlendeEnhet(behandling)
        assertThat(enhet).isEqualTo("4820")
    }

    @Test
    fun `Skal sjekke at enhet blir valgt basert på arbeidsfordeling og ikke oppgave`() {
        val behandling = lagBehandling()

        every {
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(any(),
                                                                               any())
        } returns null

        every { integrasjonClient.hentBehandlendeEnhet(any()) } returns listOf(Arbeidsfordelingsenhet("9999",
                                                                                                      "Ukjent"))
        every { personopplysningerService.hentPersoninfoMedRelasjoner(any()) } returns PersonInfo(LocalDate.now())
        every { behandlingService.hentAktivForFagsak(any()) } returns behandling
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) }
                .returns(PersonopplysningGrunnlag(behandlingId = behandling.id))

        val enhet = arbeidsfordelingService.bestemBehandlendeEnhet(behandling)
        assertThat(enhet).isEqualTo("9999")
    }

    @Test
    fun `Skal kaste feil hvis enhet fra arbeidsfordeling og oppgave er null`() {
        val behandling = lagBehandling()

        every {
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(any(),
                                                                               any())
        } returns null

        every { integrasjonClient.hentBehandlendeEnhet(any()) } returns emptyList()
        every { personopplysningerService.hentPersoninfoMedRelasjoner(any()) } returns PersonInfo(LocalDate.now())
        every { behandlingService.hentAktivForFagsak(any()) } returns behandling
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) }
                .returns(PersonopplysningGrunnlag(behandlingId = behandling.id))

        assertThrows<Feil> {  arbeidsfordelingService.bestemBehandlendeEnhet(behandling) }
    }
}