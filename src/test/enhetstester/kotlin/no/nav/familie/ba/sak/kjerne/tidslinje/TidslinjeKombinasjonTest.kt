package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilStringTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TidslinjeKombinasjonTest {
    private val kombinator = { venstre: Char?, høyre: Char? ->
        (venstre?.toString() ?: "").trim() + (høyre?.toString() ?: "").trim()
    }

    @Test
    fun testEndeligeLikeLangTidslinjer() {
        assertTidslinjer(
            linje1 = "abcdef",
            linje2 = "fedcba",
            "af",
            "be",
            "cd",
            "dc",
            "eb",
            "fa",
        )
    }

    @Test
    fun testEndeligeTidslinjerMedForskjelligLengde() {
        assertTidslinjer(
            linje1 = "  ab",
            linje2 = "fedcba",
            "f",
            "e",
            "ad",
            "bc",
            "b",
            "a",
        )
    }

    @Test
    fun testUendeligeTidslinjerFremover() {
        assertTidslinjer(
            linje1 = "abc>",
            linje2 = "abacd>",
            "aa",
            "bb",
            "ca",
            "cc",
            "cd",
            ">",
        )
    }

    @Test
    fun testUendeligeTidslinjerBeggeVeier() {
        assertTidslinjer(
            linje1 = "<a",
            linje2 = "<abacd>",
            "<",
            "aa",
            "b",
            "a",
            "c",
            "d",
            ">",
        )
    }

    private fun assertTidslinjer(
        linje1: String,
        linje2: String,
        vararg forventet: String,
    ) {
        val fom = jan(2020)
        val char1 = linje1.tilCharTidslinje(fom)
        val char2 = linje2.tilCharTidslinje(fom)

        val kombinertePerioder = char1.kombinerMed(char2, kombinator).tilPerioder()
        val forventedePerioder = forventet.toList().tilStringTidslinje(fom).tilPerioder()

        Assertions.assertEquals(forventedePerioder, kombinertePerioder)
    }
}
