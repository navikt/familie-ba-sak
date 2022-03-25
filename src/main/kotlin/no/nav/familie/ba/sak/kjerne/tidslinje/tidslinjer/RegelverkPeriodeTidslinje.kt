package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.ListeKombinator
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

private val nødvendigeVilkår = listOf(
    Vilkår.UNDER_18_ÅR,
    Vilkår.BOR_MED_SØKER,
    Vilkår.GIFT_PARTNERSKAP,
    Vilkår.LOVLIG_OPPHOLD,
    Vilkår.BOSATT_I_RIKET
)

private val eøsVilkår = listOf(
    Vilkår.BOR_MED_SØKER,
    Vilkår.LOVLIG_OPPHOLD,
    Vilkår.BOSATT_I_RIKET
)

class RegelverkPeriodeKombinator : ListeKombinator<VilkårRegelverkResultat, Regelverk> {
    override fun kombiner(alleVilkårResultater: Iterable<VilkårRegelverkResultat>): Regelverk {
        val oppfyllerNødvendigVilkår = alleVilkårResultater
            .filter { it.resultat == Resultat.OPPFYLT }
            .map { it.vilkår }
            .containsAll(nødvendigeVilkår)

        if (!oppfyllerNødvendigVilkår)
            return Regelverk.NASJONALE_REGLER

        val alleRelevanteVilkårErEøsVilkår = alleVilkårResultater
            .filter {
                it.regelverk == Regelverk.EØS_FORORDNINGEN
            }.map { it.vilkår }
            .containsAll(eøsVilkår)

        return if (alleRelevanteVilkårErEøsVilkår) Regelverk.EØS_FORORDNINGEN else Regelverk.NASJONALE_REGLER
    }
}
