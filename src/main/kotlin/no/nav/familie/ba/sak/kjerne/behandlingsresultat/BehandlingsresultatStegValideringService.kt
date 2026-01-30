package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatValideringUtils.andelerMedYtelseTypeErInnvilgetInneværendeMånedOgToMånederFramITid
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatValideringUtils.erEndringIUtbetalingUtenomYtelseType
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatValideringUtils.rekjørNesteMånedHvisYtelseTypeErInnvilgetToMånederFramITid
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder
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
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.steg.BehandlingsresultatSteg
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.tilPerioder
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
    private val clockProvider: ClockProvider,
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
                    "Det skal ikke skje for endre migreringsdatobehandlinger." +
                    "Endringer må gjøres i en separat behandling.",
                "Det finnes endringer i behandlingen som har økonomisk konsekvens for bruker." +
                    "Det skal ikke skje for endre migreringsdatobehandlinger." +
                    "Endringer må gjøres i en separat behandling.",
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
}
