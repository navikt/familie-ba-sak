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

    val morBorIriket = erMorBosattIRiket(morsSisteBosted)
    val barnUnder18 = erBarnUnder18(barna.map { it.fødselsdato })
    val barnBorMedSøker = erBarnBosattMedSøker(barna.map { it.bostedsadresser.sisteAdresse() }, morsSisteBosted)
    val barnErUgift = erBarnErUgift(barna.map { it.sivilstander.sisteSivilstand() })
    val barnErBosattIRiket = erBarnBosattIRiket((barna.map { it.bostedsadresser.sisteAdresse() }))

    return AutomatiskVilkårsvurdering(
            morBosattIRiket = if (morBorIriket) OppfyllerVilkår.JA else OppfyllerVilkår.NEI,
            barnErUnder18 = if (barnUnder18) OppfyllerVilkår.JA else OppfyllerVilkår.NEI,
            barnBorMedSøker = if (barnBorMedSøker) OppfyllerVilkår.JA else OppfyllerVilkår.NEI,
            barnErUgift = if (barnErUgift) OppfyllerVilkår.JA else OppfyllerVilkår.NEI,
            barnErBosattIRiket = if (barnErBosattIRiket) OppfyllerVilkår.JA else OppfyllerVilkår.NEI,
    )
}

fun erMorBosattIRiket(morsSisteBosted: GrBostedsadresse?): Boolean {
    return ((morsSisteBosted != null && morsSisteBosted.periode == null) ||
            morsSisteBosted?.periode?.erInnenfor(LocalDate.now()) == true)
}

fun erBarnUnder18(barnasFødselsDataoer: List<LocalDate>): Boolean {
    return (barnasFødselsDataoer.none { it.plusYears(18).isBefore(LocalDate.now()) })
}

fun erBarnBosattMedSøker(barnasAdresser: List<GrBostedsadresse?>, morsSisteBosted: GrBostedsadresse?): Boolean {
    return (barnasAdresser.none {
        !(GrBostedsadresse.erSammeAdresse(it, morsSisteBosted) &&
          it?.periode?.erInnenfor(LocalDate.now()) ?: false)
    })
}

fun erBarnErUgift(barnasSivilstand: List<GrSivilstand?>): Boolean {
    return (barnasSivilstand.all {
        (it?.type == SIVILSTAND.UGIFT ||
         it?.type == SIVILSTAND.UOPPGITT)
    })
}

fun erBarnBosattIRiket(barnasAdresser: List<GrBostedsadresse?>): Boolean {
    return (barnasAdresser.all {
        (it != null && it.periode == null) || it?.periode?.erInnenfor(LocalDate.now()) == true
    })
}