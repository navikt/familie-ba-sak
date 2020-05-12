package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagBehandlingResultat
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.familie.ba.sak.behandling.vilkår.VilkårDiff.oppdaterteBehandlingsresultater
import org.junit.jupiter.api.Assertions


class OppdaterBehandlingGrunnlagTest {



    @Test
    fun testoppdateringsmetode() {
        val fnr1 = randomFnr()
        val behandling = lagBehandling()
        val resA = lagBehandlingResultatA(behandling = behandling, fnr = fnr1)
        val resB = lagBehandlingResultatB(behandling = behandling, fnr = fnr1)

        val (oppdatert, gammelt) = oppdaterteBehandlingsresultater(resA, resB)
        Assertions.assertEquals(2, oppdatert.personResultater.first().vilkårResultater.size)
        Assertions.assertTrue(gammelt.personResultater.isEmpty())
    }

    fun lagBehandlingResultatA(fnr: String, behandling: Behandling): BehandlingResultat {
        val behandlingResultat = BehandlingResultat(
                behandling = behandling
        )
        val personResultat = PersonResultat(
                behandlingResultat = behandlingResultat,
                personIdent = fnr)
        personResultat.vilkårResultater =
                setOf(VilkårResultat(personResultat = personResultat,
                                     vilkårType = Vilkår.BOSATT_I_RIKET,
                                     resultat = Resultat.JA,
                                     periodeFom = LocalDate.now(),
                                     periodeTom = LocalDate.now(),
                                     begrunnelse = ""))
        behandlingResultat.personResultater = setOf(personResultat)
        return behandlingResultat
    }

    fun lagBehandlingResultatB(fnr: String, behandling: Behandling): BehandlingResultat {
        val behandlingResultat = BehandlingResultat(
                behandling = behandling
        )
        val personResultat = PersonResultat(
                behandlingResultat = behandlingResultat,
                personIdent = fnr)
        personResultat.vilkårResultater =
                setOf(VilkårResultat(personResultat = personResultat,
                                     vilkårType = Vilkår.BOSATT_I_RIKET,
                                     resultat = Resultat.JA,
                                     periodeFom = LocalDate.now(),
                                     periodeTom = LocalDate.now(),
                                     begrunnelse = ""),
                      VilkårResultat(personResultat = personResultat,
                                     vilkårType = Vilkår.LOVLIG_OPPHOLD,
                                     resultat = Resultat.JA,
                                     periodeFom = LocalDate.now(),
                                     periodeTom = LocalDate.now(),
                                     begrunnelse = ""))
        behandlingResultat.personResultater = setOf(personResultat)
        return behandlingResultat
    }
    /*
fun personResultaterInnvilget(søkerIdent: String,
                              barnIdent: String,
                              barnFødselsdato: LocalDate,
                              behandlingResultat: BehandlingResultat): List<PersonResultat> {

    val personResultat1 = PersonResultat(
            behandlingResultat = behandlingResultat,
            personIdent = søkerIdent,
            vilkårResultater = emptySet())
    val personResultat2 = PersonResultat(
            behandlingResultat = behandlingResultat,
            personIdent = barnIdent,
            vilkårResultater = emptySet())
    personResultat1.vilkårResultater =
            setOf(VilkårResultat(personResultat = personResultat1, vilkårType = Vilkår.BOSATT_I_RIKET,
                                 resultat = Resultat.JA,
                                 periodeFom = LocalDate.of(2018, 5, 8),
                                 periodeTom = null,
                                 begrunnelse = ""))
    personResultat2.vilkårResultater = setOf(
            VilkårResultat(personResultat = personResultat2,
                           vilkårType = Vilkår.BOSATT_I_RIKET,
                           resultat = Resultat.JA,
                           periodeFom = LocalDate.of(2018, 5, 8),
                           periodeTom = null,
                           begrunnelse = ""),
            VilkårResultat(personResultat = personResultat2,
                           vilkårType = Vilkår.UNDER_18_ÅR,
                           resultat = Resultat.JA,
                           periodeFom = barnFødselsdato,
                           periodeTom = barnFødselsdato.plusYears(18),
                           begrunnelse = ""),
            VilkårResultat(personResultat = personResultat2,
                           vilkårType = Vilkår.GIFT_PARTNERSKAP,
                           resultat = Resultat.JA,
                           periodeFom = LocalDate.of(2018, 5, 8),
                           periodeTom = null,
                           begrunnelse = ""),
            VilkårResultat(personResultat = personResultat2,
                           vilkårType = Vilkår.BOR_MED_SØKER,
                           resultat = Resultat.JA,
                           periodeFom = LocalDate.of(2018, 5, 8),
                           periodeTom = null,
                           begrunnelse = "")
    ))
    return listOf(personResultat1, personResultat2)
}
*/

}