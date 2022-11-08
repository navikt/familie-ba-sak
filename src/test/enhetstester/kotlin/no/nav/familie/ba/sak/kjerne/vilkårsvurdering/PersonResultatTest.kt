package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.beskjærPå18År
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilForskjøvetTidslinjerForHvertVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilFørskjøvetVilkårResultatTidslinjeMap
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.YearMonth

class PersonResultatTest {
    private val mars2020 = YearMonth.of(2020, 3)
    private val april2020 = YearMonth.of(2020, 4)
    private val mai2020 = YearMonth.of(2020, 5)
    private val juni2020 = YearMonth.of(2020, 6)
    private val juli2020 = YearMonth.of(2020, 7)

    @Test
    fun `Skal forskyve en måned`() {
        val søker = lagPerson()

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = lagBehandling(),
            resultat = Resultat.OPPFYLT
        )

        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søker.aktør
        )
        val vilkårResultater = Vilkår.hentVilkårFor(søker.type)
            .map {
                VilkårResultat(
                    personResultat = personResultat,
                    periodeFom = mars2020.førsteDagIInneværendeMåned(),
                    periodeTom = mai2020.sisteDagIInneværendeMåned(),
                    vilkårType = it,
                    resultat = Resultat.OPPFYLT,
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id,
                    utdypendeVilkårsvurderinger = emptyList()
                )
            }

        personResultat.setSortedVilkårResultater(vilkårResultater.toSet())

        val førskjøvetVilkårResultatTidslinjeMap = setOf(personResultat).tilFørskjøvetVilkårResultatTidslinjeMap()
        Assertions.assertEquals(1, førskjøvetVilkårResultatTidslinjeMap.size)

        val forskjøvedeVedtaksperioder = førskjøvetVilkårResultatTidslinjeMap[søker.aktør]!!.perioder()
        Assertions.assertEquals(april2020, forskjøvedeVedtaksperioder.first().fraOgMed.tilYearMonth())
        Assertions.assertEquals(juni2020, forskjøvedeVedtaksperioder.first().tilOgMed.tilYearMonth())
    }

    @Test
    fun `Skal ikke lage mellomrom i back2back perioder`() {
        val søker = lagPerson()

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = lagBehandling(),
            resultat = Resultat.OPPFYLT
        )

        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søker.aktør
        )
        val vilkårResultater = listOf(
            VilkårResultat(
                personResultat = personResultat,
                periodeFom = mars2020.førsteDagIInneværendeMåned(),
                periodeTom = april2020.sisteDagIInneværendeMåned(),
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                begrunnelse = "",
                behandlingId = vilkårsvurdering.behandling.id,
                utdypendeVilkårsvurderinger = emptyList()
            ),
            VilkårResultat(
                personResultat = personResultat,
                periodeFom = mai2020.førsteDagIInneværendeMåned(),
                periodeTom = juni2020.sisteDagIInneværendeMåned(),
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                begrunnelse = "",
                behandlingId = vilkårsvurdering.behandling.id,
                utdypendeVilkårsvurderinger = emptyList()
            )
        )

        personResultat.setSortedVilkårResultater(vilkårResultater.toSet())

        val førskjøvetVilkårResultatTidslinjeMap = setOf(personResultat).tilFørskjøvetVilkårResultatTidslinjeMap()
        Assertions.assertEquals(1, førskjøvetVilkårResultatTidslinjeMap.size)

        val forskjøvedeVedtaksperioder = førskjøvetVilkårResultatTidslinjeMap[søker.aktør]!!.perioder()
        Assertions.assertEquals(april2020, forskjøvedeVedtaksperioder.first().fraOgMed.tilYearMonth())
        Assertions.assertEquals(mai2020, forskjøvedeVedtaksperioder.first().tilOgMed.tilYearMonth())

        Assertions.assertEquals(juni2020, forskjøvedeVedtaksperioder.elementAt(1).fraOgMed.tilYearMonth())
        Assertions.assertEquals(juli2020, forskjøvedeVedtaksperioder.elementAt(1).tilOgMed.tilYearMonth())
    }

    @Test
    fun `Skal opphøre neste måned dersom vilkår ikke varer ut måneden`() {
        val søker = lagPerson()

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = lagBehandling(),
            resultat = Resultat.OPPFYLT
        )

        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søker.aktør
        )
        val vilkårResultater = listOf(
            VilkårResultat(
                personResultat = personResultat,
                periodeFom = mars2020.førsteDagIInneværendeMåned(),
                periodeTom = april2020.sisteDagIInneværendeMåned().minusDays(1),
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                begrunnelse = "",
                behandlingId = vilkårsvurdering.behandling.id,
                utdypendeVilkårsvurderinger = emptyList()
            ),
            VilkårResultat(
                personResultat = personResultat,
                periodeFom = mai2020.førsteDagIInneværendeMåned(),
                periodeTom = juni2020.sisteDagIInneværendeMåned(),
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                begrunnelse = "",
                behandlingId = vilkårsvurdering.behandling.id,
                utdypendeVilkårsvurderinger = emptyList()
            )
        )

        personResultat.setSortedVilkårResultater(vilkårResultater.toSet())

        val førskjøvetVilkårResultatTidslinjeMap = setOf(personResultat).tilFørskjøvetVilkårResultatTidslinjeMap()
        Assertions.assertEquals(1, førskjøvetVilkårResultatTidslinjeMap.size)

        val forskjøvedeVedtaksperioder =
            førskjøvetVilkårResultatTidslinjeMap[søker.aktør]!!.perioder().filter { it.innhold != null }
        Assertions.assertEquals(2, forskjøvedeVedtaksperioder.size)

        Assertions.assertEquals(april2020, forskjøvedeVedtaksperioder.first().fraOgMed.tilYearMonth())
        Assertions.assertEquals(april2020, forskjøvedeVedtaksperioder.first().tilOgMed.tilYearMonth())

        Assertions.assertEquals(juni2020, forskjøvedeVedtaksperioder.elementAt(1).fraOgMed.tilYearMonth())
        Assertions.assertEquals(juli2020, forskjøvedeVedtaksperioder.elementAt(1).tilOgMed.tilYearMonth())
    }

    @Test
    fun `Skal beskjære på 18 års vilkåret`() {
        val august2022 = YearMonth.of(2022, 8)
        val september2022 = YearMonth.of(2022, 9)

        val juli2040 = YearMonth.of(2040, 7)

        val barn = lagPerson(type = PersonType.BARN)

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = barn.aktør,
            behandling = lagBehandling(),
            resultat = Resultat.OPPFYLT
        )

        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn.aktør
        )
        val vilkårResultater = Vilkår.hentVilkårFor(barn.type)
            .filter { it != Vilkår.UNDER_18_ÅR }
            .map {
                VilkårResultat(
                    personResultat = personResultat,
                    periodeFom = august2022.førsteDagIInneværendeMåned(),
                    periodeTom = null,
                    vilkårType = it,
                    resultat = Resultat.OPPFYLT,
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id,
                    utdypendeVilkårsvurderinger = emptyList()
                )
            } + VilkårResultat(
            personResultat = personResultat,
            periodeFom = august2022.førsteDagIInneværendeMåned(),
            periodeTom = august2022.førsteDagIInneværendeMåned().til18ÅrsVilkårsdato(),
            vilkårType = Vilkår.UNDER_18_ÅR,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            behandlingId = vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = emptyList()
        )

        personResultat.setSortedVilkårResultater(vilkårResultater.toSet())

        val førskjøvetVilkårResultatTidslinjeMap = setOf(personResultat).tilFørskjøvetVilkårResultatTidslinjeMap()
        Assertions.assertEquals(1, førskjøvetVilkårResultatTidslinjeMap.size)

        val forskjøvedeVedtaksperioder = førskjøvetVilkårResultatTidslinjeMap[barn.aktør]!!.perioder()
        Assertions.assertEquals(september2022, forskjøvedeVedtaksperioder.first().fraOgMed.tilYearMonth())
        Assertions.assertEquals(juli2040, forskjøvedeVedtaksperioder.first().tilOgMed.tilYearMonth())
    }

    @Test
    fun `Skal håndtere uendelighet`() {
        val søker = lagPerson()

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = lagBehandling(),
            resultat = Resultat.OPPFYLT
        )

        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søker.aktør
        )
        val vilkårResultater = Vilkår.hentVilkårFor(søker.type)
            .map {
                VilkårResultat(
                    personResultat = personResultat,
                    periodeFom = null,
                    periodeTom = null,
                    vilkårType = it,
                    resultat = Resultat.OPPFYLT,
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id,
                    utdypendeVilkårsvurderinger = emptyList()
                )
            }

        personResultat.setSortedVilkårResultater(vilkårResultater.toSet())

        val førskjøvetVilkårResultatTidslinjeMap = setOf(personResultat).tilFørskjøvetVilkårResultatTidslinjeMap()
        Assertions.assertEquals(1, førskjøvetVilkårResultatTidslinjeMap.size)

        val forskjøvedeVedtaksperioder = førskjøvetVilkårResultatTidslinjeMap[søker.aktør]!!.perioder()
        Assertions.assertEquals(null, forskjøvedeVedtaksperioder.first().fraOgMed.tilYearMonthEllerNull())
        Assertions.assertEquals(null, forskjøvedeVedtaksperioder.first().tilOgMed.tilYearMonthEllerNull())
    }

    @Test
    fun `Skal beholde split på utdypende vilkårsvurdering`() {
        val søker = lagPerson()

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = lagBehandling(),
            resultat = Resultat.OPPFYLT
        )

        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søker.aktør
        )
        val vilkårResultater = Vilkår.hentVilkårFor(søker.type)
            .map {
                VilkårResultat(
                    personResultat = personResultat,
                    periodeFom = mars2020.førsteDagIInneværendeMåned(),
                    periodeTom = april2020.sisteDagIInneværendeMåned(),
                    vilkårType = it,
                    resultat = Resultat.OPPFYLT,
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.VURDERING_ANNET_GRUNNLAG)
                )
            } + Vilkår.hentVilkårFor(søker.type).map {
            VilkårResultat(
                personResultat = personResultat,
                periodeFom = mai2020.førsteDagIInneværendeMåned(),
                periodeTom = null,
                vilkårType = it,
                resultat = Resultat.OPPFYLT,
                begrunnelse = "",
                behandlingId = vilkårsvurdering.behandling.id,
                utdypendeVilkårsvurderinger = emptyList()
            )
        }

        personResultat.setSortedVilkårResultater(vilkårResultater.toSet())

        val førskjøvetVilkårResultatTidslinjeMap = setOf(personResultat).tilFørskjøvetVilkårResultatTidslinjeMap()
        Assertions.assertEquals(1, førskjøvetVilkårResultatTidslinjeMap.size)

        val forskjøvedeVedtaksperioder = førskjøvetVilkårResultatTidslinjeMap[søker.aktør]!!.perioder()
        Assertions.assertEquals(april2020, forskjøvedeVedtaksperioder.first().fraOgMed.tilYearMonthEllerNull())
        Assertions.assertEquals(mai2020, forskjøvedeVedtaksperioder.first().tilOgMed.tilYearMonthEllerNull())

        Assertions.assertEquals(juni2020, forskjøvedeVedtaksperioder.elementAt(1).fraOgMed.tilYearMonthEllerNull())
        Assertions.assertEquals(null, forskjøvedeVedtaksperioder.elementAt(1).tilOgMed.tilYearMonthEllerNull())
    }

    @Test
    fun `skal håndtere bor med søker-overlapp`() {
        val søker = lagPerson()

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = lagBehandling(),
            resultat = Resultat.OPPFYLT
        )

        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søker.aktør
        ).also {
            it.setSortedVilkårResultater(
                setOf(
                    VilkårResultat(
                        personResultat = it,
                        periodeFom = mars2020.førsteDagIInneværendeMåned(),
                        periodeTom = null,
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id
                    ),
                    VilkårResultat(
                        personResultat = it,
                        periodeFom = null,
                        periodeTom = null,
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        resultat = Resultat.IKKE_OPPFYLT,
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id,
                        erEksplisittAvslagPåSøknad = true
                    )
                )
            )
        }
        assertDoesNotThrow { setOf(personResultat).tilFørskjøvetVilkårResultatTidslinjeMap() }
    }

    @Test
    fun `Skal lage riktig splitt når bor med søker går fra delt bosted til fullt`() {
        val fom = LocalDate.now().minusMonths(7).førsteDagIInneværendeMåned()
        val deltBostedTom = LocalDate.now().minusMonths(1).sisteDagIMåned()
        val barnets18årsdag = LocalDate.now().plusYears(14)

        val vilkårResultater = lagVilkårForPerson(
            fom = fom,
            tom = null,
            spesielleVilkår = setOf(
                lagVilkårResultat(
                    periodeFom = fom,
                    periodeTom = deltBostedTom,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED)
                ),
                lagVilkårResultat(
                    periodeFom = deltBostedTom.plusMonths(1).førsteDagIInneværendeMåned(),
                    periodeTom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT
                )
            ),
            maksTom = barnets18årsdag
        )

        val tidslinjer = vilkårResultater.tilForskjøvetTidslinjerForHvertVilkår(barnets18årsdag.minusYears(18))

        Assertions.assertEquals(5, tidslinjer.size)

        val borMedSøkerTidslinje = tidslinjer.first()
        val borMedSøkerPerioder = borMedSøkerTidslinje.perioder()

        Assertions.assertEquals(2, borMedSøkerPerioder.size)

        val deltBostedPeriode = borMedSøkerPerioder.first()
        val fullPeriode = borMedSøkerPerioder.last()

        Assertions.assertEquals(fom.plusMonths(1).toYearMonth(), deltBostedPeriode.fraOgMed.tilYearMonth())
        Assertions.assertEquals(deltBostedTom.toYearMonth(), deltBostedPeriode.tilOgMed.tilYearMonth())
        Assertions.assertEquals(deltBostedTom.plusMonths(1).toYearMonth(), fullPeriode.fraOgMed.tilYearMonth())
        Assertions.assertNull(fullPeriode.tilOgMed.tilYearMonthEllerNull())

        tidslinjer.subList(1, tidslinjer.size).forEach {
            Assertions.assertEquals(1, it.perioder().size)
            val periode = it.perioder().first()
            Assertions.assertEquals(fom.plusMonths(1).toYearMonth(), periode.fraOgMed.tilYearMonth())
            Assertions.assertNull(fullPeriode.tilOgMed.tilYearMonthEllerNull())
        }
    }

    @Test
    fun `Skal lage riktig splitt når bor med søker går fra fullt til delt bosted`() {
        val fom = LocalDate.now().minusMonths(7).førsteDagIInneværendeMåned()
        val deltBostedTom = LocalDate.now().minusMonths(1).sisteDagIMåned()
        val barnets18årsdag = LocalDate.now().plusYears(14)

        val vilkårResultater = lagVilkårForPerson(
            fom = fom,
            tom = null,
            spesielleVilkår = setOf(
                lagVilkårResultat(
                    periodeFom = fom,
                    periodeTom = deltBostedTom,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT
                ),
                lagVilkårResultat(
                    periodeFom = deltBostedTom.plusMonths(1).førsteDagIInneværendeMåned(),
                    periodeTom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED)
                )
            ),
            maksTom = barnets18årsdag
        )

        val tidslinjer = vilkårResultater.tilForskjøvetTidslinjerForHvertVilkår(barnets18årsdag.minusYears(18))

        Assertions.assertEquals(5, tidslinjer.size)

        val borMedSøkerTidslinje = tidslinjer.first()
        val borMedSøkerPerioder = borMedSøkerTidslinje.perioder()

        Assertions.assertEquals(2, borMedSøkerPerioder.size)

        val deltBostedPeriode = borMedSøkerPerioder.first()
        val fullPeriode = borMedSøkerPerioder.last()

        Assertions.assertEquals(fom.plusMonths(1).toYearMonth(), deltBostedPeriode.fraOgMed.tilYearMonth())
        Assertions.assertEquals(deltBostedTom.plusMonths(1).toYearMonth(), deltBostedPeriode.tilOgMed.tilYearMonth())
        Assertions.assertEquals(deltBostedTom.plusMonths(2).toYearMonth(), fullPeriode.fraOgMed.tilYearMonth())
        Assertions.assertNull(fullPeriode.tilOgMed.tilYearMonthEllerNull())

        tidslinjer.subList(1, tidslinjer.size).forEach {
            Assertions.assertEquals(1, it.perioder().size)
            val periode = it.perioder().first()
            Assertions.assertEquals(fom.plusMonths(1).toYearMonth(), periode.fraOgMed.tilYearMonth())
            Assertions.assertNull(fullPeriode.tilOgMed.tilYearMonthEllerNull())
        }
    }

    @Test
    fun `Skal lage riktig splitt når bor med søker er oppfylt i to back2back-perioder uten utdypende vilkårsvurdering`() {
        val fom = LocalDate.now().minusMonths(7).førsteDagIInneværendeMåned()
        val b2bTom = LocalDate.now().minusMonths(1).sisteDagIMåned()
        val barnets18årsdag = LocalDate.now().plusYears(14)

        val vilkårResultater = lagVilkårForPerson(
            fom = fom,
            tom = null,
            spesielleVilkår = setOf(
                lagVilkårResultat(
                    periodeFom = fom,
                    periodeTom = b2bTom,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT
                ),
                lagVilkårResultat(
                    periodeFom = b2bTom.plusMonths(1).førsteDagIInneværendeMåned(),
                    periodeTom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT
                )
            ),
            maksTom = barnets18årsdag
        )

        val tidslinjer = vilkårResultater.tilForskjøvetTidslinjerForHvertVilkår(barnets18årsdag.minusYears(18))

        Assertions.assertEquals(5, tidslinjer.size)

        val borMedSøkerTidslinje = tidslinjer.first()
        val borMedSøkerPerioder = borMedSøkerTidslinje.perioder()

        Assertions.assertEquals(2, borMedSøkerPerioder.size)

        val førstePeriode = borMedSøkerPerioder.first()
        val andrePeriode = borMedSøkerPerioder.last()

        Assertions.assertEquals(fom.plusMonths(1).toYearMonth(), førstePeriode.fraOgMed.tilYearMonth())
        Assertions.assertEquals(b2bTom.toYearMonth(), førstePeriode.tilOgMed.tilYearMonth())
        Assertions.assertEquals(b2bTom.plusMonths(1).toYearMonth(), andrePeriode.fraOgMed.tilYearMonth())
        Assertions.assertNull(andrePeriode.tilOgMed.tilYearMonthEllerNull())

        tidslinjer.subList(1, tidslinjer.size).forEach {
            Assertions.assertEquals(1, it.perioder().size)
            val periode = it.perioder().first()
            Assertions.assertEquals(fom.plusMonths(1).toYearMonth(), periode.fraOgMed.tilYearMonth())
            Assertions.assertNull(andrePeriode.tilOgMed.tilYearMonthEllerNull())
        }
    }

    @Test
    fun `Skal kutte UNDER_18 tidslinjen måneden før 18-årsdag`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(18))

        val under18VilkårResultat = listOf(
            lagVilkårResultat(
                periodeFom = barn.fødselsdato,
                periodeTom = barn.fødselsdato.plusYears(18).minusDays(1),
                vilkårType = Vilkår.UNDER_18_ÅR,
                resultat = Resultat.OPPFYLT
            )
        )

        val under18årVilkårTidslinje: Tidslinje<VilkårResultat, Måned> = tidslinje { under18VilkårResultat.map { Periode(fraOgMed = it.periodeFom!!.tilMånedTidspunkt().neste(), tilOgMed = it.periodeTom!!.tilMånedTidspunkt(), innhold = it) } }

        val under18PerioderFørBeskjæring = under18årVilkårTidslinje.perioder()

        Assertions.assertEquals(1, under18PerioderFørBeskjæring.size)
        Assertions.assertEquals(barn.fødselsdato.plusMonths(1).toYearMonth(), under18PerioderFørBeskjæring.first().fraOgMed.tilYearMonth())
        Assertions.assertEquals(barn.fødselsdato.plusYears(18).toYearMonth(), under18PerioderFørBeskjæring.first().tilOgMed.tilYearMonth())

        val tidslinjeBeskåret = under18årVilkårTidslinje.beskjærPå18År(barn.fødselsdato)

        val under18PerioderEtterBeskjæring = tidslinjeBeskåret.perioder()

        Assertions.assertEquals(1, under18PerioderEtterBeskjæring.size)
        Assertions.assertEquals(barn.fødselsdato.plusMonths(1).toYearMonth(), under18PerioderEtterBeskjæring.first().fraOgMed.tilYearMonth())
        Assertions.assertEquals(barn.fødselsdato.plusYears(18).minusMonths(1).toYearMonth(), under18PerioderEtterBeskjæring.first().tilOgMed.tilYearMonth())
    }

    private fun lagVilkårForPerson(
        fom: LocalDate,
        tom: LocalDate? = null,
        spesielleVilkår: Set<VilkårResultat> = emptySet(),
        generiskeVilkår: List<Vilkår> = listOf(Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD, Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP),
        maksTom: LocalDate? = null,
        personResultat: PersonResultat? = null
    ): Set<VilkårResultat> {
        return spesielleVilkår + generiskeVilkår.map {
            lagVilkårResultat(
                periodeFom = fom,
                periodeTom = if (it == Vilkår.UNDER_18_ÅR) maksTom else tom,
                vilkårType = it,
                resultat = Resultat.OPPFYLT,
                personResultat = personResultat
            )
        }
    }
}
