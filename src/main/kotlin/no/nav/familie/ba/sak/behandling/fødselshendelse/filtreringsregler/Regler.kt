package no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.common.erFraInneværendeEllerForrigeMåned
import no.nav.familie.ba.sak.common.erFraInneværendeMåned
import no.nav.nare.core.evaluations.Evaluering
import java.time.LocalDate

internal fun morHarGyldigFødselsnummer(fakta: Fakta): Evaluering {
    return when (!erDnummer(fakta.mor.personIdent.ident)) {
        true -> Evaluering.ja("Søker har gyldig fødselsnummer")
        false -> Evaluering.nei("Søker har D-nummer")
    }
}

internal fun barnetHarGyldigFødselsnummer(fakta: Fakta): Evaluering {
    return when (!erDnummer(fakta.barn.personIdent.ident)) {
        true -> Evaluering.ja("Barnet har gyldig fødselsnummer")
        false -> Evaluering.nei("Barnet har D-nummer")
    }
}

internal fun barnetErUnder6mnd(fakta: Fakta): Evaluering {
    return when (LocalDate.now().minusMonths(6).isBefore(fakta.barn.fødselsdato)) {
        true -> Evaluering.ja("Barnet er under 6 måneder")
        false -> Evaluering.nei("Barnet er over 6 måneder")
    }
}

internal fun morErOver18år(fakta: Fakta): Evaluering {
    return when (LocalDate.now().isAfter(fakta.mor.fødselsdato.plusYears(18))) {
        true -> Evaluering.ja("Søker er over 18 år")
        false -> Evaluering.nei("Søker er under 18 år")
    }
}

internal fun merEnn5mndSidenForrigeBarn(fakta: Fakta): Evaluering {
    val barnetsFødselsdato = fakta.barn.fødselsdato
    val listenAvAndreBarnUnder5måneder = fakta.restenAvBarna.filter {
        it.fødselsdato.isAfter(barnetsFødselsdato.minusMonths(5))
    }
    return when (listenAvAndreBarnUnder5måneder.isEmpty()) {
        true -> Evaluering.ja("Mor har ikke barn som er født med mindre enn fem måneders mellomrom fra aktuelt barn.")
        false -> Evaluering.nei("Mor har barn som er født med mindre enn fem måneders mellomrom fra aktuelt barn.")
    }
}

internal fun morLever(fakta: Fakta): Evaluering {
    return when (fakta.morLever) {
        true -> Evaluering.ja("Det er ikke registrert dødsfall på mor")
        false -> Evaluering.nei("Det er registrert dødsfall på mor")
    }
}

internal fun barnetLever(fakta: Fakta): Evaluering {
    return when (fakta.barnetLever) {
        true -> Evaluering.ja("Det er ikke registrert dødsfall på barnet")
        false -> Evaluering.nei("Det er registrert dødsfall på barnet")
    }
}

internal fun morHarIkkeVerge(fakta: Fakta): Evaluering {
    return when (!fakta.morHarVerge) {
        true -> Evaluering.ja("Mor har ikke verge")
        false -> Evaluering.nei("Mor har verge")
    }
}

internal fun barnetsFødselsdatoInnebærerIkkeEtterbetaling(fakta: Fakta): Evaluering {
    val dagIMånedenForDagensDato = LocalDate.now().dayOfMonth
    return when {
        dagIMånedenForDagensDato < 20 && !fakta.barn.fødselsdato.erFraInneværendeEllerForrigeMåned() ->
            Evaluering.nei("Barnets fødseldato vil med dagens dato innebære etterbetaling ved vedtak")
        dagIMånedenForDagensDato >= 20 && !fakta.barn.fødselsdato.erFraInneværendeMåned() ->
            Evaluering.nei("Barnets fødseldato vil med dagens dato innebære etterbetaling ved vedtak")
        else -> Evaluering.ja("Barnets fødseldato vil med dagens dato ikke innebære etterbetaling")
    }
}

private fun erDnummer(personIdent: String): Boolean {
    return personIdent.substring(0, 1).toInt() > 3
}