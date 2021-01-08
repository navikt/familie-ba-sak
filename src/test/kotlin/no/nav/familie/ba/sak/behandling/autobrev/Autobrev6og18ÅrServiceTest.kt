package no.nav.familie.ba.sak.behandling.autobrev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.UtbetalingBegrunnelseRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class Autobrev6og18ÅrServiceTest {

    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    val persongrunnlagService= mockk<PersongrunnlagService>()
    val behandlingService = mockk<BehandlingService>()
    val stegService = mockk<StegService>()
    val vedtakService = mockk<VedtakService>()
    val taskRepository = mockk<TaskRepository>()
    val utbetalingBegrunnelseRepository = mockk<UtbetalingBegrunnelseRepository>(relaxed = true)

    val autobrev6og18ÅrService = Autobrev6og18ÅrService(personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
                                                        behandlingService = behandlingService,
                                                        stegService = stegService,
                                                        vedtakService = vedtakService,
                                                        taskRepository = taskRepository,
                                                        persongrunnlagService = persongrunnlagService,
                                                        utbetalingBegrunnelseRepository = utbetalingBegrunnelseRepository)

    @Test
    fun `Verifiser at løpende fagsak med avsluttede behandlinger og barn på 18 ikke oppretter en behandling for omregning`() {
        val behandling = lagBehandling().also {
            it.fagsak.status = FagsakStatus.LØPENDE
            it.status = BehandlingStatus.AVSLUTTET
        }

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(alder = 18, behandling)

        every { behandlingService.hentAktivForFagsak(behandling.fagsak.id) } returns behandling
        every { behandlingService.opprettBehandling(any()) } returns behandling
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id) } returns personopplysningGrunnlag


        val autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(fagsakId = behandling.fagsak.id,
                                                    alder = Alder.atten.år,
                                                    årMåned = inneværendeMåned()
        )

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsAlder(autobrev6og18ÅrDTO)

        verify(exactly = 0) { stegService.håndterVilkårsvurdering(any()) }
    }

    @Test
    fun `Verifiser at behandling for omregning ikke opprettes om barn med angitt ålder ikke finnes`() {
        val behandling = lagBehandling().also {
            it.fagsak.status = FagsakStatus.LØPENDE
            it.status = BehandlingStatus.AVSLUTTET
        }

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(alder = 7, behandling)

        every { behandlingService.hentAktivForFagsak(behandling.fagsak.id) } returns behandling
        every { behandlingService.opprettBehandling(any()) } returns behandling
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id) } returns personopplysningGrunnlag

        val autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(fagsakId = behandling.fagsak.id,
                                                    alder = Alder.seks.år,
                                                    årMåned = inneværendeMåned())

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsAlder(autobrev6og18ÅrDTO)

        verify(exactly = 0) { stegService.håndterVilkårsvurdering(any()) }
    }

    @Test
    fun `Verifiser at behandling for omregning ikke opprettes om fagsak ikke er løpende`() {
        val behandling = lagBehandling().also {
            it.fagsak.status = FagsakStatus.OPPRETTET
            it.status = BehandlingStatus.AVSLUTTET
        }

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(alder = 6, behandling)

        every { behandlingService.hentAktivForFagsak(behandling.fagsak.id) } returns behandling
        every { behandlingService.opprettBehandling(any()) } returns behandling
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id) } returns personopplysningGrunnlag

        val autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(fagsakId = behandling.fagsak.id,
                                                    alder = Alder.seks.år,
                                                    årMåned = inneværendeMåned())

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsAlder(autobrev6og18ÅrDTO)

        verify(exactly = 0) { stegService.håndterVilkårsvurdering(any()) }
    }

    @Test
    fun `Verifiser at behandling for omregning kaster fei om behandling ikke er avsluttet`() {
        val behandling = lagBehandling().also {
            it.fagsak.status = FagsakStatus.LØPENDE
            it.status = BehandlingStatus.OPPRETTET
        }

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(alder = 6, behandling)

        every { behandlingService.hentAktivForFagsak(behandling.fagsak.id) } returns behandling
        every { behandlingService.opprettBehandling(any()) } returns behandling
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id) } returns personopplysningGrunnlag

        val autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(fagsakId = behandling.fagsak.id,
                                                    alder = Alder.seks.år,
                                                    årMåned = inneværendeMåned())

        Assertions.assertThrows(IllegalStateException::class.java) {
            autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsAlder(autobrev6og18ÅrDTO)
        }

        verify(exactly = 0) { stegService.håndterVilkårsvurdering(any()) }
    }

    private fun lagTestPersonopplysningGrunnlag(alder: Long, behandling: Behandling): PersonopplysningGrunnlag {
        val barn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(alder))
        val søker = tilfeldigSøker()

        return lagTestPersonopplysningGrunnlag(behandlingId = behandling.id,
                                                                       søkerPersonIdent = søker.personIdent.ident,
                                                                       barnasIdenter = listOf(barn.personIdent.ident),
                                                                       barnFødselsdato = LocalDate.now()
                                                                               .minusYears(alder))
    }
}