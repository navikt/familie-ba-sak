package no.nav.familie.ba.sak.behandling.filtreringsregler

import no.nav.nare.core.evaluations.Evaluering
import java.time.LocalDate

internal fun søkerHarGyldigFødselsnummer(fakta: Fakta): Evaluering {
    return when (!erDnummer(fakta.søker.personIdent.ident)) {
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

internal fun søkerErOver18år(fakta: Fakta): Evaluering {
    return when (LocalDate.now().isAfter(fakta.søker.fødselsdato.plusYears(18))) {
        true -> Evaluering.ja("Søker er over 18 år")
        false -> Evaluering.nei("Søker er under 18 år")
    }
}


private fun erDnummer(personIdent: String): Boolean {
    return personIdent.substring(0, 1).toInt() > 3
}