package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidsrom
import no.nav.familie.ba.sak.kjerne.tidslinje.komprimer
import no.nav.familie.ba.sak.kjerne.tidslinje.rangeTo
import java.time.YearMonth

internal class CharTidslinje(private val tegn: String, private val startMåned: Tidspunkt) : Tidslinje<Char>() {
    override fun tidsrom(): Tidsrom {
        val fom = when (tegn.first()) {
            '<' -> startMåned.somUendeligLengeSiden()
            else -> startMåned
        }

        val sluttMåned = startMåned.flytt(tegn.length.toLong() - 1)
        val tom = when (tegn.last()) {
            '>' -> sluttMåned.somUendeligLengeTil()
            else -> sluttMåned
        }

        return fom..tom
    }

    override fun perioder(): Collection<Periode<Char>> {

        return tidsrom().mapIndexed { index, tidspunkt ->
            val c = when (index) {
                0 -> if (tegn[index] == '<') tegn[index + 1] else tegn[index]
                tegn.length - 1 -> if (tegn[index] == '>') tegn[index - 1] else tegn[index]
                else -> tegn[index]
            }
            Periode(tidspunkt.somFraOgMed(), tidspunkt.somTilOgMed(), c)
        }
    }
}

fun String.tilCharTidslinje(fom: YearMonth): Tidslinje<Char> =
    CharTidslinje(this, Tidspunkt.Companion.med(fom)).komprimer()

fun String.tilCharTidslinje(fom: Tidspunkt): Tidslinje<Char> = CharTidslinje(this, fom).komprimer()
