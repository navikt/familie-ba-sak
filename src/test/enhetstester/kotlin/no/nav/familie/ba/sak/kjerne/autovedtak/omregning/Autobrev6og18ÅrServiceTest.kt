package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class Autobrev6og18ÅrServiceTest {

    private val autovedtakService = mockk<AutovedtakService>()
    private val autovedtakStegService = mockk<AutovedtakStegService>()
    private val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val fagsakService = mockk<FagsakService>(relaxed = true)
    private val infotrygdService = mockk<InfotrygdService>(relaxed = true)
    private val stegService = mockk<StegService>()
    private val vedtakService = mockk<VedtakService>(relaxed = true)
    private val taskRepository = mockk<TaskRepositoryWrapper>(relaxed = true)
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()

    private val autovedtakBrevService = AutovedtakBrevService(
        behandlingService = behandlingService,
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        fagsakService = fagsakService,
        autovedtakService = autovedtakService,
        vedtakService = vedtakService,
        infotrygdService = infotrygdService,
        vedtaksperiodeService = vedtaksperiodeService,
        taskRepository = taskRepository
    )

    private val autobrev6og18ÅrService = Autobrev6og18ÅrService(
        personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        autovedtakBrevService = autovedtakBrevService,
        autovedtakStegService = autovedtakStegService,
        andelerTilkjentYtelseOgEndreteUtbetalingerService = AndelerTilkjentYtelseOgEndreteUtbetalingerService(
            andelTilkjentYtelseRepository,
            mockk()
        )
    )

    @Test
    fun `Verifiser at løpende fagsak med avsluttede behandlinger og barn på 18 ikke oppretter en behandling for omregning`() {
        val (behandling, søker) = initMock(alder = 18)

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
        val (behandling, søker) = initMock(alder = 7)

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
        val (behandling, søker) = initMock(fagsakStatus = FagsakStatus.OPPRETTET, alder = 6)

        val autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(
            fagsakId = behandling.fagsak.id,
            alder = Alder.SEKS.år,
            årMåned = inneværendeMåned()
        )

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO)

        verify(exactly = 0) { stegService.håndterVilkårsvurdering(any()) }
    }

    @Test
    fun `Verifiser at behandling for omregning blir trigget for løpende fagsak med barn som fyller inneværende måned`() {
        val (behandling, søker) = initMock(alder = 6)

        val autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(
            fagsakId = behandling.fagsak.id,
            alder = Alder.SEKS.år,
            årMåned = inneværendeMåned()
        )

        every { stegService.håndterVilkårsvurdering(any()) } returns behandling
        every { stegService.håndterNyBehandling(any()) } returns behandling
        every { vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(any(), any()) } just runs
        every { taskRepository.save(any()) } returns Task(type = "test", payload = "")
        every { autovedtakStegService.kjørBehandlingOmregning(any(), any()) } returns ""

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO)

        verify(exactly = 1) {
            autovedtakStegService.kjørBehandlingOmregning(
                any(),
                any()
            )
        }
    }

    @Test
    fun `Verifiser at behandling for omregning ikke blir trigget for løpende fagsak med barn som fyller inneværende måned, hvis barnet ikke har løpende andel tilkjent ytelse`() {
        val (behandling, søker) = initMock(alder = 6)

        val autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(
            fagsakId = behandling.fagsak.id,
            alder = Alder.SEKS.år,
            årMåned = inneværendeMåned()
        )

        every { stegService.håndterVilkårsvurdering(any()) } returns behandling
        every { stegService.håndterNyBehandling(any()) } returns behandling

        val barn18årUtenAktivTilkjentYtelse =
            tilfeldigPerson(LocalDate.now().minusYears(Alder.SEKS.år.toLong()).minusMonths(1))
        val barn10årMedAktivTilkjentYtelse = tilfeldigPerson(LocalDate.now().minusYears(10))

        every {
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                behandling.id
            )
        } returns listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = YearMonth.now().minusMonths(1), // en gammel ytelse
                beløp = 1054,
                person = barn18årUtenAktivTilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = YearMonth.now().plusYears(4),
                beløp = 1054,
                person = barn10årMedAktivTilkjentYtelse // den aktive er på et annet barn
            )
        )
        every { vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(any(), any()) } just runs
        every { taskRepository.save(any()) } returns Task(type = "test", payload = "")
        every { autovedtakStegService.kjørBehandlingOmregning(any(), any()) } returns ""

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO)

        verify(exactly = 0) {
            autovedtakStegService.kjørBehandlingOmregning(
                any(),
                any()
            )
        }
    }

    @Test
    fun `Verifiser at vi ikke oppretter behandling hvis brev er sendt fra infotrygd`() {
        val (behandling, søker) = initMock(alder = 6)

        val autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(
            fagsakId = behandling.fagsak.id,
            alder = Alder.SEKS.år,
            årMåned = inneværendeMåned()
        )

        every { infotrygdService.harSendtbrev(any(), any()) } returns true
        every { stegService.håndterVilkårsvurdering(any()) } returns behandling
        every { stegService.håndterNyBehandling(any()) } returns behandling
        every { vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(any(), any()) } just runs
        every { taskRepository.save(any()) } returns Task(type = "test", payload = "")
        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO)

        verify(exactly = 0) {
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                any(),
                any(),
                any()
            )
        }
        verify(exactly = 0) {
            vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(
                any(),
                any()
            )
        }
        verify(exactly = 0) { autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(any()) }
        verify(exactly = 0) { taskRepository.save(any()) }
    }

    private fun initMock(
        behandlingStatus: BehandlingStatus = BehandlingStatus.AVSLUTTET,
        fagsakStatus: FagsakStatus = FagsakStatus.LØPENDE,
        alder: Long
    ): Pair<Behandling, Person> {
        val behandling = lagBehandling().also {
            it.fagsak.status = fagsakStatus
            it.status = behandlingStatus
        }

        val søker = tilfeldigSøker()
        val barn = tilfeldigPerson(LocalDate.now().minusYears(alder))

        every {
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                behandling.id
            )
        } returns listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = YearMonth.now().plusYears(5),
                beløp = 1054,
                person = barn
            )
        )

        every { infotrygdService.harSendtbrev(any(), any()) } returns false
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns behandling
        every { behandlingService.opprettBehandling(any()) } returns behandling
        every { behandlingHentOgPersisterService.hentBehandlinger(any()) } returns emptyList()
        every { behandlingService.harBehandlingsårsakAlleredeKjørt(any(), any(), any()) } returns false
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id) } returns lagTestPersonopplysningGrunnlag(
            behandling.id,
            søker,
            barn
        )
        return Pair(behandling, søker)
    }
}
