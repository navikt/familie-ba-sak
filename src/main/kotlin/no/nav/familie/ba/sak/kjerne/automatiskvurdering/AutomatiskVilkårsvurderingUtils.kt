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
fun vilkårsvurdering(personopplysningGrunnlag: PersonopplysningGrunnlag,
                     nyeBarnsIdenter: List<String>): AutomatiskVilkårsvurdering {
    //sommerteam antar at hvis mor har en registrert nåværende adresse er hun bosatt i riket
    val mor = personopplysningGrunnlag.søker
    val barna = personopplysningGrunnlag.barna.filter { nyeBarnsIdenter.contains(it.personIdent.ident) }
    val morsSisteBosted = if (mor.bostedsadresser.isEmpty()) null else mor.bostedsadresser.sisteAdresse()
    //Sommerteam hopper over sjekk om mor og barn har lovlig opphold

    val erMorBosattIRiket = erMorBosattIRiket(morsSisteBosted)
    val erBarnUnder18 = barna.map { Pair(it.personIdent.ident, erBarnetUnder18(it.fødselsdato)) }
    val erBarnBosattMedSøker =
            barna.map { Pair(it.personIdent.ident, erBarnetBosattMedSøker(it.bostedsadresser.sisteAdresse(), morsSisteBosted)) }
    val erBarnUgift = barna.map { Pair(it.personIdent.ident, erBarnetUgift(it.sivilstander.sisteSivilstand())) }
    val erBarnBosattIRiket = barna.map { Pair(it.personIdent.ident, erBarnBosattIRiket(it.bostedsadresser.sisteAdresse())) }

    return AutomatiskVilkårsvurdering(
            morBosattIRiket = if (erMorBosattIRiket) OppfyllerVilkår.JA else OppfyllerVilkår.NEI,
            barnErUnder18 = erBarnUnder18.map {
                Pair(it.first, (if (it.second) OppfyllerVilkår.JA else OppfyllerVilkår.NEI))
            },
            barnBorMedSøker = erBarnBosattMedSøker.map {
                Pair(it.first, (if (it.second) OppfyllerVilkår.JA else OppfyllerVilkår.NEI))
            },
            barnErUgift = erBarnUgift.map {
                Pair(it.first, (if (it.second) OppfyllerVilkår.JA else OppfyllerVilkår.NEI))
            },
            barnErBosattIRiket = erBarnBosattIRiket.map {
                Pair(it.first, (if (it.second) OppfyllerVilkår.JA else OppfyllerVilkår.NEI))
            }
    )
}

fun erMorBosattIRiket(morsSisteBosted: GrBostedsadresse?): Boolean {
    return ((morsSisteBosted != null && morsSisteBosted.periode == null) ||
            morsSisteBosted?.periode?.erInnenfor(LocalDate.now()) == true)
}

fun erBarnetUnder18(barnetsFødselsDataoer: LocalDate): Boolean {
    return barnetsFødselsDataoer.plusYears(18).isAfter(LocalDate.now())
}

fun erBarnetBosattMedSøker(barnetsAdresser: GrBostedsadresse?, morsSisteBosted: GrBostedsadresse?): Boolean {
    return GrBostedsadresse.erSammeAdresse(barnetsAdresser, morsSisteBosted) &&
           barnetsAdresser?.periode?.erInnenfor(LocalDate.now()) ?: false
}

fun erBarnetUgift(barnetsSivilstand: GrSivilstand?): Boolean {
    return (barnetsSivilstand?.type == SIVILSTAND.UGIFT || barnetsSivilstand?.type == SIVILSTAND.UOPPGITT)
}

fun erBarnBosattIRiket(barnetsAdresser: GrBostedsadresse?): Boolean {
    return (barnetsAdresser != null && barnetsAdresser.periode == null) ||
           barnetsAdresser?.periode?.erInnenfor(LocalDate.now()) == true
}