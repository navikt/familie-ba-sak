package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.nare.Resultat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårResultatComparatorTest {

    @Test
    fun `Sorter vilkårresultater på periodeFom, resultat, type`() {
        val vilkårResultater = setOf(
                lagVilkårResultat(periodeFom = LocalDate.of(1814, 5, 17),
                                  resultat = Resultat.IKKE_VURDERT,
                                  type = Vilkår.BOR_MED_SØKER),
                lagVilkårResultat(periodeFom = LocalDate.of(1814, 5, 17),
                                  resultat = Resultat.IKKE_VURDERT,
                                  type = Vilkår.BOR_MED_SØKER),
                lagVilkårResultat(periodeFom = LocalDate.of(1814, 5, 17),
                                  resultat = Resultat.IKKE_OPPFYLT,
                                  type = Vilkår.BOR_MED_SØKER),
                lagVilkårResultat(periodeFom = LocalDate.of(1814, 5, 17),
                                  resultat = Resultat.IKKE_VURDERT,
                                  type = Vilkår.GIFT_PARTNERSKAP))
        print(vilkårResultater.toSortedSet(VilkårResultat.VilkårResultatComparator))
    }

    @Test
    fun `Filtrerer ulike vilkår som overlapper på periodeFom, resultat, type`() {
        val vilkårResultater = setOf(
                lagVilkårResultat(periodeFom = LocalDate.of(1814, 5, 17),
                                  resultat = Resultat.IKKE_VURDERT,
                                  type = Vilkår.BOR_MED_SØKER))
        val vilkårForSøker = setOf(Vilkår.BOSATT_I_RIKET,
                                   Vilkår.LOVLIG_OPPHOLD)
    }

    private fun lagVilkårResultat(periodeFom: LocalDate, resultat: Resultat, type: Vilkår) =
            VilkårResultat(personResultat = null,
                           periodeFom = periodeFom,
                           periodeTom = null,
                           vilkårType = type,
                           resultat = resultat,
                           begrunnelse = "",
                           behandlingId = 0,
                           regelInput = null,
                           regelOutput = null)
}