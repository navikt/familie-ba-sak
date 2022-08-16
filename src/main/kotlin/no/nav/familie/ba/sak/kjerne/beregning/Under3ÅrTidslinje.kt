package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilMånedTidspunkt
import java.time.LocalDate

data class Under3ÅrPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)

open class Under3ÅrTidslinje(
    private val under3ÅrPerioder: List<Under3ÅrPeriode>,
) : Tidslinje<Under3ÅrPeriode, Måned>() {

    override fun lagPerioder(): List<Periode<Under3ÅrPeriode, Måned>> {
        return under3ÅrPerioder.map {
            Periode(
                fraOgMed = it.fom.tilMånedTidspunkt(),
                tilOgMed = it.tom.tilMånedTidspunkt(),
                innhold = it
            )
        }
    }
}
