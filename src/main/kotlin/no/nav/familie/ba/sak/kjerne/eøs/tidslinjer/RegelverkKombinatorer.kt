package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.RegelverkResultat.IKKE_FULLT_VURDERT
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.RegelverkResultat.IKKE_OPPFYLT
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
        eøsVilkår.size > 0 || nasjonaleVilkår.size > 0 -> OPPFYLT_BLANDET_REGELVERK
        else -> OPPFYLT_REGELVERK_IKKE_SATT
    }
}

fun RegelverkResultat?.kombinerMed(resultat: RegelverkResultat?) = when (this) {
    null -> when (resultat) {
        null -> null
        else -> IKKE_FULLT_VURDERT
    }
    OPPFYLT_EØS_FORORDNINGEN -> when (resultat) {
        null -> IKKE_FULLT_VURDERT
        OPPFYLT_EØS_FORORDNINGEN -> OPPFYLT_EØS_FORORDNINGEN
        OPPFYLT_NASJONALE_REGLER -> OPPFYLT_BLANDET_REGELVERK
        OPPFYLT_BLANDET_REGELVERK -> OPPFYLT_BLANDET_REGELVERK
        OPPFYLT_REGELVERK_IKKE_SATT -> OPPFYLT_BLANDET_REGELVERK
        IKKE_FULLT_VURDERT -> IKKE_FULLT_VURDERT
        IKKE_OPPFYLT -> IKKE_OPPFYLT
    }
    OPPFYLT_NASJONALE_REGLER -> when (resultat) {
        null -> IKKE_FULLT_VURDERT
        OPPFYLT_EØS_FORORDNINGEN -> OPPFYLT_BLANDET_REGELVERK
        OPPFYLT_NASJONALE_REGLER -> OPPFYLT_NASJONALE_REGLER
        OPPFYLT_BLANDET_REGELVERK -> OPPFYLT_BLANDET_REGELVERK
        OPPFYLT_REGELVERK_IKKE_SATT -> OPPFYLT_BLANDET_REGELVERK
        IKKE_FULLT_VURDERT -> IKKE_FULLT_VURDERT
        IKKE_OPPFYLT -> IKKE_OPPFYLT
    }
    OPPFYLT_BLANDET_REGELVERK -> when (resultat) {
        null -> IKKE_FULLT_VURDERT
        OPPFYLT_EØS_FORORDNINGEN -> OPPFYLT_BLANDET_REGELVERK
        OPPFYLT_NASJONALE_REGLER -> OPPFYLT_BLANDET_REGELVERK
        OPPFYLT_BLANDET_REGELVERK -> OPPFYLT_BLANDET_REGELVERK
        OPPFYLT_REGELVERK_IKKE_SATT -> OPPFYLT_BLANDET_REGELVERK
        IKKE_FULLT_VURDERT -> IKKE_FULLT_VURDERT
        IKKE_OPPFYLT -> IKKE_OPPFYLT
    }
    OPPFYLT_REGELVERK_IKKE_SATT -> when (resultat) {
        null -> IKKE_FULLT_VURDERT
        OPPFYLT_EØS_FORORDNINGEN -> OPPFYLT_BLANDET_REGELVERK
        OPPFYLT_NASJONALE_REGLER -> OPPFYLT_BLANDET_REGELVERK
        OPPFYLT_BLANDET_REGELVERK -> OPPFYLT_BLANDET_REGELVERK
        OPPFYLT_REGELVERK_IKKE_SATT -> OPPFYLT_REGELVERK_IKKE_SATT
        IKKE_FULLT_VURDERT -> IKKE_FULLT_VURDERT
        IKKE_OPPFYLT -> IKKE_OPPFYLT
    }
    IKKE_OPPFYLT -> IKKE_OPPFYLT
    IKKE_FULLT_VURDERT -> IKKE_FULLT_VURDERT
}
