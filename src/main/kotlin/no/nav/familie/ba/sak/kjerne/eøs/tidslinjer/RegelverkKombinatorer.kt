package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.RegelverkResultat.OPPFYLT_BLANDET_REGELVERK
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.RegelverkResultat.OPPFYLT_NASJONALE_REGLER
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.RegelverkResultat.OPPFYLT_REGELVERK_IKKE_SATT
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

fun kombinerVilkårResultaterTilRegelverkResultat(
    personType: PersonType,
    alleVilkårResultater: Iterable<VilkårRegelverkResultat>
): RegelverkResultat? {

    val nødvendigeVilkår = Vilkår.hentVilkårFor(personType)
    val regelverkVilkår = nødvendigeVilkår.filter { it.harRegelverk }

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
        eøsVilkår.containsAll(regelverkVilkår) -> OPPFYLT_EØS_FORORDNINGEN
        nasjonaleVilkår.containsAll(regelverkVilkår) -> OPPFYLT_NASJONALE_REGLER
        eøsVilkår.isNotEmpty() || nasjonaleVilkår.isNotEmpty() -> OPPFYLT_BLANDET_REGELVERK
        else -> OPPFYLT_REGELVERK_IKKE_SATT
    }
}
