package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagValutakurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class AutomatiskOppdaterValutakursUtilTest {
    val barnAktører = setOf(randomAktør())
    val valutakursdato: LocalDate = LocalDate.now()
    val valutakode = "DKK"

    @Test
    fun `finnFørsteEndringIValutakurs skal returnere TIDENES_ENDE hvis begge er tomme`() {
        val valutakurserDenneBehandling = emptyList<Valutakurs>()
        val valutakurserForrigeBehandling = emptyList<Valutakurs>()

        val actual = finnFørsteEndringIValutakurs(valutakurserDenneBehandling, valutakurserForrigeBehandling)

        assertThat(actual).isEqualTo(TIDENES_ENDE.toYearMonth())
    }

    @Test
    fun `finnFørsteEndringIValutakurs skal returnere første endring`() {
        val fom = YearMonth.now()
        val valutakurserDenneBehandling = listOf(valutakurs(fom = fom, tom = fom.plusMonths(1)))
        val valutakurserForrigeBehandling = emptyList<Valutakurs>()

        val actual = finnFørsteEndringIValutakurs(valutakurserDenneBehandling, valutakurserForrigeBehandling)

        assertThat(actual).isEqualTo(fom)
    }

    @Test
    fun `finnFørsteEndringIValutakurs skal returnere TIDENES_ENDE hvis ingen endringer`() {
        val fom = YearMonth.now()
        val valutakurserDenneBehandling = listOf(valutakurs(fom = fom, tom = fom.plusMonths(1)))
        val valutakurserForrigeBehandling = listOf(valutakurs(fom = fom, tom = fom.plusMonths(1)))

        val actual = finnFørsteEndringIValutakurs(valutakurserDenneBehandling, valutakurserForrigeBehandling)

        assertThat(actual).isEqualTo(TIDENES_ENDE.toYearMonth())
    }

    @Test
    fun `finnFørsteEndringIValutakurs skal returnere første endring i tidslinje med endring`() {
        val fom = YearMonth.now()
        val valutakurserDenneBehandling = listOf(valutakurs(fom = fom, tom = fom.plusMonths(2), kurs = BigDecimal.ONE))
        val valutakurserForrigeBehandling =
            listOf(
                valutakurs(fom = fom, tom = fom.plusMonths(1), kurs = BigDecimal.ONE),
                valutakurs(fom = fom.plusMonths(2), tom = fom.plusMonths(2), kurs = BigDecimal.TWO),
            )

        val expected = fom.plusMonths(2)
        val actual = finnFørsteEndringIValutakurs(valutakurserDenneBehandling, valutakurserForrigeBehandling)

        assertThat(actual).isEqualTo(expected)
    }

    private fun valutakurs(
        fom: YearMonth,
        tom: YearMonth,
        kurs: BigDecimal = BigDecimal.ONE,
    ) = lagValutakurs(fom = fom, tom = tom, kurs = kurs, barnAktører = barnAktører, valutakursdato = valutakursdato, valutakode = valutakode, vurderingsform = Vurderingsform.MANUELL)
}
