package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtAlleOpprettedeEndringerErUtfylt
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtEndringerErTilknyttetAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerPeriodeInnenforTilkjentytelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerÅrsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.steg.BehandlingsresultatSteg
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.slf4j.LoggerFactory
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
        val behandling = tilkjentYtelse.behandling
        val vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandlingId = behandling.id)
        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(tilkjentYtelse.behandling)
                ?: throw Feil("Kan ikke kjøre finnmarkstillegg behandling dersom det ikke finnes en tidligere iverksatt behandling")

        val andelerNåværendeBehandling = tilkjentYtelse.andelerTilkjentYtelse.toList()
        val andelerForrigeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeBehandling.id)

        BehandlingsresultatValideringUtils.validerFinnmarkstilleggBehandling(
            andelerNåværendeBehandling = andelerNåværendeBehandling,
            andelerForrigeBehandling = andelerForrigeBehandling,
            vilkårsvurdering = vilkårsvurdering,
            inneværendeMåned = YearMonth.now(clockProvider.get()),
        )
    }

    fun validerSvalbardtilleggBehandling(tilkjentYtelse: TilkjentYtelse) {
        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(tilkjentYtelse.behandling)
                ?: throw Feil("Kan ikke kjøre svalbardtillegg behandling dersom det ikke finnes en tidligere iverksatt behandling")

        val andelerNåværendeBehandling = tilkjentYtelse.andelerTilkjentYtelse.toList()
        val andelerFraForrigeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeBehandling.id)

        BehandlingsresultatValideringUtils.validerSvalbardtilleggBehandling(
            andelerNåværendeBehandling = andelerNåværendeBehandling,
            andelerForrigeBehandling = andelerFraForrigeBehandling,
            inneværendeMåned = YearMonth.now(clockProvider.get()),
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

    companion object {
        private val logger = LoggerFactory.getLogger(BehandlingsresultatStegValideringService::class.java)
    }
}
