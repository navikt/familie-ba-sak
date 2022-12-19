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

    fun hentTrekkILøpendeUtbetalinger() = repository.findAll().map { tilRest(it) }

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

    fun oppdaterTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling) =
        repository.findById(trekkILøpendeUtbetaling.identifikator.id)
            .map {
                it.copy(
                    id = trekkILøpendeUtbetaling.identifikator.id,
                    fom = trekkILøpendeUtbetaling.periode.fom,
                    tom = trekkILøpendeUtbetaling.periode.tom,
                    feilutbetaltBeløp = trekkILøpendeUtbetaling.feilutbetaltBeløp
                )
            }
            .map { repository.save(it) }
            .map { tilRest(it) }
            .orElseThrow { Feil("Finner ikke trekk i løpende utbetaling") }
}
