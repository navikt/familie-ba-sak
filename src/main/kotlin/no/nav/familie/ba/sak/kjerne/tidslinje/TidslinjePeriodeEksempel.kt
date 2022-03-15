package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.time.LocalDate

enum class TidslinjeTema {
    VILKÅR_PERIODER_BARN
}

data class TidslinjeEksempel<Data : PeriodeData>(
    val perioder: List<PeriodeEksempel<Data>>,
    val tema: TidslinjeTema
) {
    fun tilLocalDateTimeline(): LocalDateTimeline<List<PeriodeData>> {
        return LocalDateTimeline(this.perioder.sortedBy { it.fom }.map {
            LocalDateSegment(
                it.fom,
                it.tom,
                listOf(it.data)
            )
        })
    }
}

fun List<LocalDateTimeline<List<PeriodeData>>>.kombinerTidslinjer(): LocalDateTimeline<List<PeriodeData>> {
    val restenAvTidslinjene = this.dropLast(1)
    val sisteTidslinje = this.last()
    return restenAvTidslinjene.fold(sisteTidslinje) { sammenlagt, neste ->
        kombinerTidslinjer(sammenlagt, neste)
    }
}

internal fun kombinerTidslinjer(
    sammenlagtTidslinje: LocalDateTimeline<List<PeriodeData>>,
    tidslinje: LocalDateTimeline<List<PeriodeData>>
): LocalDateTimeline<List<PeriodeData>> {
    val sammenlagt =
        sammenlagtTidslinje.combine(
            tidslinje,
            StandardCombinators::bothValues,
            LocalDateTimeline.JoinStyle.CROSS_JOIN
        ) as LocalDateTimeline<List<List<PeriodeData>>>

    return LocalDateTimeline(
        sammenlagt.toSegments().map {
            LocalDateSegment(it.fom, it.tom, it.value.flatten())
        }
    )
}

interface PeriodeData {
    fun hentKriterie(): Any
}

data class PeriodeEksempel<Data : PeriodeData>(
    val fom: LocalDate,
    val tom: LocalDate,
    val data: Data
)
