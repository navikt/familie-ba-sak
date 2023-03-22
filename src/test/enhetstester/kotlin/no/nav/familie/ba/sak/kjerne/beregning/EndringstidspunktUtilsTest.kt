package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class EndringstidspunktUtilsTest {

    @Test
    fun `Skal returnere tidligste dato som endringstidspunkt`() {
        val endringstidspunkt = utledEndringstidspunkt(
            endringstidspunktUtbetalingsbeløp = YearMonth.of(2020, 1),
            endringstidspunktKompetanse = YearMonth.of(2019, 12),
            endringstidspunktVilkårsvurdering = YearMonth.of(2017, 5),
            endringstidspunktEndretUtbetalingAndeler = null
        )

        assertEquals(LocalDate.of(2017, 5, 1), endringstidspunkt)
    }

    @Test
    fun `Skal returnere tidenes ende som endringstidspunkt hvis det ikke finnes noen endringer i beløp, vilkårsvurdering, endret andeler eller kompetanse`() {
        val endringstidspunkt = utledEndringstidspunkt(
            endringstidspunktUtbetalingsbeløp = null,
            endringstidspunktKompetanse = null,
            endringstidspunktVilkårsvurdering = null,
            endringstidspunktEndretUtbetalingAndeler = null
        )

        assertEquals(TIDENES_ENDE, endringstidspunkt)
    }
}
