package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import no.nav.familie.ba.sak.common.MockedDateProvider
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockEcbService
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockUnleashNextMedContextService
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockUnleashService
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockVurderingsstrategiForValutakurserRepository
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsoppdragGenerator
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsoppdragGeneratorService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.MånedligValutajusteringSevice
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.AutomatiskBeslutningService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatSteg
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
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
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.AutomatiskOppdaterValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.EndretUtbetalingAndelTidslinjeService
import no.nav.familie.ba.sak.kjerne.steg.BeslutteVedtak
import no.nav.familie.ba.sak.kjerne.steg.FerdigstillBehandling
import no.nav.familie.ba.sak.kjerne.steg.IverksettMotOppdrag
import no.nav.familie.ba.sak.kjerne.steg.RegistrerPersongrunnlag
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdrag
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingTilBehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.steg.VilkårsvurderingSteg
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.EøsSkjemaerForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("CucumberMock")

class CucumberMock(
    dataFraCucumber: BegrunnelseTeksterStepDefinition,
    nyBehanldingId: Long,
    forrigeBehandling: Behandling?,
    efSakRestClientMock: EfSakRestClient = mockEfSakRestClient(),
    ecbService: ECBService = mockEcbService(dataFraCucumber),
    scope: CoroutineScope? = null,
) {
    val mockedDateProvider = MockedDateProvider(dataFraCucumber.dagensDato)
    val persongrunnlagService = mockPersongrunnlagService(dataFraCucumber)
    val fagsakService = mockFagsakService(dataFraCucumber)
    val fagsakRepository = mockFagsakRepository(dataFraCucumber)
    val oppgaveService = mockOppgaveService()
    val personopplysningerService = mockPersonopplysningerService(dataFraCucumber)
    val tilgangService = mockTilgangService()
    val vilkårsvurderingForNyBehandlingService = mockVilkårsvurderingForNyBehandlingService(dataFraCucumber)
    val vilkårService = mockVilkårService(dataFraCucumber)
    val tilbakestillBehandlingService = mockTilbakestillBehandlingService()
    val personopplysningGrunnlagRepository = mockPersonopplysningGrunnlagRepository(dataFraCucumber.persongrunnlag)
    val personopplysningGrunnlagForNyBehandlingService = mockPersonopplysningGrunnlagForNyBehandlingService(dataFraCucumber)
    val personidentService = mockPersonidentService(dataFraCucumber, nyBehanldingId)
    val tilkjentYtelseRepository = mockTilkjentYtelseRepository(dataFraCucumber)
    val vilkårsvurderingRepository = mockVilkårsvurderingRepository(dataFraCucumber)
    val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockAndelerTilkjentYtelseOgEndreteUtbetalingerService(dataFraCucumber)
    val andelTilkjentYtelseRepository = mockAndelTilkjentYtelseRepository(dataFraCucumber)
    val vilkårsvurderingService = VilkårsvurderingService(vilkårsvurderingRepository, sanityService = mockk())
    val vilkårsvurderingTidslinjeService = mockVilkårsvurderingTidslinjeService(vilkårsvurderingRepository, vilkårsvurderingService, persongrunnlagService)
    val loggService = mockLoggService()
    val behandlingHentOgPersisterService = mockBehandlingHentOgPersisterService(forrigeBehandling = forrigeBehandling, dataFraCucumber = dataFraCucumber, idForNyBehandling = nyBehanldingId)
    val periodeOvergangsstønadGrunnlagRepository = mockPeriodeOvergangsstønadGrunnlagRepository(dataFraCucumber)
    val søknadGrunnlagService = mockSøknadGrunnlagService(dataFraCucumber)
    val endretUtbetalingAndelHentOgPersisterService = mockEndretUtbetalingAndelHentOgPersisterService(dataFraCucumber)
    val vedtakRepository = mockVedtakRepository(dataFraCucumber)
    val dokumentGenereringService = mockDokumentGenereringService()
    val vedtaksperiodeHentOgPersisterService = mockVedtaksperiodeHentOgPersisterService(dataFraCucumber)
    val kompetanseRepository = mockKompetanseRepository(dataFraCucumber)
    val valutakursRepository = mockValutakursRepository(dataFraCucumber)
    val utenlandskPeriodebeløpRepository = mockUtenlandskPeriodebeløpRepository(dataFraCucumber)
    val endretUtbetalingAndelRepository = mockEndretUtbetalingAndelRepository(dataFraCucumber)
    val simuleringService = mockSimuleringService()
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
    val opprettTaskService = mockk<OpprettTaskService>()
    val endretUtbetalingAndelTidslinjeService = EndretUtbetalingAndelTidslinjeService(endretUtbetalingAndelHentOgPersisterService)
    val vurderingsstrategiForValutakurserRepository = mockVurderingsstrategiForValutakurserRepository()

    val behandlingstemaService =
        BehandlingstemaService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            loggService = loggService,
            oppgaveService = oppgaveService,
            vilkårsvurderingTidslinjeService = vilkårsvurderingTidslinjeService,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
        )

    val småbarnstilleggService =
        SmåbarnstilleggService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            efSakRestClient = efSakRestClientMock,
            periodeOvergangsstønadGrunnlagRepository = periodeOvergangsstønadGrunnlagRepository,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            persongrunnlagService = persongrunnlagService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
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
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            behandlingRepository = mockk(),
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            småbarnstilleggService = småbarnstilleggService,
            tilkjentYtelseEndretAbonnenter = listOf(tilpassDifferanseberegningEtterTilkjentYtelseService),
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
        )

    val vedtakService =
        VedtakService(
            vedtakRepository = vedtakRepository,
            dokumentGenereringService = dokumentGenereringService,
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
            småbarnstilleggService = småbarnstilleggService,
            refusjonEøsRepository = mockk(),
            integrasjonClient = mockk(),
            valutakursRepository = valutakursRepository,
            utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository,
            unleashNextMedContextService = unleashNextMedContextService,
        )

    val behandlingService =
        BehandlingService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingstemaService = behandlingstemaService,
            behandlingSøknadsinfoService = mockk<BehandlingSøknadsinfoService>(),
            behandlingMigreringsinfoRepository = mockk<BehandlingMigreringsinfoRepository>(),
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

    val tilpassValutakurserTilUtenlandskePeriodebeløpService = TilpassValutakurserTilUtenlandskePeriodebeløpService(valutakursRepository = valutakursRepository, utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository, endringsabonnenter = valutakursAbonnenter)

    val tilbakestillBehandlingFraUtenlandskPeriodebeløpEndringService = TilbakestillBehandlingFraUtenlandskPeriodebeløpEndringService(tilbakestillBehandlingTilBehandlingsresultatService = tilbakestillBehandlingTilBehandlingsresultatService)

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
        )

    val tilpassDifferanseberegningEtterUtenlandskPeriodebeløpService =
        TilpassDifferanseberegningEtterUtenlandskPeriodebeløpService(
            valutakursRepository = valutakursRepository,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            barnasDifferanseberegningEndretAbonnenter = listOf(tilpassDifferanseberegningSøkersYtelserService),
            automatiskOppdaterValutakursService = automatiskOppdaterValutakursService,
            unleashNextMedContextService = unleashNextMedContextService,
        )

    val utenlandskPeriodebeløpEndretAbonnenter =
        listOf(
            tilpassDifferanseberegningEtterUtenlandskPeriodebeløpService,
            tilbakestillBehandlingFraUtenlandskPeriodebeløpEndringService,
            tilpassValutakurserTilUtenlandskePeriodebeløpService,
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
            unleashService = unleashService,
        )

    val endringsabonnenterForKompetanse = listOf(tilpassUtenlandskePeriodebeløpTilKompetanserService, tilbakestillBehandlingFraKompetanseEndringService)

    val tilpassKompetanserTilRegelverkService = TilpassKompetanserTilRegelverkService(vilkårsvurderingTidslinjeService, endretUtbetalingAndelTidslinjeService, kompetanseRepository, endringsabonnenter = endringsabonnenterForKompetanse)

    val kompetanseService = KompetanseService(kompetanseRepository, endringsabonnenter = endringsabonnenterForKompetanse)

    val eøsSkjemaerForNyBehandlingService = EøsSkjemaerForNyBehandlingService(kompetanseService = kompetanseService, utenlandskPeriodebeløpService = utenlandskPeriodebeløpService, valutakursService = valutakursService)

    val behandlingsresultatService =
        BehandlingsresultatService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            søknadGrunnlagService = søknadGrunnlagService,
            personidentService = personidentService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            endretUtbetalingAndelHentOgPersisterService = endretUtbetalingAndelHentOgPersisterService,
            kompetanseService = kompetanseService,
            localDateProvider = mockedDateProvider,
        )

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
            valutakursService = valutakursService,
        )

    val saksbehandlerContext = SaksbehandlerContext("")
    val totrinnskontrollService = TotrinnskontrollService(behandlingService = behandlingService, totrinnskontrollRepository = totrinnskontrollRepository, saksbehandlerContext = saksbehandlerContext)

    val tilkjentYtelseValideringService =
        TilkjentYtelseValideringService(
            beregningService = beregningService,
            totrinnskontrollService = totrinnskontrollService,
            persongrunnlagService = persongrunnlagService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        )

    val utbetalingsoppdragGeneratorService =
        UtbetalingsoppdragGeneratorService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingService = behandlingService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            utbetalingsoppdragGenerator = UtbetalingsoppdragGenerator(),
            endretUtbetalingAndelHentOgPersisterService = endretUtbetalingAndelHentOgPersisterService,
            unleashNextMedContextService = unleashNextMedContextService,
        )

    val økonomiService =
        ØkonomiService(
            økonomiKlient = mockØkonomiKlient(),
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
            utbetalingsoppdragGeneratorService = utbetalingsoppdragGeneratorService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
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

    val registrerPersongrunnlag =
        RegistrerPersongrunnlag(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vilkårsvurderingForNyBehandlingService = vilkårsvurderingForNyBehandlingService,
            personopplysningGrunnlagForNyBehandlingService = personopplysningGrunnlagForNyBehandlingService,
            eøsSkjemaerForNyBehandlingService = eøsSkjemaerForNyBehandlingService,
        )

    val månedligValutajusteringSevice = MånedligValutajusteringSevice(ecbService = ecbService, valutakursService = valutakursService)

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
            månedligValutajusteringSevice = månedligValutajusteringSevice,
            localDateProvider = mockedDateProvider,
            automatiskOppdaterValutakursService = automatiskOppdaterValutakursService,
            unleashNextMedContextService = unleashNextMedContextService,
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
            automatiskOppdaterValutakursService = automatiskOppdaterValutakursService,
            valutakursRepository = valutakursRepository,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            simuleringService = simuleringService,
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
            småbarnstilleggService = småbarnstilleggService,
            taskService = taskService,
            beregningService = beregningService,
            autovedtakService = autovedtakService,
            oppgaveService = oppgaveService,
            vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
            localDateProvider = mockedDateProvider,
            påVentService = mockPåVentService,
            opprettTaskService = opprettTaskService,
            stegService = stegService,
        )
    val iverksettMotOppdragTask = IverksettMotOppdragTask(stegService, behandlingHentOgPersisterService, personidentService, taskRepository)
    val ferdigstillBehandlingTask = FerdigstillBehandlingTask(stegService = stegService, behandlingHentOgPersisterService = behandlingHentOgPersisterService)
    val statusFraOppdragTask = StatusFraOppdragTask(stegService, behandlingHentOgPersisterService, personidentService, taskRepository)

    val taskservices = listOf(iverksettMotOppdragTask, ferdigstillBehandlingTask, statusFraOppdragTask)
}
