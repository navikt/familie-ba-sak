package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.periode
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    @Test
    fun `Skal svare true om at nye perioder med full OS påvirker behandling`() {
        val personIdent = randomFnr()
        val barnAktør = tilAktør(randomFnr())

        val påvirkerFagsak =
            vedtakOmOvergangsstønadPåvirkerFagsak(
                småbarnstilleggBarnetrygdGenerator =
                    SmåbarnstilleggBarnetrygdGenerator(
                        behandlingId = 1L,
                        tilkjentYtelse = lagInitiellTilkjentYtelse(),
                    ),
                nyePerioderMedFullOvergangsstønad =
                    listOf(
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent,
                            fomDato = LocalDate.now().minusMonths(6),
                            tomDato = LocalDate.now().plusMonths(6),
                        ),
                    ),
                forrigeAndelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                            person = tilfeldigPerson(aktør = barnAktør),
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                            person = tilfeldigPerson(aktør = tilAktør(personIdent)),
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.SMÅBARNSTILLEGG,
                            person = tilfeldigPerson(aktør = tilAktør(personIdent)),
                        ),
                    ),
                barnasAktørerOgFødselsdatoer = listOf(Pair(barnAktør, LocalDate.now().minusYears(2))),
            )

        assertTrue(påvirkerFagsak)
    }

    @Test
    fun `Skal svare false om at nye perioder med full OS påvirker behandling`() {
        val personIdent = randomAktør()
        val barnIdent = randomAktør()

        val påvirkerFagsak =
            vedtakOmOvergangsstønadPåvirkerFagsak(
                småbarnstilleggBarnetrygdGenerator =
                    SmåbarnstilleggBarnetrygdGenerator(
                        behandlingId = 1L,
                        tilkjentYtelse = lagInitiellTilkjentYtelse(),
                    ),
                nyePerioderMedFullOvergangsstønad =
                    listOf(
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now().minusMonths(10),
                            tomDato = LocalDate.now().plusMonths(6),
                        ),
                    ),
                forrigeAndelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                            person = tilfeldigPerson(aktør = barnIdent),
                            aktør = barnIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.SMÅBARNSTILLEGG,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                    ),
                barnasAktørerOgFødselsdatoer = listOf(Pair(barnIdent, LocalDate.now().minusYears(2))),
            )

        assertFalse(påvirkerFagsak)
    }

    @Test
    fun `Skal svare false om at nye perioder med full OS påvirker behandling ved flere perioder`() {
        val personIdent = randomAktør()
        val barnIdent = randomAktør()

        val påvirkerFagsak =
            vedtakOmOvergangsstønadPåvirkerFagsak(
                småbarnstilleggBarnetrygdGenerator =
                    SmåbarnstilleggBarnetrygdGenerator(
                        behandlingId = 1L,
                        tilkjentYtelse = lagInitiellTilkjentYtelse(),
                    ),
                nyePerioderMedFullOvergangsstønad =
                    listOf(
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now().minusMonths(10),
                            tomDato = LocalDate.now().minusMonths(6),
                        ),
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now().minusMonths(4),
                            tomDato = LocalDate.now().plusMonths(2),
                        ),
                    ),
                forrigeAndelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                            person = tilfeldigPerson(aktør = barnIdent),
                            aktør = barnIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().minusMonths(6),
                            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().minusMonths(6),
                            ytelseType = YtelseType.SMÅBARNSTILLEGG,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(4),
                            tom = YearMonth.now().plusMonths(2),
                            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(4),
                            tom = YearMonth.now().plusMonths(2),
                            ytelseType = YtelseType.SMÅBARNSTILLEGG,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                    ),
                barnasAktørerOgFødselsdatoer = listOf(Pair(barnIdent, LocalDate.now().minusYears(2))),
            )

        assertFalse(påvirkerFagsak)
    }

    @Test
    fun `skal ikke behandle vedtak om overgangsstønad når vedtaket ikke fører til endring i utbetaling`() {
        val personIdent = randomAktør()
        val barnIdent = randomAktør()

        val påvirkerFagsak =
            vedtakOmOvergangsstønadPåvirkerFagsak(
                småbarnstilleggBarnetrygdGenerator =
                    SmåbarnstilleggBarnetrygdGenerator(
                        behandlingId = 1L,
                        tilkjentYtelse = lagInitiellTilkjentYtelse(),
                    ),
                nyePerioderMedFullOvergangsstønad =
                    listOf(
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now().minusMonths(10),
                            tomDato = LocalDate.now().minusMonths(1),
                        ),
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now(),
                            tomDato = LocalDate.now().plusMonths(6),
                        ),
                    ),
                forrigeAndelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(10),
                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                            person = tilfeldigPerson(aktør = barnIdent),
                            aktør = barnIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(10),
                            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.SMÅBARNSTILLEGG,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                    ),
                barnasAktørerOgFødselsdatoer = listOf(Pair(barnIdent, LocalDate.now().minusYears(2))),
            )

        assertFalse(påvirkerFagsak)
    }

    @Test
    fun `skal behandle vedtak om overgangsstønad når vedtaket fører til endring i utbetaling`() {
        val personIdent = randomAktør()
        val barnIdent = randomAktør()

        val påvirkerFagsak =
            vedtakOmOvergangsstønadPåvirkerFagsak(
                småbarnstilleggBarnetrygdGenerator =
                    SmåbarnstilleggBarnetrygdGenerator(
                        behandlingId = 1L,
                        tilkjentYtelse = lagInitiellTilkjentYtelse(),
                    ),
                nyePerioderMedFullOvergangsstønad =
                    listOf(
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now().minusMonths(10),
                            tomDato = LocalDate.now().minusMonths(1),
                        ),
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now(),
                            tomDato = LocalDate.now().plusMonths(8),
                        ),
                    ),
                forrigeAndelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(10),
                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                            person = tilfeldigPerson(aktør = barnIdent),
                            aktør = barnIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(10),
                            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.SMÅBARNSTILLEGG,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                    ),
                barnasAktørerOgFødselsdatoer = listOf(Pair(barnIdent, LocalDate.now().minusYears(2))),
            )

        assertTrue(påvirkerFagsak)
    }

    @Test
    fun `Skal legge til innvilgelsesbegrunnelse for småbarnstillegg`() {
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().førsteDagIInneværendeMåned(),
                tom = LocalDate.now().plusMonths(3).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING,
            )

        val oppdatertVedtaksperiodeMedBegrunnelser =
            finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
                vedtaksperioderMedBegrunnelser =
                    listOf(
                        vedtaksperiodeMedBegrunnelser,
                    ),
                innvilgetMånedPeriode =
                    MånedPeriode(
                        fom = YearMonth.now(),
                        tom = vedtaksperiodeMedBegrunnelser.tom!!.toYearMonth(),
                    ),
                redusertMånedPeriode = null,
            )

        assertNotNull(oppdatertVedtaksperiodeMedBegrunnelser)
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.any { it.standardbegrunnelse == Standardbegrunnelse.INNVILGET_SMÅBARNSTILLEGG })
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.none { it.standardbegrunnelse == Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD })
    }

    @Test
    fun `Skal legge til reduksjonsbegrunnelse for småbarnstillegg`() {
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().nesteMåned().førsteDagIInneværendeMåned(),
                tom = LocalDate.now().plusMonths(3).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING,
            )

        val oppdatertVedtaksperiodeMedBegrunnelser =
            finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
                vedtaksperioderMedBegrunnelser =
                    listOf(
                        vedtaksperiodeMedBegrunnelser,
                    ),
                innvilgetMånedPeriode = null,
                redusertMånedPeriode =
                    MånedPeriode(
                        fom = YearMonth.now().nesteMåned(),
                        tom = vedtaksperiodeMedBegrunnelser.tom!!.toYearMonth(),
                    ),
            )

        assertNotNull(oppdatertVedtaksperiodeMedBegrunnelser)
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.none { it.standardbegrunnelse == Standardbegrunnelse.INNVILGET_SMÅBARNSTILLEGG })
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.any { it.standardbegrunnelse == Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD })
    }

    @Test
    fun `Skal legge til reduksjonsbegrunnelse fra inneværende måned for småbarnstillegg`() {
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().førsteDagIInneværendeMåned(),
                tom = LocalDate.now().plusMonths(3).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING,
            )

        val oppdatertVedtaksperiodeMedBegrunnelser =
            finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
                vedtaksperioderMedBegrunnelser =
                    listOf(
                        vedtaksperiodeMedBegrunnelser,
                    ),
                innvilgetMånedPeriode = null,
                redusertMånedPeriode =
                    MånedPeriode(
                        fom = YearMonth.now(),
                        tom = vedtaksperiodeMedBegrunnelser.tom!!.toYearMonth(),
                    ),
            )

        assertNotNull(oppdatertVedtaksperiodeMedBegrunnelser)
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.none { it.standardbegrunnelse == Standardbegrunnelse.INNVILGET_SMÅBARNSTILLEGG })
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.any { it.standardbegrunnelse == Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD })
    }

    @Test
    fun `Skal kaste feil om det ikke finnes innvilget eller redusert periode å begrunne`() {
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().nesteMåned().førsteDagIInneværendeMåned(),
                tom = LocalDate.now().plusMonths(3).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING,
            )

        assertThrows<VedtaksperiodefinnerSmåbarnstilleggFeil> {
            finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
                vedtaksperioderMedBegrunnelser =
                    listOf(
                        vedtaksperiodeMedBegrunnelser,
                    ),
                innvilgetMånedPeriode = null,
                redusertMånedPeriode = null,
            )
        }
    }

    @Test
    fun `Skal kunne automatisk iverksette småbarnstillegg når endringer i OS kun er frem i tid`() {
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(2),
                    tom = YearMonth.now().minusMonths(10),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
            )

        val nyeAndeler =
            forrigeAndeler +
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.now(),
                        tom = YearMonth.now().plusMonths(2),
                        ytelseType = YtelseType.SMÅBARNSTILLEGG,
                    ),
                )

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) =
            hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
                forrigeSmåbarnstilleggAndeler = forrigeAndeler,
                nyeSmåbarnstilleggAndeler = nyeAndeler,
            )

        assertTrue(
            kanAutomatiskIverksetteSmåbarnstillegg(
                innvilgedeMånedPerioder = innvilgedeMånedPerioder,
                reduserteMånedPerioder = reduserteMånedPerioder,
            ),
        )
    }

    @Test
    fun `Skal ikke kunne automatisk iverksette småbarnstillegg når endringer i OS er tilbake og frem i tid`() {
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(2),
                    tom = YearMonth.now().minusMonths(10),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
            )

        val nyeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(2),
                    tom = YearMonth.now().minusMonths(5),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now(),
                    tom = YearMonth.now().plusMonths(2),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
            )

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) =
            hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
                forrigeSmåbarnstilleggAndeler = forrigeAndeler,
                nyeSmåbarnstilleggAndeler = nyeAndeler,
            )

        assertFalse(
            kanAutomatiskIverksetteSmåbarnstillegg(
                innvilgedeMånedPerioder = innvilgedeMånedPerioder,
                reduserteMånedPerioder = reduserteMånedPerioder,
            ),
        )
    }

    @Test
    fun `Skal ikke kunne automatisk iverksette småbarnstillegg når endringer i OS er 2 måneder frem i tid`() {
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(2),
                    tom = YearMonth.now().minusMonths(10),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
            )

        val nyeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(2),
                    tom = YearMonth.now().minusMonths(5),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().plusMonths(2),
                    tom = YearMonth.now().plusMonths(4),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
            )

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) =
            hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
                forrigeSmåbarnstilleggAndeler = forrigeAndeler,
                nyeSmåbarnstilleggAndeler = nyeAndeler,
            )

        assertFalse(
            kanAutomatiskIverksetteSmåbarnstillegg(
                innvilgedeMånedPerioder = innvilgedeMånedPerioder,
                reduserteMånedPerioder = reduserteMånedPerioder,
            ),
        )
    }

    @Test
    fun `Skal ikke kunne automatisk iverksette småbarnstillegg når reduksjon i OS kun tilbake i tid`() {
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(2),
                    tom = YearMonth.now().minusMonths(10),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
            )

        val nyeAndeler = emptyList<AndelTilkjentYtelse>()

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) =
            hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
                forrigeSmåbarnstilleggAndeler = forrigeAndeler,
                nyeSmåbarnstilleggAndeler = nyeAndeler,
            )

        assertFalse(
            kanAutomatiskIverksetteSmåbarnstillegg(
                innvilgedeMånedPerioder = innvilgedeMånedPerioder,
                reduserteMånedPerioder = reduserteMånedPerioder,
            ),
        )
    }
}
