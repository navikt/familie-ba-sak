package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

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
    fun leggTilTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling) {
        repository.save(
            TrekkILøpendeUtbetaling(
                behandlingId = trekkILøpendeUtbetaling.behandlingId,
                fom = trekkILøpendeUtbetaling.fom,
                tom = trekkILøpendeUtbetaling.tom,
                feilutbetaltBeløp = trekkILøpendeUtbetaling.feilutbetaltBeløp
            )
        )
        loggService.loggTrekkILøpendeUtbetalingLagtTil(behandlingId = trekkILøpendeUtbetaling.behandlingId)
    }

    fun fjernTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling) {
        repository.delete(
            TrekkILøpendeUtbetaling(
                behandlingId = trekkILøpendeUtbetaling.behandlingId,
                fom = trekkILøpendeUtbetaling.fom,
                tom = trekkILøpendeUtbetaling.tom,
                feilutbetaltBeløp = trekkILøpendeUtbetaling.feilutbetaltBeløp
            )
        )
        loggService.loggTrekkILøpendeUtbetalingFjernet(behandlingId = trekkILøpendeUtbetaling.behandlingId)
    }

    fun hentTrekkILøpendeUtbetalinger() = repository.findAll().map {
        RestTrekkILøpendeUtbetaling(
            behandlingId = it.behandlingId,
            fom = it.fom,
            tom = it.tom,
            feilutbetaltBeløp = it.feilutbetaltBeløp
        )
    }
}
