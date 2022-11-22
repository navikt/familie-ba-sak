package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TrekkILøpendeUtbetalingService(
    @Autowired
    private val repository: TrekkILøpendeUtbetalingRepository
) {
    fun leggTilTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling) {
        repository.save(
            TrekkILøpendeUtbetaling(
                behandlingId = trekkILøpendeUtbetaling.behandlingId,
                fom = trekkILøpendeUtbetaling.fom,
                tom = trekkILøpendeUtbetaling.tom,
                sum = trekkILøpendeUtbetaling.sum
            )
        )
    }

    fun hentTrekkILøpendeUtbetalinger() = repository.findAll().map {
        RestTrekkILøpendeUtbetaling(
            behandlingId = it.behandlingId,
            fom = it.fom,
            tom = it.tom,
            sum = it.sum
        )
    }
}
