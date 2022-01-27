package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class Autobrev6og18ÅrServiceTest {

    private val autovedtakService = mockk<AutovedtakService>(relaxed = true)
    private val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val infotrygdService = mockk<InfotrygdService>(relaxed = true)
    private val stegService = mockk<StegService>()
    private val vedtakService = mockk<VedtakService>(relaxed = true)
    private val taskRepository = mockk<TaskRepositoryWrapper>(relaxed = true)
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()

    private val autobrevService = AutobrevService(
        behandlingService = behandlingService,
        autovedtakService = autovedtakService,
        vedtakService = vedtakService,
        infotrygdService = infotrygdService,
        vedtaksperiodeService = vedtaksperiodeService,
        taskRepository = taskRepository,
    )

    private val autobrev6og18ÅrService = Autobrev6og18ÅrService(
        personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
        behandlingService = behandlingService,
        vedtakService = vedtakService,
        vedtaksperiodeService = vedtaksperiodeService,
        autobrevService = autobrevService
    )

    @Test
    fun `Verifiser at løpende fagsak med avsluttede behandlinger og barn på 18 ikke oppretter en behandling for omregning`() {
        val behandling = initMock(alder = 18)

        val autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(
            fagsakId = behandling.fagsak.id,
            alder = Alder.ATTEN.år,
            årMåned = inneværendeMåned()
        )

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO)

        verify(exactly = 0) { stegService.håndterVilkårsvurdering(any()) }
    }

    @Test
    fun `Verifiser at behandling for omregning ikke opprettes om barn med angitt ålder ikke finnes`() {
        val behandling = initMock(alder = 7)

        val autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(
            fagsakId = behandling.fagsak.id,
            alder = Alder.SEKS.år,
            årMåned = inneværendeMåned()
        )

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO)

        verify(exactly = 0) { stegService.håndterVilkårsvurdering(any()) }
    }

    @Test
    fun `Verifiser at behandling for omregning ikke opprettes om fagsak ikke er løpende`() {
        val behandling = initMock(fagsakStatus = FagsakStatus.OPPRETTET, alder = 6)

        val autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(
            fagsakId = behandling.fagsak.id,
            alder = Alder.SEKS.år,
            årMåned = inneværendeMåned()
        )

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO)

        verify(exactly = 0) { stegService.håndterVilkårsvurdering(any()) }
    }

    @Test
    fun `Verifiser at behandling for omregning kaster feil om behandling ikke er avsluttet`() {
        val behandling = initMock(BehandlingStatus.OPPRETTET, alder = 6)

        val autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(
            fagsakId = behandling.fagsak.id,
            alder = Alder.SEKS.år,
            årMåned = inneværendeMåned()
        )

        Assertions.assertThrows(IllegalStateException::class.java) {
            autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO)
        }
    }

    @Test
    fun `Verifiser at behandling for omregning blir opprettet og prosessert for løpende fagsak med barn som fyller inneværende måned`() {
        val behandling = initMock(alder = 6)

        val autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(
            fagsakId = behandling.fagsak.id,
            alder = Alder.SEKS.år,
            årMåned = inneværendeMåned()
        )

        every { stegService.håndterVilkårsvurdering(any()) } returns behandling
        every { stegService.håndterNyBehandling(any()) } returns behandling
        every { persongrunnlagService.hentSøker(any()) } returns tilfeldigSøker()
        every { vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(any(), any()) } just runs
        every { taskRepository.save(any()) } returns Task(type = "test", payload = "")
        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO)

        verify(exactly = 1) {
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                any(),
                any(),
                any()
            )
        }
        verify(exactly = 1) {
            vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(
                any(),
                any()
            )
        }
        verify(exactly = 1) { autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(any()) }
        verify(exactly = 1) { taskRepository.save(any()) }
    }

    private fun initMock(
        behandlingStatus: BehandlingStatus = BehandlingStatus.AVSLUTTET,
        fagsakStatus: FagsakStatus = FagsakStatus.LØPENDE,
        alder: Long
    ): Behandling {
        val behandling = lagBehandling().also {
            it.fagsak.status = fagsakStatus
            it.status = behandlingStatus
        }

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(alder = alder, behandling)

        every { behandlingService.hentAktivForFagsak(behandling.fagsak.id) } returns behandling
        every { behandlingService.opprettBehandling(any()) } returns behandling
        every { behandlingService.hentBehandlinger(any()) } returns emptyList()
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id) } returns personopplysningGrunnlag
        return behandling
    }

    private fun lagTestPersonopplysningGrunnlag(alder: Long, behandling: Behandling): PersonopplysningGrunnlag {
        val barn = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(alder))
        val søker = tilfeldigSøker()

        return lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
            barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
            barnFødselsdato = LocalDate.now()
                .minusYears(alder)
        )
    }
}
