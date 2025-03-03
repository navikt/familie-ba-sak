package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class SmåbarnstilleggUtilsTest {
    @BeforeEach
    fun førHverTest() {
        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2022, 12, 31)
    }

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    @Test
    fun `Skal generere tidslinje for barn med rett til småbarnstillegg kun hvor barn er under 3 år`() {
        val barn = lagPerson(fødselsdato = LocalDate.now().minusYears(4), type = PersonType.BARN)

        val barnasAndeler =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = barn.fødselsdato.plusMonths(1).toYearMonth(),
                    tom = YearMonth.now(),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn,
                ),
            )

        val generertePerioder =
            lagTidslinjeForPerioderMedBarnSomGirRettTilSmåbarnstillegg(
                barnasAndeler = barnasAndeler,
                barnasAktørerOgFødselsdatoer = listOf(Pair(barn.aktør, barn.fødselsdato)),
            ).tilPerioder()

        assertEquals(1, generertePerioder.size)
        assertEquals(barn.fødselsdato.plusMonths(1).toYearMonth(), generertePerioder.single().fom?.toYearMonth())
        assertEquals(barn.fødselsdato.plusYears(3).toYearMonth(), generertePerioder.single().tom?.toYearMonth())
        assertEquals(BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_UTBETALING, generertePerioder.single().verdi)
    }

    @Test
    fun `Skal generere tidslinje for barn med rett til småbarnstillegg med riktig utbetalings-info for ett barn`() {
        val barn = lagPerson(fødselsdato = LocalDate.now().minusYears(4), type = PersonType.BARN)

        val brytningstidspunkt = LocalDate.now().minusYears(3)

        val barnasAndeler =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = barn.fødselsdato.plusMonths(1).toYearMonth(),
                    tom = brytningstidspunkt.toYearMonth(),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn,
                    prosent = BigDecimal.ZERO,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = brytningstidspunkt.plusMonths(1).toYearMonth(),
                    tom = YearMonth.now(),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn,
                ),
            )

        val generertePerioder =
            lagTidslinjeForPerioderMedBarnSomGirRettTilSmåbarnstillegg(
                barnasAndeler = barnasAndeler,
                barnasAktørerOgFødselsdatoer = listOf(Pair(barn.aktør, barn.fødselsdato)),
            ).tilPerioder().sortedBy { it.fom }

        assertEquals(2, generertePerioder.size)
        assertEquals(barn.fødselsdato.plusMonths(1).toYearMonth(), generertePerioder.first().fom?.toYearMonth())
        assertEquals(brytningstidspunkt.toYearMonth(), generertePerioder.first().tom?.toYearMonth())
        assertEquals(BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_NULLUTBETALING, generertePerioder.first().verdi)

        assertEquals(brytningstidspunkt.plusMonths(1).toYearMonth(), generertePerioder.last().fom?.toYearMonth())
        assertEquals(barn.fødselsdato.plusYears(3).toYearMonth(), generertePerioder.last().tom?.toYearMonth())
        assertEquals(BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_UTBETALING, generertePerioder.last().verdi)
    }

    @Test
    fun `Skal generere tidslinje for barn med rett til småbarnstillegg med riktig utbetalings-info når det er flere barn`() {
        val barn1 = lagPerson(fødselsdato = LocalDate.now().minusYears(4), type = PersonType.BARN)
        val barn2 = lagPerson(fødselsdato = LocalDate.now().minusYears(6), type = PersonType.BARN)
        val barn3 = lagPerson(fødselsdato = LocalDate.now().minusYears(1), type = PersonType.BARN)

        val brytningstidspunkt1 = LocalDate.now().minusYears(3).minusMonths(6)
        val brytningstidspunkt2 = LocalDate.now().minusYears(2)

        val barnasAndeler =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = barn1.fødselsdato.plusMonths(1).toYearMonth(),
                    tom = brytningstidspunkt1.toYearMonth(),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn1,
                    prosent = BigDecimal.ZERO,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = brytningstidspunkt1.plusMonths(1).toYearMonth(),
                    tom = brytningstidspunkt2.toYearMonth(),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn1,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = brytningstidspunkt2.plusMonths(1).toYearMonth(),
                    tom = YearMonth.now().plusYears(5),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn1,
                    prosent = BigDecimal.ZERO,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = barn2.fødselsdato.plusMonths(1).toYearMonth(),
                    tom = YearMonth.now().plusYears(5),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn2,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = barn3.fødselsdato.plusMonths(1).toYearMonth(),
                    tom = YearMonth.now(),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn3,
                ),
            )

        val generertePerioder =
            lagTidslinjeForPerioderMedBarnSomGirRettTilSmåbarnstillegg(
                barnasAndeler = barnasAndeler,
                barnasAktørerOgFødselsdatoer =
                    listOf(
                        Pair(barn1.aktør, barn1.fødselsdato),
                        Pair(barn2.aktør, barn2.fødselsdato),
                        Pair(barn3.aktør, barn3.fødselsdato),
                    ),
            ).tilPerioder().sortedBy { it.fom }

        assertEquals(3, generertePerioder.size)
        assertEquals(barn2.fødselsdato.plusMonths(1).toYearMonth(), generertePerioder.first().fom?.toYearMonth())
        assertEquals(brytningstidspunkt2.toYearMonth(), generertePerioder.first().tom?.toYearMonth())
        assertEquals(BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_UTBETALING, generertePerioder.first().verdi)

        assertEquals(brytningstidspunkt2.plusMonths(1).toYearMonth(), generertePerioder[1].fom?.toYearMonth())
        assertEquals(barn3.fødselsdato.toYearMonth(), generertePerioder[1].tom?.toYearMonth())
        assertEquals(BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_NULLUTBETALING, generertePerioder[1].verdi)

        assertEquals(barn3.fødselsdato.plusMonths(1).toYearMonth(), generertePerioder[2].fom?.toYearMonth())
        assertEquals(LocalDate.now().toYearMonth(), generertePerioder[2].tom?.toYearMonth())
        assertEquals(BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_UTBETALING, generertePerioder[2].verdi)
    }

    @Test
    fun `Skal kun få småbarnstillegg når alle tre tidslinjene har oppfylt kravene`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val overgangsstønadPerioder =
            listOf(
                InternPeriodeOvergangsstønad(
                    personIdent = søker.aktør.aktivFødselsnummer(),
                    fomDato = LocalDate.now().minusYears(2),
                    tomDato = LocalDate.now().plusYears(1),
                ),
            )

        val utvidetAndeler =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.now().minusYears(3),
                    tom = YearMonth.now().plusYears(1),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    person = søker,
                ),
            )

        val barnSomGirRettTilSmåbarnstilleggTidslinje =
            listOf(
                periode(
                    verdi = BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_UTBETALING,
                    fom = YearMonth.now().minusYears(4),
                    tom = YearMonth.now().minusYears(1),
                ),
            ).tilTidslinje()

        val kombinertTidslinje =
            kombinerAlleTidslinjerTilProsentTidslinje(
                perioderMedFullOvergangsstønadTidslinje = overgangsstønadPerioder.tilTidslinje(),
                utvidetBarnetrygdTidslinje = utvidetAndeler.tilTidslinje(),
                barnSomGirRettTilSmåbarnstilleggTidslinje = barnSomGirRettTilSmåbarnstilleggTidslinje,
            )

        val perioderMedSmåbarnstillegg = kombinertTidslinje.tilPerioderIkkeNull()

        assertEquals(1, perioderMedSmåbarnstillegg.size)
        assertEquals(YearMonth.now().minusYears(2), perioderMedSmåbarnstillegg.single().fom?.toYearMonth())
        assertEquals(YearMonth.now().minusYears(1), perioderMedSmåbarnstillegg.single().tom?.toYearMonth())
        assertEquals(BigDecimal(100), perioderMedSmåbarnstillegg.single().verdi.prosent)
    }

    @Test
    fun `Skal få småbarnstillegg med nullutbetaling når utvidet eller barn er overstyrt til 0kr`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val brytningstidspunkt1 = YearMonth.now().minusYears(3)
        val brytningstidspunkt2 = YearMonth.now().minusYears(2)

        val overgangsstønadPerioder =
            listOf(
                InternPeriodeOvergangsstønad(
                    personIdent = søker.aktør.aktivFødselsnummer(),
                    fomDato = LocalDate.now().minusYears(5),
                    tomDato = LocalDate.now().plusYears(1),
                ),
            )

        val utvidetAndeler =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.now().minusYears(4),
                    tom = brytningstidspunkt1,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    person = søker,
                    prosent = BigDecimal.ZERO,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = brytningstidspunkt1.plusMonths(1),
                    tom = YearMonth.now(),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    person = søker,
                ),
            )

        val barnsSomGirRettTilSmåbarnstilleggTidslinje =
            listOf(
                periode(
                    verdi = BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_UTBETALING,
                    fom = YearMonth.now().minusYears(5),
                    tom = brytningstidspunkt2,
                ),
                periode(
                    verdi = BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_NULLUTBETALING,
                    fom = brytningstidspunkt2.plusMonths(1),
                    tom = YearMonth.now().minusYears(1),
                ),
            ).tilTidslinje()

        val kombinertTidslinje =
            kombinerAlleTidslinjerTilProsentTidslinje(
                perioderMedFullOvergangsstønadTidslinje = overgangsstønadPerioder.tilTidslinje(),
                utvidetBarnetrygdTidslinje = utvidetAndeler.tilTidslinje(),
                barnSomGirRettTilSmåbarnstilleggTidslinje = barnsSomGirRettTilSmåbarnstilleggTidslinje,
            )

        val perioderMedSmåbarnstillegg = kombinertTidslinje.tilPerioderIkkeNull().toList()

        assertEquals(3, perioderMedSmåbarnstillegg.size)

        assertEquals(YearMonth.now().minusYears(4), perioderMedSmåbarnstillegg[0].fom?.toYearMonth())
        assertEquals(brytningstidspunkt1, perioderMedSmåbarnstillegg[0].tom?.toYearMonth())
        assertEquals(BigDecimal.ZERO, perioderMedSmåbarnstillegg[0].verdi.prosent)

        assertEquals(brytningstidspunkt1.plusMonths(1), perioderMedSmåbarnstillegg[1].fom?.toYearMonth())
        assertEquals(brytningstidspunkt2, perioderMedSmåbarnstillegg[1].tom?.toYearMonth())
        assertEquals(BigDecimal(100), perioderMedSmåbarnstillegg[1].verdi.prosent)

        assertEquals(brytningstidspunkt2.plusMonths(1), perioderMedSmåbarnstillegg[2].fom?.toYearMonth())
        assertEquals(YearMonth.now().minusYears(1), perioderMedSmåbarnstillegg[2].tom?.toYearMonth())
        assertEquals(BigDecimal.ZERO, perioderMedSmåbarnstillegg[2].verdi.prosent)
    }
}
