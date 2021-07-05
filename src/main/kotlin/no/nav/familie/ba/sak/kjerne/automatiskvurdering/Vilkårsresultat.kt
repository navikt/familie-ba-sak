package no.nav.familie.ba.sak.kjerne.automatiskvurdering

data class Vilkårsresultat(val type: VilkårType, val resultat: VilkårsVurdering)

enum class VilkårType(val beskrivelse: String) {
    BOSTATT_I_RIKET("Fødselshendelse: Mor er ikke bosatt i riket"),
    UNDER_18("Fødselshendelse: Barn over 18 år"),
    BOR_MED_SØKER("Fødselshendelse: Barnet ikke bosatt med mor"),
    GIFT_PARTNERSKAP("Fødselshendelse: Barnet er gift"),
    BOSATT_I_RIKET("Fødselshendelse: Barnet er ikke bosatt i riket")
}

enum class VilkårsVurdering {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT
}