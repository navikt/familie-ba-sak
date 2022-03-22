package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.tidslinje.ListeKombinator
import no.nav.familie.ba.sak.kjerne.tidslinje.ToveisKombinator
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

private val nødvendigeVilkårSøker = listOf(
    Vilkår.LOVLIG_OPPHOLD,
    Vilkår.BOSATT_I_RIKET
)

private val nødvendigeVilkårSøkerMedUtvidet = nødvendigeVilkårSøker + Vilkår.UTVIDET_BARNETRYGD

private val nødvendigeVilkårBarn = listOf(
    Vilkår.UNDER_18_ÅR,
    Vilkår.BOR_MED_SØKER,
    Vilkår.GIFT_PARTNERSKAP,
    Vilkår.LOVLIG_OPPHOLD,
    Vilkår.BOSATT_I_RIKET
)

class SøkerOppfyllerVilkårKombinator : ListeKombinator<VilkårRegelverkResultat, Resultat> {
    override fun kombiner(liste: Iterable<VilkårRegelverkResultat>): Resultat {
        val nødvendigeVilkårAvhengigAvVurdering =
            if (liste.any {
                    it.vilkår == Vilkår.UTVIDET_BARNETRYGD
                }) nødvendigeVilkårSøkerMedUtvidet else nødvendigeVilkårSøker

        return when {
            liste.all { it.resultat == Resultat.OPPFYLT } &&
                liste.map { it.vilkår }.distinct().containsAll(nødvendigeVilkårAvhengigAvVurdering) -> Resultat.OPPFYLT
            liste.any { it.resultat == Resultat.IKKE_OPPFYLT } -> Resultat.IKKE_OPPFYLT
            else -> Resultat.IKKE_VURDERT
        }
    }
}

class BarnOppfyllerVilkårKombinator : ListeKombinator<VilkårRegelverkResultat, Resultat> {
    override fun kombiner(liste: Iterable<VilkårRegelverkResultat>): Resultat {
        return when {
            liste.all { it.resultat == Resultat.OPPFYLT } &&
                liste.map { it.vilkår }.distinct().containsAll(nødvendigeVilkårBarn) -> Resultat.OPPFYLT
            liste.any { it.resultat == Resultat.IKKE_OPPFYLT } -> Resultat.IKKE_OPPFYLT
            else -> Resultat.IKKE_VURDERT
        }
    }
}

class BarnIKombinasjonMedSøkerOppfyllerVilkårKombinator : ToveisKombinator<Resultat, Resultat, Resultat> {
    override fun kombiner(venstre: Resultat?, høyre: Resultat?): Resultat {
        return when {
            venstre == Resultat.OPPFYLT && høyre == Resultat.OPPFYLT -> Resultat.OPPFYLT
            venstre == Resultat.IKKE_OPPFYLT || høyre == Resultat.IKKE_OPPFYLT -> Resultat.IKKE_OPPFYLT
            else -> Resultat.IKKE_VURDERT
        }
    }
}
