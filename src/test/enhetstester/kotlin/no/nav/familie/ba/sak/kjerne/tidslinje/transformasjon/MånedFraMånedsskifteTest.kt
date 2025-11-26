package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import no.nav.familie.tidslinje.TidsEnhet
import no.nav.familie.tidslinje.tomTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MånedFraMånedsskifteTest {
    @Test
    fun `skal gi tom tidslinje hvis alle dager er inni én måned`() {
        val dagTidslinje = "aaaaaa".tilCharTidslinje(7.des(2021))
        val månedTidslinje = dagTidslinje.tilMånedFraMånedsskifteIkkeNull { _, _ -> 'b' }

        val forventet = tomTidslinje<Char>(1.jan(2022), tidsEnhet = TidsEnhet.MÅNED)

        assertEquals(forventet, månedTidslinje)
    }

    @Test
    fun `skal gi én måned ved ett månedsskifte`() {
        val dagTidslinje = "abcdefg".tilCharTidslinje(28.nov(2021))
        val månedTidslinje =
            dagTidslinje
                .tilMånedFraMånedsskifteIkkeNull { _, verdiFørsteDagDenneMåned ->
                    verdiFørsteDagDenneMåned
                }

        assertEquals("d".tilCharTidslinje(des(2021)).tilMåned { it.single() }, månedTidslinje)
    }

    @Test
    fun `skal gi to måneder ved to månedsskifter`() {
        val dagTidslinje = "abcdefghijklmnopqrstuvwxyzæøå0123456789".tilCharTidslinje(28.nov(2021))
        val månedTidslinje =
            dagTidslinje
                .tilMånedFraMånedsskifteIkkeNull { verdiSisteDagForrigeMåned, _ ->
                    verdiSisteDagForrigeMåned
                }

        assertEquals("c4".tilCharTidslinje(des(2021)).tilMåned { it.single() }, månedTidslinje)
    }

    @Test
    fun `skal gi tom tidslinje hvis månedsskiftet mangler verdi på begge sider`() {
        val dagTidslinje =
            "abcdefghijklmnopqrstuvwxyzæøå0123456789"
                .tilCharTidslinje(28.nov(2021))
                .mapIkkeNull {
                    when (it) {
                        'c', 'd', '4', '5' -> null

                        // 30/11, 1/12, 31/12 og 1/1 mangler verdi
                        else -> it
                    }
                }

        val månedTidslinje = dagTidslinje.tilMånedFraMånedsskifteIkkeNull { _, _ -> 'A' }

        val forventet = tomTidslinje<Char>(1.feb(2022), tidsEnhet = TidsEnhet.MÅNED)

        assertEquals(forventet, månedTidslinje)
    }

    @Test
    fun `skal gi tom tidslinje hvis månedsskiftet mangler verdi på én av sidene`() {
        val dagTidslinje =
            "abcdefghijklmnopqrstuvwxyzæøå0123456789"
                .tilCharTidslinje(28.nov(2021))
                .mapIkkeNull {
                    when (it) {
                        'c', '5' -> null

                        // 30/11 og 1/1 mangler verdi
                        else -> it
                    }
                }

        val månedTidslinje = dagTidslinje.tilMånedFraMånedsskifteIkkeNull { _, _ -> 'A' }

        val forventet = tomTidslinje<Char>(1.feb(2022), tidsEnhet = TidsEnhet.MÅNED)

        assertEquals(forventet, månedTidslinje)
    }
}
