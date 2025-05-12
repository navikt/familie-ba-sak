﻿package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import no.nav.familie.ba.sak.TestClockProvider.Companion.lagClockProviderMedFastTidspunkt
import no.nav.familie.ba.sak.common.MockedDateProvider
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockBehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockEcbService
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockTilbakekrevingsvedtakMotregningRepository
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockUnleashNextMedContextService
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockUnleashService
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockVurderingsstrategiForValutakurserRepository
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.BehandlingsinformasjonUtleder
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.EndretMigreringsdatoUtleder
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.KlassifiseringKorrigerer
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.OppdaterTilkjentYtelseService
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.UtbetalingsoppdragGenerator
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.internal.TestVerktøyService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.MånedligValutajusteringService
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.AutomatiskBeslutningService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.EksternBehandlingRelasjonService
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatSteg
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseGenerator
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.PatchetAndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.TilpassDifferanseberegningEtterTilkjentYtelseService
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.TilpassDifferanseberegningEtterUtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.TilpassDifferanseberegningEtterValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.TilpassDifferanseberegningSøkersYtelserService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilbakestillBehandlingFraKompetanseEndringService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilbakestillBehandlingFraUtenlandskPeriodebeløpEndringService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilbakestillBehandlingFraValutakursEndringService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassKompetanserTilRegelverkService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassUtenlandskePeriodebeløpTilKompetanserService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassValutakurserTilUtenlandskePeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.utbetaling.UtbetalingTidslinjeService
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.AutomatiskOppdaterValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad.OvergangsstønadService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.småbarnstillegg.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.steg.BeslutteVedtak
import no.nav.familie.ba.sak.kjerne.steg.FerdigstillBehandling
import no.nav.familie.ba.sak.kjerne.steg.IverksettMotOppdrag
import no.nav.familie.ba.sak.kjerne.steg.RegistrerPersongrunnlag
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdrag
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingTilBehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingTilSimuleringService
import no.nav.familie.ba.sak.kjerne.steg.VilkårsvurderingSteg
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.EøsSkjemaerForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.PersonopplysningGrunnlagForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregningBrevService
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregningService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("CucumberMock")

class CucumberMock(
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
    nyBehandlingId: Long,
    forrigeBehandling: Behandling? = dataFraCucumber.behandlingTilForrigeBehandling[nyBehandlingId]?.let { dataFraCucumber.behandlinger[it] },
    efSakRestClientMock: EfSakRestClient = mockEfSakRestClient(),
    ecbService: ECBService = mockEcbService(dataFraCucumber),
    scope: CoroutineScope? = null,
) {
    val clockProvider = lagClockProviderMedFastTidspunkt(dataFraCucumber.dagensDato)
    val mockedDateProvider = MockedDateProvider(dataFraCucumber.dagensDato)
    val persongrunnlagService = mockPersongrunnlagService(dataFraCucumber)
    val fagsakService = mockFagsakService(dataFraCucumber)
    val fagsakRepository = mockFagsakRepository(dataFraCucumber)
    val oppgaveService = mockOppgaveService()
    val personopplysningerService = mockPersonopplysningerService(dataFraCucumber)
    val tilgangService = mockTilgangService()
    val vilkårService = mockVilkårService(dataFraCucumber)
    val tilbakestillBehandlingService = mockTilbakestillBehandlingService()
    val personopplysningGrunnlagRepository = mockPersonopplysningGrunnlagRepository(dataFraCucumber.persongrunnlag)
    val personidentService = mockPersonidentService(dataFraCucumber)
    val tilkjentYtelseRepository = mockTilkjentYtelseRepository(dataFraCucumber)
    val vilkårsvurderingRepository = mockVilkårsvurderingRepository(dataFraCucumber)
    val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockAndelerTilkjentYtelseOgEndreteUtbetalingerService(dataFraCucumber)
    val andelTilkjentYtelseRepository = mockAndelTilkjentYtelseRepository(dataFraCucumber)
    val vilkårsvurderingService = VilkårsvurderingService(vilkårsvurderingRepository, sanityService = mockk())
    val vilkårsvurderingTidslinjeService = mockVilkårsvurderingTidslinjeService(vilkårsvurderingRepository, vilkårsvurderingService, persongrunnlagService)
    val loggService = mockLoggService()
    val behandlingHentOgPersisterService = mockBehandlingHentOgPersisterService(forrigeBehandling = forrigeBehandling, dataFraCucumber = dataFraCucumber, idForNyBehandling = nyBehandlingId)
    val periodeOvergangsstønadGrunnlagRepository = mockPeriodeOvergangsstønadGrunnlagRepository(dataFraCucumber)
    val søknadGrunnlagRepository = mockSøknadGrunnlagRepository(dataFraCucumber)
    val endretUtbetalingAndelHentOgPersisterService = mockEndretUtbetalingAndelHentOgPersisterService(dataFraCucumber)
    val vedtakRepository = mockVedtakRepository(dataFraCucumber)
    val dokumentGenereringService = mockDokumentGenereringService()
    val vedtaksperiodeHentOgPersisterService = mockVedtaksperiodeHentOgPersisterService(dataFraCucumber)
    val kompetanseRepository = mockKompetanseRepository(dataFraCucumber)
    val valutakursRepository = mockValutakursRepository(dataFraCucumber)
    val utenlandskPeriodebeløpRepository = mockUtenlandskPeriodebeløpRepository(dataFraCucumber)
    val tilbakekrevingsvedtakMotregningRepository = mockTilbakekrevingsvedtakMotregningRepository(dataFraCucumber)
    val endretUtbetalingAndelRepository = mockEndretUtbetalingAndelRepository(dataFraCucumber)
    val simuleringService = mockSimuleringService()
    val startSatsendring = mockStartSatsendring()
    val totrinnskontrollRepository = mockTotrinnskontrollRepository(dataFraCucumber)
    val taskService = mockTaskService()
    val saksstatistikkEventPublisher = mockSaksstatistikkEventPublisher()
    val arbeidsfordelingService = mockArbeidsfordelingService()
    val behandlingMetrikker = mockBehandlingMetrikker()
    val tilbakekrevingService = mockTilbakekrevingService()
    val taskRepository = MockTasker().mockTaskRepositoryWrapper(this, scope)
    val unleashNextMedContextService = mockUnleashNextMedContextService()
    val unleashService = mockUnleashService()
    val mockPåVentService = mockk<SettPåVentService>()
    val vurderingsstrategiForValutakurserRepository = mockVurderingsstrategiForValutakurserRepository()
    val brevmottakerService = mockk<BrevmottakerService>()
    val behandlingMigreringsinfoRepository = mockBehandlingMigreringsinfoRepository()
    val patchetAndelTilkjentYtelseRepository = mockk<PatchetAndelTilkjentYtelseRepository>()
    val eksternBehandlingRelasjonService = mockk<EksternBehandlingRelasjonService>()

    init {
        dataFraCucumber.toggles.forEach { (behandlingId, togglesForBehandling) ->
            togglesForBehandling.forEach { (toggleId, isEnabled) ->
                val featureToggle = FeatureToggle.entries.find { it.navn == toggleId } ?: throw IllegalStateException("$toggleId does not exist")
                every { unleashNextMedContextService.isEnabled(featureToggle, behandlingId) } returns isEnabled
            }
        }
    }

    val behandlingstemaService =
        BehandlingstemaService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            loggService = loggService,
            oppgaveService = oppgaveService,
            vilkårsvurderingTidslinjeService = vilkårsvurderingTidslinjeService,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            clockProvider = clockProvider,
        )

    val overgangsstønadService =
        OvergangsstønadService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            efSakRestClient = efSakRestClientMock,
            periodeOvergangsstønadGrunnlagRepository = periodeOvergangsstønadGrunnlagRepository,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            persongrunnlagService = persongrunnlagService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            localDateProvider = mockedDateProvider,
        )

    val tilpassDifferanseberegningSøkersYtelserService =
        TilpassDifferanseberegningSøkersYtelserService(
            persongrunnlagService = persongrunnlagService,
            kompetanseRepository = kompetanseRepository,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
        )

    val tilpassDifferanseberegningEtterTilkjentYtelseService =
        TilpassDifferanseberegningEtterTilkjentYtelseService(
            valutakursRepository = valutakursRepository,
            utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            barnasDifferanseberegningEndretAbonnenter = listOf(tilpassDifferanseberegningSøkersYtelserService),
        )

    val beregningService =
        BeregningService(
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            behandlingRepository = mockk(),
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            tilkjentYtelseEndretAbonnenter = listOf(tilpassDifferanseberegningEtterTilkjentYtelseService),
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            tilkjentYtelseGenerator = TilkjentYtelseGenerator(overgangsstønadService, vilkårsvurderingService),
        )

    val utbetalingTidslinjeService = UtbetalingTidslinjeService(beregningService)

    val vedtakService =
        VedtakService(
            vedtakRepository = vedtakRepository,
            dokumentGenereringService = dokumentGenereringService,
        )

    val søknadGrunnlagService =
        SøknadGrunnlagService(
            søknadGrunnlagRepository = søknadGrunnlagRepository,
            personidentService = personidentService,
            persongrunnlagService = persongrunnlagService,
        )

    val testVerktøyService =
        TestVerktøyService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vilkårService = vilkårService,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            endretUtbetalingRepository = endretUtbetalingAndelRepository,
            vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
            vedtakRepository = vedtakRepository,
            kompetanseRepository = kompetanseRepository,
            utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository,
            valutakursRepository = valutakursRepository,
            søknadGrunnlagService = søknadGrunnlagService,
        )

    val vedtaksperiodeService =
        VedtaksperiodeService(
            personidentService = personidentService,
            persongrunnlagService = persongrunnlagService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
            vedtakRepository = vedtakRepository,
            sanityService = mockk(),
            søknadGrunnlagService = søknadGrunnlagService,
            endretUtbetalingAndelRepository = endretUtbetalingAndelRepository,
            kompetanseRepository = kompetanseRepository,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            feilutbetaltValutaRepository = mockk(),
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vilkårsvurderingService = vilkårsvurderingService,
            overgangsstønadService = overgangsstønadService,
            refusjonEøsRepository = mockk(),
            integrasjonClient = mockk(),
            valutakursRepository = valutakursRepository,
            utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository,
        )

    val behandlingService =
        BehandlingService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingstemaService = behandlingstemaService,
            behandlingSøknadsinfoService = mockk<BehandlingSøknadsinfoService>(),
            behandlingMigreringsinfoRepository = mockBehandlingMigreringsinfoRepository(),
            behandlingMetrikker = behandlingMetrikker,
            saksstatistikkEventPublisher = saksstatistikkEventPublisher,
            fagsakRepository = fagsakRepository,
            vedtakRepository = vedtakRepository,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            loggService = loggService,
            arbeidsfordelingService = arbeidsfordelingService,
            infotrygdService = mockk<InfotrygdService>(),
            vedtaksperiodeService = vedtaksperiodeService,
            taskRepository = taskRepository,
            vilkårsvurderingService = vilkårsvurderingService,
            unleashService = unleashNextMedContextService,
            eksternBehandlingRelasjonService = eksternBehandlingRelasjonService,
        )

    val tilbakestillBehandlingTilBehandlingsresultatService =
        TilbakestillBehandlingTilBehandlingsresultatService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingService = behandlingService,
            vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
            vedtakRepository = vedtakRepository,
            tilbakekrevingService = tilbakekrevingService,
        )

    val tilpassDifferanseberegningEtterValutakursService = TilpassDifferanseberegningEtterValutakursService(utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository, tilkjentYtelseRepository = tilkjentYtelseRepository, barnasDifferanseberegningEndretAbonnenter = listOf(tilpassDifferanseberegningSøkersYtelserService))
    val tilbakestillBehandlingFraValutakursEndringService =
        TilbakestillBehandlingFraValutakursEndringService(
            tilbakestillBehandlingTilBehandlingsresultatService = tilbakestillBehandlingTilBehandlingsresultatService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        )

    val valutakursAbonnenter = listOf(tilpassDifferanseberegningEtterValutakursService, tilbakestillBehandlingFraValutakursEndringService)

    val tilpassValutakurserTilUtenlandskePeriodebeløpService = TilpassValutakurserTilUtenlandskePeriodebeløpService(valutakursRepository = valutakursRepository, utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository, endringsabonnenter = valutakursAbonnenter, clockProvider = clockProvider)

    val tilbakestillBehandlingFraUtenlandskPeriodebeløpEndringService = TilbakestillBehandlingFraUtenlandskPeriodebeløpEndringService(tilbakestillBehandlingTilBehandlingsresultatService = tilbakestillBehandlingTilBehandlingsresultatService)

    val tilbakestillBehandlingTilSimuleringService = TilbakestillBehandlingTilSimuleringService(behandlingHentOgPersisterService, behandlingService)

    val valutakursService = ValutakursService(valutakursRepository = valutakursRepository, endringsabonnenter = valutakursAbonnenter)

    val automatiskOppdaterValutakursService =
        AutomatiskOppdaterValutakursService(
            valutakursService = valutakursService,
            vedtaksperiodeService = vedtaksperiodeService,
            localDateProvider = mockedDateProvider,
            ecbService = ecbService,
            utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            tilpassValutakurserTilUtenlandskePeriodebeløpService = tilpassValutakurserTilUtenlandskePeriodebeløpService,
            simuleringService = simuleringService,
            vurderingsstrategiForValutakurserRepository = vurderingsstrategiForValutakurserRepository,
            unleashNextMedContextService = unleashNextMedContextService,
            tilpassDifferanseberegningEtterValutakursService = tilpassDifferanseberegningEtterValutakursService,
        )

    val tilpassDifferanseberegningEtterUtenlandskPeriodebeløpService =
        TilpassDifferanseberegningEtterUtenlandskPeriodebeløpService(
            valutakursRepository = valutakursRepository,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            barnasDifferanseberegningEndretAbonnenter = listOf(tilpassDifferanseberegningSøkersYtelserService),
            automatiskOppdaterValutakursService = automatiskOppdaterValutakursService,
        )

    val utenlandskPeriodebeløpEndretAbonnenter =
        listOf(
            tilpassDifferanseberegningEtterUtenlandskPeriodebeløpService,
            tilbakestillBehandlingFraUtenlandskPeriodebeløpEndringService,
            tilpassValutakurserTilUtenlandskePeriodebeløpService,
        )

    val tilbakekrevingsvedtakMotregningService =
        TilbakekrevingsvedtakMotregningService(
            tilbakekrevingsvedtakMotregningRepository = tilbakekrevingsvedtakMotregningRepository,
            loggService = loggService,
            behandlingService = behandlingHentOgPersisterService,
            tilbakestillBehandlingTilSimuleringService = tilbakestillBehandlingTilSimuleringService,
        )

    val tilbakekrevingsvedtakMotregningBrevService =
        TilbakekrevingsvedtakMotregningBrevService(
            tilbakekrevingsvedtakMotregningRepository = tilbakekrevingsvedtakMotregningRepository,
            dokumentGenereringService = dokumentGenereringService,
        )

    val utenlandskPeriodebeløpService =
        UtenlandskPeriodebeløpService(
            utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository,
            endringsabonnenter = utenlandskPeriodebeløpEndretAbonnenter,
        )

    val tilbakestillBehandlingFraKompetanseEndringService = TilbakestillBehandlingFraKompetanseEndringService(tilbakestillBehandlingTilBehandlingsresultatService)

    val tilpassUtenlandskePeriodebeløpTilKompetanserService =
        TilpassUtenlandskePeriodebeløpTilKompetanserService(
            utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository,
            endringsabonnenter = utenlandskPeriodebeløpEndretAbonnenter,
            kompetanseRepository = kompetanseRepository,
            clockProvider = clockProvider,
        )

    val endringsabonnenterForKompetanse = listOf(tilpassUtenlandskePeriodebeløpTilKompetanserService, tilbakestillBehandlingFraKompetanseEndringService)

    val tilpassKompetanserTilRegelverkService =
        TilpassKompetanserTilRegelverkService(
            vilkårsvurderingTidslinjeService = vilkårsvurderingTidslinjeService,
            utbetalingTidslinjeService = utbetalingTidslinjeService,
            endretUtbetalingAndelHentOgPersisterService = endretUtbetalingAndelHentOgPersisterService,
            kompetanseRepository = kompetanseRepository,
            endringsabonnenter = endringsabonnenterForKompetanse,
            clockProvider = clockProvider,
        )

    val kompetanseService = KompetanseService(kompetanseRepository, endringsabonnenter = endringsabonnenterForKompetanse)

    val eøsSkjemaerForNyBehandlingService = EøsSkjemaerForNyBehandlingService(kompetanseService = kompetanseService, utenlandskPeriodebeløpService = utenlandskPeriodebeløpService, valutakursService = valutakursService)

    val behandlingsresultatService =
        BehandlingsresultatService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            søknadGrunnlagService = søknadGrunnlagService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            endretUtbetalingAndelHentOgPersisterService = endretUtbetalingAndelHentOgPersisterService,
            kompetanseService = kompetanseService,
            localDateProvider = mockedDateProvider,
            utenlandskPeriodebeløpService = utenlandskPeriodebeløpService,
        )

    val småbarnstilleggService = SmåbarnstilleggService(beregningService)

    val behandlingsresultatSteg =
        BehandlingsresultatSteg(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingService = behandlingService,
            simuleringService = simuleringService,
            vedtakService = vedtakService,
            vedtaksperiodeService = vedtaksperiodeService,
            behandlingsresultatService = behandlingsresultatService,
            vilkårService = vilkårService,
            persongrunnlagService = persongrunnlagService,
            beregningService = beregningService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository,
            valutakursRepository = valutakursRepository,
            localDateProvider = mockedDateProvider,
            kompetanseRepository = kompetanseRepository,
            småbarnstilleggService = småbarnstilleggService,
            tilbakestillBehandlingService = tilbakestillBehandlingService,
        )

    val saksbehandlerContext = SaksbehandlerContext("", mockk(), mockUnleashNextMedContextService())
    val totrinnskontrollService = TotrinnskontrollService(behandlingService = behandlingService, totrinnskontrollRepository = totrinnskontrollRepository, saksbehandlerContext = saksbehandlerContext)

    val tilkjentYtelseValideringService =
        TilkjentYtelseValideringService(
            beregningService = beregningService,
            totrinnskontrollService = totrinnskontrollService,
            persongrunnlagService = persongrunnlagService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        )

    val utbetalingsoppdragGenerator =
        UtbetalingsoppdragGenerator(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            unleashNextMedContextService = unleashNextMedContextService,
            klassifiseringKorrigerer =
                KlassifiseringKorrigerer(
                    tilkjentYtelseRepository,
                ),
            behandlingsinformasjonUtleder =
                BehandlingsinformasjonUtleder(
                    EndretMigreringsdatoUtleder(
                        behandlingHentOgPersisterService,
                        behandlingMigreringsinfoRepository,
                        tilkjentYtelseRepository,
                    ),
                    clockProvider,
                ),
            utbetalingsgenerator = Utbetalingsgenerator(),
        )

    val oppdaterTilkjentYtelseService =
        OppdaterTilkjentYtelseService(
            endretUtbetalingAndelHentOgPersisterService,
            tilkjentYtelseRepository,
            patchetAndelTilkjentYtelseRepository,
            clockProvider,
        )

    val økonomiService =
        ØkonomiService(
            økonomiKlient = mockØkonomiKlient(),
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
            utbetalingsoppdragGenerator = utbetalingsoppdragGenerator,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            oppdaterTilkjentYtelseService = oppdaterTilkjentYtelseService,
        )

    val håndterIverksettMotØkonomiSteg =
        IverksettMotOppdrag(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vedtakService = vedtakService,
            økonomiService = økonomiService,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
            totrinnskontrollService = totrinnskontrollService,
            taskRepository = taskRepository,
        )

    val statusFraOppdrag =
        StatusFraOppdrag(
            økonomiService = økonomiService,
            taskRepository = taskRepository,
        )

    val settPåVentService =
        SettPåVentService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            saksstatistikkEventPublisher = saksstatistikkEventPublisher,
            settPåVentRepository = mockSettPåVentRepository(),
            loggService = loggService,
            oppgaveService = oppgaveService,
            unleashService = unleashNextMedContextService,
            tilbakekrevingsvedtakMotregningService = mockk(relaxed = true),
        )

    val snikeIKøenService =
        spyk(
            SnikeIKøenService(
                behandlingHentOgPersisterService = behandlingHentOgPersisterService,
                påVentService = settPåVentService,
                loggService = loggService,
                tilbakestillBehandlingService = tilbakestillBehandlingService,
            ),
        )

    val personopplysningGrunnlagForNyBehandlingService =
        PersonopplysningGrunnlagForNyBehandlingService(
            personidentService = personidentService,
            beregningService = beregningService,
            persongrunnlagService = persongrunnlagService,
        )

    val endretUtbetalingAndelService =
        EndretUtbetalingAndelService(
            endretUtbetalingAndelRepository = endretUtbetalingAndelRepository,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            beregningService = beregningService,
            persongrunnlagService = persongrunnlagService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            vilkårsvurderingService = vilkårsvurderingService,
            endretUtbetalingAndelOppdatertAbonnementer = emptyList(),
            endretUtbetalingAndelHentOgPersisterService = endretUtbetalingAndelHentOgPersisterService,
        )

    val vilkårsvurderingForNyBehandlingService =
        VilkårsvurderingForNyBehandlingService(
            vilkårsvurderingService = vilkårsvurderingService,
            behandlingService = behandlingService,
            persongrunnlagService = persongrunnlagService,
            behandlingstemaService = behandlingstemaService,
            endretUtbetalingAndelService = endretUtbetalingAndelService,
            vilkårsvurderingMetrics = mockk(),
            andelerTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )

    val registrerPersongrunnlag =
        RegistrerPersongrunnlag(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vilkårsvurderingForNyBehandlingService = vilkårsvurderingForNyBehandlingService,
            personopplysningGrunnlagForNyBehandlingService = personopplysningGrunnlagForNyBehandlingService,
            eøsSkjemaerForNyBehandlingService = eøsSkjemaerForNyBehandlingService,
        )

    val månedligValutajusteringService = MånedligValutajusteringService(ecbService = ecbService, valutakursService = valutakursService)

    val vilkårsvurderingSteg =
        VilkårsvurderingSteg(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingstemaService = behandlingstemaService,
            vilkårService = vilkårService,
            beregningService = beregningService,
            persongrunnlagService = persongrunnlagService,
            tilbakestillBehandlingService = tilbakestillBehandlingService,
            tilpassKompetanserTilRegelverkService = tilpassKompetanserTilRegelverkService,
            vilkårsvurderingForNyBehandlingService = vilkårsvurderingForNyBehandlingService,
            månedligValutajusteringService = månedligValutajusteringService,
            localDateProvider = mockedDateProvider,
            automatiskOppdaterValutakursService = automatiskOppdaterValutakursService,
        )

    val ferdigstillBehandlingSteg =
        FerdigstillBehandling(
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            beregningService = beregningService,
            behandlingService = behandlingService,
            behandlingMetrikker = behandlingMetrikker,
            loggService = loggService,
            snikeIKøenService = snikeIKøenService,
        )

    val automatiskBeslutningService = AutomatiskBeslutningService(simuleringService)

    val beslutteVedtakSteg =
        BeslutteVedtak(
            totrinnskontrollService = totrinnskontrollService,
            vedtakService = vedtakService,
            behandlingService = behandlingService,
            beregningService = beregningService,
            taskRepository = taskRepository,
            loggService = loggService,
            vilkårsvurderingService = vilkårsvurderingService,
            unleashService = unleashNextMedContextService,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
            saksbehandlerContext = saksbehandlerContext,
            automatiskBeslutningService = automatiskBeslutningService,
            simuleringService = simuleringService,
            tilbakekrevingService = tilbakekrevingService,
            brevmottakerService = brevmottakerService,
            tilbakekrevingsvedtakMotregningService = tilbakekrevingsvedtakMotregningService,
            tilbakekrevingsvedtakMotregningBrevService = tilbakekrevingsvedtakMotregningBrevService,
        )

    val stegService =
        spyk(
            StegService(
                steg =
                    listOf(
                        registrerPersongrunnlag,
                        vilkårsvurderingSteg,
                        behandlingsresultatSteg,
                        håndterIverksettMotØkonomiSteg,
                        statusFraOppdrag,
                        ferdigstillBehandlingSteg,
                        beslutteVedtakSteg,
                    ),
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                behandlingHentOgPersisterService = behandlingHentOgPersisterService,
                beregningService = beregningService,
                søknadGrunnlagService = søknadGrunnlagService,
                tilgangService = tilgangService,
                infotrygdFeedService = mockk(),
                satsendringService = mockk(),
                personopplysningerService = personopplysningerService,
                automatiskBeslutningService = mockk(),
                opprettTaskService = mockk(),
                satskjøringRepository = mockk(),
                unleashService = unleashNextMedContextService,
            ),
        )

    val autovedtakService =
        AutovedtakService(
            stegService = stegService,
            behandlingService = behandlingService,
            vedtakService = vedtakService,
            loggService = loggService,
            totrinnskontrollService = totrinnskontrollService,
            tilbakestillBehandlingTilBehandlingsresultatService = tilbakestillBehandlingTilBehandlingsresultatService,
        )

    val autovedtakSmåbarnstilleggService =
        AutovedtakSmåbarnstilleggService(
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vedtakService = vedtakService,
            behandlingService = behandlingService,
            vedtaksperiodeService = vedtaksperiodeService,
            overgangsstønadService = overgangsstønadService,
            taskService = taskService,
            autovedtakService = autovedtakService,
            oppgaveService = oppgaveService,
            vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
            localDateProvider = mockedDateProvider,
            påVentService = mockPåVentService,
            stegService = stegService,
            småbarnstilleggService = småbarnstilleggService,
        )
    val iverksettMotOppdragTask = IverksettMotOppdragTask(stegService, behandlingHentOgPersisterService, taskRepository)
    val ferdigstillBehandlingTask = FerdigstillBehandlingTask(stegService = stegService, behandlingHentOgPersisterService = behandlingHentOgPersisterService)
    val statusFraOppdragTask = StatusFraOppdragTask(stegService, behandlingHentOgPersisterService, personidentService, taskRepository)

    val taskservices = listOf(iverksettMotOppdragTask, ferdigstillBehandlingTask, statusFraOppdragTask)
}
