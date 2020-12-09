package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingUtils.flyttResultaterTilInitielt
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingUtils.lagFjernAdvarsel
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.nare.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppdaterBehandlingGrunnlagTest {

    @Test
    fun `Skal legge til nytt vilkår`() {
        val fnr1 = randomFnr()
        val behandling = lagBehandling()
        val resA = lagBehandlingResultat(behandling = behandling, fnr = listOf(fnr1))
        val resB = lagBehandlingResultatB(behandling = behandling, fnr = listOf(fnr1))

        val (oppdatert, gammelt) = flyttResultaterTilInitielt(resB, resA)
        Assertions.assertEquals(3, oppdatert.personResultater.first().vilkårResultater.size)
        Assertions.assertEquals(Resultat.OPPFYLT,
                                oppdatert.personResultater.first()
                                        .vilkårResultater.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }?.resultat
        )
        Assertions.assertTrue(gammelt.personResultater.isEmpty())
    }

    @Test
    fun `Skal fjerne vilkår`() {
        val fnr1 = randomFnr()
        val behandling = lagBehandling()
        val resA = lagBehandlingResultatB(behandling = behandling, fnr = listOf(fnr1))
        val resB = lagBehandlingResultat(behandling = behandling, fnr = listOf(fnr1))

        val (oppdatert, gammelt) = flyttResultaterTilInitielt(resB, resA)
        Assertions.assertEquals(2, oppdatert.personResultater.first().vilkårResultater.size)
        Assertions.assertEquals(Resultat.OPPFYLT,
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
        val behandling = lagBehandling()
        val resA = lagBehandlingResultat(behandling = behandling, fnr = listOf(fnr1))
        val resB = lagBehandlingResultat(behandling = behandling, fnr = listOf(fnr1, fnr2))

        val (oppdatert, gammelt) = flyttResultaterTilInitielt(resB, resA)
        Assertions.assertEquals(2, oppdatert.personResultater.size)
        Assertions.assertEquals(0, gammelt.personResultater.size)
    }

    @Test
    fun `Skal fjerne person på vilkårsvurdering`() {
        val fnr1 = randomFnr()
        val fnr2 = randomFnr()
        val behandling = lagBehandling()
        val resA = lagBehandlingResultat(behandling = behandling, fnr = listOf(fnr1, fnr2))
        val resB = lagBehandlingResultat(behandling = behandling, fnr = listOf(fnr1))

        val (oppdatert, gammelt) = flyttResultaterTilInitielt(resB, resA)
        Assertions.assertEquals(1, oppdatert.personResultater.size)
        Assertions.assertEquals(1, gammelt.personResultater.size)
    }

    @Test
    fun `Skal lage advarsel tekst`() {
        val fnr1 = randomFnr()
        val fnr2 = randomFnr()
        val behandling = lagBehandling()
        val resultat1 = lagBehandlingResultat(behandling = behandling, fnr = listOf(fnr1, fnr2))
        val resultat2 = lagBehandlingResultat(behandling = behandling, fnr = listOf(fnr2))

        val resterende = flyttResultaterTilInitielt(resultat2, resultat1).second
        val fjernedeVilkår = resultat1.personResultater.first().vilkårResultater.toList()
        val generertAdvarsel = lagFjernAdvarsel(resterende.personResultater)

        Assertions.assertEquals("Du har gjort endringer i behandlingsgrunnlaget. Dersom du går videre vil vilkår for følgende personer fjernes:\n" +
                                fnr1 + ":\n" +
                                "   - " + fjernedeVilkår[0].vilkårType.spesifikasjon.beskrivelse + "\n" +
                                "   - " + fjernedeVilkår[1].vilkårType.spesifikasjon.beskrivelse + "\n", generertAdvarsel)
    }

    fun lagBehandlingResultat(fnr: List<String>, behandling: Behandling): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(
                behandling = behandling
        )

        vilkårsvurdering.personResultater = fnr.map {
            val personResultat = PersonResultat(
                    vilkårsvurdering = vilkårsvurdering,
                    personIdent = it)

            personResultat.setVilkårResultater(
                    setOf(VilkårResultat(personResultat = personResultat,
                                         vilkårType = Vilkår.BOSATT_I_RIKET,
                                         resultat = Resultat.OPPFYLT,
                                         periodeFom = LocalDate.now(),
                                         periodeTom = LocalDate.now(),
                                         begrunnelse = "",
                                         behandlingId = behandling.id,
                                         regelInput = null,
                                         regelOutput = null),
                          VilkårResultat(personResultat = personResultat,
                                         vilkårType = Vilkår.GIFT_PARTNERSKAP,
                                         resultat = Resultat.OPPFYLT,
                                         periodeFom = LocalDate.now(),
                                         periodeTom = LocalDate.now(),
                                         begrunnelse = "",
                                         behandlingId = behandling.id,
                                         regelInput = null,
                                         regelOutput = null)))

            personResultat
        }.toSet()

        return vilkårsvurdering
    }

    fun lagBehandlingResultatB(fnr: List<String>, behandling: Behandling): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(
                behandling = behandling
        )

        vilkårsvurdering.personResultater = fnr.map {
            val personResultat = PersonResultat(
                    vilkårsvurdering = vilkårsvurdering,
                    personIdent = it)

            personResultat.setVilkårResultater(
                    setOf(
                            VilkårResultat(personResultat = personResultat,
                                           vilkårType = Vilkår.BOSATT_I_RIKET,
                                           resultat = Resultat.OPPFYLT,
                                           periodeFom = LocalDate.now(),
                                           periodeTom = LocalDate.now(),
                                           begrunnelse = "",
                                           behandlingId = behandling.id,
                                           regelInput = null,
                                           regelOutput = null
                            ),
                            VilkårResultat(personResultat = personResultat,
                                           vilkårType = Vilkår.GIFT_PARTNERSKAP,
                                           resultat = Resultat.OPPFYLT,
                                           periodeFom = LocalDate.now(),
                                           periodeTom = LocalDate.now(),
                                           begrunnelse = "",
                                           behandlingId = behandling.id,
                                           regelInput = null,
                                           regelOutput = null
                            ),
                            VilkårResultat(personResultat = personResultat,
                                           vilkårType = Vilkår.LOVLIG_OPPHOLD,
                                           resultat = Resultat.OPPFYLT,
                                           periodeFom = LocalDate.now(),
                                           periodeTom = LocalDate.now(),
                                           begrunnelse = "",
                                           behandlingId = behandling.id,
                                           regelInput = null,
                                           regelOutput = null
                            ))
            )
            personResultat
        }.toSet()

        return vilkårsvurdering
    }
}