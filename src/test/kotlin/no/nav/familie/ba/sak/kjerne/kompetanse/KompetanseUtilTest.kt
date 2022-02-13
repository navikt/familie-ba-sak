package no.nav.familie.ba.sak.kjerne.kompetanse

import no.nav.familie.ba.sak.kjerne.kompetanse.domene.Kompetanse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class KompetanseUtilTest {

    @Test
    fun test() {

        val kompetanse = Kompetanse(
            id = 1,
            behandlingId = 1L,
            fom = YearMonth.of(2021, 1),
            tom = YearMonth.of(2021, 12),
            barn = setOf(111, 222, 333)
        )

        val oppdatertKompetanse = Kompetanse(
            id = 1,
            behandlingId = 1L,
            fom = YearMonth.of(2021, 3),
            tom = YearMonth.of(2021, 10),
            barn = setOf(111)
        )

        val restKompetanser = KompetanseUtil.finnRestKompetanser(kompetanse, oppdatertKompetanse)

        assertEquals(3, restKompetanser.size)
        assertTrue(
            restKompetanser.contains(
                Kompetanse(
                    id = 0,
                    behandlingId = 1L,
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2021, 2),
                    barn = setOf(111)
                )
            )

        )
        assertTrue(
            restKompetanser.contains(
                Kompetanse(
                    id = 0,
                    behandlingId = 1L,
                    fom = YearMonth.of(2021, 11),
                    tom = YearMonth.of(2021, 12),
                    barn = setOf(111)
                )
            )
        )
        assertTrue(
            restKompetanser.contains(
                Kompetanse(
                    id = 0,
                    behandlingId = 1L,
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2021, 12),
                    barn = setOf(222, 333)
                )
            )
        )
    }
}
