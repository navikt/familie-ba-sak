package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.matematikk

import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class BigDecimalTidslinjeTest {
    private val tidslinje1 =
        listOf(BigDecimal(1.5), BigDecimal(2.5), BigDecimal(4))
            .mapIndexed { index, verdi ->
                Periode(
                    verdi = verdi,
                    fom = LocalDate.now().plusDays(index.toLong()),
                    tom = LocalDate.now().plusDays(index.toLong()),
                )
            }.tilTidslinje()

    private val tidslinje2 =
        listOf(BigDecimal(10), BigDecimal(10), BigDecimal(10))
            .mapIndexed { index, verdi ->
                Periode(
                    verdi = verdi,
                    fom = LocalDate.now().plusDays(index.toLong()),
                    tom = LocalDate.now().plusDays(index.toLong()),
                )
            }.tilTidslinje()

    private val tidslinjeMap =
        mapOf(
            randomAktør() to tidslinje1,
            randomAktør() to tidslinje2,
        )

    @Test
    fun minus() {
        val aktør = randomAktør()
        val tidslinje1AsMap = mapOf(aktør to tidslinje1)
        val tidslinje2AsMap = mapOf(aktør to tidslinje2)
        val subtrahertePerioder = tidslinje2AsMap.minus(tidslinje1AsMap)[aktør]!!.tilPerioder().map { it.verdi }

        assertThat(subtrahertePerioder).containsExactly(BigDecimal(8.5), BigDecimal(7.5), BigDecimal(6))
    }

    @Test
    fun sum() {
        val summertePerioder = tidslinjeMap.sum().tilPerioder()

        assertThat(summertePerioder.map { it.verdi }).containsExactly(BigDecimal(11.5), BigDecimal(12.5), BigDecimal(14))
    }

    @Test
    fun rundAvTilHeleTall() {
        val avrundedeTall = tidslinje1.rundAvTilHeltall().tilPerioder().map { it.verdi }

        assertThat(avrundedeTall).containsExactly(BigDecimal(2), BigDecimal(3), BigDecimal(4))
    }
}
