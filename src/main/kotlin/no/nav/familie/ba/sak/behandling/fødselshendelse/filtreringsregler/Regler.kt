package no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler.utfall.FiltreringsregelIkkeOppfylt.*
import no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler.utfall.FiltreringsregelOppfylt.*
import no.nav.familie.ba.sak.common.erFraInneværendeEllerForrigeMåned
import no.nav.familie.ba.sak.common.erFraInneværendeMåned
import no.nav.familie.ba.sak.nare.Evaluering

internal fun morErOver18år(fakta: Fakta): Evaluering {
    return when (fakta.dagensDato.isAfter(fakta.mor.fødselsdato.plusYears(18))) {
        true -> Evaluering.ja(MOR_ER_OVER_18_ÅR
        )
        false -> Evaluering.nei(MOR_ER_UNDER_18_ÅR)
    }
}

internal fun merEnn5mndSidenForrigeBarn(fakta: Fakta): Evaluering {
    return when (fakta.barnaFraHendelse.all { barnFraHendelse ->
        fakta.restenAvBarna.all { barnFraHendelse.fødselsdato.isAfter(it.fødselsdato.plusMonths(5)) }
    }) {
        true -> Evaluering.ja(MER_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL)
        false -> Evaluering.nei(MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL)
    }
}

internal fun morLever(fakta: Fakta): Evaluering {
    return when (fakta.morLever) {
        true -> Evaluering.ja(MOR_LEVER)
        false -> Evaluering.nei(MOR_LEVER_IKKE)
    }
}

internal fun barnetLever(fakta: Fakta): Evaluering {
    return when (fakta.barnetLever) {
        true -> Evaluering.ja(BARNET_LEVER)
        false -> Evaluering.nei(BARNET_LEVER_IKKE)
    }
}

internal fun morHarIkkeVerge(fakta: Fakta): Evaluering {
    return when (!fakta.morHarVerge) {
        true -> Evaluering.ja(MOR_ER_MYNDIG)
        false -> Evaluering.nei(MOR_ER_UMYNDIG)
    }
}

internal fun barnetsFødselsdatoInnebærerIkkeEtterbetaling(fakta: Fakta): Evaluering {
    val dagIMånedenForDagensDato = fakta.dagensDato.dayOfMonth
    return when {
        dagIMånedenForDagensDato < 21 && !fakta.barnaFraHendelse.all {
            it.fødselsdato.erFraInneværendeEllerForrigeMåned(fakta.dagensDato)
        } -> Evaluering.nei(SAKEN_MEDFØRER_ETTERBETALING)
        dagIMånedenForDagensDato >= 21 && !fakta.barnaFraHendelse.all {
            it.fødselsdato.erFraInneværendeMåned(fakta.dagensDato)
        } -> Evaluering.nei(SAKEN_MEDFØRER_ETTERBETALING)
        else -> Evaluering.ja(SAKEN_MEDFØRER_IKKE_ETTERBETALING)
    }
}
