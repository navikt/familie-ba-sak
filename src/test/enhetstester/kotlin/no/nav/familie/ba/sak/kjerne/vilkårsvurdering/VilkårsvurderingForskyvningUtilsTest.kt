package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.nov
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.beskjærPå18År
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjeForOppfyltVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjerForHvertOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilTidslinjeForSplitt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

class VilkårsvurderingForskyvningUtilsTest {
    @Test
    fun `Skal lage riktig splitt når bor med søker går fra delt bosted til fullt`() {
        val fom = LocalDate.now().minusMonths(7).førsteDagIInneværendeMåned()
        val deltBostedTom = LocalDate.now().minusMonths(1).sisteDagIMåned()
        val barnets18årsdag = LocalDate.now().plusYears(14)

        val vilkårResultater =
            lagVilkårForPerson(
                fom = fom,
                tom = null,
                spesielleVilkår =
                    setOf(
                        lagVilkårResultat(
                            periodeFom = fom,
                            periodeTom = deltBostedTom,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            resultat = Resultat.OPPFYLT,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                        ),
                        lagVilkårResultat(
                            periodeFom = deltBostedTom.plusMonths(1).førsteDagIInneværendeMåned(),
                            periodeTom = null,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            resultat = Resultat.OPPFYLT,
                        ),
                    ),
                maksTom = barnets18årsdag,
            )

        val tidslinjer = vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår(barnets18årsdag.minusYears(18))

        assertEquals(5, tidslinjer.size)

        val borMedSøkerTidslinje = tidslinjer.first()
        val borMedSøkerPerioder = borMedSøkerTidslinje.tilPerioder()

        assertEquals(2, borMedSøkerPerioder.size)

        val deltBostedPeriode = borMedSøkerPerioder.first()
        val fullPeriode = borMedSøkerPerioder.last()

        assertEquals(fom.plusMonths(1), deltBostedPeriode.fom)
        assertEquals(deltBostedTom.sisteDagIMåned(), deltBostedPeriode.tom)
        assertEquals(deltBostedTom.førsteDagINesteMåned(), fullPeriode.fom)
        assertNull(fullPeriode.tom)

        tidslinjer.subList(1, tidslinjer.size).forEach {
            assertEquals(1, it.tilPerioder().size)
            val periode = it.tilPerioder().first()
            assertEquals(fom.plusMonths(1), periode.fom)
            assertNull(fullPeriode.tom)
        }
    }

    @Test
    fun `Skal lage riktig splitt når bor med søker går fra fullt til delt bosted`() {
        val fom = LocalDate.now().minusMonths(7).førsteDagIInneværendeMåned()
        val deltBostedTom = LocalDate.now().minusMonths(1).sisteDagIMåned()
        val barnets18årsdag = LocalDate.now().plusYears(14)

        val vilkårResultater =
            lagVilkårForPerson(
                fom = fom,
                tom = null,
                spesielleVilkår =
                    setOf(
                        lagVilkårResultat(
                            periodeFom = fom,
                            periodeTom = deltBostedTom,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            resultat = Resultat.OPPFYLT,
                        ),
                        lagVilkårResultat(
                            periodeFom = deltBostedTom.plusMonths(1).førsteDagIInneværendeMåned(),
                            periodeTom = null,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            resultat = Resultat.OPPFYLT,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                        ),
                    ),
                maksTom = barnets18årsdag,
            )

        val tidslinjer = vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår(barnets18årsdag.minusYears(18))

        assertEquals(5, tidslinjer.size)

        val borMedSøkerTidslinje = tidslinjer.first()
        val borMedSøkerPerioder = borMedSøkerTidslinje.tilPerioder()

        assertEquals(2, borMedSøkerPerioder.size)

        val deltBostedPeriode = borMedSøkerPerioder.first()
        val fullPeriode = borMedSøkerPerioder.last()

        assertEquals(fom.plusMonths(1), deltBostedPeriode.fom)
        assertEquals(deltBostedTom.plusMonths(1), deltBostedPeriode.tom)
        assertEquals(deltBostedTom.plusMonths(2).førsteDagIInneværendeMåned(), fullPeriode.fom)
        assertNull(fullPeriode.tom)

        tidslinjer.subList(1, tidslinjer.size).forEach {
            assertEquals(1, it.tilPerioder().size)
            val periode = it.tilPerioder().first()
            assertEquals(fom.plusMonths(1), periode.fom)
            assertNull(fullPeriode.tom)
        }
    }

    @Test
    fun `Skal lage riktig splitt når bor med søker er oppfylt i to back2back-perioder uten utdypende vilkårsvurdering`() {
        val fom = LocalDate.now().minusMonths(7).førsteDagIInneværendeMåned()
        val b2bTom = LocalDate.now().minusMonths(1).sisteDagIMåned()
        val barnets18årsdag = LocalDate.now().plusYears(14)

        val vilkårResultater =
            lagVilkårForPerson(
                fom = fom,
                tom = null,
                spesielleVilkår =
                    setOf(
                        lagVilkårResultat(
                            periodeFom = fom,
                            periodeTom = b2bTom,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            resultat = Resultat.OPPFYLT,
                        ),
                        lagVilkårResultat(
                            periodeFom = b2bTom.plusMonths(1).førsteDagIInneværendeMåned(),
                            periodeTom = null,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            resultat = Resultat.OPPFYLT,
                        ),
                    ),
                maksTom = barnets18årsdag,
            )

        val tidslinjer = vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår(barnets18årsdag.minusYears(18))

        assertEquals(5, tidslinjer.size)

        val borMedSøkerTidslinje = tidslinjer.first()
        val borMedSøkerPerioder = borMedSøkerTidslinje.tilPerioder()

        assertEquals(2, borMedSøkerPerioder.size)

        val førstePeriode = borMedSøkerPerioder.first()
        val andrePeriode = borMedSøkerPerioder.last()

        assertEquals(fom.plusMonths(1), førstePeriode.fom)
        assertEquals(b2bTom.sisteDagIMåned(), førstePeriode.tom)
        assertEquals(b2bTom.førsteDagINesteMåned(), andrePeriode.fom)
        assertNull(andrePeriode.tom)

        tidslinjer.subList(1, tidslinjer.size).forEach {
            assertEquals(1, it.tilPerioder().size)
            val periode = it.tilPerioder().first()
            assertEquals(fom.plusMonths(1), periode.fom)
            assertNull(andrePeriode.tom)
        }
    }

    @Test
    fun `Skal forskyve UNDER_18 tidslinjen og beskjære den måneden før 18-årsdag`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2022, Month.DECEMBER, 1).minusYears(18))

        val under18VilkårResultat =
            listOf(
                lagVilkårResultat(
                    periodeFom = barn.fødselsdato,
                    periodeTom = barn.fødselsdato.plusYears(18).minusDays(1),
                    vilkårType = Vilkår.UNDER_18_ÅR,
                    resultat = Resultat.OPPFYLT,
                ),
            )

        val forskjøvetTidslinje =
            under18VilkårResultat.tilForskjøvetTidslinjeForOppfyltVilkår(vilkår = Vilkår.UNDER_18_ÅR, barn.fødselsdato)

        val forskjøvedePerioder = forskjøvetTidslinje.tilPerioder()

        assertEquals(1, forskjøvedePerioder.size)

        val forskjøvetPeriode = forskjøvedePerioder.single()

        assertEquals(barn.fødselsdato.plusMonths(1), forskjøvetPeriode.fom)
        assertEquals(
            barn.fødselsdato
                .plusYears(18)
                .minusMonths(1)
                .sisteDagIMåned(),
            forskjøvetPeriode.tom,
        )
    }

    @Test
    fun `Skal forskyve UNDER_18 tidslinjen, men ikke kutte den måneden før hvis til og med dato ikke er i måneden hen fyller 18`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2022, Month.DECEMBER, 1).minusYears(18))

        val tomDato = barn.fødselsdato.plusYears(7)

        val under18VilkårResultat =
            listOf(
                lagVilkårResultat(
                    periodeFom = barn.fødselsdato,
                    periodeTom = tomDato,
                    vilkårType = Vilkår.UNDER_18_ÅR,
                    resultat = Resultat.OPPFYLT,
                ),
            )

        val forskjøvetTidslinje =
            under18VilkårResultat.tilForskjøvetTidslinjeForOppfyltVilkår(vilkår = Vilkår.UNDER_18_ÅR, barn.fødselsdato)

        val forskjøvedePerioder = forskjøvetTidslinje.tilPerioder()

        assertEquals(1, forskjøvedePerioder.size)

        val forskjøvetPeriode = forskjøvedePerioder.single()

        assertEquals(barn.fødselsdato.plusMonths(1), forskjøvetPeriode.fom)
        assertEquals(tomDato.sisteDagIMåned(), forskjøvetPeriode.tom)
    }

    @Test
    fun `Skal gi tom liste og ikke kaste feil dersom vi ikke sender med noen vilkårresultater`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2022, Month.DECEMBER, 1).minusYears(18))

        val forskjøvedeOppfylteVilkårTomListe =
            emptyList<VilkårResultat>()
                .tilForskjøvetTidslinjeForOppfyltVilkår(
                    vilkår = Vilkår.UNDER_18_ÅR,
                    fødselsdato = barn.fødselsdato,
                ).tilPerioder()
        assertEquals(forskjøvedeOppfylteVilkårTomListe.size, 0)

        val forskjøvedeVilkårTomListe =
            emptyList<VilkårResultat>()
                .tilForskjøvetTidslinje(
                    vilkår = Vilkår.UNDER_18_ÅR,
                    fødselsdato = barn.fødselsdato,
                ).tilPerioder()
        assertEquals(forskjøvedeVilkårTomListe.size, 0)
    }

    @Test
    fun `skal forskyve eksplisitt avslag når avslaget starter og opphører innenfor samme måned`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2022, Month.DECEMBER, 1).minusYears(18))

        val vilkårResultat =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.of(2023, 1, 16),
                    periodeTom = LocalDate.of(2023, 1, 31),
                    erEksplisittAvslagPåSøknad = true,
                ),
            )

        val forskjøvedeVilkår =
            vilkårResultat
                .tilForskjøvetTidslinje(
                    vilkår = Vilkår.BOSATT_I_RIKET,
                    fødselsdato = barn.fødselsdato,
                ).tilPerioderIkkeNull()

        assertEquals(1, forskjøvedeVilkår.size)

        val periode = forskjøvedeVilkår.single()
        assertEquals(1.feb(2023), periode.fom)
        assertEquals(28.feb(2023), periode.tom)
    }

    @Test
    fun `skal ikke forskyve eksplisitt avslag når avslaget blir etterfulgt av en innvilget periode`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2022, Month.DECEMBER, 1).minusYears(18))

        val vilkårResultat =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.of(2023, 1, 16),
                    periodeTom = LocalDate.of(2023, 2, 28),
                    erEksplisittAvslagPåSøknad = true,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2023, 3, 1),
                    periodeTom = LocalDate.of(2023, 5, 31),
                ),
            )

        val forskjøvedeVilkår =
            vilkårResultat
                .tilForskjøvetTidslinje(
                    vilkår = Vilkår.BOSATT_I_RIKET,
                    fødselsdato = barn.fødselsdato,
                ).tilPerioderIkkeNull()

        assertEquals(2, forskjøvedeVilkår.size)

        val periode = forskjøvedeVilkår.single { it.verdi.erEksplisittAvslagPåSøknad == true }
        assertEquals(1.feb(2023), periode.fom)
        assertEquals(28.feb(2023), periode.tom)
    }

    @Test
    fun `skal ikke forskyve eksplisitt avslag når avslaget ikke opphører innenfor samme måned`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2022, Month.DECEMBER, 1).minusYears(18))

        val vilkårResultat =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.of(2023, 1, 16),
                    periodeTom = LocalDate.of(2023, 2, 28),
                    erEksplisittAvslagPåSøknad = true,
                ),
            )

        val forskjøvedeVilkår =
            vilkårResultat
                .tilForskjøvetTidslinje(
                    vilkår = Vilkår.BOSATT_I_RIKET,
                    fødselsdato = barn.fødselsdato,
                ).tilPerioderIkkeNull()

        assertEquals(1, forskjøvedeVilkår.size)
        val periode = forskjøvedeVilkår.single()
        assertEquals(1.feb(2023), periode.fom)
        assertEquals(28.feb(2023), periode.tom)
    }

    @Test
    fun `skal ikke forskyve opphørsperioder som starter og slutter innenfor samme måned`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2022, Month.DECEMBER, 1).minusYears(18))

        val vilkårResultat =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.of(2023, 1, 16),
                    periodeTom = LocalDate.of(2023, 1, 31),
                ),
            )

        val forskjøvedeVilkår =
            vilkårResultat
                .tilForskjøvetTidslinje(
                    vilkår = Vilkår.BOSATT_I_RIKET,
                    fødselsdato = barn.fødselsdato,
                ).tilPerioderIkkeNull()

        assertEquals(0, forskjøvedeVilkår.size)
    }

    @Test
    fun `Skal kutte UNDER_18 tidslinjen måneden før 18-årsdag`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2022, Month.DECEMBER, 1).minusYears(18))

        val under18VilkårResultat =
            lagVilkårResultat(
                periodeFom = barn.fødselsdato,
                periodeTom = barn.fødselsdato.plusYears(18).minusDays(1),
                vilkårType = Vilkår.UNDER_18_ÅR,
                resultat = Resultat.OPPFYLT,
            )

        val under18årVilkårTidslinje =
            Periode(
                verdi = under18VilkårResultat,
                fom = under18VilkårResultat.periodeFom?.førsteDagINesteMåned(),
                tom = under18VilkårResultat.periodeTom?.sisteDagIMåned(),
            ).tilTidslinje()

        val under18PerioderFørBeskjæring = under18årVilkårTidslinje.tilPerioder()

        assertEquals(1, under18PerioderFørBeskjæring.size)
        assertEquals(
            barn.fødselsdato.førsteDagINesteMåned(),
            under18PerioderFørBeskjæring.first().fom,
        )
        assertEquals(
            30.nov(2022),
            under18PerioderFørBeskjæring.first().tom,
        )

        val tidslinjeBeskåret = under18årVilkårTidslinje.beskjærPå18År(barn.fødselsdato)

        val under18PerioderEtterBeskjæring = tidslinjeBeskåret.tilPerioder()

        assertEquals(1, under18PerioderEtterBeskjæring.size)
        assertEquals(
            barn.fødselsdato.plusMonths(1),
            under18PerioderEtterBeskjæring.first().fom,
        )
        assertEquals(
            barn.fødselsdato
                .plusYears(18)
                .minusMonths(1)
                .sisteDagIMåned(),
            under18PerioderEtterBeskjæring.first().tom,
        )
    }

    @Test
    fun `Skal lage korrekt tidslinje for splitting av vedtaksperioder`() {
        val februar2020 = YearMonth.of(2020, 2)
        val oktober2020 = YearMonth.of(2020, 10)
        val mars2021 = YearMonth.of(2021, 3)
        val desember2021 = YearMonth.of(2021, 12)
        val mai2022 = YearMonth.of(2022, 5)

        val søker = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2015, 5, 6))
        val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 9, 7))

        val vilkårsvurdering =
            lagVilkårsvurdering(
                søkerAktør = søker.aktør,
                behandling = lagBehandling(),
                resultat = Resultat.OPPFYLT,
            )

        val personResultatSøker =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søker.aktør,
            )

        personResultatSøker.setSortedVilkårResultater(
            lagVilkårForPerson(
                personResultat = personResultatSøker,
                fom = februar2020.førsteDagIInneværendeMåned(),
                tom = null,
                generiskeVilkår = listOf(Vilkår.LOVLIG_OPPHOLD, Vilkår.BOSATT_I_RIKET),
            ),
        )

        val personResultatBarn1 =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn1.aktør,
            )

        personResultatBarn1.setSortedVilkårResultater(
            lagVilkårForPerson(
                fom = oktober2020.førsteDagIInneværendeMåned(),
                tom = null,
                maksTom = barn1.fødselsdato.til18ÅrsVilkårsdato(),
                spesielleVilkår =
                    setOf(
                        lagVilkårResultat(
                            periodeFom = oktober2020.førsteDagIInneværendeMåned(),
                            periodeTom = desember2021.sisteDagIInneværendeMåned(),
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            resultat = Resultat.OPPFYLT,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                        ),
                        lagVilkårResultat(
                            periodeFom = desember2021.plusMonths(1).førsteDagIInneværendeMåned(),
                            periodeTom = null,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            resultat = Resultat.OPPFYLT,
                        ),
                    ),
            ),
        )

        val personResultatBarn2 =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn2.aktør,
            )

        personResultatBarn2.setSortedVilkårResultater(
            lagVilkårForPerson(
                fom = oktober2020.førsteDagIInneværendeMåned(),
                tom = null,
                maksTom = barn1.fødselsdato.til18ÅrsVilkårsdato(),
                generiskeVilkår =
                    listOf(
                        Vilkår.BOSATT_I_RIKET,
                        Vilkår.BOR_MED_SØKER,
                        Vilkår.UNDER_18_ÅR,
                        Vilkår.GIFT_PARTNERSKAP,
                    ),
                spesielleVilkår =
                    setOf(
                        lagVilkårResultat(
                            periodeFom = mars2021.førsteDagIInneværendeMåned(),
                            periodeTom = mai2022.sisteDagIInneværendeMåned(),
                            vilkårType = Vilkår.LOVLIG_OPPHOLD,
                            resultat = Resultat.OPPFYLT,
                        ),
                        lagVilkårResultat(
                            periodeFom = mai2022.plusMonths(1).førsteDagIInneværendeMåned(),
                            periodeTom = null,
                            vilkårType = Vilkår.LOVLIG_OPPHOLD,
                            resultat = Resultat.OPPFYLT,
                        ),
                    ),
            ),
        )

        val personResultater = setOf(personResultatSøker, personResultatBarn1, personResultatBarn2)

        val tidslinje =
            personResultater.tilTidslinjeForSplitt(
                personerIPersongrunnlag = listOf(søker, barn1, barn2),
                fagsakType = FagsakType.NORMAL,
            )

        val perioder = tidslinje.tilPerioder()

        val perioderRelevantForTesting = perioder.filter { it.fom!!.toYearMonth().isSameOrBefore(mai2022) }

        assertEquals(4, perioderRelevantForTesting.size)

        val periode1 = perioderRelevantForTesting[0]
        val periode2 = perioderRelevantForTesting[1]
        val periode3 = perioderRelevantForTesting[2]
        val periode4 = perioderRelevantForTesting[3]

        assertPeriode(periode = periode1, forventetFom = februar2020.plusMonths(1), forventetTom = oktober2020)
        assertPeriode(periode = periode2, forventetFom = oktober2020.plusMonths(1), forventetTom = mars2021)
        assertPeriode(periode = periode3, forventetFom = mars2021.plusMonths(1), forventetTom = desember2021)
        assertPeriode(periode = periode4, forventetFom = desember2021.plusMonths(1), forventetTom = mai2022)
    }

    private fun assertPeriode(
        periode: Periode<List<VilkårResultat>?>,
        forventetFom: YearMonth,
        forventetTom: YearMonth,
    ) {
        assertEquals(forventetFom.førsteDagIInneværendeMåned(), periode.fom)
        assertEquals(forventetTom.sisteDagIInneværendeMåned(), periode.tom)
    }

    private fun lagVilkårForPerson(
        fom: LocalDate,
        tom: LocalDate? = null,
        spesielleVilkår: Set<VilkårResultat> = emptySet(),
        generiskeVilkår: List<Vilkår> =
            listOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD,
                Vilkår.UNDER_18_ÅR,
                Vilkår.GIFT_PARTNERSKAP,
            ),
        maksTom: LocalDate? = null,
        personResultat: PersonResultat? = null,
    ): Set<VilkårResultat> =
        spesielleVilkår +
            generiskeVilkår.map {
                lagVilkårResultat(
                    periodeFom = fom,
                    periodeTom = if (it == Vilkår.UNDER_18_ÅR) maksTom else tom,
                    vilkårType = it,
                    resultat = Resultat.OPPFYLT,
                    personResultat = personResultat,
                )
            }
}
