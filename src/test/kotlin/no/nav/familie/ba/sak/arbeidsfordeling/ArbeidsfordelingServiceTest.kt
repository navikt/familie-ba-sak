package no.nav.familie.ba.sak.arbeidsfordeling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.oppgave.OppgaveService
import org.junit.jupiter.api.Test
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
    lateinit var oppgaveService: OppgaveService

    @InjectMockKs
    lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @Test
    fun `hentBehandlendeEnhet skal kjøre uten feil`() {
        val fagsak = defaultFagsak

        every { integrasjonClient.hentBehandlendeEnhet(any()) } returns listOf()
        every { integrasjonClient.hentPersoninfoFor(any()) } returns Personinfo(LocalDate.now())
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
}