package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Evaluering
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.utfall.VilkårIkkeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.utfall.VilkårOppfyltÅrsak
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND

fun vurderPersonErBosattIRiket(adresse: GrBostedsadresse?): Evaluering {
    /**
     * En person med registrert bostedsadresse er bosatt i Norge.
     * En person som mangler registrert bostedsadresse er utflyttet.
     * See: https://navikt.github.io/pdl/#_utflytting
     */
    return adresse?.let { Evaluering.oppfylt(VilkårOppfyltÅrsak.BOR_I_RIKET) }
           ?: Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.BOR_IKKE_I_RIKET)
}

fun vurderPersonErUnder18(alder: Int): Evaluering =
        if (alder < 18) Evaluering.oppfylt(VilkårOppfyltÅrsak.ER_UNDER_18_ÅR)
        else Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.ER_IKKE_UNDER_18_ÅR)


fun vurderBarnetErBosattMedSøker(søkerAdresse: GrBostedsadresse?, barnAdresse: GrBostedsadresse?): Evaluering =
        when {
            GrBostedsadresse.erSammeAdresse(søkerAdresse, barnAdresse) -> Evaluering.oppfylt(
                    VilkårOppfyltÅrsak.BARNET_BOR_MED_MOR)
            else -> Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.BARNET_BOR_IKKE_MED_MOR)
        }

fun vurderPersonErUgift(sivilstand: GrSivilstand?): Evaluering {
    return when (sivilstand?.type) {
        SIVILSTAND.UOPPGITT ->
            Evaluering.oppfylt(VilkårOppfyltÅrsak.BARN_MANGLER_SIVILSTAND)
        SIVILSTAND.GIFT, SIVILSTAND.REGISTRERT_PARTNER ->
            Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.BARN_ER_GIFT_ELLER_HAR_PARTNERSKAP)
        else -> Evaluering.oppfylt(VilkårOppfyltÅrsak.BARN_ER_IKKE_GIFT_ELLER_HAR_PARTNERSKAP)
    }
}

// Alltid true i sommer-case
fun vurderPersonHarLovligOpphold(): Evaluering =
        Evaluering.oppfylt(VilkårOppfyltÅrsak.NORDISK_STATSBORGER)

