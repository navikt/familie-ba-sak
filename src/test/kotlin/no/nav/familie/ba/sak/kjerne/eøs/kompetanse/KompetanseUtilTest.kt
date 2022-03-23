package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class KompetanseUtilTest {

    @Test
    fun testOppdatering() {

        val kompetanse = Kompetanse(
            behandlingId = 1L,
            fom = YearMonth.of(2021, 1),
            tom = YearMonth.of(2021, 12),
            barnAktørIder = setOf("111", "222", "333")
        )

        val oppdatertKompetanse = Kompetanse(
            behandlingId = 1L,
            fom = YearMonth.of(2021, 3),
            tom = YearMonth.of(2021, 10),
            barnAktørIder = setOf("111")
        )

        val restKompetanser = KompetanseUtil.finnRestKompetanser(kompetanse, oppdatertKompetanse)

        assertEquals(3, restKompetanser.size)
        assertTrue(
            restKompetanser.contains(
                Kompetanse(
                    behandlingId = 1L,
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2021, 2),
                    barnAktørIder = setOf("111")
                )
            )

        )
        assertTrue(
            restKompetanser.contains(
                Kompetanse(
                    behandlingId = 1L,
                    fom = YearMonth.of(2021, 11),
                    tom = YearMonth.of(2021, 12),
                    barnAktørIder = setOf("111")
                )
            )
        )
        assertTrue(
            restKompetanser.contains(
                Kompetanse(
                    behandlingId = 1L,
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2021, 12),
                    barnAktørIder = setOf("222", "333")
                )
            )
        )
    }

    @Test
    fun testMergePåfølgendePerioder() {
        val kompetanse1 = Kompetanse(
            fom = YearMonth.of(2021, 1),
            tom = YearMonth.of(2021, 12),
            barnAktørIder = setOf("111", "222", "333")
        )
        val kompetanse2 = kompetanse1.copy(
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 12),
        )

        val merge = KompetanseUtil.mergeKompetanser(listOf(kompetanse1, kompetanse2))
        assertEquals(1, merge.size)

        assertEquals(YearMonth.of(2021, 1), merge.first().fom)
        assertEquals(YearMonth.of(2022, 12), merge.first().tom)
    }

    @Test
    fun testMergeForPerioderMedMellomrom() {
        val kompetanse1 = Kompetanse(
            fom = YearMonth.of(2021, 1),
            tom = YearMonth.of(2021, 12),
            barnAktørIder = setOf("111", "222", "333")
        )
        val kompetanse2 = kompetanse1.copy(
            fom = YearMonth.of(2022, 2),
            tom = YearMonth.of(2022, 12),
        )

        val merge = KompetanseUtil.mergeKompetanser(listOf(kompetanse1, kompetanse2)).toList()
        assertEquals(2, merge.size)

        assertEquals(YearMonth.of(2021, 1), merge[0].fom)
        assertEquals(YearMonth.of(2021, 12), merge[0].tom)

        assertEquals(YearMonth.of(2022, 2), merge[1].fom)
        assertEquals(YearMonth.of(2022, 12), merge[1].tom)
    }

    @Test
    fun testMergeForPerioderDerTidligstePeriodeHarÅpemTOM() {
        val kompetanse1 = Kompetanse(
            fom = YearMonth.of(2021, 1),
            tom = null,
            barnAktørIder = setOf("111", "222", "333")
        )
        val kompetanse2 = kompetanse1.copy(
            fom = YearMonth.of(2022, 2),
            tom = YearMonth.of(2022, 12),
        )

        val merge = KompetanseUtil.mergeKompetanser(listOf(kompetanse1, kompetanse2)).toList()
        assertEquals(1, merge.size)

        assertEquals(YearMonth.of(2021, 1), merge[0].fom)
        assertEquals(null, merge[0].tom)
    }

    @Test
    fun testMergeForPerioderMedOverlapp() {
        val kompetanse1 = Kompetanse(
            fom = YearMonth.of(2021, 1),
            tom = YearMonth.of(2022, 3),
            barnAktørIder = setOf("111", "222", "333")
        )
        val kompetanse2 = kompetanse1.copy(
            fom = YearMonth.of(2021, 11),
            tom = YearMonth.of(2022, 12),
        )

        val merge = KompetanseUtil.mergeKompetanser(listOf(kompetanse1, kompetanse2)).toList()
        assertEquals(1, merge.size)

        assertEquals(YearMonth.of(2021, 1), merge[0].fom)
        assertEquals(YearMonth.of(2022, 12), merge[0].tom)
    }

    @Test
    fun testMergeForPerioderDerSenestePeriodeHarÅpemTOM() {
        val kompetanse1 = Kompetanse(
            fom = YearMonth.of(2021, 1),
            tom = YearMonth.of(2021, 11),
            barnAktørIder = setOf("111", "222", "333")
        )
        val kompetanse2 = kompetanse1.copy(
            fom = YearMonth.of(2021, 9),
            tom = null
        )

        val merge = KompetanseUtil.mergeKompetanser(listOf(kompetanse1, kompetanse2)).toList()
        assertEquals(1, merge.size)

        assertEquals(YearMonth.of(2021, 1), merge[0].fom)
        assertEquals(null, merge[0].tom)
    }
}
