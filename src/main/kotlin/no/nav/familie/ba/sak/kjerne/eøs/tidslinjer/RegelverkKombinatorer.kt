package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

private val nødvendigeVilkår = Vilkår.hentVilkårFor(PersonType.BARN)

private val regelverkVilkår = listOf(
    Vilkår.BOR_MED_SØKER,
    Vilkår.LOVLIG_OPPHOLD,
    Vilkår.BOSATT_I_RIKET
)

fun kombinerVilkårResultaterTilRegelverkResultat(alleVilkårResultater: Iterable<VilkårRegelverkResultat>): RegelverkResultat? {
    val oppfyllerNødvendigVilkår = alleVilkårResultater
        .filter { it.resultat == Resultat.OPPFYLT }
        .map { it.vilkår }
        .containsAll(nødvendigeVilkår)

    if (!oppfyllerNødvendigVilkår)
        return null

    val eøsVilkår = alleVilkårResultater
        .filter { it.regelverk == Regelverk.EØS_FORORDNINGEN }.map { it.vilkår }

    val nasjonaleVilkår = alleVilkårResultater
        .filter { it.regelverk == Regelverk.NASJONALE_REGLER }.map { it.vilkår }

    return when {
        eøsVilkår.containsAll(regelverkVilkår) -> RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN
        nasjonaleVilkår.containsAll(regelverkVilkår) -> RegelverkResultat.OPPFYLT_NASJONALE_REGLER
        eøsVilkår.size > 0 || nasjonaleVilkår.size > 0 -> RegelverkResultat.OPPFYLT_BLANDET_REGELVERK
        else -> RegelverkResultat.OPPFYLT_REGELVERK_IKKE_SATT
    }
}

fun kombinerVilkårResultatMedRegelverkResultat(
    resultat: Resultat?,
    regelverkResultat: RegelverkResultat?
): RegelverkResultat? {
    return when (resultat) {
        null -> null
        Resultat.OPPFYLT -> regelverkResultat
        Resultat.IKKE_OPPFYLT -> RegelverkResultat.IKKE_OPPFYLT
        Resultat.IKKE_VURDERT -> RegelverkResultat.IKKE_VURDERT
    }
}
