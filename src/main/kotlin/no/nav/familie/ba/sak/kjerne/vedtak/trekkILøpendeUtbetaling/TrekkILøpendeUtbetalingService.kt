package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TrekkILøpendeUtbetalingService(
    @Autowired
    private val repository: TrekkILøpendeUtbetalingRepository,

    @Autowired
    private val loggService: LoggService
) {
    fun leggTilTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling, behandlingId: Long): Long {
        val lagret = repository.save(
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
        repository.deleteById(id)
        loggService.loggTrekkILøpendeUtbetalingFjernet(behandlingId = behandlingId)
    }

    fun hentTrekkILøpendeUtbetaling(id: Long) = repository.findById(id).orElseThrow { throw Feil("Finner ikke feilutbetalt valuta periode med id=$id") }

    fun hentTrekkILøpendeUtbetalinger(behandlingId: Long) =
        repository.finnTrekkILøpendeUtbetalingForBehandling(behandlingId = behandlingId).map { tilRest(it) }.takeIf { it.isNotEmpty() }

    private fun tilRest(it: TrekkILøpendeUtbetaling) =
        RestTrekkILøpendeUtbetaling(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            feilutbetaltBeløp = it.feilutbetaltBeløp
        )

    fun oppdaterTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling) {
        val periode = repository.findById(trekkILøpendeUtbetaling.id).orElseThrow { Feil("Finner ikke feilutbetalt valuta med id=${trekkILøpendeUtbetaling.id}") }

        periode.fom = trekkILøpendeUtbetaling.fom
        periode.tom = trekkILøpendeUtbetaling.tom
        periode.feilutbetaltBeløp = trekkILøpendeUtbetaling.feilutbetaltBeløp

        repository.save(periode)
    }
}
