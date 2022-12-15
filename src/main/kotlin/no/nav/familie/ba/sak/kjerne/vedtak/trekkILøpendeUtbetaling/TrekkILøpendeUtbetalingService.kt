package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TrekkILøpendeUtbetalingService(
    @Autowired
    private val trekkILøpendeUtbetalingRepository: TrekkILøpendeUtbetalingRepository,

    @Autowired
    private val loggService: LoggService
) {
    fun leggTilTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling, behandlingId: Long): Long {
        val lagret = trekkILøpendeUtbetalingRepository.save(
            TrekkILøpendeUtbetaling(
                behandlingId = behandlingId,
                fom = trekkILøpendeUtbetaling.fom,
                tom = trekkILøpendeUtbetaling.tom,
                feilutbetaltBeløp = trekkILøpendeUtbetaling.feilutbetaltBeløp
            )
        )
        loggService.loggTrekkILøpendeUtbetalingLagtTil(behandlingId = behandlingId)
        return lagret.id
    }

    fun fjernTrekkILøpendeUtbetaling(id: Long, behandlingId: Long) {
        trekkILøpendeUtbetalingRepository.deleteById(id)
        loggService.loggTrekkILøpendeUtbetalingFjernet(behandlingId = behandlingId)
    }

    fun hentTrekkILøpendeUtbetaling(id: Long) = trekkILøpendeUtbetalingRepository.findById(id).orElseThrow { throw Feil("Finner ikke feilutbetalt valuta periode med id=$id") }

    fun hentTrekkILøpendeUtbetalinger(behandlingId: Long) =
        trekkILøpendeUtbetalingRepository.finnTrekkILøpendeUtbetalingForBehandling(behandlingId = behandlingId).map { tilRest(it) }

    private fun tilRest(it: TrekkILøpendeUtbetaling) =
        RestTrekkILøpendeUtbetaling(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            feilutbetaltBeløp = it.feilutbetaltBeløp
        )

    @Transactional
    fun oppdaterTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling) {
        val periode = trekkILøpendeUtbetalingRepository.findById(trekkILøpendeUtbetaling.id).orElseThrow { Feil("Finner ikke feilutbetalt valuta med id=${trekkILøpendeUtbetaling.id}") }

        periode.fom = trekkILøpendeUtbetaling.fom
        periode.tom = trekkILøpendeUtbetaling.tom
        periode.feilutbetaltBeløp = trekkILøpendeUtbetaling.feilutbetaltBeløp

        trekkILøpendeUtbetalingRepository.save(periode)
    }
}
