package no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.common.erFraInneværendeEllerForrigeMåned
import no.nav.familie.ba.sak.common.erFraInneværendeMåned
import no.nav.nare.core.evaluations.Evaluering

internal fun morErOver18år(fakta: Fakta): Evaluering {
    return when (fakta.dagensDato.isAfter(fakta.mor.fødselsdato.plusYears(18))) {
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
    val dagIMånedenForDagensDato = fakta.dagensDato.dayOfMonth
    return when {
        dagIMånedenForDagensDato < 21 && !fakta.barnaFraHendelse.any { it.fødselsdato.erFraInneværendeEllerForrigeMåned() } ->
            Evaluering.nei("Saken medfører etterbetaling.")
        dagIMånedenForDagensDato >= 21 && !fakta.barnaFraHendelse.any { it.fødselsdato.erFraInneværendeMåned() } ->
            Evaluering.nei("Saken medfører etterbetaling.")
        else -> Evaluering.ja("Saken medfører ikke etterbetaling.")
    }
}
