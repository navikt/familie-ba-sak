package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.math.BigDecimal
import java.time.LocalDate

class UtbetalingsTidslinje(
    private val utbetalingsperioder: Set<Utbetalingsperiode>,
    val tidslinje: Tidslinje<Utbetalingsperiode>,
) {
    fun erTidslinjeForPeriodeId(periodeId: Long): Boolean = utbetalingsperioder.any { it.periodeId == periodeId }

    fun sisteUtbetalingsperiode(): Utbetalingsperiode = tidslinje.tilPerioderIkkeNull().last().verdi

    fun tilUtbetalingsperioder(): List<UtbetalingsperiodeDto> =
        tidslinje.tilPerioderIkkeNull().map {
            UtbetalingsperiodeDto(
                fom = it.fom,
                tom = it.tom,
                periodeId = it.verdi.periodeId,
                forrigePeriodeId = it.verdi.forrigePeriodeId,
                beløp = it.verdi.sats,
                klassifisering = it.verdi.klassifisering,
                kildeBehandlingId = it.verdi.behandlingId,
            )
        }
}

data class UtbetalingsperiodeDto(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val periodeId: Long,
    val forrigePeriodeId: Long?,
    val beløp: BigDecimal,
    val klassifisering: String,
    val kildeBehandlingId: Long,
)
