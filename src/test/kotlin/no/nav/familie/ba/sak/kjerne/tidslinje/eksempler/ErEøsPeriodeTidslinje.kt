package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.tidslinje.ListeKombinator
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
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

class ErEøsPeriodeTidslinje(
    private val barnetsVilkårsresultater: Collection<Tidslinje<VilkårRegelverkResultat, Måned>>
) : TidslinjeSomStykkerOppTiden<Boolean, Måned>(barnetsVilkårsresultater) {

    private val kombinator = EøsPeriodeKombinator()
    override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<Måned>): Boolean {
        val alleVilkårResultater = barnetsVilkårsresultater.map { it.hentUtsnitt(tidspunkt) }
        return kombinator.kombiner(alleVilkårResultater.filterNotNull())
    }
}

class EøsPeriodeKombinator : ListeKombinator<VilkårRegelverkResultat, Boolean> {
    override fun kombiner(alleVilkårResultater: Iterable<VilkårRegelverkResultat>): Boolean {
        val oppfyllerNødvendigVilkår = alleVilkårResultater
            .filter { it.resultat == Resultat.OPPFYLT }
            .map { it.vilkår }
            .containsAll(nødvendigeVilkår)

        if (!oppfyllerNødvendigVilkår)
            return false

        val alleRelevanteVilkårErEøsVilkår = alleVilkårResultater
            .filter {
                it.regelverk == Regelverk.EØS_FORORDNINGEN
            }.map { it.vilkår }
            .containsAll(eøsVilkår)

        return alleRelevanteVilkårErEøsVilkår
    }
}
