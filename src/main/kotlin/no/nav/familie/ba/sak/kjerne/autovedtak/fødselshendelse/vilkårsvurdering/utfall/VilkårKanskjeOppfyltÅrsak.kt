package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.EvalueringÅrsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

enum class VilkårKanskjeOppfyltÅrsak(
    val beskrivelse: String,
    val vilkår: Vilkår,
) : EvalueringÅrsak {
    // Lovlig opphold
    LOVLIG_OPPHOLD_MÅ_VURDERE_LENGDEN_PÅ_OPPHOLDSTILLATELSEN(
        "Må vurdere lengden på oppholdstillatelsen.",
        Vilkår.LOVLIG_OPPHOLD,
    ),
    LOVLIG_OPPHOLD_IKKE_MULIG_Å_FASTSETTE("Kan ikke avgjøre om personen har lovlig opphold.", Vilkår.LOVLIG_OPPHOLD),
    LOVLIG_OPPHOLD_ANNEN_FORELDER_IKKE_MULIG_Å_FASTSETTE(
        "Kan ikke avgjøre om annen har lovlig opphold.",
        Vilkår.LOVLIG_OPPHOLD,
    ),

    // Bosatt i riket
    BOSATT_I_RIKET_IKKE_MULIG_Å_FASTSETTE_SKAL_BO_LENGRE_ENN_12_MND(
        "Må undersøke i søknaden om personen har planer om å bo i Norge i mer enn 12 måneder.",
        Vilkår.BOSATT_I_RIKET,
    ),
    ;

    override fun hentBeskrivelse(): String = beskrivelse

    override fun hentMetrikkBeskrivelse(): String = beskrivelse

    override fun hentIdentifikator(): String = vilkår.name

    override fun hentNavn(): String = name
}
