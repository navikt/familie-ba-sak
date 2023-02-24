package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class EndringstidspunktUtilsTest {

    val barnAktør1 = Aktør(aktørId = "1111111111111")
    val barnAktør2 = Aktør(aktørId = "2222222222222")

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

    @Test
    fun `Skal finne endringer i beløp`() {
        val person1 = lagPerson()
        val person2 = lagPerson()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            )
        )

        val andeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 2108,
                person = person2
            )
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
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            )
        )

        val andeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
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
            )
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
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
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
            )
        )

        val andeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
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
            )
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
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            )
        )

        val andeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(1).minusMonths(2),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            )
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
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusMonths(15),
                tom = inneværendeMåned().minusMonths(13),
                beløp = 1054,
                person = person2
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            )
        )

        val andeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            )
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
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            )
        )

        val andeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusMonths(15),
                tom = inneværendeMåned().minusMonths(13),
                beløp = 1054,
                person = person2
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            )
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
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            )
        )

        val andeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(1),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person1
            )
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
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned(),
                beløp = 1054,
                person = person2
            )
        )

        val andeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 2108,
                person = person1
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusMonths(1),
                beløp = 1054,
                person = person2
            )
        )

        val førsteEndringstidspunkt = andeler.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndeler
        )
        assertEquals(inneværendeMåned().førsteDagIInneværendeMåned(), førsteEndringstidspunkt)
    }

    @Test
    fun `Skal finne 0kr endring`() {
        val person1 = lagPerson(type = PersonType.BARN)

        val forrigeAndeler = emptyList<AndelTilkjentYtelseMedEndreteUtbetalinger>()

        val andeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = inneværendeMåned().minusYears(4),
                tom = inneværendeMåned().minusYears(2),
                beløp = 0,
                person = person1
            )
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
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.parse("2021-07"),
                tom = YearMonth.parse("2029-10"),
                beløp = 1054,
                person = person2
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.parse("2022-02"),
                tom = YearMonth.parse("2029-10"),
                beløp = 1054,
                person = person3,
                ytelseType = YtelseType.UTVIDET_BARNETRYGD
            )
        )

        val andeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.parse("2021-11"),
                tom = YearMonth.parse("2021-12"),
                beløp = 827,
                person = person1,
                prosent = BigDecimal(50)
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.parse("2022-01"),
                tom = YearMonth.parse("2024-02"),
                beløp = 838,
                person = person1,
                prosent = BigDecimal(50)
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.parse("2024-03"),
                tom = YearMonth.parse("2036-02"),
                beløp = 527,
                person = person1,
                prosent = BigDecimal(50)
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.parse("2021-07"),
                tom = YearMonth.parse("2029-10"),
                beløp = 1054,
                person = person2
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.parse("2022-02"),
                tom = YearMonth.parse("2029-10"),
                beløp = 1054,
                person = person3,
                ytelseType = YtelseType.UTVIDET_BARNETRYGD
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.parse("2029-11"),
                tom = YearMonth.parse("2036-02"),
                beløp = 527,
                person = person3,
                prosent = BigDecimal(50),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD
            )
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
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            ),
            lagKompetanse(fom = YearMonth.of(2022, 1))
        )
        val nyKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2021, 11),
                tom = YearMonth.of(2021, 12),
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 1),
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 2),
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            )
        )
        assertEquals(
            YearMonth.of(2022, 1),
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
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            ),
            lagKompetanse(fom = YearMonth.of(2022, 1))
        )
        val nyKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2021, 11),
                tom = YearMonth.of(2021, 12),
                søkersAktivitet = SøkersAktivitet.SELVSTENDIG_NÆRINGSDRIVENDE,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 1),
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            )
        )
        assertEquals(
            YearMonth.of(2021, 11),
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
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            ),
            lagKompetanse(fom = YearMonth.of(2022, 1))
        )
        val nyKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2021, 11),
                tom = YearMonth.of(2021, 12),
                søkersAktivitet = SøkersAktivitet.MOTTAR_UFØRETRYGD,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 1),
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 2),
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            )
        )
        assertEquals(
            YearMonth.of(2021, 11),
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
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 1),
                barnAktører = setOf(barnAktør1)
            )
        )
        val nyKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2021, 11),
                tom = YearMonth.of(2021, 12),
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 1),
                barnAktører = setOf(barnAktør1)
            )
        )
        assertEquals(
            TIDENES_ENDE.toYearMonth(),
            nyKompetansePerioder.finnFørsteEndringstidspunkt(forrigeKompetansePerioder)
        )
    }

    @Test
    fun `Skal kunne håndtere at vi legger til kompetanse på andre barn`() {
        val annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV
        val forrigeKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2022, 6),
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            )
        )
        val nyKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2022, 6),
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 6),
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør2)
            )
        )
        assertEquals(
            YearMonth.of(2022, 6),
            nyKompetansePerioder.finnFørsteEndringstidspunkt(forrigeKompetansePerioder)
        )
    }

    @Test
    fun `Skal kunne håndtere at vi fjerner kompetanse fra et barn`() {
        val annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV
        val forrigeKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2022, 6),
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            ),
            lagKompetanse(
                fom = YearMonth.of(2022, 6),
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør2)
            )
        )
        val nyKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2022, 6),
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            )
        )
        assertEquals(
            YearMonth.of(2022, 6),
            nyKompetansePerioder.finnFørsteEndringstidspunkt(forrigeKompetansePerioder)
        )
    }

    @Test
    fun `Skal kunne håndtere tomme kompetanser`() {
        val forrigeKompetansePerioder = emptyList<Kompetanse>()
        val nyKompetansePerioder = emptyList<Kompetanse>()
        assertEquals(
            TIDENES_ENDE.toYearMonth(),
            nyKompetansePerioder.finnFørsteEndringstidspunkt(forrigeKompetansePerioder)
        )
    }

    @Test
    fun `Skal kunne håndtere at nye kompetanser er tom`() {
        val annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV
        val forrigeKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2022, 6),
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            )
        )
        val nyKompetansePerioder = emptyList<Kompetanse>()
        assertEquals(
            YearMonth.of(2022, 6),
            nyKompetansePerioder.finnFørsteEndringstidspunkt(forrigeKompetansePerioder)
        )
    }

    @Test
    fun `Skal kunne håndtere at forrige kompetanser er tom`() {
        val annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV
        val forrigeKompetansePerioder = emptyList<Kompetanse>()
        val nyKompetansePerioder = listOf(
            lagKompetanse(
                fom = YearMonth.of(2022, 6),
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet = annenForeldersAktivitet,
                barnAktører = setOf(barnAktør1)
            )
        )
        assertEquals(
            YearMonth.of(2022, 6),
            nyKompetansePerioder.finnFørsteEndringstidspunkt(forrigeKompetansePerioder)
        )
    }
}
