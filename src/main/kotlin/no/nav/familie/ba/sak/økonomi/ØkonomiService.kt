package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.common.RessursUtils.assertGenerelleSuksessKriterier
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ØkonomiService(
        private val økonomiKlient: ØkonomiKlient,
        private val behandlingService: BehandlingService,
        private val behandlingResultatService: BehandlingResultatService,
        private val vedtakService: VedtakService,
        private val beregningService: BeregningService,
        private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator
) {

    fun oppdaterTilkjentYtelseOgIverksettVedtak(vedtakId: Long, saksbehandlerId: String) {
        val vedtak = vedtakService.hent(vedtakId)

        val oppdatertBehandling = vedtak.behandling
        val oppdatertTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(oppdatertBehandling.id)
        val oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(oppdatertTilstand)

        val behandlingResultatType =
                if (oppdatertBehandling.type == BehandlingType.TEKNISK_OPPHØR
                    || oppdatertBehandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT)
                    BehandlingResultatType.OPPHØRT
                else {
                    //behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = oppdatertBehandling.id)
                    // TODO: Tilpasset fastsettelse av BehandlingResultatType inntil støtte for delvis innvilgelse.
                    //  Fastsettelse nedenfor løser generering av utbetalingsoppdrag til økonomi, men det vil fortsatt se rart ut
                    //  frontend og i database vil det bli satt opphørsdato på TilkjentYtelse-nivå frem til støtte for delvis.
                    // (settes i populerTilkjentYtelse i BeregningService)
                    val hentetBehandlingResultatType =
                            behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = oppdatertBehandling.id)
                    if (hentetBehandlingResultatType == BehandlingResultatType.OPPHØRT && oppdatertTilstand.isNotEmpty()) {
                        BehandlingResultatType.DELVIS_INNVILGET
                    } else hentetBehandlingResultatType
                }

        val erFørsteBehandlingPåFagsak = behandlingService.hentBehandlinger(oppdatertBehandling.fagsak.id).size == 1

        val utbetalingsoppdrag: Utbetalingsoppdrag =
                if (erFørsteBehandlingPåFagsak) {
                    utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                            saksbehandlerId,
                            vedtak,
                            behandlingResultatType,
                            erFørsteBehandlingPåFagsak,
                            oppdaterteKjeder = oppdaterteKjeder)
                } else {
                    val forrigeBehandling = vedtakService.hent(vedtak.forrigeVedtakId!!).behandling
                    val forrigeTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeBehandling.id)
                    val forrigeKjeder = ØkonomiUtils.kjedeinndelteAndeler(forrigeTilstand)

                    utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                            saksbehandlerId,
                            vedtak,
                            behandlingResultatType,
                            erFørsteBehandlingPåFagsak,
                            forrigeKjeder = forrigeKjeder,
                            oppdaterteKjeder = oppdaterteKjeder)
                }

        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(oppdatertBehandling, utbetalingsoppdrag)
        iverksettOppdrag(oppdatertBehandling.id, utbetalingsoppdrag)
    }

    private fun iverksettOppdrag(behandlingsId: Long,
                                 utbetalingsoppdrag: Utbetalingsoppdrag) {
        Result.runCatching { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) }
                .fold(
                        onSuccess = {
                            assertGenerelleSuksessKriterier(it.body)

                            behandlingService.oppdaterStatusPåBehandling(behandlingsId, BehandlingStatus.SENDT_TIL_IVERKSETTING)
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

