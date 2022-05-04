package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.komprimer
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import java.time.YearMonth

class CharTidslinje<T : Tidsenhet>(private val tegn: String, private val startTidspunkt: Tidspunkt<T>) :
    Tidslinje<Char, T>() {

    val fraOgMed = when (tegn.first()) {
        '<' -> startTidspunkt.somUendeligLengeSiden()
        else -> startTidspunkt
    }

    val tilOgMed: Tidspunkt<T>
        get() {
            val sluttMåned = startTidspunkt.flytt(tegn.length.toLong() - 1)
            return when (tegn.last()) {
                '>' -> sluttMåned.somUendeligLengeTil()
                else -> sluttMåned
            }
        }

    override fun lagPerioder(): Collection<Periode<Char, T>> {
        val tidspunkter = fraOgMed..tilOgMed

        return tidspunkter.mapIndexed { index, tidspunkt ->
            val c = when (index) {
                0 -> if (tegn[index] == '<') tegn[index + 1] else tegn[index]
                tegn.length - 1 -> if (tegn[index] == '>') tegn[index - 1] else tegn[index]
                else -> tegn[index]
            }
            Periode(tidspunkt.somFraOgMed(), tidspunkt.somTilOgMed(), c)
        }
    }
}

fun String.tilCharTidslinje(fom: YearMonth): Tidslinje<Char, Måned> =
    CharTidslinje(this, Tidspunkt.med(fom)).komprimer()

fun <T : Tidsenhet> String.tilCharTidslinje(fom: Tidspunkt<T>): Tidslinje<Char, T> =
    CharTidslinje(this, fom).komprimer()
