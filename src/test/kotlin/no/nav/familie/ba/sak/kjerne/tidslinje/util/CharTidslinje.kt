package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.komprimer
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import java.time.YearMonth

internal class CharTidslinje(private val tegn: String, private val startMåned: MånedTidspunkt) :
    Tidslinje<Char, Måned>() {

    override fun fraOgMed() = when (tegn.first()) {
        '<' -> startMåned.somUendeligLengeSiden()
        else -> startMåned
    }

    override fun tilOgMed(): MånedTidspunkt {
        val sluttMåned = startMåned.flytt(tegn.length.toLong() - 1)
        return when (tegn.last()) {
            '>' -> sluttMåned.somUendeligLengeTil()
            else -> sluttMåned
        }
    }

    override fun lagPerioder(): Collection<Periode<Char, Måned>> {
        val tidspunkter = fraOgMed()..tilOgMed()

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
    CharTidslinje(this, Tidspunkt.Companion.med(fom)).komprimer()

fun String.tilCharTidslinje(fom: MånedTidspunkt): Tidslinje<Char, Måned> = CharTidslinje(this, fom).komprimer()
