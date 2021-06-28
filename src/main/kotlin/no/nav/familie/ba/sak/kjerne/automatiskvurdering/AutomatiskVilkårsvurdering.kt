package no.nav.familie.ba.sak.kjerne.automatiskvurdering

data class AutomatiskVilkårsvurdering(val morBosattIRiket: OppfyllerVilkår = OppfyllerVilkår.IKKE_VURDERT,
                                      val barnErUnder18: List<Pair<String, OppfyllerVilkår>> =
                                              listOf(Pair("", OppfyllerVilkår.IKKE_VURDERT)),
                                      val barnBorMedSøker: List<Pair<String, OppfyllerVilkår>> =
                                              listOf(Pair("", OppfyllerVilkår.IKKE_VURDERT)),
                                      val barnErUgift: List<Pair<String, OppfyllerVilkår>> =
                                              listOf(Pair("", OppfyllerVilkår.IKKE_VURDERT)),
                                      val barnErBosattIRiket: List<Pair<String, OppfyllerVilkår>> =
                                              listOf(Pair("", OppfyllerVilkår.IKKE_VURDERT))) {

    fun alleVilkårOppfylt(): Boolean {
        return morBosattIRiket == OppfyllerVilkår.JA &&
               barnErUnder18.all { it.second == OppfyllerVilkår.JA } &&
               barnBorMedSøker.all { it.second == OppfyllerVilkår.JA } &&
               barnErUgift.all { it.second == OppfyllerVilkår.JA } &&
               barnErBosattIRiket.all { it.second == OppfyllerVilkår.JA }
    }
}

enum class OppfyllerVilkår {
    JA,
    NEI,
    IKKE_VURDERT
}