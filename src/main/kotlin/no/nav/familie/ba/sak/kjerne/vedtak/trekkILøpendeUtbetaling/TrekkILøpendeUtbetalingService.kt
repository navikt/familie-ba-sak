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
    fun leggTilTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling): Long {
        val lagret = repository.save(
            TrekkILøpendeUtbetaling(
                behandlingId = trekkILøpendeUtbetaling.identifikator.behandlingId,
                fom = trekkILøpendeUtbetaling.periode.fom,
                tom = trekkILøpendeUtbetaling.periode.tom,
                feilutbetaltBeløp = trekkILøpendeUtbetaling.feilutbetaltBeløp
            )
        )
        loggService.loggTrekkILøpendeUtbetalingLagtTil(behandlingId = trekkILøpendeUtbetaling.identifikator.behandlingId)
        return lagret.id
    }

    fun fjernTrekkILøpendeUtbetaling(identifikator: TrekkILøpendeBehandlingRestIdentifikator) {
        repository.deleteById(identifikator.id)
        loggService.loggTrekkILøpendeUtbetalingFjernet(behandlingId = identifikator.behandlingId)
    }

    fun hentTrekkILøpendeUtbetalinger(behandlingId: Long) =
        repository.finnTrekkILøpendeUtbetalingForBehandling(behandlingId = behandlingId).map { tilRest(it) }.takeIf { it.isNotEmpty() }

    private fun tilRest(it: TrekkILøpendeUtbetaling) =
        RestTrekkILøpendeUtbetaling(
            identifikator = TrekkILøpendeBehandlingRestIdentifikator(
                id = it.id,
                behandlingId = it.behandlingId
            ),
            RestPeriode(
                fom = it.fom,
                tom = it.tom
            ),
            feilutbetaltBeløp = it.feilutbetaltBeløp
        )

    fun oppdaterTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling) {
        val periode = repository.findById(trekkILøpendeUtbetaling.identifikator.id).get()

        periode.fom = trekkILøpendeUtbetaling.periode.fom
        periode.tom = trekkILøpendeUtbetaling.periode.tom
        periode.feilutbetaltBeløp = trekkILøpendeUtbetaling.feilutbetaltBeløp

        repository.save(periode)
    }
}
