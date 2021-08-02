package no.nav.familie.ba.sak.kjerne.automatiskvurdering.vilkårsvurdering.utfall

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.EvalueringÅrsak

enum class VilkårKanskjeOppfyltÅrsak(val beskrivelse: String, val vilkår: Vilkår): EvalueringÅrsak {

    // Lovlig opphold
    LOVLIG_OPPHOLD_IKKE_MULIG_Å_FASTSETTE("Kan ikke avgjøre om personen har lovlig opphold.", Vilkår.LOVLIG_OPPHOLD);


    override fun hentBeskrivelse(): String {
        return beskrivelse
    }

    override fun hentMetrikkBeskrivelse(): String {
        return beskrivelse
    }

    override fun hentIdentifikator(): String {
        return vilkår.name
    }
}