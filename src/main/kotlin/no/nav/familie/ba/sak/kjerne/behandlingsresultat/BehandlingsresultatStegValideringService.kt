package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.SatsendringEøsKjøringService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatValideringUtils.andelerMedYtelseTypeErInnvilgetInneværendeMånedOgToMånederFramITid
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatValideringUtils.erEndringIUtbetalingUtenomYtelseType
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatValideringUtils.rekjørNesteMånedHvisYtelseTypeErInnvilgetToMånederFramITid
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.FINNMARKSTILLEGG
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.SVALBARDTILLEGG
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtAlleOpprettedeEndringerErUtfylt
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtEndringerErTilknyttetAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerPeriodeInnenforTilkjentytelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerÅrsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.BehandlingsresultatSteg
import no.nav.familie.ba.sak.kjerne.strengtfortrolig.StrengtFortroligService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerUtenNullMed
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.tidslinje.verdier
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class BehandlingsresultatStegValideringService(
    private val beregningService: BeregningService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vilkårService: VilkårService,
    private val kompetanseRepository: KompetanseRepository,
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
    private val valutakursRepository: ValutakursRepository,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val strengtFortroligService: StrengtFortroligService,
    private val persongrunnlagService: PersongrunnlagService,
    private val clockProvider: ClockProvider,
    private val satsendringEøsKjøringService: SatsendringEøsKjøringService,
) {
    fun validerIngenEndringIUtbetalingEtterMigreringsdatoenTilForrigeIverksatteBehandling(behandling: Behandling) {
        if (behandling.status == BehandlingStatus.AVSLUTTET) return

        val endringIUtbetalingTidslinje =
            beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomiTidslinje(behandling)

        val migreringsdatoForrigeIverksatteBehandling =
            beregningService
                .hentAndelerFraForrigeIverksattebehandling(behandling)
                .minOfOrNull { it.stønadFom }
                ?: TIDENES_ENDE.toYearMonth()

        endringIUtbetalingTidslinje.kastFeilVedEndringEtter(
            migreringsdatoForrigeIverksatteBehandling = migreringsdatoForrigeIverksatteBehandling,
            behandling = behandling,
        )
    }

    private fun Tidslinje<Boolean>.kastFeilVedEndringEtter(
        migreringsdatoForrigeIverksatteBehandling: YearMonth,
        behandling: Behandling,
    ) {
        val endringIUtbetalingEtterDato =
            tilPerioder()
                .filter { it.tom == null || it.tom!!.toYearMonth().isSameOrAfter(migreringsdatoForrigeIverksatteBehandling) }

        val erEndringIUtbetalingEtterMigreringsdato = endringIUtbetalingEtterDato.any { it.verdi == true }

        if (erEndringIUtbetalingEtterMigreringsdato) {
            BehandlingsresultatSteg.logger.warn("Feil i behandling $behandling.\n\nEndring i måned ${endringIUtbetalingEtterDato.first { it.verdi == true }.fom?.toYearMonth()}.")
            throw FunksjonellFeil(
                "Det finnes endringer i behandlingen som har økonomisk konsekvens for bruker." +
                    " Det skal ikke skje for endre migreringsdatobehandlinger." +
                    " Endringer må gjøres i en separat behandling.",
                "Det finnes endringer i behandlingen som har økonomisk konsekvens for bruker." +
                    " Det skal ikke skje for endre migreringsdatobehandlinger." +
                    " Endringer må gjøres i en separat behandling. Perioder med endring: ${endringIUtbetalingEtterDato.filter { it.verdi == true }.map { "(${it.fom?.toYearMonth()} - ${it.tom?.toYearMonth()})" }}.",
            )
        }
    }

    fun validerSatsendring(tilkjentYtelse: TilkjentYtelse) {
        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(tilkjentYtelse.behandling)
                ?: throw FunksjonellFeil("Kan ikke kjøre satsendring når det ikke finnes en tidligere behandling på fagsaken")
        val andelerFraForrigeBehandling =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeBehandling.id)

        validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
            andelerFraForrigeBehandling = andelerFraForrigeBehandling,
            andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.toList(),
        )
    }

    fun validerFinnmarkstilleggBehandling(tilkjentYtelse: TilkjentYtelse) {
        validerFinnmarksOgSvalbardtilleggBehandling(
            tilkjentYtelse = tilkjentYtelse,
            ytelseType = FINNMARKSTILLEGG,
        )
    }

    fun validerSvalbardtilleggBehandling(tilkjentYtelse: TilkjentYtelse) {
        validerFinnmarksOgSvalbardtilleggBehandling(
            tilkjentYtelse = tilkjentYtelse,
            ytelseType = SVALBARDTILLEGG,
        )
    }

    private fun validerFinnmarksOgSvalbardtilleggBehandling(
        tilkjentYtelse: TilkjentYtelse,
        ytelseType: YtelseType,
    ) {
        val behandling = tilkjentYtelse.behandling

        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling)
                ?: throw Feil("Kan ikke kjøre ${behandling.opprettetÅrsak.visningsnavn}-behandling dersom det ikke finnes en tidligere iverksatt behandling")

        val andelerNåværendeBehandling = tilkjentYtelse.andelerTilkjentYtelse.toList()
        val andelerForrigeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeBehandling.id)
        val inneværendeMåned = YearMonth.now(clockProvider.get())
        val ytelseTypeFormatert = ytelseType.toString().storForbokstav()

        val erEndringITilleggUtenomYtelseType =
            erEndringIUtbetalingUtenomYtelseType(
                andelerNåværendeBehandling = andelerNåværendeBehandling,
                andelerForrigeBehandling = andelerForrigeBehandling,
                ytelseType = ytelseType,
            )

        if (erEndringITilleggUtenomYtelseType) {
            val motsattTilleggFormatert = (if (ytelseType == FINNMARKSTILLEGG) SVALBARDTILLEGG else FINNMARKSTILLEGG).toString().storForbokstav()
            val begrunnelse =
                "$ytelseTypeFormatert kan ikke behandles automatisk som følge av adresseendring.\n" +
                    "Endring av $ytelseTypeFormatert fører også til endring av $motsattTilleggFormatert.\n" +
                    "Endring av $ytelseTypeFormatert og $motsattTilleggFormatert må håndteres manuelt."

            throw AutovedtakMåBehandlesManueltFeil(begrunnelse)
        }

        val andelerMedYtelseTypeErInnvilgetInneværendeMånedOgToMånederFramITid =
            andelerMedYtelseTypeErInnvilgetInneværendeMånedOgToMånederFramITid(
                andelerNåværendeBehandling = andelerNåværendeBehandling,
                andelerForrigeBehandling = andelerForrigeBehandling,
                ytelseType = ytelseType,
                inneværendeMåned = inneværendeMåned,
            )

        if (andelerMedYtelseTypeErInnvilgetInneværendeMånedOgToMånederFramITid) {
            val begrunnelse =
                "$ytelseTypeFormatert kan ikke behandles automatisk som følge av adresseendring.\n" +
                    "Automatisk behandling fører til innvilgelse av $ytelseTypeFormatert mer enn én måned fram i tid.\n" +
                    "Endring av $ytelseTypeFormatert må håndteres manuelt."

            throw AutovedtakMåBehandlesManueltFeil(begrunnelse)
        }

        rekjørNesteMånedHvisYtelseTypeErInnvilgetToMånederFramITid(
            andelerNåværendeBehandling = andelerNåværendeBehandling,
            andelerForrigeBehandling = andelerForrigeBehandling,
            ytelseType = ytelseType,
            inneværendeMåned = inneværendeMåned,
        )
    }

    fun validerAtUtenlandskPeriodebeløpOgValutakursErUtfylt(behandling: Behandling) {
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId = behandling.id)
        val valutakurser by lazy { valutakursRepository.finnFraBehandlingId(behandlingId = behandling.id) }

        if (utenlandskePeriodebeløp.any { !it.erObligatoriskeFelterSatt() } || valutakurser.any { !it.erObligatoriskeFelterSatt() }) {
            throw FunksjonellFeil("Kan ikke fullføre behandlingsresultat-steg før utbetalt i det andre landet og valutakurs er fylt ut for alle barn og perioder")
        }
    }

    fun validerKompetanse(behandlingId: Long) {
        val kompetanser = kompetanseRepository.finnFraBehandlingId(behandlingId)

        BehandlingsresultatValideringUtils.validerKompetanse(kompetanser)
    }

    fun validerEndredeUtbetalingsandeler(
        tilkjentYtelse: TilkjentYtelse,
    ) {
        val endretUtbetalingAndelMedAndelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(tilkjentYtelse.behandling.id)

        val vilkårsvurdering = vilkårService.hentVilkårsvurdering(tilkjentYtelse.behandling.id)
        val endretUtbetalingAndeler = endretUtbetalingAndelMedAndelerTilkjentYtelse.map { it.endretUtbetalingAndel }

        validerAtAlleOpprettedeEndringerErUtfylt(endretUtbetalingAndeler)
        validerAtEndringerErTilknyttetAndelTilkjentYtelse(endretUtbetalingAndelMedAndelerTilkjentYtelse)
        validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(endretUtbetalingAndelMedAndelerTilkjentYtelse)
        validerPeriodeInnenforTilkjentytelse(endretUtbetalingAndeler, tilkjentYtelse.andelerTilkjentYtelse)
        validerÅrsak(endretUtbetalingAndeler, vilkårsvurdering)
    }

    fun validerIngenEndringTilbakeITid(
        tilkjentYtelse: TilkjentYtelse,
    ) {
        val andelerDenneBehandlingen = tilkjentYtelse.andelerTilkjentYtelse
        val andelerForrigeBehandling = beregningService.hentAndelerFraForrigeVedtatteBehandling(tilkjentYtelse.behandling)

        BehandlingsresultatValideringUtils.validerIngenEndringTilbakeITid(
            andelerDenneBehandlingen = andelerDenneBehandlingen,
            andelerForrigeBehandling = andelerForrigeBehandling,
            nåMåned = YearMonth.now(clockProvider.get()),
        )
    }

    fun validerSatsErUendret(
        tilkjentYtelse: TilkjentYtelse,
    ) {
        val andelerDenneBehandlingen = tilkjentYtelse.andelerTilkjentYtelse
        val andelerForrigeBehandling = beregningService.hentAndelerFraForrigeVedtatteBehandling(tilkjentYtelse.behandling)

        BehandlingsresultatValideringUtils.validerSatsErUendret(
            andelerDenneBehandlingen = andelerDenneBehandlingen,
            andelerForrigeBehandling = andelerForrigeBehandling,
        )
    }

    fun validerAtMinstEttUtenlandskPeriodebeløpErEndret(behandling: Behandling) {
        val utbetalingsland = satsendringEøsKjøringService.hentSatsendringEøsKjøring(behandling.id).utbetalingsland
        if (finnEndredeUtenlandskePeriodebeløp(behandling, utbetalingsland).isEmpty()) {
            throw Feil("Forventet endring i minst ett utenlandsk periodebeløp for satsendring EØS i behandling ${behandling.id} i fagsak ${behandling.fagsak.id}.")
        }
    }

    fun validerIngenEndringIAndelerFørSatsendringstidspunkt(tilkjentYtelse: TilkjentYtelse) {
        val satsTidspunkt = satsendringEøsKjøringService.hentSatsendringEøsKjøring(tilkjentYtelse.behandling.id).satsTidspunkt
        val andelerForrigeBehandling = beregningService.hentAndelerFraForrigeVedtatteBehandling(tilkjentYtelse.behandling)
        BehandlingsresultatValideringUtils.validerIngenEndringFørMåned(
            andelerDenneBehandlingen = tilkjentYtelse.andelerTilkjentYtelse,
            andelerForrigeBehandling = andelerForrigeBehandling,
            grensemåned = satsTidspunkt,
        )
    }

    private fun finnEndredeUtenlandskePeriodebeløp(
        behandling: Behandling,
        utbetalingsland: String,
    ): List<UtenlandskPeriodebeløp> {
        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
                ?: throw Feil("Kan ikke kjøre EØS-satsendring uten en tidligere vedtatt behandling.")

        val upbPerBarnInneværendeBehandling =
            utenlandskPeriodebeløpRepository
                .finnFraBehandlingId(behandling.id)
                .filter { it.utbetalingsland == utbetalingsland }
                .tilSeparateTidslinjerForBarna()

        val upbPerBarnForrigeBehandling =
            utenlandskPeriodebeløpRepository
                .finnFraBehandlingId(forrigeBehandling.id)
                .filter { it.utbetalingsland == utbetalingsland }
                .tilSeparateTidslinjerForBarna()

        return upbPerBarnInneværendeBehandling
            .outerJoin(upbPerBarnForrigeBehandling) { nyUpb, gammelUpb ->
                nyUpb.takeIf { compareValues(nyUpb?.kalkulertMånedligBeløp, gammelUpb?.kalkulertMånedligBeløp) != 0 }
            }.flatMap { it.value.tilPerioderIkkeNull().verdier() }
    }

    fun validerSekundærlandKompetanse(
        behandlingId: Long,
    ) {
        val kompetansePerBarn = kompetanseRepository.finnFraBehandlingId(behandlingId).tilSeparateTidslinjerForBarna()
        val utenlandskPeriodebeløpPerBarn = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId = behandlingId).tilSeparateTidslinjerForBarna()
        val valutakursPerBarn = valutakursRepository.finnFraBehandlingId(behandlingId = behandlingId).tilSeparateTidslinjerForBarna()

        val dagensDato = YearMonth.now(clockProvider.get())

        kompetansePerBarn
            .outerJoin(utenlandskPeriodebeløpPerBarn, valutakursPerBarn) { kompetanse, utenlandskPeriodebeløp, valutakurs ->
                kompetanse?.resultat == KompetanseResultat.NORGE_ER_SEKUNDÆRLAND && (utenlandskPeriodebeløp == null || valutakurs == null)
            }.forEach { (_, perioderMedSekundærlandKompetanseUtenUtenlandskBeløpEllerValutakurs) ->
                perioderMedSekundærlandKompetanseUtenUtenlandskBeløpEllerValutakurs.tilPerioder().firstOrNull { it.verdi == true }?.let { periode ->
                    val periodeMedManglendeBeløpEllerKurs = periode.fom?.toYearMonth() ?: return@forEach
                    val meldingTilSaksbehandler =
                        if (periodeMedManglendeBeløpEllerKurs.isSameOrBefore(dagensDato)) {
                            """
                            For perioden ${periodeMedManglendeBeløpEllerKurs.tilMånedÅr()} finnes det sekundærland kompetanse som enda ikke har fått utenlandskperiode beløp eller valutakurs.
                            Gå tilbake til vilkårsvurderingen og trykk 'Neste' for å hente inn manglende utenlandskperiode beløp og valutakurs.
                            """.trimIndent()
                        } else {
                            """
                            For perioden ${periodeMedManglendeBeløpEllerKurs.tilMånedÅr()} finnes det sekundærland kompetanse med endret utbetaling i det andre landet en måned som er lengre fram i tid enn inneværende måned.
                            Det er ikke mulig å hente inn valutakurs for perioder fram i tid, og du må derfor vente til ${periodeMedManglendeBeløpEllerKurs.tilMånedÅr()} før du kan fortsette behandlingen.
                            """.trimIndent()
                        }
                    throw FunksjonellFeil(melding = meldingTilSaksbehandler)
                }
            }
    }

    fun validerIngenEndringIUtbetalingIPerioderMedSkjermedeBarn(behandling: Behandling) {
        val skjermedeBarnSaksbehandlerIkkeHarTilgangTil = strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(behandling.fagsak)
        if (skjermedeBarnSaksbehandlerIkkeHarTilgangTil.isEmpty()) return

        val forrigeVedtatteBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) ?: return
        val forrigeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeVedtatteBehandling.id)
        val nåværendeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        val forrigeAndelerForAndrePersoner = forrigeAndeler.filterNot { it.aktør.aktivFødselsnummer() in skjermedeBarnSaksbehandlerIkkeHarTilgangTil }
        val nåværendeAndelerForAndrePersoner = nåværendeAndeler.filterNot { it.aktør.aktivFødselsnummer() in skjermedeBarnSaksbehandlerIkkeHarTilgangTil }

        val endringIUtbetalingForAndrePersonerTidslinje =
            EndringIUtbetalingUtil.lagEndringIUtbetalingTidslinje(
                nåværendeAndeler = nåværendeAndelerForAndrePersoner,
                forrigeAndeler = forrigeAndelerForAndrePersoner,
            )

        val skjermedeBarnAndelerIForrigeBehandling = forrigeAndeler.filter { it.aktør.aktivFødselsnummer() in skjermedeBarnSaksbehandlerIkkeHarTilgangTil }.groupBy { it.aktør }
        val skjermedeBarnAndelerINåværendeBehandling = nåværendeAndeler.filter { it.aktør.aktivFødselsnummer() in skjermedeBarnSaksbehandlerIkkeHarTilgangTil }.groupBy { it.aktør }

        val skjermedeBarn = (skjermedeBarnAndelerIForrigeBehandling.keys + skjermedeBarnAndelerINåværendeBehandling.keys).distinct()

        skjermedeBarn.forEach { skjermetBarn ->
            val skjermetBarnsAndelerForrige = skjermedeBarnAndelerIForrigeBehandling[skjermetBarn].orEmpty()
            val skjermetBarnsAndelerNåværende = skjermedeBarnAndelerINåværendeBehandling[skjermetBarn].orEmpty()

            val endringIUtbetalingForSkjermetBarnTidslinje =
                EndringIUtbetalingUtil.lagEndringIUtbetalingTidslinje(
                    nåværendeAndeler = skjermetBarnsAndelerNåværende,
                    forrigeAndeler = skjermetBarnsAndelerForrige,
                )

            val utbetalingsTidslinjeForSkjermetBarn = lagHarUtbetalingTidslinje(skjermetBarnsAndelerForrige)

            val endringIUtbetalingForAndrePersonerSamtidigSomUtbetalingForSkjermetBarnTidslinje =
                endringIUtbetalingForAndrePersonerTidslinje.kombinerUtenNullMed(utbetalingsTidslinjeForSkjermetBarn) { erEndring, _ ->
                    erEndring
                }

            val finnesEndringIUtbetalingForAndrePersonerSamtidigSomUtbetalingForSkjermetBarn = endringIUtbetalingForAndrePersonerSamtidigSomUtbetalingForSkjermetBarnTidslinje.tilPerioder().any { it.verdi == true }
            val finnesEndringISkjermetBarnsAndeler = endringIUtbetalingForSkjermetBarnTidslinje.tilPerioder().any { it.verdi == true }

            if (finnesEndringIUtbetalingForAndrePersonerSamtidigSomUtbetalingForSkjermetBarn || finnesEndringISkjermetBarnsAndeler) {
                secureLogger.warn(
                    "Endring i utbetaling oppdaget i periode hvor skjermet barn=${skjermetBarn.aktørId} har hatt andeler. " +
                        "Behandling=${behandling.id}, fagsak=${behandling.fagsak.id}, saksbehandler=${SikkerhetContext.hentSaksbehandler()}.",
                )
                throw FunksjonellFeil(
                    melding =
                        "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} har endret utbetaling for andre personer " +
                            "i en periode hvor et skjermet barn har hatt andeler på behandling=${behandling.id}. " +
                            "Behandlingen må overføres til enhet 2103 (Vikafossen) og behandles der.",
                    frontendFeilmelding =
                        "Det er gjort endringer i utbetalingen i en periode hvor et skjermet barn har hatt utbetaling. " +
                            "Behandlingen må overføres til enhet 2103 (Vikafossen) og behandles der.",
                )
            }
        }
    }

    fun validerFalskIdentitetBehandling(tilkjentYtelse: TilkjentYtelse) {
        val andelerDenneBehandlingen = tilkjentYtelse.andelerTilkjentYtelse.tilTidslinjerPerAktørOgType()
        val andelerForrigeBehandling = beregningService.hentAndelerFraForrigeVedtatteBehandling(tilkjentYtelse.behandling).tilTidslinjerPerAktørOgType()

        andelerDenneBehandlingen.outerJoin(andelerForrigeBehandling) { andelDenneBehandling, andelForrigeBehandling ->
            // Det finnes andeler i denne behandlingen som ikke fantes i forrige behandling
            if (andelDenneBehandling != null && andelForrigeBehandling == null) {
                throw FunksjonellFeil("Det finnes nye andeler i behandling. Kan ikke innvilge nye andeler i 'Falsk identitet'-behandling.")
            }
        }
    }

    fun validerAtAlleBarnMedEksisterendeAndelerFraForrigeIverksatteBehandlingErMed(behandling: Behandling) {
        val forrigeIverksatteBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling) ?: return

        val aktørerMedAndelerIForrigeIverksatteBehandling =
            andelTilkjentYtelseRepository
                .finnAndelerTilkjentYtelseForBehandling(forrigeIverksatteBehandling.id)
                .map { it.aktør }
                .toSet()

        val aktørerIDenneBehandlingen =
            persongrunnlagService
                .hentSøkerOgBarnPåBehandlingThrows(behandling.id)
                .map { it.aktør }
                .toSet()

        val barnMedAndelerSomManglerIBehandlingenFraForrigeBehandling =
            aktørerMedAndelerIForrigeIverksatteBehandling - aktørerIDenneBehandlingen

        if (barnMedAndelerSomManglerIBehandlingenFraForrigeBehandling.isNotEmpty()) {
            secureLogger.warn(
                "Behandling ${behandling.id} mangler ${barnMedAndelerSomManglerIBehandlingenFraForrigeBehandling.size} barn med barnetrygd " +
                    "fra forrige iverksatte behandling ${forrigeIverksatteBehandling.id}: " +
                    "${barnMedAndelerSomManglerIBehandlingenFraForrigeBehandling.map { it.aktivFødselsnummer() }}.",
            )
            throw FunksjonellFeil(
                melding =
                    "Behandlingen mangler barn som har barnetrygd i forrige iverksatte behandling. " +
                        "Antall barn som mangler: ${barnMedAndelerSomManglerIBehandlingenFraForrigeBehandling.size}.",
                frontendFeilmelding =
                    "Det finnes barn med løpende barnetrygd som ikke er med i behandlingen. " +
                        "Du må legge til alle brukers barn i behandlingen.",
            )
        }
    }

    private fun lagHarUtbetalingTidslinje(andelerForBarn: List<AndelTilkjentYtelse>): Tidslinje<Boolean> =
        andelerForBarn
            .groupBy { it.type }
            .map { (_, andelerForType) ->
                andelerForType
                    .map {
                        Periode(
                            verdi = true,
                            fom = it.stønadFom.førsteDagIInneværendeMåned(),
                            tom = it.stønadTom.sisteDagIInneværendeMåned(),
                        )
                    }.tilTidslinje()
            }.kombiner { it.any() }
}
