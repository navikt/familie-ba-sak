package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.tidslinje.ListeKombinator
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

class AktørOppfyllerVilkårTidslinje(
    private val vilkårResultatTidslinjer: Collection<Tidslinje<VilkårRegelverkResultat, Måned>>,
    private val vilkårKombinator: ListeKombinator<VilkårRegelverkResultat, Boolean>
) : TidslinjeSomStykkerOppTiden<Boolean, Måned>(vilkårResultatTidslinjer) {

    override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<Måned>): Boolean {
        val vilkårResultater = vilkårResultatTidslinjer.map { it.hentUtsnitt(tidspunkt) }.filterNotNull()
        return vilkårKombinator.kombiner(vilkårResultater)
    }
}

private val nødvendigeVilkårSøker = listOf(
    Vilkår.LOVLIG_OPPHOLD,
    Vilkår.BOSATT_I_RIKET
)

private val nødvendigeVilkårBarn = listOf(
    Vilkår.UNDER_18_ÅR,
    Vilkår.BOR_MED_SØKER,
    Vilkår.GIFT_PARTNERSKAP,
    Vilkår.LOVLIG_OPPHOLD,
    Vilkår.BOSATT_I_RIKET
)

class SøkerOppfyllerVilkårKombinator : ListeKombinator<VilkårRegelverkResultat, Boolean> {
    override fun kombiner(liste: Iterable<VilkårRegelverkResultat>): Boolean {
        return liste.all { it.resultat == Resultat.OPPFYLT } &&
            liste.map { it.vilkår }.distinct().containsAll(nødvendigeVilkårSøker)
    }
}

class BarnOppfyllerVilkårKombinator : ListeKombinator<VilkårRegelverkResultat, Boolean> {
    override fun kombiner(liste: Iterable<VilkårRegelverkResultat>): Boolean {
        return liste.all { it.resultat == Resultat.OPPFYLT } &&
            liste.map { it.vilkår }.distinct().containsAll(nødvendigeVilkårBarn)
    }
}
