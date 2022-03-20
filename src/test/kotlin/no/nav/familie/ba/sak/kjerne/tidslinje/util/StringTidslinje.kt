package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsrom
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo

class StringTidslinje(
    val start: Tidspunkt,
    val s: List<String>
) : Tidslinje<String>() {

    override fun tidsrom(): Tidsrom {
        val slutt = start.flytt(s.size.toLong() - 1)
        val fom = if (s.firstOrNull() == "<") start.somUendeligLengeSiden() else start
        val tom = if (s.lastOrNull() == ">") slutt.somUendeligLengeTil() else slutt

        return fom..tom
    }

    override fun perioder(): Collection<Periode<String>> {
        return tidsrom().mapIndexed { index, tidspunkt ->
            val c = when (index) {
                0 -> if (s[index] == "<") s[index + 1] else s[index]
                s.size - 1 -> if (s[index] == ">") s[index - 1] else s[index]
                else -> s[index]
            }
            Periode(tidspunkt.somFraOgMed(), tidspunkt.somTilOgMed(), c)
        }
    }
}
