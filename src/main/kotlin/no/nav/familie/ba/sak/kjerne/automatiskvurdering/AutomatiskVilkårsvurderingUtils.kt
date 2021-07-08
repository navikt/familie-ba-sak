package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.erInnenfor
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import java.time.LocalDate

//antar at dersom perioden til sisteAdresse er null, så betyr det at personen fortsatt bor der
fun vurderPersonErBosattIRiket(adresse: GrBostedsadresse?): Resultat {
    if ((adresse != null && adresse.periode == null) ||
        adresse?.periode?.erInnenfor(LocalDate.now()) == true) return Resultat.OPPFYLT
    return Resultat.IKKE_OPPFYLT
}

fun vurderPersonErUnder18(fødselsdato: LocalDate): Resultat {
    if (fødselsdato.plusYears(18).isAfter(LocalDate.now())) return Resultat.OPPFYLT
    return Resultat.IKKE_OPPFYLT
}


fun vurderBarnetErBosattMedSøker(søkerAdresse: GrBostedsadresse?, barnAdresse: GrBostedsadresse?): Resultat {
    if (søkerAdresse == null) throw Feil("Finner ingen adresse på søker ved automatisk vurdering av vilkår BARN_BOR_MED_SØKER")
    if (barnAdresse == null) throw Feil("Finner ingen adresse på barn ved automatisk vurdering av vilkår BARN_BOR_MED_SØKER")
    val sammeSisteAdresse =
            GrBostedsadresse.erSammeAdresse(søkerAdresse, barnAdresse)

    //antar at dersom perioden til sisteAdresse er null, så betyr det at personen fortsatt bor der
    val barnBorPåSisteAdresse = barnAdresse.periode?.erInnenfor(LocalDate.now()) ?: true
    val søkerBorPåSisteAdresse = søkerAdresse.periode?.erInnenfor(LocalDate.now()) ?: true

    if (sammeSisteAdresse && barnBorPåSisteAdresse && søkerBorPåSisteAdresse) return Resultat.OPPFYLT
    return Resultat.IKKE_OPPFYLT
}

fun vurderPersonErUgift(sivilstand: GrSivilstand?): Resultat {
    return when (sivilstand?.type ?: throw Feil("Finner ikke siviltilstand")) {
        SIVILSTAND.GIFT, SIVILSTAND.REGISTRERT_PARTNER -> Resultat.IKKE_OPPFYLT
        else -> Resultat.OPPFYLT
    }
}

fun vurderPersonHarLovligOpphold(): Resultat {
    //alltid true i sommer-case
    return Resultat.OPPFYLT
}

data class under18RegelInput(
        val dagensDato: LocalDate,
        val fødselsdato: LocalDate,
)

data class borMedSøkerRegelInput(
        val søkerAdresse: GrBostedsadresse?,
        val barnAdresse: GrBostedsadresse?,
)

data class giftEllerPartnerskapRegelInput(
        val sivilstand: GrSivilstand?,
)

data class bosattIRiketRegelInput(
        val bostedsadresse: GrBostedsadresse?,
)