package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.common.Utils.midlertidigUtledBehandlingResultatType
import no.nav.familie.ba.sak.common.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.kjedeinndelteAndeler
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.oppdaterBeståendeAndelerMedOffset
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.RestSimulerResultat
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service

@Service
class ØkonomiService(
        private val økonomiKlient: ØkonomiKlient,
        private val vilkårsvurderingService: VilkårsvurderingService,
        private val beregningService: BeregningService,
        private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator,
        private val behandlingService: BehandlingService
) {

    fun oppdaterTilkjentYtelseOgIverksettVedtak(vedtak: Vedtak, saksbehandlerId: String) {

        val oppdatertBehandling = vedtak.behandling
        val utbetalingsoppdrag = genererUtbetalingsoppdrag(vedtak, saksbehandlerId)
        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(oppdatertBehandling, utbetalingsoppdrag)
        iverksettOppdrag(utbetalingsoppdrag = utbetalingsoppdrag)
    }

    /**
     * SOAP integrasjonen støtter ikke full epost som MQ,
     * så vi bruker bare første 8 tegn av saksbehandlers epost for simulering.
     * Denne verdien brukes ikke til noe i simulering.
     */
    fun hentEtterbetalingsbeløp(vedtak: Vedtak): RestSimulerResultat {
        Result.runCatching {
            økonomiKlient.hentEtterbetalingsbeløp(genererUtbetalingsoppdrag(vedtak = vedtak,
                                                                            saksbehandlerId = SikkerhetContext.hentSaksbehandler()
                                                                                    .take(8)))
        }
                .fold(
                        onSuccess = {
                            assertGenerelleSuksessKriterier(it.body)
                            return it.body?.data!!
                        },
                        onFailure = {
                            throw Exception("Henting av etterbetalingsbeløp fra simulering feilet", it)
                        }
                )
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

    private fun genererUtbetalingsoppdrag(vedtak: Vedtak, saksbehandlerId: String): Utbetalingsoppdrag {
        val oppdatertBehandling = vedtak.behandling
        val oppdatertTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(oppdatertBehandling.id)
        val oppdaterteKjeder = kjedeinndelteAndeler(oppdatertTilstand)

        val behandlingResultat =
                if (oppdatertBehandling.erTekniskOpphør()
                    || oppdatertBehandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT)
                    BehandlingResultat.OPPHØRT
                else {
                    // TODO: Midlertidig fiks før støtte for delvis innvilget
                    midlertidigUtledBehandlingResultatType(hentetBehandlingResultat = behandlingService.hent(oppdatertBehandling.id).resultat)
                }

        val erFørsteIverksatteBehandlingPåFagsak =
                beregningService.hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(oppdatertBehandling.fagsak.id).isEmpty()


        return if (erFørsteIverksatteBehandlingPåFagsak) {
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                    saksbehandlerId = saksbehandlerId,
                    vedtak = vedtak,
                    behandlingResultat = behandlingResultat,
                    erFørsteBehandlingPåFagsak = erFørsteIverksatteBehandlingPåFagsak,
                    oppdaterteKjeder = oppdaterteKjeder,
            )
        } else {
            val forrigeBehandling = behandlingService.hentForrigeBehandlingSomErIverksatt(behandling = oppdatertBehandling)
                                    ?: error("Finner ikke forrige behandling ved oppdatering av tilkjent ytelse og iverksetting av vedtak")

            val forrigeTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeBehandling.id)
            // TODO: Her bør det legges til sjekk om personident er endret. Hvis endret bør dette mappes i forrigeTilstand som benyttes videre.
            val forrigeKjeder = kjedeinndelteAndeler(forrigeTilstand)

            if (oppdatertTilstand.isNotEmpty()) {
                oppdaterBeståendeAndelerMedOffset(oppdaterteKjeder = oppdaterteKjeder, forrigeKjeder = forrigeKjeder)
                val tilkjentYtelseMedOppdaterteAndeler = oppdatertTilstand.first().tilkjentYtelse
                beregningService.lagreTilkjentYtelseMedOppdaterteAndeler(tilkjentYtelseMedOppdaterteAndeler)
            }

            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                    saksbehandlerId,
                    vedtak,
                    behandlingResultat,
                    erFørsteIverksatteBehandlingPåFagsak,
                    forrigeKjeder = forrigeKjeder,
                    oppdaterteKjeder = oppdaterteKjeder,
            )
        }
    }
}

