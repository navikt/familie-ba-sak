package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EndreTid {
    @Test
    fun tilMåned() {
        val tidslinje = "abcdef".tilCharTidslinje(29.jan(2000)) // Månedskille mellom c og d
        val månedPerioder = tidslinje.tilMåned { it.filterNotNull().max() }.tilPerioder()

        assertThat(månedPerioder.size).isEqualTo(2)

        assertThat(månedPerioder[0].verdi).isEqualTo('c')
        assertThat(månedPerioder[0].fom).isEqualTo(1.jan(2000))
        assertThat(månedPerioder[0].tom).isEqualTo(31.jan(2000))

        assertThat(månedPerioder[1].verdi).isEqualTo('f')
        assertThat(månedPerioder[1].fom).isEqualTo(1.feb(2000))
        assertThat(månedPerioder[1].tom).isEqualTo(29.feb(2000))
    }

    @Test
    fun tilMånedFraMånedskifte() {
        val tidslinje = "abcdef".tilCharTidslinje(29.jan(2000)) // Månedskille mellom c og d
        val månedPerioder =
            tidslinje
                .tilMånedFraMånedsskifte { innholdSisteDagForrigeMåned, innholdFørsteDagDenneMåned ->
                    innholdSisteDagForrigeMåned ?: innholdFørsteDagDenneMåned
                }.tilPerioder()

        assertThat(månedPerioder.size).isEqualTo(2)

        assertThat(månedPerioder[0].verdi).isEqualTo('a')
        assertThat(månedPerioder[0].fom).isEqualTo(1.jan(2000))
        assertThat(månedPerioder[0].tom).isEqualTo(31.jan(2000))

        assertThat(månedPerioder[1].verdi).isEqualTo('c')
        assertThat(månedPerioder[1].fom).isEqualTo(1.feb(2000))
        assertThat(månedPerioder[1].tom).isEqualTo(29.feb(2000))
    }
}
