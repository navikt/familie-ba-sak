package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagValutakurs
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.tilSisteVirkedag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ValutakursTest {
    @Nested
    inner class `Valider at erObligatoriskeFelterSatt` {
        @Test
        fun `gir false ved tom valutakurs`() {
            val tomValutakurs = lagValutakurs()

            assertThat(tomValutakurs.erObligatoriskeFelterSatt()).isFalse()
        }

        @Test
        fun `gir false dersom fom ikke er satt`() {
            val valutakursUtenFom =
                lagValutakurs(
                    valutakode = "NOK",
                    kurs = BigDecimal.ONE,
                    valutakursdato = LocalDate.now(),
                    vurderingsform = Vurderingsform.MANUELL,
                    barnAktører = setOf(randomAktør()),
                )

            assertThat(valutakursUtenFom.erObligatoriskeFelterSatt()).isFalse()
        }

        @Test
        fun `gir false dersom valutakode ikke er satt`() {
            val valutakursUtenValutakode =
                lagValutakurs(
                    fom = LocalDate.now().toYearMonth(),
                    kurs = BigDecimal.ONE,
                    valutakursdato = LocalDate.now(),
                    vurderingsform = Vurderingsform.MANUELL,
                    barnAktører = setOf(randomAktør()),
                )

            assertThat(valutakursUtenValutakode.erObligatoriskeFelterSatt()).isFalse()
        }

        @Test
        fun `gir false dersom kurs ikke er satt`() {
            val valutakursUtenKurs =
                lagValutakurs(
                    fom = LocalDate.now().toYearMonth(),
                    valutakode = "NOK",
                    valutakursdato = LocalDate.now(),
                    vurderingsform = Vurderingsform.MANUELL,
                    barnAktører = setOf(randomAktør()),
                )

            assertThat(valutakursUtenKurs.erObligatoriskeFelterSatt()).isFalse()
        }

        @Test
        fun `gir false dersom valutakursdato ikke er satt`() {
            val valutakursUtenValutakursdato =
                lagValutakurs(
                    fom = LocalDate.now().toYearMonth(),
                    valutakode = "NOK",
                    kurs = BigDecimal.ONE,
                    vurderingsform = Vurderingsform.MANUELL,
                    barnAktører = setOf(randomAktør()),
                )

            assertThat(valutakursUtenValutakursdato.erObligatoriskeFelterSatt()).isFalse()
        }

        @Test
        fun `gir false dersom vurderingsform ikke er satt`() {
            val valutakursUtenVurderingsform =
                lagValutakurs(
                    fom = LocalDate.now().toYearMonth(),
                    valutakode = "NOK",
                    kurs = BigDecimal.ONE,
                    valutakursdato = LocalDate.now(),
                    barnAktører = setOf(randomAktør()),
                    vurderingsform = Vurderingsform.IKKE_VURDERT,
                )

            assertThat(valutakursUtenVurderingsform.erObligatoriskeFelterSatt()).isFalse()
        }

        @Test
        fun `gir false dersom barnAktører er tomt sett`() {
            val valutakursUtenBarn =
                lagValutakurs(
                    fom = LocalDate.now().toYearMonth(),
                    valutakode = "NOK",
                    kurs = BigDecimal.ONE,
                    valutakursdato = LocalDate.now(),
                    barnAktører = emptySet(),
                    vurderingsform = Vurderingsform.MANUELL,
                )

            assertThat(valutakursUtenBarn.erObligatoriskeFelterSatt()).isFalse()
        }

        @Test
        fun `gir true dersom obligatoriske felter er satt`() {
            val valutakursMedObligatoriskeFelter =
                lagValutakurs(
                    fom = LocalDate.now().toYearMonth(),
                    valutakode = "NOK",
                    kurs = BigDecimal.ONE,
                    valutakursdato = LocalDate.now(),
                    barnAktører = setOf(randomAktør()),
                    vurderingsform = Vurderingsform.MANUELL,
                )

            assertThat(valutakursMedObligatoriskeFelter.erObligatoriskeFelterSatt()).isTrue()
        }
    }

    @Nested
    inner class `Valider at måValutakurserOppdateresForMåned` {
        @Test
        fun `gir false når alle valutakurser er oppdatert for gitt måned`() {
            val måned = LocalDate.now().toYearMonth()

            val valutakurs1 =
                lagValutakurs(
                    fom = måned,
                    tom = null,
                    valutakode = "SEK",
                    valutakursdato = måned.minusMonths(1).tilSisteVirkedag(),
                )

            val valutakurs2 =
                lagValutakurs(
                    fom = måned,
                    tom = null,
                    valutakode = "EUR",
                    valutakursdato = måned.minusMonths(1).tilSisteVirkedag(),
                )

            val valutakurser = listOf(valutakurs1, valutakurs2)

            assertThat(valutakurser.måValutakurserOppdateresForMåned(måned)).isFalse()
        }

        @Test
        fun `gir true når minst én valutakurs ikke er oppdatert for gitt måned`() {
            val måned = LocalDate.now().toYearMonth()

            val valutakurs1 =
                lagValutakurs(
                    fom = måned.minusMonths(1),
                    tom = null,
                    valutakode = "SEK",
                    valutakursdato = måned.minusMonths(2).tilSisteVirkedag(),
                )

            val valutakurs2 =
                lagValutakurs(
                    fom = måned,
                    tom = null,
                    valutakode = "EUR",
                    valutakursdato = måned.minusMonths(2).tilSisteVirkedag(),
                )

            val valutakurser = listOf(valutakurs1, valutakurs2)

            assertThat(valutakurser.måValutakurserOppdateresForMåned(måned)).isTrue()
        }

        @Test
        fun `gir true når en valutakurs har feil valutakursdato for gitt måned`() {
            val måned = LocalDate.now().toYearMonth()

            val valutakurs =
                lagValutakurs(
                    fom = måned,
                    tom = null,
                    valutakode = "SEK",
                    valutakursdato = måned.minusMonths(2).tilSisteVirkedag(),
                )

            val valutakurser = listOf(valutakurs)

            assertThat(valutakurser.måValutakurserOppdateresForMåned(måned)).isTrue()
        }
    }
}
