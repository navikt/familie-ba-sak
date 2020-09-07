package no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler

import no.nav.nare.core.evaluations.Evaluering
import java.time.LocalDate

internal fun morHarGyldigFødselsnummer(fakta: Fakta): Evaluering {
    return when (!erDnummer(fakta.mor.personIdent.ident)) {
        true -> Evaluering.ja("Mor har gyldig fødselsnummer.")
        false -> Evaluering.nei("Mor har ikke gyldig fødselsnummer.")
    }
}

internal fun barnetHarGyldigFødselsnummer(fakta: Fakta): Evaluering {

    return when(!fakta.barn.any{ !erDnummer(it.personIdent.ident) }){
        true -> Evaluering.ja("Alle barn har gyldig fødselsnummer.")
        false -> Evaluering.nei("Minst et barn har ikke gyldig fødselsnummer.")
    }
}

internal fun barnetErUnder6mnd(fakta: Fakta): Evaluering {
    return when (!fakta.barn.any{ (!LocalDate.now().minusMonths(6).isBefore(it.fødselsdato)) }) {
        true -> Evaluering.ja("Alle barn er under 6 måneder.")
        false -> Evaluering.nei("Minst et barn er over 6 måneder.")
    }
}

internal fun morErOver18år(fakta: Fakta): Evaluering {
    return when (LocalDate.now().isAfter(fakta.mor.fødselsdato.plusYears(18))) {
        true -> Evaluering.ja("Mor er over 18 år.")
        false -> Evaluering.nei("Mor er under 18 år.")
    }
}

internal fun merEnn5mndSidenForrigeBarn(fakta: Fakta): Evaluering {
    val noenUnderFemMåneder  = fakta.barn.any {
        val barnetsFødselsdato = it.fødselsdato
        val listenAvAndreBarnUnder5måneder = fakta.restenAvBarna.filter {
            it.fødselsdato.isAfter(barnetsFødselsdato.minusMonths(5))
        }
        listenAvAndreBarnUnder5måneder.isEmpty()
    }

    return when (!noenUnderFemMåneder) {
        true -> Evaluering.ja("Det har gått mer enn fem måneder siden forrige barn ble født.")
        false -> Evaluering.nei("Det har gått mindre enn fem måneder siden forrige barn ble født.")
    }
}

internal fun morLever(fakta: Fakta): Evaluering {
    return when (fakta.morLever) {
        true -> Evaluering.ja("Det er ikke registrert dødsdato på mor.")
        false -> Evaluering.nei("Det er registrert dødsdato på mor.")
    }
}

internal fun barnetLever(fakta: Fakta): Evaluering {
    return when (fakta.barnetLever) {
        true -> Evaluering.ja("Det er ikke registrert dødsdato på barnet")
        false -> Evaluering.nei("Det er registrert dødsdato på barnet")
    }
}

internal fun morHarIkkeVerge(fakta: Fakta): Evaluering {
    return when (!fakta.morHarVerge) {
        true -> Evaluering.ja("Mor er myndig.")
        false -> Evaluering.nei("Mor er umyndig.")
    }
}

private fun erDnummer(personIdent: String): Boolean {
    return personIdent.substring(0, 1).toInt() > 3
}