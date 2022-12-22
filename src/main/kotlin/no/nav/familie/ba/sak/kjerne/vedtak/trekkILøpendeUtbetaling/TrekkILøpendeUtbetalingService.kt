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
    @Transactional
    fun leggTilFeilutbetaltValutaPeriode(feilutbetaltValuta: RestTrekkILøpendeUtbetaling, behandlingId: Long): Long {
        val lagret = trekkILøpendeUtbetalingRepository.save(
            TrekkILøpendeUtbetaling(
                behandlingId = behandlingId,
                fom = feilutbetaltValuta.fom,
                tom = feilutbetaltValuta.tom,
                feilutbetaltBeløp = feilutbetaltValuta.feilutbetaltBeløp
            )
        )
        loggService.loggFeilutbetaltValutaPeriodeLagtTil(behandlingId = behandlingId)
        return lagret.id
    }

    @Transactional
    fun fjernFeilutbetaltValutaPeriode(id: Long, behandlingId: Long) {
        trekkILøpendeUtbetalingRepository.deleteById(id)
        loggService.loggFeilutbetaltValutaPeriodeFjernet(behandlingId = behandlingId)
    }

    fun hentFeilutbetaltValutaPerioder(behandlingId: Long) =
        trekkILøpendeUtbetalingRepository.finnTrekkILøpendeUtbetalingForBehandling(behandlingId = behandlingId).map { tilRest(it) }

    private fun tilRest(it: TrekkILøpendeUtbetaling) =
        RestTrekkILøpendeUtbetaling(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            feilutbetaltBeløp = it.feilutbetaltBeløp
        )

    @Transactional
    fun oppdatertFeilutbetaltValutaPeriode(feilutbetaltValuta: RestTrekkILøpendeUtbetaling, id: Long) {
        val periode = trekkILøpendeUtbetalingRepository.findById(id).orElseThrow { Feil("Finner ikke feilutbetalt valuta med id=${feilutbetaltValuta.id}") }

        periode.fom = feilutbetaltValuta.fom
        periode.tom = feilutbetaltValuta.tom
        periode.feilutbetaltBeløp = feilutbetaltValuta.feilutbetaltBeløp
    }
}
