package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PeriodeIdMigreringScheduler(private val beregningService: BeregningService,
                                  private val fagsakService: FagsakService,
                                  private val tilkjentYtelseRepository: TilkjentYtelseRepository) {

    @Scheduled(cron = "0 0 8 1 * ?")
    fun populerPeriodeOffsetOgForrigePeriodeOffsetFraUtbetalingsoppdrag() {

        val tilkjentYtelser = fagsakService.hentAlleFagsaker()
                .flatMap { beregningService.hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(it.id) }

        val tilkjentYtelseMedOppdaterteAndeler: List<TilkjentYtelse> = tilkjentYtelser.map {
            val utbetalingsperioder =
                    objectMapper.readValue(it.utbetalingsoppdrag, Utbetalingsoppdrag::class.java).utbetalingsperiode
            it.andelerTilkjentYtelse.forEach { andel ->
                val matchendeUtbetalingsperiode =
                        utbetalingsperioder.find { utbetalingsperiode -> andel.stønadFom == utbetalingsperiode.vedtakdatoFom
                                                                         && andel.stønadTom == utbetalingsperiode.vedtakdatoTom
                                                                         && andel.beløp == utbetalingsperiode.sats.toInt() }
                andel.periodeOffset = matchendeUtbetalingsperiode?.periodeId
                andel.forrigePeriodeOffset = matchendeUtbetalingsperiode?.forrigePeriodeId
            }
            LOG.info("Migrert periodeId for behandling ${it.behandling.id}")
            it
        }

        tilkjentYtelseRepository.saveAll(tilkjentYtelseMedOppdaterteAndeler)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PeriodeIdMigreringScheduler::class.java)
    }
}