package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.UkjentBostedPdl
import no.nav.nare.core.evaluations.Evaluering

internal fun barnUnder18År(fakta: Fakta): Evaluering {
    return if (fakta.alder < 18)
        Evaluering.ja("Barn er under 18 år")
    else Evaluering.nei("Barn er ikke under 18 år")
}

internal fun barnBorMedMor(fakta: Fakta): Evaluering {
    val barn = fakta.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker

    //According to Anna, only a fagsak with one søker who is a mother can be automatically processed.
    return if (søker.size == 1 &&
               søker.first().kjønn == Kjønn.KVINNE &&
               søker.first().bostedsadresse != null &&
               søker.first().bostedsadresse !is UkjentBostedPdl &&
               søker.first().bostedsadresse == barn.bostedsadresse)
        Evaluering.ja("Barnet bor med mor")
    else Evaluering.nei("Barnet bor ikke med mor")
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