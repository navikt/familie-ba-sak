package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

private val nødvendigeVilkårSøker = Vilkår.hentVilkårFor(PersonType.SØKER)

private val nødvendigeVilkårBarn = Vilkår.hentVilkårFor(PersonType.BARN)

class SøkerOppfyllerVilkårKombinator {
    fun kombiner(liste: Iterable<VilkårRegelverkResultat>): Resultat {

        val listeUtenUtvidetVilkåret = liste.filter { it.vilkår != Vilkår.UTVIDET_BARNETRYGD }
        return when {
            listeUtenUtvidetVilkåret.all { it.resultat == Resultat.OPPFYLT } &&
                listeUtenUtvidetVilkåret.map { it.vilkår }.distinct()
                    .containsAll(nødvendigeVilkårSøker) -> Resultat.OPPFYLT
            listeUtenUtvidetVilkåret.any { it.resultat == Resultat.IKKE_OPPFYLT } -> Resultat.IKKE_OPPFYLT
            else -> Resultat.IKKE_VURDERT
        }
    }
}

class BarnOppfyllerVilkårKombinator {
    fun kombiner(liste: Iterable<VilkårRegelverkResultat>): Resultat {
        return when {
            liste.all { it.resultat == Resultat.OPPFYLT } &&
                liste.map { it.vilkår }.distinct().containsAll(nødvendigeVilkårBarn) -> Resultat.OPPFYLT
            liste.any { it.resultat == Resultat.IKKE_OPPFYLT } -> Resultat.IKKE_OPPFYLT
            else -> Resultat.IKKE_VURDERT
        }
    }
}

class BarnIKombinasjonMedSøkerOppfyllerVilkårKombinator {
    fun kombiner(venstre: Resultat?, høyre: Resultat?): Resultat {
        return when {
            venstre == Resultat.OPPFYLT && høyre == Resultat.OPPFYLT -> Resultat.OPPFYLT
            venstre == Resultat.IKKE_OPPFYLT || høyre == Resultat.IKKE_OPPFYLT -> Resultat.IKKE_OPPFYLT
            else -> Resultat.IKKE_VURDERT
        }
    }
}
