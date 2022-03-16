package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.time.LocalDate

enum class TidslinjeTema {
    VILKÅR_PERIODER_BARN
}

data class TidslinjeEksempel<T>(
    val perioder: List<PeriodeEksempel<T>>,
    val tema: TidslinjeTema
) {
    fun tilLocalDateTimeline(): LocalDateTimeline<List<T>> {
        return LocalDateTimeline(
            this.perioder.sortedBy { it.fom }.map {
                LocalDateSegment(
                    it.fom,
                    it.tom,
                    listOf(it.data)
                )
            }
        )
    }
}

fun <T> List<LocalDateTimeline<List<T>>>.kombinerTidslinjer() =
    this.reduce { sammenlagt, neste ->
        kombinerTidslinjer(sammenlagt, neste)
    }

internal fun <T> kombinerTidslinjer(
    sammenlagtTidslinje: LocalDateTimeline<List<T>>,
    tidslinje: LocalDateTimeline<List<T>>
): LocalDateTimeline<List<T>> {
    val sammenlagt =
        sammenlagtTidslinje.combine(
            tidslinje,
            StandardCombinators::bothValues,
            LocalDateTimeline.JoinStyle.CROSS_JOIN
        ) as LocalDateTimeline<List<List<T>>>

    return LocalDateTimeline(
        sammenlagt.toSegments().map {
            LocalDateSegment(it.fom, it.tom, it.value.flatten())
        }
    )
}

data class PeriodeEksempel<T>(
    val fom: LocalDate,
    val tom: LocalDate,
    val data: T
)
