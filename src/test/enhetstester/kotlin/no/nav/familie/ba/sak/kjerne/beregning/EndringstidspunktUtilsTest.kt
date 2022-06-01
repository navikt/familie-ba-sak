package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class EndringstidspunktUtilsTest {

    @Test
    fun `Skal finne endringer i beløp`() {
        val person1 = lagPerson()
        val person2 = lagPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person2
            ),
        )

        val førsteEndringstidspunkt = andeler.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler
        )
        assertEquals(inneværendeMåned().minusYears(1).førsteDagIInneværendeMåned(), førsteEndringstidspunkt)
    }

    @Test
    fun `Skal finne endringer på grunn av ny overstyring`() {
        val person1 = lagPerson()
        val person2 = lagPerson(type = PersonType.BARN)

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2,
                endretUtbetalingAndeler = listOf(
                    lagEndretUtbetalingAndel(
                        person = person2,
                        fom = inneværendeMåned().minusYears(1),
                        tom = inneværendeMåned(),
                        prosent = BigDecimal(100),
                        behandlingId = 123L,
                        årsak = Årsak.DELT_BOSTED
                    )
                )
            ),
        )

        val førsteEndringstidspunkt = andeler.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler
        )
        assertEquals(inneværendeMåned().minusYears(1).førsteDagIInneværendeMåned(), førsteEndringstidspunkt)
    }

    @Test
    fun `Skal finne endringer på grunn av endret overstyring`() {
        val person1 = lagPerson()
        val person2 = lagPerson(type = PersonType.BARN)

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2,
                endretUtbetalingAndeler = listOf(
                    lagEndretUtbetalingAndel(
                        person = person2,
                        fom = inneværendeMåned().minusYears(1),
                        tom = inneværendeMåned(),
                        prosent = BigDecimal(100),
                        behandlingId = 123L,
                        årsak = Årsak.DELT_BOSTED
                    )
                )
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2,
                endretUtbetalingAndeler = listOf(
                    lagEndretUtbetalingAndel(
                        person = person2,
                        fom = inneværendeMåned().minusYears(1),
                        tom = inneværendeMåned(),
                        prosent = BigDecimal(100),
                        behandlingId = 123L,
                        årsak = Årsak.ALLEREDE_UTBETALT
                    )
                )
            ),
        )

        val førsteEndringstidspunkt = andeler.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler
        )
        assertEquals(inneværendeMåned().minusYears(1).førsteDagIInneværendeMåned(), førsteEndringstidspunkt)
    }

    @Test
    fun `Skal finne reduksjon i fomdato`() {
        val person1 = lagPerson()
        val person2 = lagPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1).minusMonths(2),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val førsteEndringstidspunkt = andeler.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler
        )
        assertEquals(
            inneværendeMåned().minusYears(1).minusMonths(2).førsteDagIInneværendeMåned(),
            førsteEndringstidspunkt
        )
    }

    @Test
    fun `Skal oppdage perioder som forsvinner`() {
        val person1 = lagPerson()
        val person2 = lagPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusMonths(15),
                tom = inneværendeMåned().minusMonths(13),
                beløp = 1054,
                person = person2
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val førsteEndringstidspunkt = andeler.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler
        )
        assertEquals(inneværendeMåned().minusMonths(15).førsteDagIInneværendeMåned(), førsteEndringstidspunkt)
    }

    @Test
    fun `Skal oppdage nye perioder`() {
        val person1 = lagPerson()
        val person2 = lagPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusMonths(15),
                tom = inneværendeMåned().minusMonths(13),
                beløp = 1054,
                person = person2
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val førsteEndringstidspunkt = andeler.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler
        )
        assertEquals(inneværendeMåned().minusMonths(15).førsteDagIInneværendeMåned(), førsteEndringstidspunkt)
    }

    @Test
    fun `Skal oppdage dersom vi har samme beløp, men på forskjellige personer`() {
        val person1 = lagPerson()
        val person2 = lagPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person1
            ),
        )

        val førsteEndringstidspunkt = andeler.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler
        )
        assertEquals(
            inneværendeMåned().minusYears(1).førsteDagIInneværendeMåned().førsteDagIInneværendeMåned(),
            førsteEndringstidspunkt
        )
    }

    @Test
    fun `Skal tåle to personer med andeler samtidig`() {
        val person1 = lagPerson()
        val person2 = lagPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusMonths(1),
                beløp = 1054,
                person = person2
            ),
        )

        val førsteEndringstidspunkt = andeler.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler
        )
        assertEquals(inneværendeMåned().førsteDagIInneværendeMåned(), førsteEndringstidspunkt)
    }

    @Test
    fun `Skal finne 0kr endring`() {
        val person1 = lagPerson(type = PersonType.BARN)

        val forrigeAndeler = emptyList<AndelTilkjentYtelse>()

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 0,
                person = person1
            ),
        )

        val førsteEndringstidspunkt = andeler.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler
        )
        assertEquals(inneværendeMåned().minusYears(4).førsteDagIInneværendeMåned(), førsteEndringstidspunkt)
    }

    @Test
    fun `Skal finne første endringstidspunkt med eksisterende delt bosted og utvidet - med nytt barn`() {
        val person1 = lagPerson(type = PersonType.BARN)
        val person2 = lagPerson(type = PersonType.BARN)
        val person3 = lagPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2021-07"),
                tom = YearMonth.parse("2029-10"),
                beløp = 1054,
                person = person2
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2022-02"),
                tom = YearMonth.parse("2029-10"),
                beløp = 1054,
                person = person3,
                ytelseType = YtelseType.UTVIDET_BARNETRYGD
            ),
        )

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2021-11"),
                tom = YearMonth.parse("2021-12"),
                beløp = 827,
                person = person1,
                prosent = BigDecimal(50)
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2022-01"),
                tom = YearMonth.parse("2024-02"),
                beløp = 838,
                person = person1,
                prosent = BigDecimal(50)
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2024-03"),
                tom = YearMonth.parse("2036-02"),
                beløp = 527,
                person = person1,
                prosent = BigDecimal(50)
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2021-07"),
                tom = YearMonth.parse("2029-10"),
                beløp = 1054,
                person = person2
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2022-02"),
                tom = YearMonth.parse("2029-10"),
                beløp = 1054,
                person = person3,
                ytelseType = YtelseType.UTVIDET_BARNETRYGD
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2029-11"),
                tom = YearMonth.parse("2036-02"),
                beløp = 527,
                person = person3,
                prosent = BigDecimal(50),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD
            ),
        )

        val førsteEndringstidspunkt = andeler.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler
        )
        assertEquals(YearMonth.parse("2021-11").førsteDagIInneværendeMåned(), førsteEndringstidspunkt)
    }

    @Test
    fun `skal finne endringer i kompetanse perioder når kompetanse perioder er delt opp i revurdering`() {
        val annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV
        val forrigeKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2021, 11),
                tom = YearMonth.of(2021, 12),
                annenForeldersAktivitet = annenForeldersAktivitet
            ),
            lagKompetanse(fom = YearMonth.of(2022, 1))
        )
        val nyKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2021, 11),
                tom = YearMonth.of(2021, 12),
                annenForeldersAktivitet = annenForeldersAktivitet
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 1),
                annenForeldersAktivitet = annenForeldersAktivitet
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 2),
                annenForeldersAktivitet = annenForeldersAktivitet
            )
        )
        assertEquals(
            LocalDate.of(2022, 1, 31),
            nyKompetansePerioder.finnFørsteEndringstidspunkt(forrigeKompetansePerioder)
        )
    }

    @Test
    fun `skal finne endringer i kompetanse perioder når revurdering har endringer på samme kompetanse perioder`() {
        val annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV
        val forrigeKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2021, 11),
                tom = YearMonth.of(2021, 12),
                søkersAktivitet = SøkersAktivitet.ARBEIDER_I_NORGE,
                annenForeldersAktivitet = annenForeldersAktivitet
            ),
            lagKompetanse(fom = YearMonth.of(2022, 1))
        )
        val nyKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2021, 11),
                tom = YearMonth.of(2021, 12),
                søkersAktivitet = SøkersAktivitet.SELVSTENDIG_NÆRINGSDRIVENDE,
                annenForeldersAktivitet = annenForeldersAktivitet
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 1),
                annenForeldersAktivitet = annenForeldersAktivitet
            )
        )
        assertEquals(
            LocalDate.of(2021, 11, 30),
            nyKompetansePerioder.finnFørsteEndringstidspunkt(forrigeKompetansePerioder)
        )
    }

    @Test
    fun `skal finne første endringspunkt når revurdering har flere endringer`() {
        val annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV
        val forrigeKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2021, 11),
                tom = YearMonth.of(2021, 12),
                søkersAktivitet = SøkersAktivitet.ARBEIDER_I_NORGE,
                annenForeldersAktivitet = annenForeldersAktivitet
            ),
            lagKompetanse(fom = YearMonth.of(2022, 1))
        )
        val nyKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2021, 11),
                tom = YearMonth.of(2021, 12),
                søkersAktivitet = SøkersAktivitet.MOTTAR_UFØRETRYGD_FRA_NORGE,
                annenForeldersAktivitet = annenForeldersAktivitet
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 1),
                annenForeldersAktivitet = annenForeldersAktivitet
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 2),
                annenForeldersAktivitet = annenForeldersAktivitet
            )
        )
        assertEquals(
            LocalDate.of(2021, 11, 30),
            nyKompetansePerioder.finnFørsteEndringstidspunkt(forrigeKompetansePerioder)
        )
    }

    @Test
    fun `skal ikke finne endringer når kompetanse perioder ikke endres`() {
        val annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV
        val forrigeKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2021, 11),
                tom = YearMonth.of(2021, 12),
                søkersAktivitet = SøkersAktivitet.ARBEIDER_I_NORGE,
                annenForeldersAktivitet = annenForeldersAktivitet
            ),
            lagKompetanse(fom = YearMonth.of(2022, 1))
        )
        val nyKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2021, 11),
                tom = YearMonth.of(2021, 12),
                søkersAktivitet = SøkersAktivitet.ARBEIDER_I_NORGE,
                annenForeldersAktivitet = annenForeldersAktivitet
            ),
            lagKompetanse(fom = YearMonth.of(2022, 1))
        )
        assertEquals(
            TIDENES_ENDE,
            nyKompetansePerioder.finnFørsteEndringstidspunkt(forrigeKompetansePerioder)
        )
    }
}
