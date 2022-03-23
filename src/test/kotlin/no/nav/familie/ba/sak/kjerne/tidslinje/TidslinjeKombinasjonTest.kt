package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.ToveisKombinator
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.komprimer
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.util.StringTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TidslinjeKombinasjonTest {

    val kombinator = object : ToveisKombinator<Char, Char, String> {
        override fun kombiner(venstre: Char?, høyre: Char?) =
            (venstre?.toString() ?: "").trim() + (høyre?.toString() ?: "").trim()
    }

    @Test
    fun testEndeligeLikeLangTidslinjer() {
        assertTidslinjer(
            linje1 = "abcdef",
            linje2 = "fedcba",
            "af", "be", "cd", "dc", "eb", "fa"
        )
    }

    @Test
    fun testEndeligeTidslinjerMedForskjelligLengde() {
        assertTidslinjer(
            linje1 = "  ab",
            linje2 = "fedcba",
            "f", "e", "ad", "bc", "b", "a"
        )
    }

    @Test
    fun testUendeligeTidslinjerFremover() {
        assertTidslinjer(
            linje1 = "abc>",
            linje2 = "abacd>",
            "aa", "bb", "ca", "c", "d", ">"
        )
    }

    @Test
    fun testUendeligeTidslinjerBeggeVeier() {
        assertTidslinjer(
            linje1 = "<a",
            linje2 = "<abacd>",
            "<", "aa", "b", "a", "c", "d", ">"
        )
    }

    private fun assertTidslinjer(linje1: String, linje2: String, vararg forventet: String) {

        val fom = jan(2020)
        val char1 = linje1.tilCharTidslinje(fom)
        val char2 = linje2.tilCharTidslinje(fom)

        val k1 = char1.snittKombinerMed(char2, kombinator)
        val k2 = char1.kombinerMed(char2, kombinator)
        val f = StringTidslinje(fom, forventet.toList()).komprimer()

        Assertions.assertEquals(k1, k2)
        Assertions.assertEquals(f, k1)
    }
}

fun <V, H, R, T : Tidsenhet> Tidslinje<V, T>.snittKombinerMed(
    tidslinje: Tidslinje<H, T>,
    toveisKombinator: ToveisKombinator<V, H, R>
): Tidslinje<R, T> {
    val v1 = this
    return object : TidslinjeSomStykkerOppTiden<R, T>(v1, tidslinje) {
        override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>): R? =
            toveisKombinator.kombiner(
                v1.hentUtsnitt(tidspunkt),
                tidslinje.hentUtsnitt(tidspunkt)
            )
    }
}
