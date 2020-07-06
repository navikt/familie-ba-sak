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

@Service
class ØkonomiService(
        private val økonomiKlient: ØkonomiKlient,
        private val behandlingService: BehandlingService,
        private val behandlingResultatService: BehandlingResultatService,
        private val vedtakService: VedtakService,
        private val beregningService: BeregningService,
        private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator
) {

    fun separerINyeOgOpphørteAndelerMotØkonomi(behandlingId: Long,
                                               forrigeBehandlingId: Long): Pair<List<AndelTilkjentYtelse>, List<AndelTilkjentYtelse>> {
        val nåværendeTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeBehandlingId).toSet()
        val oppdatertTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(behandlingId).toSet()
        /*
        val intersection = nåværendeTilstand.intersectAndeler(oppdatertTilstand)
        // TODO: Hva er mest intuitivt å lese? Dette blir dobbelt for-loop x 2, men intersect blir også dobbelt for-loop + filter på begge
         */
        val andelerSomOpphøres = nåværendeTilstand.subtractAndeler(oppdatertTilstand).toList()
        val andelerSomErNye = oppdatertTilstand.subtractAndeler(nåværendeTilstand).toList()
        return Pair(andelerSomErNye, andelerSomOpphøres)
    }

    fun oppdaterTilkjentYtelseOgIverksettVedtak(vedtakId: Long, saksbehandlerId: String) {
        // TODO: https://github.com/navikt/familie-ba-sak/blob/4c78bce83387001c952bae8452b75ace1e014b61/src/main/kotlin/no/nav/familie/ba/sak/%C3%B8konomi/%C3%98konomiService.kt#L25 . Dobbeltsjekk at det er greit å fjerne parameter behandlingsId
        val vedtak = vedtakService.hent(vedtakId)
        val behandlingResultatType =
                if (vedtak.behandling.type == BehandlingType.TEKNISK_OPPHØR
                    || vedtak.behandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT)
                    BehandlingResultatType.OPPHØRT
                else behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = vedtak.behandling.id)

        val erFørsteBehandlingPåFagsak = behandlingService.hentBehandlinger(vedtak.behandling.fagsak.id).size == 1

        val andelerTilkjentYtelse = if (!erFørsteBehandlingPåFagsak) {
            // TODO: Må sørge for at man når siste behandling er opphør ikke klarer å finne noen andeler på behandling, slik at subtract resulterer i at alle er nye
            val forrigeVedtak = vedtakService.hent(vedtak.forrigeVedtakId!!)
            val (andelerSomErNye, andelerSomOpphøres) = separerINyeOgOpphørteAndelerMotØkonomi(vedtak.behandling.id,
                                                                                               forrigeVedtak.behandling.id)
            if (behandlingResultatType == BehandlingResultatType.OPPHØRT
                && (andelerSomErNye.size < 0 || andelerSomOpphøres.size == 0)) {
                throw IllegalStateException("Kan ikke oppdatere tilkjent ytelse og iverksette vedtak fordi opphør inneholder nye " +
                                            "andeler eller mangler opphørte andeler.")
            }
            beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeVedtak.behandling.id) // TODO: Skal returnere begge deler
        } else beregningService.hentAndelerTilkjentYtelseForBehandling(vedtak.behandling.id)

        val utbetalingsoppdrag = utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId,
                vedtak,
                behandlingResultatType,
                erFørsteBehandlingPåFagsak,
                andelerTilkjentYtelse
        )

        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(vedtak.behandling, utbetalingsoppdrag)
        iverksettOppdrag(vedtak.behandling.id, utbetalingsoppdrag)
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

    companion object {
        fun Set<AndelTilkjentYtelse>.intersectAndeler(other: Set<AndelTilkjentYtelse>): Set<Pair<AndelTilkjentYtelse, AndelTilkjentYtelse>> {
            val andelerIBegge = mutableSetOf<Pair<AndelTilkjentYtelse, AndelTilkjentYtelse>>()
            this.forEach { a ->
                other.forEach { b ->
                    if (a.erTilsvarendeForUtbetaling(b)) {
                        andelerIBegge.add(Pair(a, b))
                    }
                }
            }
            return andelerIBegge
        }

        fun Set<AndelTilkjentYtelse>.subtractAndeler(other: Set<AndelTilkjentYtelse>): Set<AndelTilkjentYtelse> {
            val andelerKunIOther = mutableSetOf<AndelTilkjentYtelse>()
            this.forEach { a ->
                other.forEach { b ->
                    if (!a.erTilsvarendeForUtbetaling(b)) {
                        andelerKunIOther.add(b)
                    }
                }
            }
            return andelerKunIOther
        }
    }
}
