package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.common.erInnenfor
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse.Companion.sisteAdresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand.Companion.sisteSivilstand
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import java.time.LocalDate

//sommmerteam har laget for å vurdere saken automatisk basert på vilkår.
fun vilkårsvurdering(personopplysningGrunnlag: PersonopplysningGrunnlag): AutomatiskVilkårsvurdering {
    //sommerteam antar at hvis mor har en registrert nåværende adresse er hun bosatt i riket
    val mor = personopplysningGrunnlag.søker
    val barna = personopplysningGrunnlag.barna
    val morsSisteBosted = if (mor.bostedsadresser.isEmpty()) null else mor.bostedsadresser.sisteAdresse()
    //Sommerteam hopper over sjekk om mor og barn har lovlig opphold

    val morBorIriket = morBorIriket(morsSisteBosted)
    val barnUnder18 = barnUnder18(barna.map { it.fødselsdato })
    val barnBorMedSøker = barnBorMedSøker(barna.map { it.bostedsadresser.sisteAdresse() }, morsSisteBosted)
    val barnErUgift = barnErUgift(barna.map { it.sivilstander.sisteSivilstand() })
    val barnErBosattIRiket = barnErBosattIRiket((barna.map { it.bostedsadresser.sisteAdresse() }))

    return AutomatiskVilkårsvurdering(
            morBosattIRiket = if (morBorIriket) OppfyllerVilkår.JA else OppfyllerVilkår.NEI,
            barnErUnder18 = if (barnUnder18) OppfyllerVilkår.JA else OppfyllerVilkår.NEI,
            barnBorMedSøker = if (barnBorMedSøker) OppfyllerVilkår.JA else OppfyllerVilkår.NEI,
            barnErUgift = if (barnErUgift) OppfyllerVilkår.JA else OppfyllerVilkår.NEI,
            barnErBosattIRiket = if (barnErBosattIRiket) OppfyllerVilkår.JA else OppfyllerVilkår.NEI,
    )
}

private fun morBorIriket(morsSisteBosted: GrBostedsadresse?): Boolean {
    return !(morsSisteBosted == null || morsSisteBosted.periode?.erInnenfor(LocalDate.now()) == true)
}

private fun barnUnder18(barnasFødselsDataoer: List<LocalDate>): Boolean {
    return (barnasFødselsDataoer.none { it.plusYears(18).isBefore(LocalDate.now()) })
}

private fun barnBorMedSøker(barnasAdresser: List<GrBostedsadresse?>, morsSisteBosted: GrBostedsadresse?): Boolean {
    return (barnasAdresser.none { !GrBostedsadresse.erSammeAdresse(it, morsSisteBosted) })
}

private fun barnErUgift(barnasSivilstand: List<GrSivilstand?>): Boolean {
    return !(barnasSivilstand.any {
        !(it?.type != SIVILSTAND.UGIFT ||
          it.type != SIVILSTAND.UOPPGITT)
    })
}

private fun barnErBosattIRiket(barnasAdresser: List<GrBostedsadresse?>): Boolean {
    return !(barnasAdresser.any {
        it?.periode?.erInnenfor(LocalDate.now()) == true
    })
}