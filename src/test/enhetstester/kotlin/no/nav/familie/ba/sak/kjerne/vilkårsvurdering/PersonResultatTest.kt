package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
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
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.neste
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerNull
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.beskjærPå18År
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilForskjøvetTidslinjerForHvertOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilFørskjøvetVilkårResultatTidslinjeMap
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinjeForSplitt
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

        val tidslinjer = vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår(barnets18årsdag.minusYears(18))

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

        val tidslinjer = vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår(barnets18årsdag.minusYears(18))

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

        val tidslinjer = vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår(barnets18årsdag.minusYears(18))

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

        val under18årVilkårTidslinje: Tidslinje<VilkårResultat, Måned> = tidslinje {
            under18VilkårResultat.map {
                Periode(
                    fraOgMed = it.periodeFom!!.tilMånedTidspunkt().neste(),
                    tilOgMed = it.periodeTom!!.tilMånedTidspunkt(),
                    innhold = it
                )
            }
        }

        val under18PerioderFørBeskjæring = under18årVilkårTidslinje.perioder()

        Assertions.assertEquals(1, under18PerioderFørBeskjæring.size)
        Assertions.assertEquals(
            barn.fødselsdato.plusMonths(1).toYearMonth(),
            under18PerioderFørBeskjæring.first().fraOgMed.tilYearMonth()
        )
        Assertions.assertEquals(
            barn.fødselsdato.plusYears(18).toYearMonth(),
            under18PerioderFørBeskjæring.first().tilOgMed.tilYearMonth()
        )

        val tidslinjeBeskåret = under18årVilkårTidslinje.beskjærPå18År(barn.fødselsdato)

        val under18PerioderEtterBeskjæring = tidslinjeBeskåret.perioder()

        Assertions.assertEquals(1, under18PerioderEtterBeskjæring.size)
        Assertions.assertEquals(
            barn.fødselsdato.plusMonths(1).toYearMonth(),
            under18PerioderEtterBeskjæring.first().fraOgMed.tilYearMonth()
        )
        Assertions.assertEquals(
            barn.fødselsdato.plusYears(18).minusMonths(1).toYearMonth(),
            under18PerioderEtterBeskjæring.first().tilOgMed.tilYearMonth()
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

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = lagBehandling(),
            resultat = Resultat.OPPFYLT
        )

        val personResultatSøker = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søker.aktør
        )

        personResultatSøker.setSortedVilkårResultater(
            lagVilkårForPerson(
                personResultat = personResultatSøker,
                fom = februar2020.førsteDagIInneværendeMåned(),
                tom = null,
                generiskeVilkår = listOf(Vilkår.LOVLIG_OPPHOLD, Vilkår.BOSATT_I_RIKET)
            )
        )

        val personResultatBarn1 = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn1.aktør
        )

        personResultatBarn1.setSortedVilkårResultater(
            lagVilkårForPerson(
                fom = oktober2020.førsteDagIInneværendeMåned(),
                tom = null,
                maksTom = barn1.fødselsdato.til18ÅrsVilkårsdato(),
                spesielleVilkår = setOf(
                    lagVilkårResultat(
                        periodeFom = oktober2020.førsteDagIInneværendeMåned(),
                        periodeTom = desember2021.sisteDagIInneværendeMåned(),
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        resultat = Resultat.OPPFYLT,
                        utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED)
                    ),
                    lagVilkårResultat(
                        periodeFom = desember2021.plusMonths(1).førsteDagIInneværendeMåned(),
                        periodeTom = null,
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        resultat = Resultat.OPPFYLT
                    )
                )
            )
        )

        val personResultatBarn2 = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn2.aktør
        )

        personResultatBarn2.setSortedVilkårResultater(
            lagVilkårForPerson(
                fom = oktober2020.førsteDagIInneværendeMåned(),
                tom = null,
                maksTom = barn1.fødselsdato.til18ÅrsVilkårsdato(),
                generiskeVilkår = listOf(
                    Vilkår.BOSATT_I_RIKET,
                    Vilkår.BOR_MED_SØKER,
                    Vilkår.UNDER_18_ÅR,
                    Vilkår.GIFT_PARTNERSKAP
                ),
                spesielleVilkår = setOf(
                    lagVilkårResultat(
                        periodeFom = mars2021.førsteDagIInneværendeMåned(),
                        periodeTom = mai2022.sisteDagIInneværendeMåned(),
                        vilkårType = Vilkår.LOVLIG_OPPHOLD,
                        resultat = Resultat.OPPFYLT
                    ),
                    lagVilkårResultat(
                        periodeFom = mai2022.plusMonths(1).førsteDagIInneværendeMåned(),
                        periodeTom = null,
                        vilkårType = Vilkår.LOVLIG_OPPHOLD,
                        resultat = Resultat.OPPFYLT
                    )
                )
            )
        )

        val personResultater = setOf(personResultatSøker, personResultatBarn1, personResultatBarn2)

        val tidslinje = personResultater.tilTidslinjeForSplitt(
            listOf(søker, barn1, barn2)
        )

        val perioder = tidslinje.perioder()

        val perioderRelevantForTesting = perioder.filter { it.fraOgMed.tilYearMonth().isSameOrBefore(mai2022) }

        Assertions.assertEquals(4, perioderRelevantForTesting.size)

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
        periode: Periode<List<VilkårResultat>, Måned>,
        forventetFom: YearMonth,
        forventetTom: YearMonth
    ) {
        Assertions.assertEquals(forventetFom, periode.fraOgMed.tilYearMonth())
        Assertions.assertEquals(forventetTom, periode.tilOgMed.tilYearMonth())
    }

    private fun lagVilkårForPerson(
        fom: LocalDate,
        tom: LocalDate? = null,
        spesielleVilkår: Set<VilkårResultat> = emptySet(),
        generiskeVilkår: List<Vilkår> = listOf(
            Vilkår.BOSATT_I_RIKET,
            Vilkår.LOVLIG_OPPHOLD,
            Vilkår.UNDER_18_ÅR,
            Vilkår.GIFT_PARTNERSKAP
        ),
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
