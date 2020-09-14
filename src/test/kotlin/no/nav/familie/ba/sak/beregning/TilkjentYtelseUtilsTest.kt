package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.common.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class TilkjentYtelseUtilsTest {


    @Test
    fun `Barn som er under 6 år hele perioden får tillegg hele perioden`() {
        val periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2022, 1, 1))
        val seksårsdag = LocalDate.of(2023, 1, 1)

        assertEquals(periode, TilkjentYtelseUtils.hentPeriodeUnder6år(seksårsdag, periode.fom, periode.tom))
    }

    @Test
    fun `Barn som fyller 6 år i løpet av perioden får tilleggsperiode før seksårsdag`() {
        val periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2022, 1, 1))
        val seksårsdag = LocalDate.of(2021, 1, 1)

        assertEquals(Periode(periode.fom, seksårsdag),
                     TilkjentYtelseUtils.hentPeriodeUnder6år(seksårsdag, periode.fom, periode.tom))
    }

    @Test
    fun `Barn som er over 6 år hele perioden får ingen tillegsperiode`() {
        val periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2022, 1, 1))
        val seksårsdag = LocalDate.of(2018, 1, 1)

        assertEquals(null, TilkjentYtelseUtils.hentPeriodeUnder6år(seksårsdag, periode.fom, periode.tom))
    }
}