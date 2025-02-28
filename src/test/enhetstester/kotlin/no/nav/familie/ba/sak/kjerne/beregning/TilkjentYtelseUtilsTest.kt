package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseGenerator.genererTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

internal class TilkjentYtelseUtilsTest {
    @BeforeEach
    fun førHverTest() {
        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2022, 12, 31)
    }

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    val søker = lagPerson(type = PersonType.SØKER)
    val januar2019 = YearMonth.of(2019, 1)
    val februar2019 = YearMonth.of(2019, 2)
    val mars2019 = YearMonth.of(2019, 3)
    val april2019 = YearMonth.of(2019, 4)
    val juli2019 = YearMonth.of(2019, 7)
    val august2019 = YearMonth.of(2019, 8)
    val november2019 = YearMonth.of(2019, 11)
    val desember2019 = YearMonth.of(2019, 12)
    val august2020 = YearMonth.of(2020, 8)
    val januar2022 = YearMonth.of(2022, 1)
    val februar2022 = YearMonth.of(2022, 2)
    val mars2022 = YearMonth.of(2022, 3)
    val april2022 = YearMonth.of(2022, 4)
    val mai2022 = YearMonth.of(2022, 5)
    val juni2022 = YearMonth.of(2022, 6)
    val juli2022 = YearMonth.of(2022, 7)
    val august2022 = YearMonth.of(2022, 8)
    val november2022 = YearMonth.of(2022, 11)
    val desember2022 = YearMonth.of(2022, 12)

    // src/test/resources/scenario/Far søker om delt bosted - Mor har tidligere mottatt fult utvidet og ordinær barnetrygd
    @Test
    fun `Skal støtte endret utbetaling som delvis overlapper delt bosted på søker og barn og småbarnstillegg på søker`() {
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val månedFørBarnBlir18 =
            barnFødtAugust2019.fødselsdato
                .til18ÅrsVilkårsdato()
                .minusMonths(1)
                .toYearMonth()
        val månedFørBarnBlir6 =
            barnFødtAugust2019.fødselsdato
                .plusYears(6)
                .minusMonths(1)
                .toYearMonth()
        val barnFyller3ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(3).toYearMonth()

        val tilkjentYtelse =
            settOppScenarioOgBeregnTilkjentYtelse(
                endretAndeler =
                    listOf(
                        EndretAndel(
                            person = søker,
                            skalUtbetales = false,
                            årsak = Årsak.DELT_BOSTED,
                            fom = april2022,
                            tom = juli2022,
                        ),
                        EndretAndel(
                            person = barnFødtAugust2019,
                            skalUtbetales = false,
                            årsak = Årsak.DELT_BOSTED,
                            fom = april2022,
                            tom = juli2022,
                        ),
                    ),
                atypiskeVilkårBarna =
                    listOf(
                        AtypiskVilkår(
                            fom = mars2022.toLocalDate().førsteDagIInneværendeMåned(),
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED,
                            aktør = barnFødtAugust2019.aktør,
                        ),
                    ),
                barna = listOf(barnFødtAugust2019),
                overgangsstønadPerioder = listOf(MånedPeriode(januar2022, november2022)),
            )

        val andelerTilkjentYtelseITidsrom =
            tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(desember2022) }
        assertEquals(6, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(2, utvidetAndeler.size)
        assertThat(utvidetAndeler[0].stønadFom, Is(april2022))
        assertThat(utvidetAndeler[0].stønadTom, Is(juli2022))
        assertThat(utvidetAndeler[0].prosent, Is(BigDecimal.ZERO))
        assertThat(utvidetAndeler[1].stønadFom, Is(august2022))
        assertThat(utvidetAndeler[1].stønadTom, Is(månedFørBarnBlir18))
        assertThat(utvidetAndeler[1].prosent, Is(BigDecimal(50)))

        assertEquals(2, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, Is(april2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, Is(juli2022))
        assertThat(småbarnstilleggAndeler[0].prosent, Is(BigDecimal.ZERO))
        assertThat(småbarnstilleggAndeler[1].stønadFom, Is(august2022))
        assertThat(småbarnstilleggAndeler[1].stønadTom, Is(barnFyller3ÅrDato))
        assertThat(småbarnstilleggAndeler[1].prosent, Is(BigDecimal(100)))

        assertEquals(2, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, Is(april2022))
        assertThat(barnasAndeler[0].stønadTom, Is(juli2022))
        assertThat(barnasAndeler[0].prosent, Is(BigDecimal.ZERO))
        assertThat(barnasAndeler[1].stønadFom, Is(august2022))
        assertThat(barnasAndeler[1].stønadTom, Is(månedFørBarnBlir6))
        assertThat(barnasAndeler[1].prosent, Is(BigDecimal(50)))
    }

    // src/test/resources/scenario/Far søker om delt bosted - Mor har tidligere mottatt fult, men har ikke mottatt utvidet
    @Test
    fun `Skal støtte endret utbetaling som kun gjelder barn på delt bosted utbetaling`() {
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val månedFørBarnBlir18 =
            barnFødtAugust2019.fødselsdato
                .til18ÅrsVilkårsdato()
                .minusMonths(1)
                .toYearMonth()
        val månedFørBarnBlir6 =
            barnFødtAugust2019.fødselsdato
                .plusYears(6)
                .minusMonths(1)
                .toYearMonth()
        val barnFyller3ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(3).toYearMonth()

        val tilkjentYtelse =
            settOppScenarioOgBeregnTilkjentYtelse(
                endretAndeler =
                    listOf(
                        EndretAndel(
                            person = barnFødtAugust2019,
                            skalUtbetales = false,
                            årsak = Årsak.DELT_BOSTED,
                            fom = april2022,
                            tom = juli2022,
                        ),
                    ),
                atypiskeVilkårBarna =
                    listOf(
                        AtypiskVilkår(
                            fom = mars2022.toLocalDate().førsteDagIInneværendeMåned(),
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED,
                            aktør = barnFødtAugust2019.aktør,
                        ),
                    ),
                barna = listOf(barnFødtAugust2019),
                overgangsstønadPerioder = listOf(MånedPeriode(januar2022, november2022)),
            )

        val andelerTilkjentYtelseITidsrom =
            tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(desember2022) }
        assertEquals(4, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(1, utvidetAndeler.size)
        assertThat(utvidetAndeler[0].stønadFom, Is(april2022))
        assertThat(utvidetAndeler[0].stønadTom, Is(månedFørBarnBlir18))
        assertThat(utvidetAndeler[0].prosent, Is(BigDecimal(50)))

        assertEquals(1, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, Is(april2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, Is(barnFyller3ÅrDato))
        assertThat(småbarnstilleggAndeler[0].prosent, Is(BigDecimal(100)))

        // BARN
        assertEquals(2, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, Is(april2022))
        assertThat(barnasAndeler[0].stønadTom, Is(juli2022))
        assertThat(barnasAndeler[0].prosent, Is(BigDecimal.ZERO))
        assertThat(barnasAndeler[1].stønadFom, Is(august2022))
        assertThat(barnasAndeler[1].stønadTom, Is(månedFørBarnBlir6))
        assertThat(barnasAndeler[1].prosent, Is(BigDecimal(50)))
    }

    // src/test/resources/scenario/Mor har tidligere mottatt barnetrygden - Far har nå søkt om delt bosted og mors barnetrygd skal også deles
    @Test
    fun `Skal gi riktig resultat når barnetrygden går over til å være delt, kun småbarnstillegg og utvidet blir delt i første periode`() {
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val månedFørBarnBlir18 =
            barnFødtAugust2019.fødselsdato
                .til18ÅrsVilkårsdato()
                .minusMonths(1)
                .toYearMonth()
        val månedFørBarnBlir6 =
            barnFødtAugust2019.fødselsdato
                .plusYears(6)
                .minusMonths(1)
                .toYearMonth()
        val barnFyller3ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(3).toYearMonth()

        val tilkjentYtelse =
            settOppScenarioOgBeregnTilkjentYtelse(
                endretAndeler =
                    listOf(
                        EndretAndel(
                            person = barnFødtAugust2019,
                            skalUtbetales = true,
                            årsak = Årsak.DELT_BOSTED,
                            fom = juni2022,
                            tom = juli2022,
                        ),
                    ),
                atypiskeVilkårBarna =
                    listOf(
                        AtypiskVilkår(
                            fom = februar2022.toLocalDate().førsteDagIInneværendeMåned(),
                            tom = april2022.toLocalDate().sisteDagIMåned(),
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            aktør = barnFødtAugust2019.aktør,
                        ),
                        AtypiskVilkår(
                            fom = mai2022.toLocalDate().førsteDagIInneværendeMåned(),
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            aktør = barnFødtAugust2019.aktør,
                            utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED,
                        ),
                    ),
                atypiskeVilkårSøker =
                    listOf(
                        AtypiskVilkår(
                            fom = mai2022.toLocalDate().førsteDagIInneværendeMåned(),
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            aktør = søker.aktør,
                        ),
                    ),
                overgangsstønadPerioder = listOf(MånedPeriode(januar2022, november2022)),
                barna = listOf(barnFødtAugust2019),
            )

        val andelerTilkjentYtelseITidsrom =
            tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(desember2022) }
        assertEquals(5, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(1, utvidetAndeler.size)
        assertThat(utvidetAndeler[0].stønadFom, Is(juni2022))
        assertThat(utvidetAndeler[0].stønadTom, Is(månedFørBarnBlir18))
        assertThat(utvidetAndeler[0].prosent, Is(BigDecimal(50)))

        assertEquals(1, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, Is(juni2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, Is(barnFyller3ÅrDato))
        assertThat(småbarnstilleggAndeler[0].prosent, Is(BigDecimal(100)))

        // BARN
        assertEquals(3, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, Is(mars2022))
        assertThat(barnasAndeler[0].stønadTom, Is(mai2022))
        assertThat(barnasAndeler[0].prosent, Is(BigDecimal(100)))
        assertThat(barnasAndeler[1].stønadFom, Is(juni2022))
        assertThat(barnasAndeler[1].stønadTom, Is(juli2022))
        assertThat(barnasAndeler[1].prosent, Is(BigDecimal(100)))
        assertThat(barnasAndeler[2].stønadFom, Is(august2022))
        assertThat(barnasAndeler[2].stønadTom, Is(månedFørBarnBlir6))
        assertThat(barnasAndeler[2].prosent, Is(BigDecimal(50)))
    }

    // src/test/resources/scenario/Mor har tidligere mottatt barnetrygden - Far har nå søkt om delt bosted og mors barnetrygd skal også deles 2
    @Test
    fun `Delt, utvidet og ordinær barnetrygd deles fra juni, men skal utbetales fult fra juni til og med juli - deles som vanlig fra August`() {
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val månedFørBarnBlir18 =
            barnFødtAugust2019.fødselsdato
                .til18ÅrsVilkårsdato()
                .minusMonths(1)
                .toYearMonth()
        val månedFørBarnBlir6 =
            barnFødtAugust2019.fødselsdato
                .plusYears(6)
                .minusMonths(1)
                .toYearMonth()
        val barnFyller3ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(3).toYearMonth()

        val tilkjentYtelse =
            settOppScenarioOgBeregnTilkjentYtelse(
                endretAndeler =
                    listOf(
                        EndretAndel(
                            person = barnFødtAugust2019,
                            skalUtbetales = true,
                            årsak = Årsak.DELT_BOSTED,
                            fom = juni2022,
                            tom = juli2022,
                        ),
                        EndretAndel(
                            person = søker,
                            skalUtbetales = true,
                            årsak = Årsak.DELT_BOSTED,
                            fom = juni2022,
                            tom = juli2022,
                        ),
                    ),
                atypiskeVilkårBarna =
                    listOf(
                        AtypiskVilkår(
                            fom = februar2022.toLocalDate().førsteDagIInneværendeMåned(),
                            tom = april2022.toLocalDate().sisteDagIMåned(),
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            aktør = barnFødtAugust2019.aktør,
                        ),
                        AtypiskVilkår(
                            fom = mai2022.toLocalDate().førsteDagIInneværendeMåned(),
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            aktør = barnFødtAugust2019.aktør,
                            utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED,
                        ),
                    ),
                atypiskeVilkårSøker =
                    listOf(
                        AtypiskVilkår(
                            fom = februar2022.toLocalDate().førsteDagIInneværendeMåned(),
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            aktør = søker.aktør,
                        ),
                    ),
                barna = listOf(barnFødtAugust2019),
                overgangsstønadPerioder = listOf(MånedPeriode(januar2022, november2022)),
            )

        val andelerTilkjentYtelseITidsrom =
            tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(desember2022) }
        assertEquals(7, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(3, utvidetAndeler.size)
        assertThat(utvidetAndeler[0].stønadFom, Is(mars2022))
        assertThat(utvidetAndeler[0].stønadTom, Is(mai2022))
        assertThat(utvidetAndeler[0].prosent, Is(BigDecimal(100)))
        assertThat(utvidetAndeler[1].stønadFom, Is(juni2022))
        assertThat(utvidetAndeler[1].stønadTom, Is(juli2022))
        assertThat(utvidetAndeler[1].prosent, Is(BigDecimal(100)))
        assertThat(utvidetAndeler[2].stønadFom, Is(august2022))
        assertThat(utvidetAndeler[2].stønadTom, Is(månedFørBarnBlir18))
        assertThat(utvidetAndeler[2].prosent, Is(BigDecimal(50)))

        assertEquals(1, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, Is(mars2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, Is(barnFyller3ÅrDato))
        assertThat(småbarnstilleggAndeler[0].prosent, Is(BigDecimal(100)))

        assertEquals(3, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, Is(mars2022))
        assertThat(barnasAndeler[0].stønadTom, Is(mai2022))
        assertThat(barnasAndeler[0].prosent, Is(BigDecimal(100)))
        assertThat(barnasAndeler[1].stønadFom, Is(juni2022))
        assertThat(barnasAndeler[1].stønadTom, Is(juli2022))
        assertThat(barnasAndeler[1].prosent, Is(BigDecimal(100)))
        assertThat(barnasAndeler[2].stønadFom, Is(august2022))
        assertThat(barnasAndeler[2].stønadTom, Is(månedFørBarnBlir6))
        assertThat(barnasAndeler[2].prosent, Is(BigDecimal(50)))
    }

    // src/test/resources/scenario/Far søker om utvidet barnetrygd - Har full overgangsstønad, men søker sent og får ikke etterbetalt mer enn 3år
    @Test
    fun `Småbarnstillleg, utvidet og ordinær barnetrygd fra april, men skal ikke utbetales før august på grunn av etterbetaling 3 år`() {
        val barnFødtAugust2016 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2016, 8, 15))
        val månedFørBarnBlir18 =
            barnFødtAugust2016.fødselsdato
                .til18ÅrsVilkårsdato()
                .minusMonths(1)
                .toYearMonth()

        val tilkjentYtelse =
            settOppScenarioOgBeregnTilkjentYtelse(
                endretAndeler =
                    listOf(
                        EndretAndel(
                            person = barnFødtAugust2016,
                            skalUtbetales = false,
                            årsak = Årsak.ETTERBETALING_3ÅR,
                            fom = april2019,
                            tom = juli2019,
                        ),
                        EndretAndel(
                            person = søker,
                            skalUtbetales = false,
                            årsak = Årsak.ETTERBETALING_3ÅR,
                            fom = april2019,
                            tom = juli2019,
                        ),
                    ),
                atypiskeVilkårBarna =
                    listOf(
                        AtypiskVilkår(
                            fom = mars2019.toLocalDate().førsteDagIInneværendeMåned(),
                            tom = null,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            aktør = barnFødtAugust2016.aktør,
                        ),
                    ),
                barna = listOf(barnFødtAugust2016),
                overgangsstønadPerioder = listOf(MånedPeriode(januar2019, november2019)),
            )

        val andelerTilkjentYtelseITidsrom =
            tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(desember2019) }
        assertEquals(6, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(2, utvidetAndeler.size)
        assertThat(utvidetAndeler[0].stønadFom, Is(april2019))
        assertThat(utvidetAndeler[0].stønadTom, Is(juli2019))
        assertThat(utvidetAndeler[0].prosent, Is(BigDecimal(0)))
        assertThat(utvidetAndeler[1].stønadFom, Is(august2019))
        assertThat(utvidetAndeler[1].stønadTom, Is(månedFørBarnBlir18))
        assertThat(utvidetAndeler[1].prosent, Is(BigDecimal(100)))

        assertEquals(2, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, Is(april2019))
        assertThat(småbarnstilleggAndeler[0].stønadTom, Is(juli2019))
        assertThat(småbarnstilleggAndeler[0].prosent, Is(BigDecimal(0)))
        assertThat(småbarnstilleggAndeler[1].stønadFom, Is(august2019))
        assertThat(småbarnstilleggAndeler[1].stønadTom, Is(august2019))
        assertThat(småbarnstilleggAndeler[1].prosent, Is(BigDecimal(100)))

        // BARN
        assertEquals(2, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, Is(april2019))
        assertThat(barnasAndeler[0].stønadTom, Is(juli2019))
        assertThat(barnasAndeler[0].prosent, Is(BigDecimal(0)))
        assertThat(barnasAndeler[1].stønadFom, Is(august2019))
        assertThat(barnasAndeler[1].stønadTom, Is(august2020))
        assertThat(barnasAndeler[1].prosent, Is(BigDecimal(100)))
    }

    // src/test/resources/scenario/Far har mottatt delt utvidet barnetrygd for barn 12år - Søker nå om barnetrygd for barn som flyttet til han for over 3 år siden
    @Test
    fun `Det er småbarnstillegg på søker og ordinær barnetrygd på barn 1 fra april, men det skal ikke utbetales før august på grunn av etterbetaling 3 år - Søker og barn 2 har utbetalinger fra tidligere behandlinger som ikke skal overstyres`() {
        val barnFødtAugust2016 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2016, 8, 15))
        val månedFørBarnFødtAugust2016Blir18 =
            barnFødtAugust2016.fødselsdato
                .til18ÅrsVilkårsdato()
                .minusMonths(1)
                .toYearMonth()
        val barnFødtDesember2006 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2006, 12, 1))
        val månedFørBarnFødtDesember2006Blir18 = barnFødtDesember2006.fødselsdato.til18ÅrsVilkårsdato().toYearMonth()

        val tilkjentYtelse =
            settOppScenarioOgBeregnTilkjentYtelse(
                endretAndeler =
                    listOf(
                        EndretAndel(
                            person = barnFødtAugust2016,
                            skalUtbetales = false,
                            årsak = Årsak.ETTERBETALING_3ÅR,
                            fom = april2019,
                            tom = juli2019,
                        ),
                    ),
                atypiskeVilkårBarna =
                    listOf(
                        AtypiskVilkår(
                            fom = mars2019.toLocalDate().førsteDagIInneværendeMåned(),
                            tom = null,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            aktør = barnFødtAugust2016.aktør,
                        ),
                        AtypiskVilkår(
                            fom = januar2019.toLocalDate().førsteDagIInneværendeMåned(),
                            tom = null,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            aktør = barnFødtDesember2006.aktør,
                            utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED,
                        ),
                    ),
                atypiskeVilkårSøker =
                    listOf(
                        AtypiskVilkår(
                            fom = februar2019.toLocalDate().førsteDagIInneværendeMåned(),
                            tom = null,
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            aktør = søker.aktør,
                        ),
                    ),
                barna = listOf(barnFødtAugust2016, barnFødtDesember2006),
                overgangsstønadPerioder = listOf(MånedPeriode(januar2019, november2019)),
            )

        val andelerTilkjentYtelseITidsrom =
            tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(desember2019) }
        assertEquals(8, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }
        assertEquals(2, utvidetAndeler.size)
        assertThat(utvidetAndeler[0].stønadFom, Is(mars2019))
        assertThat(utvidetAndeler[0].stønadTom, Is(juli2019))
        assertThat(utvidetAndeler[0].prosent, Is(BigDecimal(50)))
        assertThat(utvidetAndeler[1].stønadFom, Is(august2019))
        assertThat(utvidetAndeler[1].stønadTom, Is(månedFørBarnFødtAugust2016Blir18))
        assertThat(utvidetAndeler[1].prosent, Is(BigDecimal(100)))

        assertEquals(2, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, Is(april2019))
        assertThat(småbarnstilleggAndeler[0].stønadTom, Is(juli2019))
        assertThat(småbarnstilleggAndeler[0].prosent, Is(BigDecimal(0)))
        assertThat(småbarnstilleggAndeler[1].stønadFom, Is(august2019))
        assertThat(småbarnstilleggAndeler[1].stønadTom, Is(august2019))
        assertThat(småbarnstilleggAndeler[1].prosent, Is(BigDecimal(100)))

        // BARN
        val (barn1Andeler, barn2Andeler) = barnasAndeler.partition { it.aktør == barnFødtAugust2016.aktør }
        assertEquals(2, barn1Andeler.size)
        assertThat(barn1Andeler[0].stønadFom, Is(april2019))
        assertThat(barn1Andeler[0].stønadTom, Is(juli2019))
        assertThat(barn1Andeler[0].prosent, Is(BigDecimal(0)))
        assertThat(barn1Andeler[1].stønadFom, Is(august2019))
        assertThat(barn1Andeler[1].stønadTom, Is(august2020))
        assertThat(barn1Andeler[1].prosent, Is(BigDecimal(100)))

        assertEquals(2, barn2Andeler.size)
        assertThat(barn2Andeler[0].stønadFom, Is(februar2019))
        assertThat(barn2Andeler[0].stønadTom, Is(februar2019))
        assertThat(barn2Andeler[0].prosent, Is(BigDecimal(50)))
        assertThat(barn2Andeler[1].stønadFom, Is(mars2019))
        assertThat(barn2Andeler[1].stønadTom, Is(månedFørBarnFødtDesember2006Blir18))
        assertThat(barn2Andeler[1].prosent, Is(BigDecimal(50)))
    }

    // src/test/resources/scenario/Far søker om utvidet barnetrygd for barn under 3 år - han har full overgangsstlnad for bare deler av perioden
    @Test
    fun `Skal gi riktig resultat når det overgangsstønad i deler av utbetalingen`() {
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val månedFørBarnBlir6 =
            barnFødtAugust2019.fødselsdato
                .plusYears(6)
                .minusMonths(1)
                .toYearMonth()

        val tilkjentYtelse =
            settOppScenarioOgBeregnTilkjentYtelse(
                atypiskeVilkårBarna =
                    listOf(
                        AtypiskVilkår(
                            fom = februar2022.toLocalDate().førsteDagIInneværendeMåned(),
                            tom = null,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            aktør = barnFødtAugust2019.aktør,
                        ),
                    ),
                atypiskeVilkårSøker =
                    listOf(
                        AtypiskVilkår(
                            fom = januar2022.toLocalDate().førsteDagIInneværendeMåned(),
                            tom = august2022.toLocalDate().sisteDagIMåned(),
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            aktør = søker.aktør,
                        ),
                    ),
                barna = listOf(barnFødtAugust2019),
                overgangsstønadPerioder = listOf(MånedPeriode(april2022, juni2022)),
            )

        val andelerTilkjentYtelseITidsrom =
            tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(desember2022) }
        assertEquals(3, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(1, utvidetAndeler.size)
        assertThat(utvidetAndeler[0].stønadFom, Is(mars2022))
        assertThat(utvidetAndeler[0].stønadTom, Is(august2022))
        assertThat(utvidetAndeler[0].prosent, Is(BigDecimal(100)))

        assertEquals(1, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, Is(april2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, Is(juni2022))
        assertThat(småbarnstilleggAndeler[0].prosent, Is(BigDecimal(100)))

        assertEquals(1, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, Is(mars2022))
        assertThat(barnasAndeler[0].stønadTom, Is(månedFørBarnBlir6))
        assertThat(barnasAndeler[0].prosent, Is(BigDecimal(100)))
    }

    // src/test/resources/scenario/Far søker om utvidet barnetrygd for barn under 3år, men oppfyller vilkårene kun tilbake i tid
    @Test
    fun `Skal gi riktig resultat når det overgangsstønad i deler av utbetalingen - Overgangsstønaden stopper før barn fyller 3 år fordi søker ikke lenger har rett til utvidet barnetrygd`() {
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val månedFørBarnBlir6 =
            barnFødtAugust2019.fødselsdato
                .plusYears(6)
                .minusMonths(1)
                .toYearMonth()

        val tilkjentYtelse =
            settOppScenarioOgBeregnTilkjentYtelse(
                atypiskeVilkårBarna =
                    listOf(
                        AtypiskVilkår(
                            fom = februar2022.toLocalDate().førsteDagIInneværendeMåned(),
                            tom = null,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            aktør = barnFødtAugust2019.aktør,
                        ),
                    ),
                atypiskeVilkårSøker =
                    listOf(
                        AtypiskVilkår(
                            fom = januar2022.toLocalDate().førsteDagIInneværendeMåned(),
                            tom = juni2022.toLocalDate().sisteDagIMåned(),
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            aktør = søker.aktør,
                        ),
                    ),
                barna = listOf(barnFødtAugust2019),
                overgangsstønadPerioder = listOf(MånedPeriode(april2022, august2022)),
            )

        val andelerTilkjentYtelseITidsrom =
            tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(desember2022) }
        assertEquals(3, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(1, utvidetAndeler.size)
        assertThat(utvidetAndeler[0].stønadFom, Is(mars2022))
        assertThat(utvidetAndeler[0].stønadTom, Is(juni2022))
        assertThat(utvidetAndeler[0].prosent, Is(BigDecimal(100)))

        assertEquals(1, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, Is(april2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, Is(juni2022))
        assertThat(småbarnstilleggAndeler[0].prosent, Is(BigDecimal(100)))

        assertEquals(1, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, Is(mars2022))
        assertThat(barnasAndeler[0].stønadTom, Is(månedFørBarnBlir6))
        assertThat(barnasAndeler[0].prosent, Is(BigDecimal(100)))
    }

    // src/test/resources/scenario/Far søker om utvidet barnetrygd for barn under 3 år - Har full overgangsstønad som opphører når barnet fyller 3 år
    @Test
    fun `Skal gi riktig resultat når søker har rett på ordinær og utvidet barnetrygd fra mars og rett på overgangsstønad fra April`() {
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val månedFørBarnBlir18 =
            barnFødtAugust2019.fødselsdato
                .til18ÅrsVilkårsdato()
                .minusMonths(1)
                .toYearMonth()
        val månedFørBarnBlir6 =
            barnFødtAugust2019.fødselsdato
                .plusYears(6)
                .minusMonths(1)
                .toYearMonth()

        val tilkjentYtelse =
            settOppScenarioOgBeregnTilkjentYtelse(
                atypiskeVilkårBarna =
                    listOf(
                        AtypiskVilkår(
                            fom = februar2022.toLocalDate().førsteDagIInneværendeMåned(),
                            tom = null,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            aktør = barnFødtAugust2019.aktør,
                        ),
                    ),
                overgangsstønadPerioder = listOf(MånedPeriode(april2022, desember2022)),
                barna = listOf(barnFødtAugust2019),
            )

        val andelerTilkjentYtelseITidsrom =
            tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(desember2022) }

        assertEquals(3, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(1, utvidetAndeler.size)
        assertThat(utvidetAndeler[0].stønadFom, Is(mars2022))
        assertThat(utvidetAndeler[0].stønadTom, Is(månedFørBarnBlir18))
        assertThat(utvidetAndeler[0].prosent, Is(BigDecimal(100)))

        assertEquals(1, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, Is(april2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, Is(august2022))
        assertThat(småbarnstilleggAndeler[0].prosent, Is(BigDecimal(100)))

        assertEquals(1, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, Is(mars2022))
        assertThat(barnasAndeler[0].stønadTom, Is(månedFørBarnBlir6))
        assertThat(barnasAndeler[0].prosent, Is(BigDecimal(100)))
    }

    private data class EndretAndel(
        val fom: YearMonth,
        val tom: YearMonth,
        val person: Person,
        val årsak: Årsak,
        val skalUtbetales: Boolean,
    )

    private fun settOppScenarioOgBeregnTilkjentYtelse(
        endretAndeler: List<EndretAndel> = emptyList(),
        atypiskeVilkårBarna: List<AtypiskVilkår> = emptyList(),
        atypiskeVilkårSøker: List<AtypiskVilkår> = emptyList(),
        barna: List<Person>,
        overgangsstønadPerioder: List<MånedPeriode>,
    ): TilkjentYtelse {
        val vilkårsvurdering =
            lagVilkårsvurdering(
                søker = søker,
                barn = barna,
                atypiskeVilkårBarna = atypiskeVilkårBarna,
                atypiskeVilkårSøker = atypiskeVilkårSøker,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
            )

        val endretUtbetalingAndeler =
            endretAndeler.map {
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    behandlingId = vilkårsvurdering.behandling.id,
                    person = it.person,
                    prosent = if (it.skalUtbetales) BigDecimal(100) else BigDecimal.ZERO,
                    årsak = it.årsak,
                    fom = it.fom,
                    tom = it.tom,
                )
            }

        val tilkjentYtelse =
            genererTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag =
                    lagPersonopplysningsgrunnlag(
                        personer = barna.plus(søker),
                        behandlingId = vilkårsvurdering.behandling.id,
                    ),
                endretUtbetalingAndeler = endretUtbetalingAndeler,
                fagsakType = FagsakType.NORMAL,
            ) { (_) ->
                lagOvergangsstønadPerioder(
                    perioder = overgangsstønadPerioder,
                    søkerIdent = søker.aktør.aktivFødselsnummer(),
                )
            }

        return tilkjentYtelse
    }

    private fun lagPersonopplysningsgrunnlag(
        personer: List<Person>,
        behandlingId: Long,
    ): PersonopplysningGrunnlag =
        PersonopplysningGrunnlag(
            personer = personer.toMutableSet(),
            behandlingId = behandlingId,
        )

    private fun lagOvergangsstønadPerioder(
        perioder: List<MånedPeriode>,
        søkerIdent: String,
    ): List<InternPeriodeOvergangsstønad> =
        perioder.map {
            InternPeriodeOvergangsstønad(
                søkerIdent,
                it.fom.førsteDagIInneværendeMåned(),
                it.tom.sisteDagIInneværendeMåned(),
            )
        }

    private data class AtypiskVilkår(
        val aktør: Aktør,
        val fom: LocalDate,
        val tom: LocalDate? = null,
        val resultat: Resultat = Resultat.OPPFYLT,
        val vilkårType: Vilkår,
        val utdypendeVilkårsvurdering: UtdypendeVilkårsvurdering? = null,
    )

    private fun lagVilkårResultat(
        personResultat: PersonResultat,
        fom: LocalDate,
        tom: LocalDate? = null,
        resultat: Resultat = Resultat.OPPFYLT,
        vilkårType: Vilkår,
        utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
    ): VilkårResultat =
        VilkårResultat(
            personResultat = personResultat,
            vilkårType = vilkårType,
            resultat = resultat,
            periodeFom = fom,
            periodeTom = tom,
            begrunnelse = "",
            sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
        )

    private fun lagVilkårsvurdering(
        søker: Person,
        barn: List<Person>,
        behandlingUnderkategori: BehandlingUnderkategori,
        atypiskeVilkårSøker: List<AtypiskVilkår> = emptyList(),
        atypiskeVilkårBarna: List<AtypiskVilkår> = emptyList(),
    ): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())

        val eldsteBarn = barn.minBy { it.fødselsdato }

        val søkerPersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                person = søker,
                behandlingUnderkategori = behandlingUnderkategori,
                standardFom = eldsteBarn.fødselsdato,
                atypiskeVilkår = atypiskeVilkårSøker,
            )

        val barnasPersonResultater =
            barn.map { barnet ->
                lagPersonResultat(
                    vilkårsvurdering = vilkårsvurdering,
                    person = barnet,
                    behandlingUnderkategori = behandlingUnderkategori,
                    standardFom = barnet.fødselsdato,
                    atypiskeVilkår = atypiskeVilkårBarna.filter { it.aktør == barnet.aktør },
                )
            }

        vilkårsvurdering.personResultater = barnasPersonResultater.toSet().plus(søkerPersonResultat)
        return vilkårsvurdering
    }

    private fun lagPersonResultat(
        vilkårsvurdering: Vilkårsvurdering,
        person: Person,
        behandlingUnderkategori: BehandlingUnderkategori,
        standardFom: LocalDate,
        atypiskeVilkår: List<AtypiskVilkår>,
    ): PersonResultat {
        val personResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = person.aktør,
            )

        val vilkårForPersonType =
            Vilkår.hentVilkårFor(
                personType = person.type,
                fagsakType = FagsakType.NORMAL,
                behandlingUnderkategori = behandlingUnderkategori,
            )

        val ordinæreVilkårResultater =
            vilkårForPersonType.map { vilkår ->
                lagVilkårResultat(
                    personResultat = personResultat,
                    vilkårType = vilkår,
                    resultat = Resultat.OPPFYLT,
                    fom = standardFom,
                    tom = if (vilkår == Vilkår.UNDER_18_ÅR) person.fødselsdato.til18ÅrsVilkårsdato() else null,
                )
            }

        val atypiskeVilkårTyper = atypiskeVilkår.map { it.vilkårType }

        val oppdaterteVilkårResultater =
            ordinæreVilkårResultater
                .filter { it.vilkårType !in atypiskeVilkårTyper }
                .plus(
                    atypiskeVilkår.map {
                        lagVilkårResultat(
                            personResultat = personResultat,
                            fom = it.fom,
                            tom = it.tom,
                            vilkårType = it.vilkårType,
                            resultat = it.resultat,
                            utdypendeVilkårsvurderinger = if (it.utdypendeVilkårsvurdering != null) listOf(it.utdypendeVilkårsvurdering) else emptyList(),
                        )
                    },
                )

        personResultat.setSortedVilkårResultater(oppdaterteVilkårResultater.toSet())
        return personResultat
    }
}
