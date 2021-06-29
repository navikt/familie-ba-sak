package no.nav.familie.ba.sak.kjerne.automatiskvurdering

data class Vilkårsresultat(val type: VilkårType, val resultat: VilkårsVurdering)

enum class VilkårType {
    MOR_ER_BOSTATT_I_RIKET,
    BARN_ER_UNDER_18,
    BARN_BOR_MED_SØKER,
    BARN_ER_UGIFT,
    BARN_ER_BOSATT_I_RIKET
}

enum class VilkårsVurdering {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT
}

