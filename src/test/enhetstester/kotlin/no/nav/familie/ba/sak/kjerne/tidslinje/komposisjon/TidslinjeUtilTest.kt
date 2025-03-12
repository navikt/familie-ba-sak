package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TidslinjeUtilTest {
    private val person = lagPerson(fødselsdato = 15.jan(2020))

    @Test
    fun erUnder18ÅrVilkårTidslinje() {
        val erUnder18ÅrVilkårTidslinje = erUnder18ÅrVilkårTidslinje(person.fødselsdato)
        val perioder = erUnder18ÅrVilkårTidslinje.tilPerioder()

        assertThat(perioder).hasSize(1)
        assertThat(perioder[0].verdi).isTrue()
        assertThat(perioder[0].fom).isEqualTo(1.feb(2020))
        assertThat(perioder[0].tom).isEqualTo(31.des(2037))
    }

    @Test
    fun erUnder6ÅrTidslinje() {
        val erUnder6ÅrTidslinje = erUnder6ÅrTidslinje(person)
        val perioder = erUnder6ÅrTidslinje.tilPerioder()

        assertThat(perioder).hasSize(1)
        assertThat(perioder[0].verdi).isTrue()
        assertThat(perioder[0].fom).isEqualTo(1.jan(2020))
        assertThat(perioder[0].tom).isEqualTo(31.des(2025))
    }

    @Test
    fun erTilogMed3ÅrTidslinje() {
        val erUnder6ÅrTidslinje = erTilogMed3ÅrTidslinje(person.fødselsdato)
        val perioder = erUnder6ÅrTidslinje.tilPerioder()

        assertThat(perioder).hasSize(1)
        assertThat(perioder[0].verdi).isTrue()
        assertThat(perioder[0].fom).isEqualTo(1.feb(2020))
        assertThat(perioder[0].tom).isEqualTo(31.jan(2023))
    }

    @Test
    fun opprettBooleanTidslinjeYearMonth() {
        val tidslinje = opprettBooleanTidslinje(jan(2020), mar(2020))
        val perioder = tidslinje.tilPerioder()

        assertThat(perioder).hasSize(1)
        assertThat(perioder[0].verdi).isTrue()
        assertThat(perioder[0].fom).isEqualTo(1.jan(2020))
        assertThat(perioder[0].tom).isEqualTo(31.mar(2020))
    }

    @Test
    fun opprettBooleanTidslinjeLocalDate() {
        val tidslinje = opprettBooleanTidslinje(15.jan(2020), 15.mar(2020))
        val perioder = tidslinje.tilPerioder()

        assertThat(perioder).hasSize(1)
        assertThat(perioder[0].verdi).isTrue()
        assertThat(perioder[0].fom).isEqualTo(15.jan(2020))
        assertThat(perioder[0].tom).isEqualTo(15.mar(2020))
    }
}
