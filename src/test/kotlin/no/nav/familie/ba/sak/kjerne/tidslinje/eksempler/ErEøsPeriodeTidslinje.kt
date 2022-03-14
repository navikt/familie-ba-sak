package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

class ErEøsPeriodeTidslinje(
    private val barnetsVilkårsresultater: Collection<Tidslinje<VilkårResultat>>
) : KalkulerendeTidslinje<Boolean>(barnetsVilkårsresultater) {

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

    override fun kalkulerInnhold(tidspunkt: Tidspunkt): Boolean? {
        val alleVilkårResultater = barnetsVilkårsresultater.map { it.hentUtsnitt(tidspunkt) }
        val oppfyllerNødvendigVilkår = alleVilkårResultater
            .filter { it?.resultat == Resultat.OPPFYLT }
            .map { it?.vilkårType }
            .containsAll(nødvendigeVilkår)

        if (!oppfyllerNødvendigVilkår)
            return false

        val alleRelevanteVilkårErEøsVilkår = alleVilkårResultater
            .filter {
                it?.vurderesEtter == Regelverk.EØS_FORORDNINGEN
            }.map { it?.vilkårType }
            .containsAll(eøsVilkår)

        return alleRelevanteVilkårErEøsVilkår
    }
}
