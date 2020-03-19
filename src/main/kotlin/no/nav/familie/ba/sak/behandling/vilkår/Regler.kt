package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.nare.core.evaluations.Evaluering

internal fun barnUnder18ÅrOgBorMedSøker(fakta: Fakta): Evaluering {
    return if (fakta.alder < 18) //TODO: Sjekk borsammen-flagg når data på plass
        Evaluering.ja("Barn er under 18 år og bor med søker")
    else Evaluering.nei("Barn er ikke under 18 år eller bor ikke med søker")
}

internal fun bosattINorge(fakta: Fakta): Evaluering {
    return if (fakta.personForVurdering.id !== null) //TODO: Implementere når data på plass
        Evaluering.ja("Person bosatt i Norge")
    else Evaluering.nei("Person ikke bosatt i Norge")
}