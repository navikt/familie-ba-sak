package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import no.nav.familie.ba.sak.common.LocalDateProvider
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.tilPersonEnkel
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.MånedligValutajusteringSevice
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatSteg
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilAndelerTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.brev.DokumentGenereringService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
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
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.RegistrerPersongrunnlag
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingTilBehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.steg.VilkårsvurderingSteg
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.EøsSkjemaerForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.PersonopplysningGrunnlagForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import no.nav.familie.prosessering.internal.TaskService
import java.time.LocalDate

class CucumberMock(
    dataFraCucumber: BegrunnelseTeksterStepDefinition,
    nyBehanldingId: Long,
    forrigeBehandling: Behandling?,
    efSakRestClientMock: EfSakRestClient = mockEfSakRestClient(),
    ecbService: ECBService = mockk<ECBService>(),
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
    val vilkårsvurderingService = mockVilkårsvurderingService(dataFraCucumber)
    val vilkårsvurderingTidslinjeService = mockVilkårsvurderingTidslinjeService(vilkårsvurderingRepository, vilkårsvurderingService, persongrunnlagService)
    val loggService = mockLoggService()
    val behandlingHentOgPersisterService = mockBehandlingHentOgPersisterService(forrigeBehandling = forrigeBehandling, dataFraCucumber = dataFraCucumber, idForNyBehandling = nyBehanldingId)
    val periodeOvergangsstønadGrunnlagRepository = mockPeriodeOvergangsstønadGrunnlagRepository(dataFraCucumber)
    val tilpassKompetanserTilRegelverkService = mockTilpassKompetanserTilRegelverkService()
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
    val totrinnskontrollService = mockTotrinnskontrollService()
    val taskService = mockTaskService()
    val saksstatistikkEventPublisher = mockSaksstatistikkEventPublisher()
    val arbeidsfordelingService = mockArbeidsfordelingService()
    val behandlingMetrikker = mockBehandlingMetrikker()
    val tilbakekrevingService = mockTilbakekrevingService()
    val taskRepository = mockTaskRepositoryWrapper()

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
            unleashService = mockk(),
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
    val tilbakestillBehandlingFraValutakursEndringService = TilbakestillBehandlingFraValutakursEndringService(tilbakestillBehandlingTilBehandlingsresultatService = tilbakestillBehandlingTilBehandlingsresultatService)

    val valutakursAbonnenter = listOf(tilpassDifferanseberegningEtterValutakursService, tilbakestillBehandlingFraValutakursEndringService)

    val tilpassValutakurserTilUtenlandskePeriodebeløpService = TilpassValutakurserTilUtenlandskePeriodebeløpService(valutakursRepository = valutakursRepository, utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository, endringsabonnenter = valutakursAbonnenter)

    val tilbakestillBehandlingFraUtenlandskPeriodebeløpEndringService = TilbakestillBehandlingFraUtenlandskPeriodebeløpEndringService(tilbakestillBehandlingTilBehandlingsresultatService = tilbakestillBehandlingTilBehandlingsresultatService)

    val tilpassDifferanseberegningEtterUtenlandskPeriodebeløpService = TilpassDifferanseberegningEtterUtenlandskPeriodebeløpService(valutakursRepository = valutakursRepository, tilkjentYtelseRepository = tilkjentYtelseRepository, barnasDifferanseberegningEndretAbonnenter = listOf(tilpassDifferanseberegningSøkersYtelserService))

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
        )

    val kompetanseService = KompetanseService(kompetanseRepository, endringsabonnenter = listOf(tilpassUtenlandskePeriodebeløpTilKompetanserService, tilbakestillBehandlingFraKompetanseEndringService))

    val valutakursService = ValutakursService(valutakursRepository = valutakursRepository, endringsabonnenter = valutakursAbonnenter)

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
        )

    val stegService =
        spyk(
            StegService(
                steg = listOf(registrerPersongrunnlag, vilkårsvurderingSteg, behandlingsresultatSteg),
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
        )
}

private fun mockOppgaveService(): OppgaveService {
    val oppgaveService = mockk<OppgaveService>()
    every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any()) } returns ""
    return oppgaveService
}

private fun mockTilbakekrevingService(): TilbakekrevingService {
    val tilbakekrevingService = mockk<TilbakekrevingService>()
    every { tilbakekrevingService.slettTilbakekrevingPåBehandling(any()) } returns null
    return tilbakekrevingService
}

private fun mockBehandlingMetrikker(): BehandlingMetrikker {
    val behandlingMetrikker = mockk<BehandlingMetrikker>()
    every { behandlingMetrikker.tellNøkkelTallVedOpprettelseAvBehandling(any()) } just runs
    return behandlingMetrikker
}

private fun mockArbeidsfordelingService(): ArbeidsfordelingService {
    val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    every { arbeidsfordelingService.fastsettBehandlendeEnhet(any(), any()) } just runs
    return arbeidsfordelingService
}

private fun mockSaksstatistikkEventPublisher(): SaksstatistikkEventPublisher {
    val saksstatistikkEventPublisher = mockk<SaksstatistikkEventPublisher>()
    every { saksstatistikkEventPublisher.publiserSaksstatistikk(any()) } just runs
    every { saksstatistikkEventPublisher.publiserBehandlingsstatistikk(any()) } just runs
    return saksstatistikkEventPublisher
}

private fun mockTaskService(): TaskService {
    val taskService = mockk<TaskService>()
    every { taskService.save(any()) } answers { firstArg() }
    return taskService
}

private fun mockTilbakestillBehandlingService(): TilbakestillBehandlingService {
    val tilbakestillBehandlingService = mockk<TilbakestillBehandlingService>()
    every { tilbakestillBehandlingService.tilbakestillDataTilVilkårsvurderingssteg(any()) } just runs
    return tilbakestillBehandlingService
}

private fun mockVilkårService(dataFraCucumber: BegrunnelseTeksterStepDefinition): VilkårService {
    val vilkårService = mockk<VilkårService>()
    every { vilkårService.hentVilkårsvurdering(any()) } answers {
        val behandlingsId = firstArg<Long>()
        dataFraCucumber.vilkårsvurderinger[behandlingsId]!!
    }
    every { vilkårService.hentVilkårsvurderingThrows(any()) } answers {
        val behandlingsId = firstArg<Long>()
        dataFraCucumber.vilkårsvurderinger[behandlingsId]!!
    }
    return vilkårService
}

private fun mockPersongrunnlagService(dataFraCucumber: BegrunnelseTeksterStepDefinition): PersongrunnlagService {
    val persongrunnlagService = mockk<PersongrunnlagService>()
    every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(any()) } answers {
        val behandlingId = firstArg<Long>()
        val personopplysningGrunnlag = dataFraCucumber.persongrunnlag[behandlingId] ?: error("Fant ikke persongrunnlag for behandling $behandlingId")
        personopplysningGrunnlag.personer.map { it.tilPersonEnkel() }
    }
    every { persongrunnlagService.hentAktivThrows(any()) } answers {
        val behandlingsId = firstArg<Long>()
        dataFraCucumber.persongrunnlag[behandlingsId]!!
    }
    every { persongrunnlagService.hentBarna(any<Long>()) } answers {
        val behandlingsId = firstArg<Long>()
        dataFraCucumber.persongrunnlag[behandlingsId]!!.barna
    }
    return persongrunnlagService
}

private fun mockEøsSkjemaerForNyBehandlingService(): EøsSkjemaerForNyBehandlingService {
    val eøsSkjemaerForNyBehandlingService = mockk<EøsSkjemaerForNyBehandlingService>()
    every { eøsSkjemaerForNyBehandlingService.kopierEøsSkjemaer(any(), any()) } just runs
    return eøsSkjemaerForNyBehandlingService
}

private fun mockVilkårsvurderingForNyBehandlingService(dataFraCucumber: BegrunnelseTeksterStepDefinition): VilkårsvurderingForNyBehandlingService {
    val vilkårsvurderingForNyBehandlingService = mockk<VilkårsvurderingForNyBehandlingService>()
    every { vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(any(), any(), any()) } answers {
        val behandling = firstArg<Behandling>()
        val forrigeBehandling = secondArg<Behandling>()

        val forrigeVilkårsvurdering = dataFraCucumber.vilkårsvurderinger[forrigeBehandling.id]!!
        val vilkårsvurderingSmåbarnstilleggbehandling = forrigeVilkårsvurdering.copy(behandling = behandling).kopier()

        dataFraCucumber.vilkårsvurderinger[behandling.id] = vilkårsvurderingSmåbarnstilleggbehandling
    }
    return vilkårsvurderingForNyBehandlingService
}

private fun mockTilgangService(): TilgangService {
    val tilgangService = mockk<TilgangService>()
    every { tilgangService.validerTilgangTilBehandling(any(), any()) } just runs
    every { tilgangService.verifiserHarTilgangTilHandling(any(), any()) } just runs
    return tilgangService
}

private fun mockPersonopplysningerService(dataFraCucumber: BegrunnelseTeksterStepDefinition): PersonopplysningerService {
    val personopplysningerService = mockk<PersonopplysningerService>()
    every { personopplysningerService.hentPersoninfoEnkel(any()) } answers {
        val aktør = firstArg<Aktør>()
        dataFraCucumber.persongrunnlag.values.flatMap { it.personer }.first { it.aktør == aktør }.tilPersonInfo()
    }
    return personopplysningerService
}

private fun mockFagsakService(dataFraCucumber: BegrunnelseTeksterStepDefinition): FagsakService {
    val fagsakService = mockk<FagsakService>()
    every { fagsakService.hentNormalFagsak(any()) } answers {
        val aktør = firstArg<Aktør>()
        dataFraCucumber.fagsaker.values.single { it.aktør == aktør }
    }
    return fagsakService
}

private fun mockFagsakRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): FagsakRepository {
    val fagsakRepository = mockk<FagsakRepository>()
    every { fagsakRepository.finnFagsak(any()) } answers {
        val id = firstArg<Long>()
        dataFraCucumber.fagsaker.values.single { it.id == id }
    }
    return fagsakRepository
}

private fun mockTotrinnskontrollService(): TotrinnskontrollService {
    val totrinnskontrollService = mockk<TotrinnskontrollService>()
    every { totrinnskontrollService.opprettAutomatiskTotrinnskontroll(any()) } just runs
    return totrinnskontrollService
}

private fun mockSimuleringService(): SimuleringService {
    val simuleringService = mockk<SimuleringService>()
    every { simuleringService.oppdaterSimuleringPåBehandling(any()) } returns emptyList()
    return simuleringService
}

private fun mockEndretUtbetalingAndelRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): EndretUtbetalingAndelRepository {
    val endretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.endredeUtbetalinger[behandlingId] ?: emptyList()
    }
    return endretUtbetalingAndelRepository
}

private fun mockUtenlandskPeriodebeløpRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): UtenlandskPeriodebeløpRepository {
    val utenlandskPeriodebeløpRepository = mockk<UtenlandskPeriodebeløpRepository>()
    every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.utenlandskPeriodebeløp[behandlingId] ?: emptyList()
    }
    every { utenlandskPeriodebeløpRepository.deleteAll(any<Iterable<UtenlandskPeriodebeløp>>()) } answers {
        val utenlandskPeriodebeløp = firstArg<Iterable<UtenlandskPeriodebeløp>>()
        utenlandskPeriodebeløp.forEach {
            dataFraCucumber.utenlandskPeriodebeløp[it.behandlingId] = dataFraCucumber.utenlandskPeriodebeløp[it.behandlingId]?.filter { utenlandskPeriodebeløp -> utenlandskPeriodebeløp != it } ?: emptyList()
        }
    }
    every { utenlandskPeriodebeløpRepository.saveAll(any<Iterable<UtenlandskPeriodebeløp>>()) } answers {
        val utenlandskPeriodebeløp = firstArg<Iterable<UtenlandskPeriodebeløp>>()
        utenlandskPeriodebeløp.forEach {
            dataFraCucumber.utenlandskPeriodebeløp[it.behandlingId] = (dataFraCucumber.utenlandskPeriodebeløp[it.behandlingId] ?: emptyList()) + it
        }
        utenlandskPeriodebeløp.toList()
    }
    return utenlandskPeriodebeløpRepository
}

private fun mockValutakursRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): ValutakursRepository {
    val valutakursRepository = mockk<ValutakursRepository>()
    every { valutakursRepository.finnFraBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.valutakurs[behandlingId] ?: emptyList()
    }
    every { valutakursRepository.deleteAll(any<Iterable<Valutakurs>>()) } answers {
        val valutakurser = firstArg<Iterable<Valutakurs>>()
        valutakurser.forEach {
            dataFraCucumber.valutakurs[it.behandlingId] = dataFraCucumber.valutakurs[it.behandlingId]?.filter { valutakurs -> valutakurs != it } ?: emptyList()
        }
    }
    every { valutakursRepository.saveAll(any<Iterable<Valutakurs>>()) } answers {
        val valutakurser = firstArg<Iterable<Valutakurs>>()
        valutakurser.forEach {
            dataFraCucumber.valutakurs[it.behandlingId] = (dataFraCucumber.valutakurs[it.behandlingId] ?: emptyList()) + it
        }
        valutakurser.toList()
    }

    return valutakursRepository
}

private fun mockKompetanseRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): KompetanseRepository {
    val kompetanseRepository = mockk<KompetanseRepository>()
    every { kompetanseRepository.finnFraBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.kompetanser[behandlingId] ?: emptyList()
    }
    every { kompetanseRepository.deleteAll(any<Iterable<Kompetanse>>()) } answers {
        val kompetanser = firstArg<Iterable<Kompetanse>>()
        kompetanser.forEach {
            dataFraCucumber.kompetanser[it.behandlingId] = dataFraCucumber.kompetanser[it.behandlingId]?.filter { kompetanse -> kompetanse != it } ?: emptyList()
        }
    }
    every { kompetanseRepository.saveAll(any<Iterable<Kompetanse>>()) } answers {
        val kompetanser = firstArg<Iterable<Kompetanse>>()
        kompetanser.forEach {
            dataFraCucumber.kompetanser[it.behandlingId] = (dataFraCucumber.kompetanser[it.behandlingId] ?: emptyList()) + it
        }
        kompetanser.toMutableList()
    }
    return kompetanseRepository
}

private fun mockVedtaksperiodeHentOgPersisterService(dataFraCucumber: BegrunnelseTeksterStepDefinition): VedtaksperiodeHentOgPersisterService {
    val vedtaksperiodeHentOgPersisterService = mockk<VedtaksperiodeHentOgPersisterService>()
    every { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(any()) } answers {
        val vedtak = firstArg<Vedtak>()
        dataFraCucumber.vedtaksperioderMedBegrunnelser = dataFraCucumber.vedtaksperioderMedBegrunnelser.filter { it.vedtak.id != vedtak.id }
    }
    every { vedtaksperiodeHentOgPersisterService.lagre(any<List<VedtaksperiodeMedBegrunnelser>>()) } answers {
        val vedtaksperioder = firstArg<List<VedtaksperiodeMedBegrunnelser>>()
        dataFraCucumber.vedtaksperioderMedBegrunnelser = dataFraCucumber.vedtaksperioderMedBegrunnelser + vedtaksperioder
        vedtaksperioder
    }
    every { vedtaksperiodeHentOgPersisterService.lagre(any<VedtaksperiodeMedBegrunnelser>()) } answers {
        val vedtaksperiode = firstArg<VedtaksperiodeMedBegrunnelser>()
        dataFraCucumber.vedtaksperioderMedBegrunnelser = dataFraCucumber.vedtaksperioderMedBegrunnelser + vedtaksperiode
        vedtaksperiode
    }
    every { vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(any()) } answers {
        val vedtakId = firstArg<Long>()
        dataFraCucumber.vedtaksperioderMedBegrunnelser.filter { it.vedtak.id == vedtakId }
    }
    return vedtaksperiodeHentOgPersisterService
}

private fun mockDokumentGenereringService(): DokumentGenereringService {
    val dokumentGenereringService = mockk<DokumentGenereringService>()
    every { dokumentGenereringService.genererBrevForVedtak(any()) } returns byteArrayOf()
    return dokumentGenereringService
}

private fun mockVedtakRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): VedtakRepository {
    val vedtakRepository = mockk<VedtakRepository>()
    every { vedtakRepository.findByBehandlingAndAktiv(any()) } answers {
        val behandlingId = firstArg<Long>()
        opprettEllerHentVedtak(dataFraCucumber, behandlingId)
    }
    every { vedtakRepository.findByBehandlingAndAktivOptional(any()) } answers {
        val behandlingId = firstArg<Long>()
        opprettEllerHentVedtak(dataFraCucumber, behandlingId)
    }
    every { vedtakRepository.save(any()) } answers {
        val oppdatertVedtak = firstArg<Vedtak>()
        lagreVedtak(dataFraCucumber, oppdatertVedtak)
    }
    every { vedtakRepository.saveAndFlush(any()) } answers {
        val oppdatertVedtak = firstArg<Vedtak>()
        lagreVedtak(dataFraCucumber, oppdatertVedtak)
    }
    return vedtakRepository
}

private fun lagreVedtak(
    dataFraCucumber: BegrunnelseTeksterStepDefinition,
    oppdatertVedtak: Vedtak,
): Vedtak {
    dataFraCucumber.vedtaksliste = dataFraCucumber.vedtaksliste.map { if (it.id == oppdatertVedtak.id) oppdatertVedtak else it }.toMutableList()
    if (oppdatertVedtak.id !in dataFraCucumber.vedtaksliste.map { it.id }) {
        dataFraCucumber.vedtaksliste.add(oppdatertVedtak)
    }
    return oppdatertVedtak
}

private fun mockKompetanseService(dataFraCucumber: BegrunnelseTeksterStepDefinition): KompetanseService {
    val kompetanseService = mockk<KompetanseService>()
    every { kompetanseService.hentKompetanser(any()) } answers {
        val behandlingId = firstArg<BehandlingId>()
        dataFraCucumber.kompetanser[behandlingId.id] ?: emptyList()
    }
    return kompetanseService
}

private fun mockEndretUtbetalingAndelHentOgPersisterService(dataFraCucumber: BegrunnelseTeksterStepDefinition): EndretUtbetalingAndelHentOgPersisterService {
    val endretUtbetalingAndelHentOgPersisterService = mockk<EndretUtbetalingAndelHentOgPersisterService>()
    every { endretUtbetalingAndelHentOgPersisterService.hentForBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.endredeUtbetalinger[behandlingId] ?: emptyList()
    }
    return endretUtbetalingAndelHentOgPersisterService
}

private fun mockSøknadGrunnlagService(dataFraCucumber: BegrunnelseTeksterStepDefinition): SøknadGrunnlagService {
    val søknadGrunnlagService = mockk<SøknadGrunnlagService>()
    every { søknadGrunnlagService.hentAktiv(any()) } answers {
        val behandlingId = firstArg<Long>()
        val behandling = dataFraCucumber.behandlinger[behandlingId]!!
        if (behandling.opprettetÅrsak == BehandlingÅrsak.SØKNAD) {
            error("Ikke implementert")
        } else {
            null
        }
    }
    return søknadGrunnlagService
}

private fun mockTilpassKompetanserTilRegelverkService(): TilpassKompetanserTilRegelverkService {
    val tilpassKompetanserTilRegelverkService = mockk<TilpassKompetanserTilRegelverkService>()
    every { tilpassKompetanserTilRegelverkService.tilpassKompetanserTilRegelverk(any()) } just runs
    return tilpassKompetanserTilRegelverkService
}

private fun mockPeriodeOvergangsstønadGrunnlagRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): PeriodeOvergangsstønadGrunnlagRepository {
    val periodeOvergangsstønadGrunnlagRepository = mockk<PeriodeOvergangsstønadGrunnlagRepository>()
    every { periodeOvergangsstønadGrunnlagRepository.deleteByBehandlingId(any()) } just runs
    every { periodeOvergangsstønadGrunnlagRepository.saveAll(any<List<PeriodeOvergangsstønadGrunnlag>>()) } answers {
        val overgangstønadsperioder = firstArg<List<PeriodeOvergangsstønadGrunnlag>>()
        val behandlingId = overgangstønadsperioder.firstOrNull()?.behandlingId

        if (behandlingId != null) {
            dataFraCucumber.overgangsstønader[behandlingId] = overgangstønadsperioder.map { it.tilInternPeriodeOvergangsstønad() }
        }

        overgangstønadsperioder
    }
    every { periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()

        dataFraCucumber.overgangsstønader[behandlingId]?.map {
            PeriodeOvergangsstønadGrunnlag(
                behandlingId = behandlingId,
                aktør = dataFraCucumber.persongrunnlag[behandlingId]!!.søker.aktør,
                fom = it.fomDato,
                tom = it.tomDato,
                datakilde = Datakilde.EF,
            )
        } ?: emptyList()
    }
    return periodeOvergangsstønadGrunnlagRepository
}

private fun mockLoggService(): LoggService {
    val loggService = mockk<LoggService>()
    every { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any(), any()) } just runs
    every { loggService.opprettBehandlingLogg(any()) } just runs
    every { loggService.opprettVilkårsvurderingLogg(any(), any(), any()) } returns null
    return loggService
}

private fun mockVilkårsvurderingTidslinjeService(
    vilkårsvurderingRepository: VilkårsvurderingRepository,
    vilkårsvurderingService: VilkårsvurderingService,
    persongrunnlagService: PersongrunnlagService,
): VilkårsvurderingTidslinjeService {
    val vilkårsvurderingTidslinjeService =
        VilkårsvurderingTidslinjeService(
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            vilkårsvurderingService = vilkårsvurderingService,
            persongrunnlagService = persongrunnlagService,
        )
    return vilkårsvurderingTidslinjeService
}

private fun mockVilkårsvurderingService(dataFraCucumber: BegrunnelseTeksterStepDefinition): VilkårsvurderingService {
    val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    every { vilkårsvurderingService.hentAktivForBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.vilkårsvurderinger[behandlingId]!!
    }
    every { vilkårsvurderingService.hentAktivForBehandlingThrows(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.vilkårsvurderinger[behandlingId]!!
    }
    return vilkårsvurderingService
}

private fun mockAndelTilkjentYtelseRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): AndelTilkjentYtelseRepository {
    val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.andelerTilkjentYtelse[behandlingId] ?: emptyList()
    }
    return andelTilkjentYtelseRepository
}

private fun mockAndelerTilkjentYtelseOgEndreteUtbetalingerService(dataFraCucumber: BegrunnelseTeksterStepDefinition): AndelerTilkjentYtelseOgEndreteUtbetalingerService {
    val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()
    every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } answers {
        val behandlingId = firstArg<Long>()
        val andelerTilkjentYtelse = dataFraCucumber.andelerTilkjentYtelse[behandlingId] ?: emptyList()
        val endredeUtbetalinger = dataFraCucumber.endredeUtbetalinger[behandlingId] ?: emptyList()

        andelerTilkjentYtelse.tilAndelerTilkjentYtelseMedEndreteUtbetalinger(endredeUtbetalinger)
    }
    every {
        andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(any())
    } answers {
        val behandlingId = firstArg<Long>()
        val andelerTilkjentYtelse = dataFraCucumber.andelerTilkjentYtelse[behandlingId] ?: emptyList()
        val endredeUtbetalinger = dataFraCucumber.endredeUtbetalinger[behandlingId] ?: emptyList()

        endredeUtbetalinger.tilEndretUtbetalingAndelMedAndelerTilkjentYtelse(andelerTilkjentYtelse)
    }
    return andelerTilkjentYtelseOgEndreteUtbetalingerService
}

private fun mockVilkårsvurderingRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): VilkårsvurderingRepository {
    val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    every { vilkårsvurderingRepository.findByBehandlingAndAktiv(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.vilkårsvurderinger[behandlingId]!!
    }
    return vilkårsvurderingRepository
}

private fun mockEfSakRestClient(): EfSakRestClient {
    val efSakRestClient = mockk<EfSakRestClient>()
    every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } answers {
        EksternePerioderResponse(emptyList())
    }
    return efSakRestClient
}

private fun mockTilkjentYtelseRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): TilkjentYtelseRepository {
    val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    every { tilkjentYtelseRepository.findByBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        val andeler = dataFraCucumber.andelerTilkjentYtelse[behandlingId]
        TilkjentYtelse(
            behandling = dataFraCucumber.behandlinger[behandlingId]!!,
            andelerTilkjentYtelse = andeler!!.toMutableSet(),
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
        )
    }
    every { tilkjentYtelseRepository.findByBehandlingOptional(any()) } answers {
        val behandlingId = firstArg<Long>()
        val andeler = dataFraCucumber.andelerTilkjentYtelse[behandlingId]
        andeler?.let {
            TilkjentYtelse(
                behandling = dataFraCucumber.behandlinger[behandlingId]!!,
                andelerTilkjentYtelse = andeler.toMutableSet(),
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
            )
        }
    }
    every { tilkjentYtelseRepository.slettTilkjentYtelseFor(any()) } just runs
    every { tilkjentYtelseRepository.save(any()) } answers {
        val tilkjentYtelse = firstArg<TilkjentYtelse>()
        dataFraCucumber.andelerTilkjentYtelse[tilkjentYtelse.behandling.id] = tilkjentYtelse.andelerTilkjentYtelse.toList()
        tilkjentYtelse
    }
    every { tilkjentYtelseRepository.saveAndFlush(any()) } answers {
        val tilkjentYtelse = firstArg<TilkjentYtelse>()
        dataFraCucumber.andelerTilkjentYtelse[tilkjentYtelse.behandling.id] = tilkjentYtelse.andelerTilkjentYtelse.toList()
        tilkjentYtelse
    }
    return tilkjentYtelseRepository
}

private fun mockPersonidentService(
    dataFraCucumber: BegrunnelseTeksterStepDefinition,
    småbarnstilleggBehandlingId: Long,
): PersonidentService {
    val personidentService = mockk<PersonidentService>()
    every { personidentService.hentOgLagreAktør(any(), any()) } answers {
        val personIdent = firstArg<String>()
        dataFraCucumber.persongrunnlag[småbarnstilleggBehandlingId]!!.personer.single { it.aktør.aktivFødselsnummer() == personIdent }.aktør
    }
    return personidentService
}

private fun mockPersonopplysningGrunnlagForNyBehandlingService(dataFraCucumber: BegrunnelseTeksterStepDefinition): PersonopplysningGrunnlagForNyBehandlingService {
    val personopplysningGrunnlagForNyBehandlingService = mockk<PersonopplysningGrunnlagForNyBehandlingService>()
    every { personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(any(), any(), any(), any()) } answers {
        val behandling = firstArg<Behandling>()
        val forrigeBehandling = secondArg<Behandling>()

        val persongrunnlagForrigeBehandling = dataFraCucumber.persongrunnlag[forrigeBehandling.id]!!
        dataFraCucumber.persongrunnlag[behandling.id] = persongrunnlagForrigeBehandling.copy(behandlingId = behandling.id)
    }
    return personopplysningGrunnlagForNyBehandlingService
}

private fun mockBehandlingHentOgPersisterService(
    forrigeBehandling: Behandling?,
    dataFraCucumber: BegrunnelseTeksterStepDefinition,
    idForNyBehandling: Long,
): BehandlingHentOgPersisterService {
    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns forrigeBehandling
    every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(any()) } returns forrigeBehandling
    every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(any()) } answers {
        val fagsakId = firstArg<Long>()
        dataFraCucumber.behandlinger.values.singleOrNull { it.fagsak.id == fagsakId && it.status != BehandlingStatus.AVSLUTTET }
    }
    every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(any()) } answers {
        val fagsakId = firstArg<Long>()
        dataFraCucumber.behandlinger.values.filter { it.fagsak.id == fagsakId && it.status == BehandlingStatus.AVSLUTTET }.maxByOrNull { it.aktivertTidspunkt }
    }
    every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(any()) } answers {
        val behandling = firstArg<Behandling>()
        dataFraCucumber.behandlinger.values.filter { it.fagsak.id == behandling.fagsak.id && it.id != behandling.id && it.status == BehandlingStatus.AVSLUTTET }.maxByOrNull { it.aktivertTidspunkt }
    }
    every { behandlingHentOgPersisterService.hent(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.behandlinger[behandlingId]!!
    }
    every { behandlingHentOgPersisterService.finnAktivForFagsak(any()) } answers {
        forrigeBehandling
    }
    every { behandlingHentOgPersisterService.lagreOgFlush(any()) } answers {
        val behandling = firstArg<Behandling>()
        oppdaterEllerLagreBehandling(dataFraCucumber, behandling, idForNyBehandling)
    }
    every { behandlingHentOgPersisterService.lagreEllerOppdater(any(), any()) } answers {
        val behandling = firstArg<Behandling>()
        oppdaterEllerLagreBehandling(dataFraCucumber, behandling, idForNyBehandling)
    }
    return behandlingHentOgPersisterService
}

private fun oppdaterEllerLagreBehandling(
    dataFraCucumber: BegrunnelseTeksterStepDefinition,
    behandlingSomSkalLagres: Behandling,
    idForNyBehandling: Long,
): Behandling {
    val behandling = if (behandlingSomSkalLagres.id == 0L) behandlingSomSkalLagres.copy(id = idForNyBehandling) else behandlingSomSkalLagres

    dataFraCucumber.behandlinger[behandling.id] = behandling
    return behandling
}

private fun mockVedtaksperiodeService(): VedtaksperiodeService {
    val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    every { vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(any()) } just runs
    return vedtaksperiodeService
}

private fun mockPersonopplysningGrunnlagRepository(behandlingIdTilPersongrunnlag: Map<Long, PersonopplysningGrunnlag>): PersonopplysningGrunnlagRepository {
    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } answers {
        val behandlingsId = firstArg<Long>()
        behandlingIdTilPersongrunnlag[behandlingsId] ?: error("Fant ikke personopplysninggrunnlag for behandling $behandlingsId")
    }
    return personopplysningGrunnlagRepository
}

private fun mockTaskRepositoryWrapper(): TaskRepositoryWrapper {
    val taskRepositoryWrapper = mockk<TaskRepositoryWrapper>()
    every { taskRepositoryWrapper.save(any()) } answers { firstArg() }
    return taskRepositoryWrapper
}

class MockedDateProvider(val mockedDate: LocalDate) : LocalDateProvider {
    override fun now(): LocalDate = this.mockedDate
}

private fun Person.tilPersonInfo(): PersonInfo =
    PersonInfo(
        fødselsdato = fødselsdato,
        navn = navn,
        kjønn = kjønn,
        forelderBarnRelasjon = emptySet(),
        forelderBarnRelasjonMaskert = emptySet(),
        adressebeskyttelseGradering = null,
        bostedsadresser = emptyList(),
        sivilstander = emptyList(),
        opphold = emptyList(),
        statsborgerskap = emptyList(),
        dødsfall = null,
        kontaktinformasjonForDoedsbo = null,
    )

fun opprettEllerHentVedtak(
    dataFraCucumber: BegrunnelseTeksterStepDefinition,
    behandlingId: Long,
): Vedtak {
    val vedtakForBehandling =
        dataFraCucumber.vedtaksliste.find { it.behandling.id == behandlingId }
            ?: lagVedtak(dataFraCucumber.behandlinger[behandlingId]!!)

    if (vedtakForBehandling !in dataFraCucumber.vedtaksliste) {
        dataFraCucumber.vedtaksliste.add(vedtakForBehandling)
    }

    return vedtakForBehandling
}
