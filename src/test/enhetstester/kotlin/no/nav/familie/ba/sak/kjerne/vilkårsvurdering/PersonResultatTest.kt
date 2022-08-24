package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilFørskjøvetVilkårResultatTidslinjeMap
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
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
            ),
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
            ),
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
        val august2122 = YearMonth.of(2122, 8)

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
            .map {
                VilkårResultat(
                    personResultat = personResultat,
                    periodeFom = august2022.førsteDagIInneværendeMåned(),
                    periodeTom = august2122.sisteDagIInneværendeMåned(),
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

        val forskjøvedeVedtaksperioder = førskjøvetVilkårResultatTidslinjeMap[barn.aktør]!!.perioder()
        Assertions.assertEquals(september2022, forskjøvedeVedtaksperioder.first().fraOgMed.tilYearMonth())
        Assertions.assertEquals(juli2040, forskjøvedeVedtaksperioder.first().tilOgMed.tilYearMonth())
    }
}
