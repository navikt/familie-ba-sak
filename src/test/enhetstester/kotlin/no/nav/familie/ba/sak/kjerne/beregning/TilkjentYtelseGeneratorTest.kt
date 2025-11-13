package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.tilyyyyMMdd
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad.OvergangsstønadService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.lagDødsfallFraPdl
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class TilkjentYtelseGeneratorTest {
    private val overgangsstønadServiceMock: OvergangsstønadService = mockk()
    private val vilkårsvurderingServiceMock: VilkårsvurderingService = mockk()
    private val featureToggleServiceMock: FeatureToggleService = mockk()
    private val tilkjentYtelseGenerator: TilkjentYtelseGenerator = TilkjentYtelseGenerator(overgangsstønadServiceMock, vilkårsvurderingServiceMock, featureToggleServiceMock)

    @BeforeEach
    fun førHverTest() {
        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2022, 12, 31)
    }

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    private val søker = lagPerson(type = PersonType.SØKER)
    private val januar2019 = YearMonth.of(2019, 1)
    private val februar2019 = YearMonth.of(2019, 2)
    private val mars2019 = YearMonth.of(2019, 3)
    private val april2019 = YearMonth.of(2019, 4)
    private val juli2019 = YearMonth.of(2019, 7)
    private val august2019 = YearMonth.of(2019, 8)
    private val november2019 = YearMonth.of(2019, 11)
    private val desember2019 = YearMonth.of(2019, 12)
    private val august2020 = YearMonth.of(2020, 8)
    private val januar2022 = YearMonth.of(2022, 1)
    private val februar2022 = YearMonth.of(2022, 2)
    private val mars2022 = YearMonth.of(2022, 3)
    private val april2022 = YearMonth.of(2022, 4)
    private val mai2022 = YearMonth.of(2022, 5)
    private val juni2022 = YearMonth.of(2022, 6)
    private val juli2022 = YearMonth.of(2022, 7)
    private val august2022 = YearMonth.of(2022, 8)
    private val november2022 = YearMonth.of(2022, 11)
    private val desember2022 = YearMonth.of(2022, 12)

    @Test
    fun `Barn som fyller 6 år i det vilkårene er oppfylt får andel måneden etter`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 2)
        val barnSeksårsdag = barnFødselsdato.plusYears(6)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnSeksårsdag,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18),
            )

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val tilkjentYtelse =
            tilkjentYtelseGenerator.genererTilkjentYtelse(
                behandling = vilkårsvurdering.behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )

        assertEquals(1, tilkjentYtelse.andelerTilkjentYtelse.size)

        val andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first()
        assertEquals(
            MånedPeriode(
                barnSeksårsdag.nesteMåned(),
                barnFødselsdato.plusYears(18).forrigeMåned(),
            ),
            MånedPeriode(andelTilkjentYtelse.stønadFom, andelTilkjentYtelse.stønadTom),
        )
    }

    @Test
    fun `Barn som fyller 6 år i det vilkårene ikke lenger er oppfylt får andel den måneden også`() {
        val barnSeksårsdag = LocalDate.of(2022, 2, 2)
        val barnFødselsdato = barnSeksårsdag.minusYears(6)

        val vilkårOppfyltFom = barnSeksårsdag.minusMonths(2)
        val vilkårOppfyltTom = barnSeksårsdag.plusDays(2)
        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = vilkårOppfyltFom,
                vilkårOppfyltTom = vilkårOppfyltTom,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18),
            )

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val tilkjentYtelse =
            tilkjentYtelseGenerator.genererTilkjentYtelse(
                behandling = vilkårsvurdering.behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )

        assertEquals(2, tilkjentYtelse.andelerTilkjentYtelse.size)

        val andelTilkjentYtelseFør6År = tilkjentYtelse.andelerTilkjentYtelse.first()
        assertEquals(
            MånedPeriode(vilkårOppfyltFom.nesteMåned(), barnSeksårsdag.forrigeMåned()),
            MånedPeriode(andelTilkjentYtelseFør6År.stønadFom, andelTilkjentYtelseFør6År.stønadTom),
        )
        assertEquals(1676, andelTilkjentYtelseFør6År.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseEtter6År = tilkjentYtelse.andelerTilkjentYtelse.last()
        assertEquals(
            MånedPeriode(barnSeksårsdag.toYearMonth(), barnSeksårsdag.toYearMonth()),
            MånedPeriode(andelTilkjentYtelseEtter6År.stønadFom, andelTilkjentYtelseEtter6År.stønadTom),
        )
        assertEquals(1054, andelTilkjentYtelseEtter6År.kalkulertUtbetalingsbeløp)
    }

    @Test
    fun `Det skal utbetales for inneværende måned hvis barn dør minst 1 måned før 18 års datoen`() {
        val barnFødselsDato = LocalDate.of(2012, 2, 2)
        val barnDødsfallsDato = LocalDate.of(2030, 1, 1)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsDato,
                vilkårOppfyltFom = barnFødselsDato,
                vilkårOppfyltTom = barnDødsfallsDato,
                barnDødsfallDato = barnDødsfallsDato,
                under18ÅrVilkårOppfyltTom = barnDødsfallsDato,
            )

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val tilkjentYtelse =
            tilkjentYtelseGenerator.genererTilkjentYtelse(
                behandling = vilkårsvurdering.behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )

        assertEquals(2, tilkjentYtelse.andelerTilkjentYtelse.size)

        val andelFør6År = tilkjentYtelse.andelerTilkjentYtelse.first()
        val andelEtter6År = tilkjentYtelse.andelerTilkjentYtelse.last()

        assertEquals(YearMonth.of(2014, 2), andelFør6År.stønadFom)
        assertEquals(YearMonth.of(2019, 2), andelFør6År.stønadTom)

        assertEquals(YearMonth.of(2019, 3), andelEtter6År.stønadFom)
        assertEquals(YearMonth.of(2030, 1), andelEtter6År.stønadTom)
    }

    @Test
    fun `Det skal ikke utbetales for inneværende måned hvis barn dør samme måned som 18 års datoen`() {
        val barnFødselsDato = LocalDate.of(2012, 2, 20)
        val barnDødsfallsDato = LocalDate.of(2030, 2, 2)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsDato,
                vilkårOppfyltFom = barnFødselsDato,
                vilkårOppfyltTom = barnDødsfallsDato,
                barnDødsfallDato = barnDødsfallsDato,
                under18ÅrVilkårOppfyltTom = barnDødsfallsDato,
            )

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val tilkjentYtelse =
            tilkjentYtelseGenerator.genererTilkjentYtelse(
                behandling = vilkårsvurdering.behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )

        assertEquals(2, tilkjentYtelse.andelerTilkjentYtelse.size)

        val andelFør6År = tilkjentYtelse.andelerTilkjentYtelse.first()
        val andelEtter6År = tilkjentYtelse.andelerTilkjentYtelse.last()

        assertEquals(YearMonth.of(2014, 2), andelFør6År.stønadFom)
        assertEquals(YearMonth.of(2019, 2), andelFør6År.stønadTom)

        assertEquals(YearMonth.of(2019, 3), andelEtter6År.stønadFom)
        assertEquals(YearMonth.of(2030, 1), andelEtter6År.stønadTom)
    }

    @Test
    fun `1 barn får normal utbetaling med satsendring fra september 2020, september 2021 og januar 2022`() {
        val barnFødselsdato = LocalDate.of(2021, 2, 2)
        val barnSeksårsdag = barnFødselsdato.plusYears(6)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18),
            )

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = vilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                ).andelerTilkjentYtelse
                .toList()
                .sortedBy { it.stønadFom }

        assertEquals(4, andeler.size)

        val andelTilkjentYtelseFør6ÅrSeptember2020 = andeler[0]
        assertEquals(
            MånedPeriode(barnFødselsdato.nesteMåned(), YearMonth.of(2021, 8)),
            MånedPeriode(
                andelTilkjentYtelseFør6ÅrSeptember2020.stønadFom,
                andelTilkjentYtelseFør6ÅrSeptember2020.stønadTom,
            ),
        )
        assertEquals(1354, andelTilkjentYtelseFør6ÅrSeptember2020.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseFør6ÅrSeptember2021 = andeler[1]
        assertEquals(
            MånedPeriode(YearMonth.of(2021, 9), YearMonth.of(2021, 12)),
            MånedPeriode(
                andelTilkjentYtelseFør6ÅrSeptember2021.stønadFom,
                andelTilkjentYtelseFør6ÅrSeptember2021.stønadTom,
            ),
        )
        assertEquals(1654, andelTilkjentYtelseFør6ÅrSeptember2021.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseFør6ÅrJanuar2022 = andeler[2]
        assertEquals(
            MånedPeriode(YearMonth.of(2022, 1), barnSeksårsdag.forrigeMåned()),
            MånedPeriode(
                andelTilkjentYtelseFør6ÅrJanuar2022.stønadFom,
                andelTilkjentYtelseFør6ÅrJanuar2022.stønadTom,
            ),
        )
        assertEquals(1676, andelTilkjentYtelseFør6ÅrJanuar2022.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseEtter6År = andeler[3]
        assertEquals(
            MånedPeriode(barnSeksårsdag.toYearMonth(), barnFødselsdato.plusYears(18).forrigeMåned()),
            MånedPeriode(andelTilkjentYtelseEtter6År.stønadFom, andelTilkjentYtelseEtter6År.stønadTom),
        )
        assertEquals(1054, andelTilkjentYtelseEtter6År.kalkulertUtbetalingsbeløp)
    }

    @Test
    fun `Halvt beløp av grunnsats utbetales ved delt bosted`() {
        val barnFødselsdato = LocalDate.of(2021, 2, 2)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18),
            )

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = vilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                ).andelerTilkjentYtelse
                .toList()
                .sortedBy { it.stønadFom }

        val andelTilkjentYtelseFør6ÅrSeptember2020 = andeler[0]
        assertEquals(677, andelTilkjentYtelseFør6ÅrSeptember2020.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseFør6ÅrSeptember2021 = andeler[1]
        assertEquals(827, andelTilkjentYtelseFør6ÅrSeptember2021.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseFør6ÅrJanuar2022 = andeler[2]
        assertEquals(838, andelTilkjentYtelseFør6ÅrJanuar2022.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseEtter6År = andeler[3]
        assertEquals(527, andelTilkjentYtelseEtter6År.kalkulertUtbetalingsbeløp)
    }

    @Test
    fun `Skal opprette riktig tilkjent ytelse-perioder for perioder på barn som ikke er back-to-back over månedskiftet`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 5)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18),
            )

        val oppdatertVilkårsvurdering =
            oppdaterBosattIRiketMedBack2BackPerioder(
                vilkårsvurdering = vilkårsvurdering,
                personResultat = vilkårsvurdering.personResultater.find { !it.erSøkersResultater() }!!,
                barnFødselsdato = barnFødselsdato,
                backToBackTom = LocalDate.of(2019, 8, 31),
                backToBackFom = LocalDate.of(2019, 9, 2),
            )

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns oppdatertVilkårsvurdering

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = oppdatertVilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                ).andelerTilkjentYtelse
                .toList()
                .sortedBy { it.stønadFom }

        assertEquals(YearMonth.of(2019, 8), andeler[1].stønadTom)
        assertEquals(YearMonth.of(2019, 10), andeler[2].stønadFom)
    }

    @Test
    fun `Skal opprette riktig tilkjent ytelse-perioder for perioder på søker som ikke er back-to-back over månedskiftet`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 5)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18),
            )

        val oppdatertVilkårsvurdering =
            oppdaterBosattIRiketMedBack2BackPerioder(
                vilkårsvurdering = vilkårsvurdering,
                personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!,
                barnFødselsdato = barnFødselsdato,
                backToBackTom = LocalDate.of(2019, 8, 31),
                backToBackFom = LocalDate.of(2019, 9, 2),
            )

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns oppdatertVilkårsvurdering

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = oppdatertVilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                ).andelerTilkjentYtelse
                .toList()
                .sortedBy { it.stønadFom }

        assertEquals(YearMonth.of(2019, 8), andeler[1].stønadTom)
        assertEquals(YearMonth.of(2019, 10), andeler[2].stønadFom)
    }

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
                            personer = setOf(søker),
                            skalUtbetales = false,
                            årsak = Årsak.DELT_BOSTED,
                            fom = april2022,
                            tom = juli2022,
                        ),
                        EndretAndel(
                            personer = setOf(barnFødtAugust2019),
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
        assertThat(utvidetAndeler[0].stønadFom, `is`(april2022))
        assertThat(utvidetAndeler[0].stønadTom, `is`(juli2022))
        assertThat(utvidetAndeler[0].prosent, `is`(BigDecimal.ZERO))
        assertThat(utvidetAndeler[1].stønadFom, `is`(august2022))
        assertThat(utvidetAndeler[1].stønadTom, `is`(månedFørBarnBlir18))
        assertThat(utvidetAndeler[1].prosent, `is`(BigDecimal(50)))

        assertEquals(2, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, `is`(april2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, `is`(juli2022))
        assertThat(småbarnstilleggAndeler[0].prosent, `is`(BigDecimal.ZERO))
        assertThat(småbarnstilleggAndeler[1].stønadFom, `is`(august2022))
        assertThat(småbarnstilleggAndeler[1].stønadTom, `is`(barnFyller3ÅrDato))
        assertThat(småbarnstilleggAndeler[1].prosent, `is`(BigDecimal(100)))

        assertEquals(2, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, `is`(april2022))
        assertThat(barnasAndeler[0].stønadTom, `is`(juli2022))
        assertThat(barnasAndeler[0].prosent, `is`(BigDecimal.ZERO))
        assertThat(barnasAndeler[1].stønadFom, `is`(august2022))
        assertThat(barnasAndeler[1].stønadTom, `is`(månedFørBarnBlir6))
        assertThat(barnasAndeler[1].prosent, `is`(BigDecimal(50)))
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
                            personer = setOf(barnFødtAugust2019),
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
        assertThat(utvidetAndeler[0].stønadFom, `is`(april2022))
        assertThat(utvidetAndeler[0].stønadTom, `is`(månedFørBarnBlir18))
        assertThat(utvidetAndeler[0].prosent, `is`(BigDecimal(50)))

        assertEquals(1, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, `is`(april2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, `is`(barnFyller3ÅrDato))
        assertThat(småbarnstilleggAndeler[0].prosent, `is`(BigDecimal(100)))

        // BARN
        assertEquals(2, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, `is`(april2022))
        assertThat(barnasAndeler[0].stønadTom, `is`(juli2022))
        assertThat(barnasAndeler[0].prosent, `is`(BigDecimal.ZERO))
        assertThat(barnasAndeler[1].stønadFom, `is`(august2022))
        assertThat(barnasAndeler[1].stønadTom, `is`(månedFørBarnBlir6))
        assertThat(barnasAndeler[1].prosent, `is`(BigDecimal(50)))
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
                            personer = setOf(barnFødtAugust2019),
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
        assertThat(utvidetAndeler[0].stønadFom, `is`(juni2022))
        assertThat(utvidetAndeler[0].stønadTom, `is`(månedFørBarnBlir18))
        assertThat(utvidetAndeler[0].prosent, `is`(BigDecimal(50)))

        assertEquals(1, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, `is`(juni2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, `is`(barnFyller3ÅrDato))
        assertThat(småbarnstilleggAndeler[0].prosent, `is`(BigDecimal(100)))

        // BARN
        assertEquals(3, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, `is`(mars2022))
        assertThat(barnasAndeler[0].stønadTom, `is`(mai2022))
        assertThat(barnasAndeler[0].prosent, `is`(BigDecimal(100)))
        assertThat(barnasAndeler[1].stønadFom, `is`(juni2022))
        assertThat(barnasAndeler[1].stønadTom, `is`(juli2022))
        assertThat(barnasAndeler[1].prosent, `is`(BigDecimal(100)))
        assertThat(barnasAndeler[2].stønadFom, `is`(august2022))
        assertThat(barnasAndeler[2].stønadTom, `is`(månedFørBarnBlir6))
        assertThat(barnasAndeler[2].prosent, `is`(BigDecimal(50)))
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
                            personer = setOf(barnFødtAugust2019),
                            skalUtbetales = true,
                            årsak = Årsak.DELT_BOSTED,
                            fom = juni2022,
                            tom = juli2022,
                        ),
                        EndretAndel(
                            personer = setOf(søker),
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
        assertThat(utvidetAndeler[0].stønadFom, `is`(mars2022))
        assertThat(utvidetAndeler[0].stønadTom, `is`(mai2022))
        assertThat(utvidetAndeler[0].prosent, `is`(BigDecimal(100)))
        assertThat(utvidetAndeler[1].stønadFom, `is`(juni2022))
        assertThat(utvidetAndeler[1].stønadTom, `is`(juli2022))
        assertThat(utvidetAndeler[1].prosent, `is`(BigDecimal(100)))
        assertThat(utvidetAndeler[2].stønadFom, `is`(august2022))
        assertThat(utvidetAndeler[2].stønadTom, `is`(månedFørBarnBlir18))
        assertThat(utvidetAndeler[2].prosent, `is`(BigDecimal(50)))

        assertEquals(1, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, `is`(mars2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, `is`(barnFyller3ÅrDato))
        assertThat(småbarnstilleggAndeler[0].prosent, `is`(BigDecimal(100)))

        assertEquals(3, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, `is`(mars2022))
        assertThat(barnasAndeler[0].stønadTom, `is`(mai2022))
        assertThat(barnasAndeler[0].prosent, `is`(BigDecimal(100)))
        assertThat(barnasAndeler[1].stønadFom, `is`(juni2022))
        assertThat(barnasAndeler[1].stønadTom, `is`(juli2022))
        assertThat(barnasAndeler[1].prosent, `is`(BigDecimal(100)))
        assertThat(barnasAndeler[2].stønadFom, `is`(august2022))
        assertThat(barnasAndeler[2].stønadTom, `is`(månedFørBarnBlir6))
        assertThat(barnasAndeler[2].prosent, `is`(BigDecimal(50)))
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
                            personer = setOf(barnFødtAugust2016),
                            skalUtbetales = false,
                            årsak = Årsak.ETTERBETALING_3ÅR,
                            fom = april2019,
                            tom = juli2019,
                        ),
                        EndretAndel(
                            personer = setOf(søker),
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
        assertThat(utvidetAndeler[0].stønadFom, `is`(april2019))
        assertThat(utvidetAndeler[0].stønadTom, `is`(juli2019))
        assertThat(utvidetAndeler[0].prosent, `is`(BigDecimal(0)))
        assertThat(utvidetAndeler[1].stønadFom, `is`(august2019))
        assertThat(utvidetAndeler[1].stønadTom, `is`(månedFørBarnBlir18))
        assertThat(utvidetAndeler[1].prosent, `is`(BigDecimal(100)))

        assertEquals(2, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, `is`(april2019))
        assertThat(småbarnstilleggAndeler[0].stønadTom, `is`(juli2019))
        assertThat(småbarnstilleggAndeler[0].prosent, `is`(BigDecimal(0)))
        assertThat(småbarnstilleggAndeler[1].stønadFom, `is`(august2019))
        assertThat(småbarnstilleggAndeler[1].stønadTom, `is`(august2019))
        assertThat(småbarnstilleggAndeler[1].prosent, `is`(BigDecimal(100)))

        // BARN
        assertEquals(2, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, `is`(april2019))
        assertThat(barnasAndeler[0].stønadTom, `is`(juli2019))
        assertThat(barnasAndeler[0].prosent, `is`(BigDecimal(0)))
        assertThat(barnasAndeler[1].stønadFom, `is`(august2019))
        assertThat(barnasAndeler[1].stønadTom, `is`(august2020))
        assertThat(barnasAndeler[1].prosent, `is`(BigDecimal(100)))
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
                            personer = setOf(barnFødtAugust2016),
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
        assertThat(utvidetAndeler[0].stønadFom, `is`(mars2019))
        assertThat(utvidetAndeler[0].stønadTom, `is`(juli2019))
        assertThat(utvidetAndeler[0].prosent, `is`(BigDecimal(50)))
        assertThat(utvidetAndeler[1].stønadFom, `is`(august2019))
        assertThat(utvidetAndeler[1].stønadTom, `is`(månedFørBarnFødtAugust2016Blir18))
        assertThat(utvidetAndeler[1].prosent, `is`(BigDecimal(100)))

        assertEquals(2, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, `is`(april2019))
        assertThat(småbarnstilleggAndeler[0].stønadTom, `is`(juli2019))
        assertThat(småbarnstilleggAndeler[0].prosent, `is`(BigDecimal(0)))
        assertThat(småbarnstilleggAndeler[1].stønadFom, `is`(august2019))
        assertThat(småbarnstilleggAndeler[1].stønadTom, `is`(august2019))
        assertThat(småbarnstilleggAndeler[1].prosent, `is`(BigDecimal(100)))

        // BARN
        val (barn1Andeler, barn2Andeler) = barnasAndeler.partition { it.aktør == barnFødtAugust2016.aktør }
        assertEquals(2, barn1Andeler.size)
        assertThat(barn1Andeler[0].stønadFom, `is`(april2019))
        assertThat(barn1Andeler[0].stønadTom, `is`(juli2019))
        assertThat(barn1Andeler[0].prosent, `is`(BigDecimal(0)))
        assertThat(barn1Andeler[1].stønadFom, `is`(august2019))
        assertThat(barn1Andeler[1].stønadTom, `is`(august2020))
        assertThat(barn1Andeler[1].prosent, `is`(BigDecimal(100)))

        assertEquals(2, barn2Andeler.size)
        assertThat(barn2Andeler[0].stønadFom, `is`(februar2019))
        assertThat(barn2Andeler[0].stønadTom, `is`(februar2019))
        assertThat(barn2Andeler[0].prosent, `is`(BigDecimal(50)))
        assertThat(barn2Andeler[1].stønadFom, `is`(mars2019))
        assertThat(barn2Andeler[1].stønadTom, `is`(månedFørBarnFødtDesember2006Blir18))
        assertThat(barn2Andeler[1].prosent, `is`(BigDecimal(50)))
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
        assertThat(utvidetAndeler[0].stønadFom, `is`(mars2022))
        assertThat(utvidetAndeler[0].stønadTom, `is`(august2022))
        assertThat(utvidetAndeler[0].prosent, `is`(BigDecimal(100)))

        assertEquals(1, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, `is`(april2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, `is`(juni2022))
        assertThat(småbarnstilleggAndeler[0].prosent, `is`(BigDecimal(100)))

        assertEquals(1, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, `is`(mars2022))
        assertThat(barnasAndeler[0].stønadTom, `is`(månedFørBarnBlir6))
        assertThat(barnasAndeler[0].prosent, `is`(BigDecimal(100)))
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
        assertThat(utvidetAndeler[0].stønadFom, `is`(mars2022))
        assertThat(utvidetAndeler[0].stønadTom, `is`(juni2022))
        assertThat(utvidetAndeler[0].prosent, `is`(BigDecimal(100)))

        assertEquals(1, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, `is`(april2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, `is`(juni2022))
        assertThat(småbarnstilleggAndeler[0].prosent, `is`(BigDecimal(100)))

        assertEquals(1, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, `is`(mars2022))
        assertThat(barnasAndeler[0].stønadTom, `is`(månedFørBarnBlir6))
        assertThat(barnasAndeler[0].prosent, `is`(BigDecimal(100)))
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
        assertThat(utvidetAndeler[0].stønadFom, `is`(mars2022))
        assertThat(utvidetAndeler[0].stønadTom, `is`(månedFørBarnBlir18))
        assertThat(utvidetAndeler[0].prosent, `is`(BigDecimal(100)))

        assertEquals(1, småbarnstilleggAndeler.size)
        assertThat(småbarnstilleggAndeler[0].stønadFom, `is`(april2022))
        assertThat(småbarnstilleggAndeler[0].stønadTom, `is`(august2022))
        assertThat(småbarnstilleggAndeler[0].prosent, `is`(BigDecimal(100)))

        assertEquals(1, barnasAndeler.size)
        assertThat(barnasAndeler[0].stønadFom, `is`(mars2022))
        assertThat(barnasAndeler[0].stønadTom, `is`(månedFørBarnBlir6))
        assertThat(barnasAndeler[0].prosent, `is`(BigDecimal(100)))
    }

    @Test
    fun `genrering av utvidet skal avhenge av endret utbetaling med årsak ENDRE_MOTTAKER`() {
        val barnMedFullUtbetaling = lagPerson(type = PersonType.BARN, fødselsdato = januar2019.atDay(1))
        val barnMedHalvUtbetaling = lagPerson(type = PersonType.BARN, fødselsdato = januar2019.atDay(1))

        val tilkjentYtelse =
            settOppScenarioOgBeregnTilkjentYtelse(
                endretAndeler =
                    listOf(
                        EndretAndel(
                            personer = setOf(barnMedFullUtbetaling),
                            skalUtbetales = false,
                            årsak = Årsak.ENDRE_MOTTAKER,
                            fom = februar2022,
                            tom = mars2022,
                        ),
                        EndretAndel(
                            personer = setOf(barnMedHalvUtbetaling),
                            skalUtbetales = false,
                            årsak = Årsak.ENDRE_MOTTAKER,
                            fom = april2022,
                            tom = mai2022,
                        ),
                        EndretAndel(
                            personer = setOf(barnMedFullUtbetaling, barnMedHalvUtbetaling),
                            skalUtbetales = false,
                            årsak = Årsak.ENDRE_MOTTAKER,
                            fom = juni2022,
                            tom = juli2022,
                        ),
                    ),
                atypiskeVilkårBarna =
                    listOf(
                        AtypiskVilkår(
                            fom = januar2022.førsteDagIInneværendeMåned(),
                            tom = juli2022.sisteDagIInneværendeMåned(),
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            aktør = barnMedFullUtbetaling.aktør,
                        ),
                        AtypiskVilkår(
                            fom = januar2022.førsteDagIInneværendeMåned(),
                            tom = juli2022.sisteDagIInneværendeMåned(),
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            aktør = barnMedHalvUtbetaling.aktør,
                            utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED,
                        ),
                    ),
                atypiskeVilkårSøker =
                    listOf(
                        AtypiskVilkår(
                            fom = barnMedFullUtbetaling.fødselsdato,
                            tom = null,
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            aktør = søker.aktør,
                        ),
                    ),
                barna = listOf(barnMedFullUtbetaling, barnMedHalvUtbetaling),
                overgangsstønadPerioder = emptyList(),
            )

        assertEquals(9, tilkjentYtelse.andelerTilkjentYtelse.size)

        val (søkersAndeler, barnasAndeler) = tilkjentYtelse.andelerTilkjentYtelse.partition { it.erSøkersAndel() }

        // SØKER
        assertEquals(3, søkersAndeler.size)
        assertThat(søkersAndeler[0].stønadFom, `is`(februar2022))
        assertThat(søkersAndeler[0].stønadTom, `is`(mars2022))
        assertThat(søkersAndeler[0].prosent, `is`(BigDecimal(50)))

        assertThat(søkersAndeler[1].stønadFom, `is`(april2022))
        assertThat(søkersAndeler[1].stønadTom, `is`(mai2022))
        assertThat(søkersAndeler[1].prosent, `is`(BigDecimal(100)))

        assertThat(søkersAndeler[2].stønadFom, `is`(juni2022))
        assertThat(søkersAndeler[2].stønadTom, `is`(juli2022))
        assertThat(søkersAndeler[2].prosent, `is`(BigDecimal(0)))

        // BARN
        val (barnMedFullUtbetalingAndeler, barnMedHalvUtbetalingAndeler) = barnasAndeler.partition { it.aktør == barnMedFullUtbetaling.aktør }

        // BARN MED FULL UTBETALING
        assertEquals(3, barnMedFullUtbetalingAndeler.size)
        assertThat(barnMedFullUtbetalingAndeler[0].stønadFom, `is`(februar2022))
        assertThat(barnMedFullUtbetalingAndeler[0].stønadTom, `is`(mars2022))
        assertThat(barnMedFullUtbetalingAndeler[0].prosent, `is`(BigDecimal(0)))

        assertThat(barnMedFullUtbetalingAndeler[1].stønadFom, `is`(april2022))
        assertThat(barnMedFullUtbetalingAndeler[1].stønadTom, `is`(mai2022))
        assertThat(barnMedFullUtbetalingAndeler[1].prosent, `is`(BigDecimal(100)))

        assertThat(barnMedFullUtbetalingAndeler[2].stønadFom, `is`(juni2022))
        assertThat(barnMedFullUtbetalingAndeler[2].stønadTom, `is`(juli2022))
        assertThat(barnMedFullUtbetalingAndeler[2].prosent, `is`(BigDecimal(0)))

        // BARN MED HALV UTBETALING
        assertEquals(3, barnMedHalvUtbetalingAndeler.size)
        assertThat(barnMedHalvUtbetalingAndeler[0].stønadFom, `is`(februar2022))
        assertThat(barnMedHalvUtbetalingAndeler[0].stønadTom, `is`(mars2022))
        assertThat(barnMedHalvUtbetalingAndeler[0].prosent, `is`(BigDecimal(50)))

        assertThat(barnMedHalvUtbetalingAndeler[1].stønadFom, `is`(april2022))
        assertThat(barnMedHalvUtbetalingAndeler[1].stønadTom, `is`(mai2022))
        assertThat(barnMedHalvUtbetalingAndeler[1].prosent, `is`(BigDecimal(0)))

        assertThat(barnMedHalvUtbetalingAndeler[2].stønadFom, `is`(juni2022))
        assertThat(barnMedHalvUtbetalingAndeler[2].stønadTom, `is`(juli2022))
        assertThat(barnMedHalvUtbetalingAndeler[2].prosent, `is`(BigDecimal(0)))
    }

    private fun oppdaterBosattIRiketMedBack2BackPerioder(
        vilkårsvurdering: Vilkårsvurdering,
        personResultat: PersonResultat,
        barnFødselsdato: LocalDate,
        backToBackTom: LocalDate? = null,
        backToBackFom: LocalDate? = null,
    ): Vilkårsvurdering {
        personResultat.setSortedVilkårResultater(
            personResultat.vilkårResultater
                .filter { it.vilkårType != Vilkår.BOSATT_I_RIKET }
                .toSet() +
                setOf(
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = barnFødselsdato,
                        periodeTom = backToBackTom,
                        begrunnelse = "",
                        sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                    ),
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = backToBackFom,
                        periodeTom = null,
                        begrunnelse = "",
                        sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                    ),
                ),
        )

        vilkårsvurdering.personResultater =
            vilkårsvurdering.personResultater.filter { it.aktør != personResultat.aktør }.toSet() +
            setOf(
                personResultat,
            )

        return vilkårsvurdering
    }

    private fun genererVilkårsvurderingOgPersonopplysningGrunnlag(
        barnFødselsdato: LocalDate,
        vilkårOppfyltFom: LocalDate,
        vilkårOppfyltTom: LocalDate? = barnFødselsdato.plusYears(18),
        barnDødsfallDato: LocalDate? = null,
        erDeltBosted: Boolean = false,
        under18ÅrVilkårOppfyltTom: LocalDate?,
    ): Pair<Vilkårsvurdering, PersonopplysningGrunnlag> {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val søkerAktørId = lagAktør(søkerFnr)
        val barnAktørId = lagAktør(barnFnr)

        val behandling = lagBehandling()

        val vilkårsvurdering =
            lagVilkårsvurdering(
                søkerAktør = søkerAktørId,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
                søkerPeriodeFom = LocalDate.of(2014, 1, 1),
                søkerPeriodeTom = null,
            )

        val barnResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnAktørId)
        barnResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barnResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = vilkårOppfyltFom,
                    periodeTom = vilkårOppfyltTom,
                    begrunnelse = "",
                    sistEndretIBehandlingId = behandling.id,
                ),
                VilkårResultat(
                    personResultat = barnResultat,
                    vilkårType = Vilkår.UNDER_18_ÅR,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnFødselsdato,
                    periodeTom = under18ÅrVilkårOppfyltTom,
                    begrunnelse = "",
                    sistEndretIBehandlingId = behandling.id,
                ),
                VilkårResultat(
                    personResultat = barnResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnFødselsdato,
                    periodeTom = null,
                    begrunnelse = "",
                    sistEndretIBehandlingId = behandling.id,
                ),
                VilkårResultat(
                    personResultat = barnResultat,
                    vilkårType = Vilkår.GIFT_PARTNERSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnFødselsdato,
                    periodeTom = null,
                    begrunnelse = "",
                    sistEndretIBehandlingId = behandling.id,
                ),
                VilkårResultat(
                    personResultat = barnResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnFødselsdato,
                    periodeTom = null,
                    begrunnelse = "",
                    sistEndretIBehandlingId = behandling.id,
                    utdypendeVilkårsvurderinger =
                        listOfNotNull(
                            if (erDeltBosted) UtdypendeVilkårsvurdering.DELT_BOSTED else null,
                        ),
                ),
            ),
        )

        vilkårsvurdering.personResultater = setOf(vilkårsvurdering.personResultater.first(), barnResultat)

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)

        val barn =
            Person(
                aktør = lagAktør(barnFnr),
                type = PersonType.BARN,
                personopplysningGrunnlag = personopplysningGrunnlag,
                fødselsdato = barnFødselsdato,
                navn = "Barn",
                kjønn = Kjønn.MANN,
            ).apply {
                sivilstander = mutableListOf(GrSivilstand(type = SIVILSTANDTYPE.UGIFT, person = this))
                barnDødsfallDato?.let { dødsfall = lagDødsfallFraPdl(this, it.tilyyyyMMdd(), null) }
            }
        val søker =
            Person(
                aktør = lagAktør(søkerFnr),
                type = PersonType.SØKER,
                personopplysningGrunnlag = personopplysningGrunnlag,
                fødselsdato = barnFødselsdato.minusYears(20),
                navn = "Barn",
                kjønn = Kjønn.MANN,
            ).apply { sivilstander = mutableListOf(GrSivilstand(type = SIVILSTANDTYPE.UGIFT, person = this)) }
        personopplysningGrunnlag.personer.add(søker)
        personopplysningGrunnlag.personer.add(barn)

        return Pair(vilkårsvurdering, personopplysningGrunnlag)
    }

    private data class EndretAndel(
        val fom: YearMonth,
        val tom: YearMonth,
        val personer: Set<Person>,
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
        every { overgangsstønadServiceMock.hentOgLagrePerioderMedOvergangsstønadForBehandling(any(), any()) } returns mockkObject()
        every { overgangsstønadServiceMock.hentPerioderMedFullOvergangsstønad(any<Behandling>()) } answers {
            lagOvergangsstønadPerioder(
                perioder = overgangsstønadPerioder,
                søkerIdent = søker.aktør.aktivFødselsnummer(),
            )
        }

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
                    personer = it.personer,
                    prosent = if (it.skalUtbetales) BigDecimal(100) else BigDecimal.ZERO,
                    årsak = it.årsak,
                    fom = it.fom,
                    tom = it.tom,
                )
            }

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val tilkjentYtelse =
            tilkjentYtelseGenerator.genererTilkjentYtelse(
                behandling = vilkårsvurdering.behandling,
                personopplysningGrunnlag =
                    lagPersonopplysningsgrunnlag(
                        personer = barna.plus(søker),
                        behandlingId = vilkårsvurdering.behandling.id,
                    ),
                endretUtbetalingAndeler = endretUtbetalingAndeler,
            )
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
