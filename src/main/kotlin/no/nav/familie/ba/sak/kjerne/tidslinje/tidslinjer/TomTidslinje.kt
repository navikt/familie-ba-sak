package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Uendelighet
import java.time.LocalDate
import java.time.YearMonth

class TomTidslinje<I, T : Tidsenhet> : Tidslinje<I, T>() {
    override fun fraOgMed(): Tidspunkt<T> = NullTidspunkt.fraOgMed()
    override fun tilOgMed(): Tidspunkt<T> = NullTidspunkt.tilOgMed()

    override fun lagPerioder(): Collection<Periode<I, T>> = emptyList()

    private data class NullTidspunkt<T : Tidsenhet> constructor(val uendelighet: Uendelighet) :
        Tidspunkt<T>(uendelighet) {

        companion object {
            // Vi plasserer fra-og-med uendelig langt inn i fremtiden og til-og-med uendelig langt bak i fortiden
            // Dermed er tidslinjen "maksimalt tom"
            fun <T : Tidsenhet> fraOgMed() = NullTidspunkt<T>(uendelighet = Uendelighet.FREMTID)
            fun <T : Tidsenhet> tilOgMed() = NullTidspunkt<T>(uendelighet = Uendelighet.FORTID)
        }

        val nullTidspunktException = IllegalStateException("Dette er et NULL-tidspunkt")

        override fun tilFørsteDagIMåneden(): Tidspunkt<Dag> {
            return NullTidspunkt(uendelighet)
        }

        override fun tilSisteDagIMåneden(): Tidspunkt<Dag> {
            return NullTidspunkt(uendelighet)
        }

        override fun tilInneværendeMåned(): Tidspunkt<Måned> {
            return NullTidspunkt(uendelighet)
        }

        override fun tilLocalDateEllerNull(): LocalDate? {
            return null
        }

        override fun tilLocalDate(): LocalDate {
            throw nullTidspunktException
        }

        override fun tilYearMonthEllerNull(): YearMonth? {
            return null
        }

        override fun tilYearMonth(): YearMonth {
            throw nullTidspunktException
        }

        override fun flytt(tidsenheter: Long): Tidspunkt<T> {
            return this // Er uendelig, så enhver flytting forblir uendelig
        }

        override fun somEndelig(): Tidspunkt<T> {
            return this // Forblir uendelig
        }

        override fun somUendeligLengeSiden(): Tidspunkt<T> {
            return fraOgMed()
        }

        override fun somUendeligLengeTil(): Tidspunkt<T> {
            return tilOgMed()
        }

        override fun somFraOgMed(): Tidspunkt<T> {
            return fraOgMed()
        }

        override fun somFraOgMed(dato: LocalDate): Tidspunkt<T> {
            throw nullTidspunktException
        }

        override fun somTilOgMed(): Tidspunkt<T> {
            return tilOgMed()
        }

        override fun somTilOgMed(dato: LocalDate): Tidspunkt<T> {
            throw nullTidspunktException
        }

        override fun sammenliknMed(tidspunkt: Tidspunkt<T>): Int {
            return if (tidspunkt is NullTidspunkt) {
                if (this.uendelighet == tidspunkt.uendelighet)
                    0
                else if (this.uendelighet == Uendelighet.FORTID) {
                    -1
                } else
                    1
            } else when (uendelighet) {
                Uendelighet.FREMTID -> 1 // Dette skal alltid være det seneste tidspunktet i alle sammenlikninger
                Uendelighet.FORTID -> -1 // Dette skal alltid være det tidligste tidspunktet i alle sammenlikninger
                else -> throw nullTidspunktException // Skal ikke inntreffe
            }
        }
    }
}
