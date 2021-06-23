package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.common.erInnenfor
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse.Companion.sisteAdresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand.Companion.sisteSivilstand
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import java.time.LocalDate

object AutomatiskVilkårsVurderingUtils {

    //sommmerteam har laget for å vurdere saken automatisk basert på vilkår.
    fun vilkårsVurdering(personopplysningGrunnlag: PersonopplysningGrunnlag): AutomatiskVilkårsVurdering {
        //mor bosatt i riket
        val mor = personopplysningGrunnlag.søker
        val barna = personopplysningGrunnlag.barna
        val morsSisteBosted = if (mor.bostedsadresser.isEmpty()) null else mor.bostedsadresser.sisteAdresse()
        if (morsSisteBosted == null || morsSisteBosted.periode?.erInnenfor(LocalDate.now()) == true) {
            println("$morsSisteBosted og  ${morsSisteBosted?.periode}")
            return AutomatiskVilkårsVurdering(false, OppfyllerVilkår.NEI)
        }
        //Sommerteam hopper over sjekk om mor og barn har lovlig opphold

        if (barna.any { it.fødselsdato.plusYears(18).isBefore(LocalDate.now()) }) {
            println(barna.fold("") { acc, personInfo -> acc + personInfo + " og " + personInfo.fødselsdato + ", " })
            return AutomatiskVilkårsVurdering(false, OppfyllerVilkår.JA, OppfyllerVilkår.NEI)
        }
        if (barna.any { !GrBostedsadresse.erSammeAdresse(it.bostedsadresser.sisteAdresse(), morsSisteBosted) }) {
            println("mor adresse type: ${mor.bostedsadresser.sisteAdresse()}")
            barna.forEach {
                println("" + mor.bostedsadresser.sisteAdresse() + " og " + it.bostedsadresser.sisteAdresse())
            }
            return AutomatiskVilkårsVurdering(false, OppfyllerVilkår.JA, OppfyllerVilkår.JA, OppfyllerVilkår.NEI)
        }
        if (barna.any {
                    !(it.sivilstander.sisteSivilstand()?.type != SIVILSTAND.UGIFT ||
                      it.sivilstander.sisteSivilstand()?.type != SIVILSTAND.UOPPGITT)
                }) {
            println(barna.fold("") { acc, personInfo -> acc + personInfo.sivilstander.sisteSivilstand() + ", " })

            return AutomatiskVilkårsVurdering(false,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.NEI)
        }
        if (barna.any {
                    it.bostedsadresser.isEmpty() ||
                    it.bostedsadresser.sisteAdresse()?.periode?.erInnenfor(LocalDate.now()) == true
                }) {
            println(barna.fold("") { acc, personInfo -> acc + personInfo.bostedsadresser.last() + ", " })

            return AutomatiskVilkårsVurdering(false,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.NEI)
        }

        return AutomatiskVilkårsVurdering(true,
                                          OppfyllerVilkår.JA,
                                          OppfyllerVilkår.JA,
                                          OppfyllerVilkår.JA,
                                          OppfyllerVilkår.JA,
                                          OppfyllerVilkår.JA)
    }
}