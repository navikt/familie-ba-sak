package no.nav.familie.ba.sak.behandling.autobrev

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class Autobrev6og18ÅrServiceTest {

    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    val behandlingService = mockk<BehandlingService>()

    val autobrev6og18ÅrService = Autobrev6og18ÅrService(personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
                                                        behandlingService = behandlingService)

    @Test
    fun `Verifiser at løpende fagsak med avsluttede behandlinger og barn på 18 oppretter en behandling for omregning`() {
        val behandling = lagBehandling().also {
            it.fagsak.status = FagsakStatus.LØPENDE
            it.status = BehandlingStatus.AVSLUTTET
        }

        val barn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(Alder.atten.år.toLong()))
        val søker = tilfeldigSøker()

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandlingId = behandling.id,
                                                                       søkerPersonIdent = søker.personIdent.ident,
                                                                       barnasIdenter = listOf(barn.personIdent.ident),
                                                                       barnFødselsdato = LocalDate.now()
                                                                               .minusYears(Alder.atten.år.toLong()))

        every { behandlingService.hentAktivForFagsak(behandling.fagsak.id) } returns behandling
        every { behandlingService.opprettBehandling(any()) } returns behandling
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id) } returns personopplysningGrunnlag

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsAlder(behandling.fagsak.id, Alder.atten.år)

        verify(exactly = 1) { behandlingService.opprettBehandling(any()) }
    }

    //TODO: Opprett test for hver kode branche.
}