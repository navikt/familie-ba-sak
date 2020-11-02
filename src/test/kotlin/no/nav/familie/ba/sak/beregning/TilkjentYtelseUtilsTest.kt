package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.restDomene.BeregningEndringType
import no.nav.familie.ba.sak.common.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class TilkjentYtelseUtilsTest {


    @Test
    fun `Barn som er under 6 år hele perioden får tillegg hele perioden`() {
        val periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2022, 1, 1))
        val seksårsdag = LocalDate.of(2023, 1, 1)

        assertEquals(periode, SatsService.hentPeriodeTil6år(seksårsdag, periode.fom, periode.tom))
    }

    @Test
    fun `Barn som fyller 6 år i løpet av perioden får tilleggsperiode før seksårsdag`() {
        val periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2022, 1, 1))
        val seksårsdag = LocalDate.of(2021, 1, 1)

        assertEquals(Periode(periode.fom, seksårsdag.sisteDagIForrigeMåned()),
                     SatsService.hentPeriodeTil6år(seksårsdag, periode.fom, periode.tom))
    }

    @Test
    fun `Barn som er over 6 år hele perioden får ingen tillegsperiode`() {
        val periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2022, 1, 1))
        val seksårsdag = LocalDate.of(2018, 1, 1)

        assertEquals(null, SatsService.hentPeriodeTil6år(seksårsdag, periode.fom, periode.tom))
    }

    @Test
    fun `Barn som fyller 6 år i det vilkårene ikke lenger er oppfylt får andel den måneden også`() {
        val periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2021, 2, 20))
        val seksårsdag = LocalDate.of(2021, 2, 12)

        val (periodeUnder6år, periodeOver6år) = SatsService.splittPeriodePå6Årsdag(seksårsdag, periode.fom, periode.tom)

        assertEquals(Periode(periode.fom, periode.tom.minusMonths(1).sisteDagIMåned()),
                     periodeUnder6år)

        assertEquals(Periode(periode.tom.førsteDagIInneværendeMåned(), periode.tom),
                     periodeOver6år)
    }

    @Test
    fun `Barn som fyller 6 år i det vilkårene er oppfylt får andel måneden etter`() {
        val periode = Periode(LocalDate.of(2021, 2, 15), LocalDate.of(2025, 2, 12))
        val seksårsdag = LocalDate.of(2021, 2, 12)

        val (periodeUnder6år, periodeOver6år) = SatsService.splittPeriodePå6Årsdag(seksårsdag, periode.fom, periode.tom)

        assertNull(periodeUnder6år)

        assertEquals(Periode(periode.fom.førsteDagINesteMåned(), periode.tom),
                     periodeOver6år)
    }

    @Test
    fun `Uendrede beregninger får endringskode UENDRET og UENDRET_SATS`() {
        val person = tilfeldigPerson()
        val personopplysningsgrunnlag = lagTestPersonopplysningGrunnlag(0, person)
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse().also {
            it.andelerTilkjentYtelse.addAll(setOf(lagAndelTilkjentYtelse(person = person,
                                                                         fom = "2020-01-01",
                                                                         tom = "2020-08-31",
                                                                         beløp = 1054),
                                                  lagAndelTilkjentYtelse(person = person,
                                                                         fom = "2020-09-01",
                                                                         tom = "2020-12-30",
                                                                         beløp = 1354)))
        }
        val nyTilkjentYtelse = lagInitiellTilkjentYtelse().also {
            it.andelerTilkjentYtelse.addAll(setOf(lagAndelTilkjentYtelse(person = person,
                                                                         fom = "2020-01-01",
                                                                         tom = "2020-08-31",
                                                                         beløp = 1054),
                                                  lagAndelTilkjentYtelse(person = person,
                                                                         fom = "2020-09-01",
                                                                         tom = "2020-12-30",
                                                                         beløp = 1354)))
        }

        val oversikt = TilkjentYtelseUtils.hentBeregningOversikt(
                tilkjentYtelseForBehandling = nyTilkjentYtelse,
                personopplysningGrunnlag = personopplysningsgrunnlag,
                tilkjentYtelseForForrigeBehandling = forrigeTilkjentYtelse)
                .sortedBy { it.periodeFom }
        assertEquals(BeregningEndringType.UENDRET, oversikt[0].endring.type)
        assertEquals(BeregningEndringType.UENDRET_SATS, oversikt[1].endring.type)
    }

    @Test
    fun `Endrede beregninger får endringskode ENDRET og ENDRET_SATS`() {
        val person = tilfeldigPerson()
        val personopplysningsgrunnlag = lagTestPersonopplysningGrunnlag(0, person)
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse().also {
            it.andelerTilkjentYtelse.addAll(setOf(lagAndelTilkjentYtelse(person = person,
                                                                         fom = "2020-01-01",
                                                                         tom = "2020-12-30",
                                                                         beløp = 1054)))
        }
        val nyTilkjentYtelse = lagInitiellTilkjentYtelse().also {
            it.andelerTilkjentYtelse.addAll(setOf(lagAndelTilkjentYtelse(person = person,
                                                                         fom = "2020-01-01",
                                                                         tom = "2020-08-31",
                                                                         beløp = 1054),
                                                  lagAndelTilkjentYtelse(person = person,
                                                                         fom = "2020-09-01",
                                                                         tom = "2020-12-30",
                                                                         beløp = 1354)))
        }

        val oversikt = TilkjentYtelseUtils.hentBeregningOversikt(
                tilkjentYtelseForBehandling = nyTilkjentYtelse,
                personopplysningGrunnlag = personopplysningsgrunnlag,
                tilkjentYtelseForForrigeBehandling = forrigeTilkjentYtelse)
                .sortedBy { it.periodeFom }
        assertEquals(BeregningEndringType.ENDRET, oversikt[0].endring.type)
        assertEquals(BeregningEndringType.ENDRET_SATS, oversikt[1].endring.type)
    }
}