package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsoppdragGenerator
import no.nav.familie.ba.sak.integrasjoner.økonomi.valider
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.kjedeinndelteAndeler
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.oppdaterBeståendeAndelerMedOffset
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class ØkonomiService(
    private val økonomiKlient: ØkonomiKlient,
    private val beregningService: BeregningService,
    private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator,
    private val behandlingService: BehandlingService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val featureToggleService: FeatureToggleService,
) {

    fun oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(vedtak: Vedtak, saksbehandlerId: String) {

        val oppdatertBehandling = vedtak.behandling
        val utbetalingsoppdrag = genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(vedtak, saksbehandlerId)
        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(oppdatertBehandling, utbetalingsoppdrag)
        iverksettOppdrag(utbetalingsoppdrag)
    }

    private fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag) {
        try {
            økonomiKlient.iverksettOppdrag(utbetalingsoppdrag)
        } catch (e: Exception) {
            throw Exception("Iverksetting mot oppdrag feilet", e)
        }
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatus =
        Result.runCatching { økonomiKlient.hentStatus(oppdragId) }
            .fold(
                onSuccess = { return it.data!! },
                onFailure = { throw Exception("Henting av status mot oppdrag feilet", it) }
            )

    @Transactional
    fun genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
        vedtak: Vedtak,
        saksbehandlerId: String,
        erSimulering: Boolean = false,
    ): Utbetalingsoppdrag {
        val oppdatertBehandling = vedtak.behandling
        val oppdatertTilstand =
            beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(oppdatertBehandling.id)

        val oppdaterteKjeder = kjedeinndelteAndeler(oppdatertTilstand)

        val erFørsteIverksatteBehandlingPåFagsak =
            beregningService.hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(oppdatertBehandling.fagsak.id)
                .isEmpty()

        val utbetalingsoppdrag = if (erFørsteIverksatteBehandlingPåFagsak) {
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                erFørsteBehandlingPåFagsak = erFørsteIverksatteBehandlingPåFagsak,
                oppdaterteKjeder = oppdaterteKjeder,
                erSimulering = erSimulering,
            )
        } else {
            val forrigeBehandling =
                behandlingService.hentForrigeBehandlingSomErIverksatt(behandling = oppdatertBehandling)
                    ?: error("Finner ikke forrige behandling ved oppdatering av tilkjent ytelse og iverksetting av vedtak")

            val forrigeTilstand =
                beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(forrigeBehandling.id)
            // TODO: Her bør det legges til sjekk om personident er endret. Hvis endret bør dette mappes i forrigeTilstand som benyttes videre.
            val forrigeKjeder = kjedeinndelteAndeler(forrigeTilstand)

            val sisteOffsetPåFagsak = hentSisteOffsetPåFagsak(behandling = oppdatertBehandling)

            if (oppdatertTilstand.isNotEmpty()) {
                oppdaterBeståendeAndelerMedOffset(oppdaterteKjeder = oppdaterteKjeder, forrigeKjeder = forrigeKjeder)
                val tilkjentYtelseMedOppdaterteAndeler = oppdatertTilstand.first().tilkjentYtelse
                beregningService.lagreTilkjentYtelseMedOppdaterteAndeler(tilkjentYtelseMedOppdaterteAndeler)
            }

            val utbetalingsoppdrag = utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                erFørsteBehandlingPåFagsak = erFørsteIverksatteBehandlingPåFagsak,
                forrigeKjeder = forrigeKjeder,
                sisteOffsetPåFagsak = sisteOffsetPåFagsak,
                oppdaterteKjeder = oppdaterteKjeder,
                erSimulering = erSimulering,
                endretMigreringsDato = beregnOmMigreringsDatoErEndret(
                    vedtak.behandling,
                    forrigeTilstand.minByOrNull { it.stønadFom }?.stønadFom
                ),
            )

            if (!erSimulering && (
                oppdatertBehandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT || behandlingService.hent(
                        oppdatertBehandling.id
                    ).resultat == BehandlingResultat.OPPHØRT
                )
            )
                validerOpphørsoppdrag(utbetalingsoppdrag)

            utbetalingsoppdrag
        }

        return utbetalingsoppdrag.also { it.valider(vedtak.behandling.resultat) }
    }

    fun hentSisteOffsetPåFagsak(behandling: Behandling): Int? =
        behandlingService.hentForrigeBehandlingSomErIverksatt(behandling = behandling)
            ?.let { forrigeIverksattBehandling ->

                beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(forrigeIverksattBehandling.id)
                    .takeIf { it.isNotEmpty() }
                    ?.let { andelerTilkjentYtelse ->
                        andelerTilkjentYtelse.maxByOrNull { it.periodeOffset!! }?.periodeOffset?.toInt()
                    }
                    ?: hentSisteOffsetPåFagsak(forrigeIverksattBehandling)
            }

    private fun validerOpphørsoppdrag(utbetalingsoppdrag: Utbetalingsoppdrag) {
        val (opphørsperioder, annet) = utbetalingsoppdrag.utbetalingsperiode.partition { it.opphør != null }
        if (annet.size > opphørsperioder.size)
            error("Generert utbetalingsoppdrag for opphør inneholder flere nye oppdragsperioder enn det finnes opphørsperioder.")
        if (opphørsperioder.isEmpty())
            error("Generert utbetalingsoppdrag for opphør mangler opphørsperioder.")
    }

    private fun beregnOmMigreringsDatoErEndret(behandling: Behandling, forrigeTilstandFraDato: YearMonth?): YearMonth? {
        if (!featureToggleService.isEnabled(FeatureToggleConfig.MIGRERING_NYTT_REVURDERINGSDATO)) {
            return null
        }

        val erMigrertSak =
            behandlingService.hentBehandlinger(behandling.fagsak.id)
                .any { it.type == BehandlingType.MIGRERING_FRA_INFOTRYGD }

        if (!erMigrertSak) {
            return null
        }

        val nyttTilstandFraDato = vilkårsvurderingRepository
            .findByBehandlingAndAktiv(behandling.id)
            ?.personResultater
            ?.flatMap { it.vilkårResultater }
            ?.filter { it.periodeFom != null }
            ?.filter { it.vilkårType != Vilkår.UNDER_18_ÅR && it.vilkårType != Vilkår.GIFT_PARTNERSKAP }
            ?.minByOrNull { it.periodeFom!! }
            ?.periodeFom
            ?.toYearMonth()
            ?.plusMonths(1)

        return if (forrigeTilstandFraDato != null && forrigeTilstandFraDato.isAfter(nyttTilstandFraDato)) {
            nyttTilstandFraDato
        } else {
            null
        }
    }
}
