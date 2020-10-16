package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.common.RessursUtils.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.common.Utils.midlertidigUtledBehandlingResultatType
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.kjedeinndelteAndeler
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.oppdaterBeståendeAndelerMedOffset
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service

@Service
class ØkonomiService(
        private val økonomiKlient: ØkonomiKlient,
        private val behandlingResultatService: BehandlingResultatService,
        private val vedtakService: VedtakService,
        private val beregningService: BeregningService,
        private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator,
        private val behandlingService: BehandlingService
) {

    fun oppdaterTilkjentYtelseOgIverksettVedtak(vedtakId: Long, saksbehandlerId: String) {
        val vedtak = vedtakService.hent(vedtakId)

        val oppdatertBehandling = vedtak.behandling
        val oppdatertTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(oppdatertBehandling.id)
        val oppdaterteKjeder = kjedeinndelteAndeler(oppdatertTilstand)

        val behandlingResultatType =
                if (oppdatertBehandling.type == BehandlingType.TEKNISK_OPPHØR
                    || oppdatertBehandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT)
                    BehandlingResultatType.OPPHØRT
                else {
                    // TODO: Midlertidig fiks før støtte for delvis innvilget
                    midlertidigUtledBehandlingResultatType(
                            hentetBehandlingResultatType = behandlingResultatService.hentBehandlingResultatTypeFraBehandling(
                                    behandling = oppdatertBehandling))
                    //behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = oppdatertBehandling.id)
                }

        val erFørsteIverksatteBehandlingPåFagsak =
                beregningService.hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(oppdatertBehandling.fagsak.id).isEmpty()

        val utbetalingsoppdrag: Utbetalingsoppdrag =
                if (erFørsteIverksatteBehandlingPåFagsak) {
                    utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                            saksbehandlerId,
                            vedtak,
                            behandlingResultatType,
                            erFørsteIverksatteBehandlingPåFagsak,
                            oppdaterteKjeder = oppdaterteKjeder)
                } else {
                    val forrigeBehandling = behandlingService.hentForrigeFerdigstilteBehandling(fagsakId = oppdatertBehandling.fagsak.id, behandlingFørFølgende = oppdatertBehandling)
                                            ?: error("Finner ikke forrige behandling ved oppdatering av tilkjent ytelse og iverksetting av vedtak")

                    val forrigeTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeBehandling.id)
                    // TODO: Her bør det legges til sjekk om personident er endret. Hvis endret bør dette mappes i forrigeTilstand som benyttes videre.
                    val forrigeKjeder = kjedeinndelteAndeler(forrigeTilstand)

                    oppdaterBeståendeAndelerMedOffset(oppdaterteKjeder = oppdaterteKjeder, forrigeKjeder = forrigeKjeder)
                    beregningService.lagreTilkjentYtelseMedOppdaterteAndeler(oppdatertTilstand.first().tilkjentYtelse)

                    utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                            saksbehandlerId,
                            vedtak,
                            behandlingResultatType,
                            erFørsteIverksatteBehandlingPåFagsak,
                            forrigeKjeder = forrigeKjeder,
                            oppdaterteKjeder = oppdaterteKjeder)
                }

        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(oppdatertBehandling, utbetalingsoppdrag)
        iverksettOppdrag(utbetalingsoppdrag)
    }

    private fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag) {
        Result.runCatching { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) }
                .fold(
                        onSuccess = {
                            assertGenerelleSuksessKriterier(it.body)
                        },
                        onFailure = {
                            throw Exception("Iverksetting mot oppdrag feilet", it)
                        }
                )
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatus {
        Result.runCatching { økonomiKlient.hentStatus(oppdragId) }
                .fold(
                        onSuccess = {
                            assertGenerelleSuksessKriterier(it.body)
                            return it.body?.data!!
                        },
                        onFailure = {
                            throw Exception("Henting av status mot oppdrag feilet", it)
                        }
                )
    }
}

