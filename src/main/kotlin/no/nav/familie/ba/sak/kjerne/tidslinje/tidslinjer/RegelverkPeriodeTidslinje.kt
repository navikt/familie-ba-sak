package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.ListeKombinator
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.ToveisKombinator
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

private val nødvendigeVilkår = Vilkår.hentVilkårFor(PersonType.BARN)

private val eøsVilkår = listOf(
    Vilkår.BOR_MED_SØKER,
    Vilkår.LOVLIG_OPPHOLD,
    Vilkår.BOSATT_I_RIKET
)

class RegelverkPeriodeKombinator : ListeKombinator<VilkårRegelverkResultat, Regelverk?> {
    override fun kombiner(alleVilkårResultater: Iterable<VilkårRegelverkResultat>): Regelverk? {
        val oppfyllerNødvendigVilkår = alleVilkårResultater
            .filter { it.resultat == Resultat.OPPFYLT }
            .map { it.vilkår }
            .containsAll(nødvendigeVilkår)

        if (!oppfyllerNødvendigVilkår)
            return null

        val alleRelevanteVilkårErEøsVilkår = alleVilkårResultater
            .filter {
                it.regelverk == Regelverk.EØS_FORORDNINGEN
            }.map { it.vilkår }
            .containsAll(eøsVilkår)

        return if (alleRelevanteVilkårErEøsVilkår) Regelverk.EØS_FORORDNINGEN else Regelverk.NASJONALE_REGLER
    }
}

class RegelverkOgOppfyltePerioderKombinator : ToveisKombinator<Resultat, Regelverk?, Regelverk?> {
    override fun kombiner(venstre: Resultat?, høyre: Regelverk?): Regelverk? {
        return when {
            høyre == null || venstre == null -> null
            venstre != Resultat.OPPFYLT -> null
            venstre == Resultat.OPPFYLT && høyre == Regelverk.EØS_FORORDNINGEN -> Regelverk.EØS_FORORDNINGEN
            else -> Regelverk.NASJONALE_REGLER
        }
    }
}
