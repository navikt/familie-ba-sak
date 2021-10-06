package no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.utfall

import no.nav.familie.ba.sak.kjerne.fødselshendelse.EvalueringÅrsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

enum class VilkårKanskjeOppfyltÅrsak(val beskrivelse: String, val vilkår: Vilkår) : EvalueringÅrsak {

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
