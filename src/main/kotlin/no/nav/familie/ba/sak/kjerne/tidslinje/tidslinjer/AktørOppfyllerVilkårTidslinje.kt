package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.ListeKombinator
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.ToveisKombinator
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

private val nødvendigeVilkårSøker = Vilkår.hentVilkårFor(PersonType.SØKER)

private val nødvendigeVilkårBarn = Vilkår.hentVilkårFor(PersonType.BARN)

class SøkerOppfyllerVilkårKombinator : ListeKombinator<VilkårRegelverkResultat, Resultat> {
    override fun kombiner(liste: Iterable<VilkårRegelverkResultat>): Resultat {

        return when {
            liste.all { it.resultat == Resultat.OPPFYLT } &&
                liste.map { it.vilkår }.distinct().containsAll(nødvendigeVilkårSøker) -> Resultat.OPPFYLT
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
