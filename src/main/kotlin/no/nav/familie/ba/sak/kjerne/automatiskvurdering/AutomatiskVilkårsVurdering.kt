package no.nav.familie.ba.sak.kjerne.automatiskvurdering

data class AutomatiskVilkårsVurdering(val resultat: Boolean,
                                      val morBosattIRiket: OppfyllerVilkår = OppfyllerVilkår.IKKE_VURDERT,
                                      val barnErUnder18: OppfyllerVilkår = OppfyllerVilkår.IKKE_VURDERT,
                                      val barnBorMedSøker: OppfyllerVilkår = OppfyllerVilkår.IKKE_VURDERT,
                                      val barnErUgift: OppfyllerVilkår = OppfyllerVilkår.IKKE_VURDERT,
                                      val barnErBosattIRiket: OppfyllerVilkår = OppfyllerVilkår.IKKE_VURDERT)

enum class OppfyllerVilkår {
    JA,
    NEI,
    IKKE_VURDERT
}