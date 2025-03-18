package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.beskjærPå18År
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjeForOppfyltVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjerForHvertOppfylteVilkår
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
        assertEquals(deltBostedTom.plusMonths(1).sisteDagIMåned(), deltBostedPeriode.tom)
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
