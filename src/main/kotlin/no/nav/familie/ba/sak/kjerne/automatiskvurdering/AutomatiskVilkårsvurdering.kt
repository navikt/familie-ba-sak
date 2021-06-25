package no.nav.familie.ba.sak.kjerne.automatiskvurdering

data class AutomatiskVilkårsvurdering(val morBosattIRiket: OppfyllerVilkår = OppfyllerVilkår.IKKE_VURDERT,
                                      val barnErUnder18: OppfyllerVilkår = OppfyllerVilkår.IKKE_VURDERT,
                                      val barnBorMedSøker: OppfyllerVilkår = OppfyllerVilkår.IKKE_VURDERT,
                                      val barnErUgift: OppfyllerVilkår = OppfyllerVilkår.IKKE_VURDERT,
                                      val barnErBosattIRiket: OppfyllerVilkår = OppfyllerVilkår.IKKE_VURDERT) {

    fun alleVilkårOppfylt(): Boolean {
        return morBosattIRiket == OppfyllerVilkår.JA &&
               barnErUnder18 == OppfyllerVilkår.JA &&
               barnBorMedSøker == OppfyllerVilkår.JA &&
               barnErUgift == OppfyllerVilkår.JA &&
               barnErBosattIRiket == OppfyllerVilkår.JA
    }
}

enum class OppfyllerVilkår {
    JA,
    NEI,
    IKKE_VURDERT
}