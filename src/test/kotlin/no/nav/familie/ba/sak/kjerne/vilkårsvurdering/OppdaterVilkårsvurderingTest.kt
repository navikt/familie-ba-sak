package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.flyttResultaterTilInitielt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.lagFjernAdvarsel
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppdaterVilkårsvurderingTest {

    @Test
    fun `Skal legge til nytt vilkår`() {
        val fnr1 = randomFnr()
        val aktørId1 = randomAktørId()
        val behandling = lagBehandling()
        val resA = lagBehandlingResultat(behandling = behandling, fnrAktørId = listOf(Pair(fnr1, aktørId1)))
        val resB = lagBehandlingResultatB(behandling = behandling, fnrAktørId = listOf(Pair(fnr1, aktørId1)))

        val (oppdatert, gammelt) = flyttResultaterTilInitielt(resB, resA)
        Assertions.assertEquals(3, oppdatert.personResultater.first().vilkårResultater.size)
        Assertions.assertEquals(
            Resultat.OPPFYLT,
            oppdatert.personResultater.first()
                .vilkårResultater.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }?.resultat
        )
        Assertions.assertTrue(gammelt.personResultater.isEmpty())
    }

    @Test
    fun `Skal fjerne vilkår`() {
        val fnr1 = randomFnr()
        val aktørId1 = randomAktørId()
        val behandling = lagBehandling()
        val resA = lagBehandlingResultatB(behandling = behandling, fnrAktørId = listOf(Pair(fnr1, aktørId1)))
        val resB = lagBehandlingResultat(behandling = behandling, fnrAktørId = listOf(Pair(fnr1, aktørId1)))

        val (oppdatert, gammelt) = flyttResultaterTilInitielt(resB, resA)
        Assertions.assertEquals(2, oppdatert.personResultater.first().vilkårResultater.size)
        Assertions.assertEquals(
            Resultat.OPPFYLT,
            oppdatert.personResultater.first()
                .vilkårResultater.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }?.resultat
        )
        Assertions.assertEquals(1, gammelt.personResultater.size)
        Assertions.assertEquals(1, gammelt.personResultater.first().vilkårResultater.size)
    }

    @Test
    fun `Skal legge til person på vilkårsvurdering`() {
        val fnr1 = randomFnr()
        val fnr2 = randomFnr()
        val aktørId1 = randomAktørId()
        val aktørId2 = randomAktørId()
        val behandling = lagBehandling()
        val resA = lagBehandlingResultat(behandling = behandling, fnrAktørId = listOf(Pair(fnr1, aktørId1)))
        val resB = lagBehandlingResultat(
            behandling = behandling,
            fnrAktørId = listOf(Pair(fnr1, aktørId1), Pair(fnr2, aktørId2))
        )

        val (oppdatert, gammelt) = flyttResultaterTilInitielt(resB, resA)
        Assertions.assertEquals(2, oppdatert.personResultater.size)
        Assertions.assertEquals(0, gammelt.personResultater.size)
    }

    @Test
    fun `Skal fjerne person på vilkårsvurdering`() {
        val fnr1 = randomFnr()
        val fnr2 = randomFnr()
        val aktørId1 = randomAktørId()
        val aktørId2 = randomAktørId()
        val behandling = lagBehandling()
        val resA = lagBehandlingResultat(
            behandling = behandling,
            fnrAktørId = listOf(Pair(fnr1, aktørId1), Pair(fnr2, aktørId2))
        )
        val resB = lagBehandlingResultat(behandling = behandling, fnrAktørId = listOf(Pair(fnr1, aktørId1)))

        val (oppdatert, gammelt) = flyttResultaterTilInitielt(resB, resA)
        Assertions.assertEquals(1, oppdatert.personResultater.size)
        Assertions.assertEquals(1, gammelt.personResultater.size)
    }

    @Test
    fun `Skal lage advarsel tekst`() {
        val fnr1 = randomFnr()
        val fnr2 = randomFnr()
        val aktørId1 = randomAktørId()
        val aktørId2 = randomAktørId()
        val behandling = lagBehandling()
        val resultat1 = lagBehandlingResultat(
            behandling = behandling,
            fnrAktørId = listOf(Pair(fnr1, aktørId1), Pair(fnr2, aktørId2))
        )
        val resultat2 = lagBehandlingResultat(behandling = behandling, fnrAktørId = listOf(Pair(fnr2, aktørId2)))

        val resterende = flyttResultaterTilInitielt(resultat2, resultat1).second
        val fjernedeVilkår = resultat1.personResultater.first().vilkårResultater.toList()
        val generertAdvarsel = lagFjernAdvarsel(resterende.personResultater)

        Assertions.assertEquals(
            "Du har gjort endringer i behandlingsgrunnlaget. Dersom du går videre vil vilkår for følgende personer fjernes:\n" +
                fnr1 + ":\n" +
                "   - " + fjernedeVilkår[0].vilkårType.beskrivelse + "\n" +
                "   - " + fjernedeVilkår[1].vilkårType.beskrivelse + "\n",
            generertAdvarsel
        )
    }

    @Test
    fun `Skal beholde vilkår om utvidet barnetrygd når det eksisterer løpende sak med utvidet`() {
        val søkerFnr = randomFnr()
        val søkerAktørId = randomAktørId()
        val behandling = lagBehandling()

        val initUtenUtvidetVilkår =
            lagVilkårsvurderingMedForskjelligeTyperVilkår(søkerFnr, søkerAktørId, behandling, listOf())
        val aktivMedUtvidetVilkår =
            lagVilkårsvurderingMedForskjelligeTyperVilkår(
                søkerFnr,
                søkerAktørId,
                behandling,
                listOf(Vilkår.UTVIDET_BARNETRYGD)
            )

        val (nyInit, nyAktiv) = flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initUtenUtvidetVilkår,
            aktivVilkårsvurdering = aktivMedUtvidetVilkår,
            løpendeUnderkategori = BehandlingUnderkategori.UTVIDET
        )

        val nyInitInnholderUtvidetVilkår =
            nyInit.personResultater.first().vilkårResultater.any { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        Assertions.assertTrue(nyInitInnholderUtvidetVilkår)
        Assertions.assertTrue(nyAktiv.personResultater.isEmpty())
    }

    @Test
    fun `Skal fjerne vilkår om utvidet barnetrygd når det ikke eksisterer løpende sak med utvidet`() {
        val søkerFnr = randomFnr()
        val søkerAktørId = randomAktørId()
        val behandling = lagBehandling()

        val initUtenUtvidetVilkår =
            lagVilkårsvurderingMedForskjelligeTyperVilkår(søkerFnr, søkerAktørId, behandling, listOf())
        val aktivMedUtvidetVilkår =
            lagVilkårsvurderingMedForskjelligeTyperVilkår(
                søkerFnr,
                søkerAktørId,
                behandling,
                listOf(Vilkår.UTVIDET_BARNETRYGD)
            )

        val (nyInit, nyAktiv) = flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initUtenUtvidetVilkår,
            aktivVilkårsvurdering = aktivMedUtvidetVilkår,
            løpendeUnderkategori = BehandlingUnderkategori.ORDINÆR
        )
        val nyInitInnholderIkkeUtvidetVilkår =
            nyInit.personResultater.first().vilkårResultater.none { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
        val nyAktivInneholderUtvidetVilkår =
            nyAktiv.personResultater.first().vilkårResultater.any { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        Assertions.assertTrue(nyInitInnholderIkkeUtvidetVilkår)
        Assertions.assertTrue(nyAktivInneholderUtvidetVilkår)
    }

    fun lagVilkårsvurderingMedForskjelligeTyperVilkår(
        søkerFnr: String,
        søkerAktørId: AktørId,
        behandling: Behandling,
        vilkår: List<Vilkår>
    ): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val personResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søkerFnr, aktørId = søkerAktørId)
        val vilkårResultater =
            vilkår.map { lagVilkårResultat(vilkårType = it, personResultat = personResultat) }.toSet()
        personResultat.setSortedVilkårResultater(vilkårResultater)
        vilkårsvurdering.personResultater = setOf(personResultat)
        return vilkårsvurdering
    }

    fun lagBehandlingResultat(fnrAktørId: List<Pair<String, AktørId>>, behandling: Behandling): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling
        )

        vilkårsvurdering.personResultater = fnrAktørId.map {
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                personIdent = it.first,
                aktørId = it.second
            )

            personResultat.setSortedVilkårResultater(
                setOf(
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.now(),
                        periodeTom = LocalDate.now(),
                        begrunnelse = "",
                        behandlingId = behandling.id
                    ),
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.GIFT_PARTNERSKAP,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.now(),
                        periodeTom = LocalDate.now(),
                        begrunnelse = "",
                        behandlingId = behandling.id
                    )
                )
            )

            personResultat
        }.toSet()

        return vilkårsvurdering
    }

    fun lagBehandlingResultatB(fnrAktørId: List<Pair<String, AktørId>>, behandling: Behandling): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling
        )

        vilkårsvurdering.personResultater = fnrAktørId.map {
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                personIdent = it.first,
                aktørId = it.second
            )

            personResultat.setSortedVilkårResultater(
                setOf(
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.now(),
                        periodeTom = LocalDate.now(),
                        begrunnelse = "",
                        behandlingId = behandling.id
                    ),
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.GIFT_PARTNERSKAP,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.now(),
                        periodeTom = LocalDate.now(),
                        begrunnelse = "",
                        behandlingId = behandling.id
                    ),
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.LOVLIG_OPPHOLD,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.now(),
                        periodeTom = LocalDate.now(),
                        begrunnelse = "",
                        behandlingId = behandling.id
                    )
                )
            )
            personResultat
        }.toSet()

        return vilkårsvurdering
    }
}
