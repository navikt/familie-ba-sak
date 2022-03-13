package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.PeriodeInnhold
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.idListe
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

    override fun kalkulerInnhold(tidspunkt: Tidspunkt): PeriodeInnhold<Boolean> {
        val alleVilkårResultatUtsnitt = barnetsVilkårsresultater.map { it.hentUtsnitt(tidspunkt) }
        val oppfyllerNødvendigVilkår = alleVilkårResultatUtsnitt
            .filter { it.innhold?.resultat == Resultat.OPPFYLT }
            .map { it.innhold?.vilkårType }
            .containsAll(nødvendigeVilkår)

        if (!oppfyllerNødvendigVilkår)
            return PeriodeInnhold(false, alleVilkårResultatUtsnitt.idListe())

        val alleRelevanteVilkårErEøsVilkår = alleVilkårResultatUtsnitt
            .filter {
                it.innhold?.vurderesEtter == Regelverk.EØS_FORORDNINGEN
            }.map { it.innhold?.vilkårType }
            .containsAll(eøsVilkår)

        return PeriodeInnhold(alleRelevanteVilkårErEøsVilkår, alleVilkårResultatUtsnitt.idListe())
    }
}
