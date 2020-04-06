package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.nare.core.evaluations.Evaluering

internal fun barnUnder18År(fakta: Fakta): Evaluering {
    return if (fakta.alder < 18)
        Evaluering.ja("Barn er under 18 år")
    else Evaluering.nei("Barn er ikke under 18 år")
}

internal fun barnBorMedSøker(fakta: Fakta): Evaluering {
    return if (fakta.personForVurdering.id !== null) //TODO: Implementere når data på plass
        Evaluering.ja("Barnet bor med søker")
    else Evaluering.nei("Barnet bor ikke med søker")
}

internal fun bosattINorge(fakta: Fakta): Evaluering {
    return if (fakta.personForVurdering.id !== null) //TODO: Implementere når data på plass
        Evaluering.ja("Person bosatt i Norge")
    else Evaluering.nei("Person ikke bosatt i Norge")
}

internal fun lovligOpphold(fakta: Fakta): Evaluering {
    return if (fakta.personForVurdering.id !== null) //TODO: Implementere når data på plass
        Evaluering.ja("Person har lovlig opphold i Norge")
    else Evaluering.nei("Person har lovlig opphold i Norge")
}

internal fun giftEllerPartneskap(fakta: Fakta): Evaluering {
    return if (fakta.personForVurdering.id !== null) //TODO: Implementere når data på plass
        Evaluering.ja("Person har lovlig opphold i Norge")
    else Evaluering.nei("Person har lovlig opphold i Norge")
}