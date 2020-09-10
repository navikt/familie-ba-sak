package no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.common.erFraInneværendeEllerForrigeMåned
import no.nav.familie.ba.sak.common.erFraInneværendeMåned
import no.nav.nare.core.evaluations.Evaluering
import java.time.LocalDate

internal fun morHarGyldigFødselsnummer(fakta: Fakta): Evaluering {
    return when (!erDnummer(fakta.mor.personIdent.ident)) {
        true -> Evaluering.ja("Mor har gyldig fødselsnummer.")
        false -> Evaluering.nei("Mor har ikke gyldig fødselsnummer.")
    }
}

internal fun barnetHarGyldigFødselsnummer(fakta: Fakta): Evaluering {
    return when (fakta.barnaFraHendelse.none { erDnummer(it.personIdent.ident) }) {
        true -> Evaluering.ja("Alle barn har gyldig fødselsnummer.")
        false -> Evaluering.nei("Minst et barn har ikke gyldig fødselsnummer.")
    }
}

internal fun barnetErUnder6mnd(fakta: Fakta): Evaluering {
    return when (fakta.barnaFraHendelse.none { it.fødselsdato.isBefore(LocalDate.now().minusMonths(6)) }) {
        true -> Evaluering.ja("Alle barna er under 6 måneder.")
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
    return when (fakta.barnaFraHendelse.all { barnFraHendelse ->
        fakta.restenAvBarna.all { barnFraHendelse.fødselsdato.isAfter(it.fødselsdato.plusMonths(5)) }
    }) {
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
        true -> Evaluering.ja("Det er ikke registrert dødsdato på barnet.")
        false -> Evaluering.nei("Det er registrert dødsdato på barnet.")
    }
}

internal fun morHarIkkeVerge(fakta: Fakta): Evaluering {
    return when (!fakta.morHarVerge) {
        true -> Evaluering.ja("Mor er myndig.")
        false -> Evaluering.nei("Mor er umyndig.")
    }
}

internal fun barnetsFødselsdatoInnebærerIkkeEtterbetaling(fakta: Fakta): Evaluering {
    val dagIMånedenForDagensDato = LocalDate.now().dayOfMonth
    return when {
        dagIMånedenForDagensDato < 21 && !fakta.barnaFraHendelse.any { it.fødselsdato.erFraInneværendeEllerForrigeMåned() } ->
            Evaluering.nei("Saken medfører etterbetaling.")
        dagIMånedenForDagensDato >= 21 && !fakta.barnaFraHendelse.any { it.fødselsdato.erFraInneværendeMåned() } ->
            Evaluering.nei("Saken medfører etterbetaling.")
        else -> Evaluering.ja("Saken medfører ikke etterbetaling.")
    }
}

private fun erDnummer(personIdent: String): Boolean {
    return personIdent.substring(0, 1).toInt() > 3
}
