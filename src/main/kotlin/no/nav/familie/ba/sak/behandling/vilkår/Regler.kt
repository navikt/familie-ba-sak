package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.GrUkjentBosted
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personinfo.SIVILSTAND
import no.nav.nare.core.evaluations.Evaluering

internal fun barnUnder18År(fakta: Fakta): Evaluering {
    return if (fakta.alder < 18)
        Evaluering.ja("Barn er under 18 år")
    else
        Evaluering.nei("Barn er ikke under 18 år")
}

internal fun harEnSøker(fakta: Fakta): Evaluering {
    val barn = fakta.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker
    return if (søker.size == 1)
        Evaluering.ja("Søknad har eksakt en søker")
    else
        Evaluering.nei("Søknad har mer enn en eller ingen søker")
}

internal fun søkerErMor(fakta: Fakta): Evaluering {
    val barn = fakta.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker

    return if (søker.isEmpty())
        Evaluering.nei(("Ingen søker"))
    else if (søker.first().kjønn == Kjønn.KVINNE)
        Evaluering.ja(("Søker er mor"))
    else Evaluering.nei(("Søker er ikke mor"))
}

internal fun barnBorMedSøker(fakta: Fakta): Evaluering {
    val barn = fakta.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker

    return if (søker.isEmpty())
        Evaluering.nei(("Ingen søker"))
    else if (søker.first().bostedsadresse != null &&
             søker.first().bostedsadresse !is GrUkjentBosted &&
             søker.first().bostedsadresse == barn.bostedsadresse)
        Evaluering.ja("Barnet bor med mor")
    else
        Evaluering.nei("Barnet bor ikke med mor")
}

internal fun bosattINorge(fakta: Fakta): Evaluering =
        fakta.personForVurdering.bostedsadresse
                ?.let { Evaluering.ja("Person bosatt i Norge") }
                ?: Evaluering.nei("Person er ikke bosatt i Norge")

internal fun lovligOpphold(fakta: Fakta): Evaluering {
    return if (fakta.personForVurdering.id !== null) //TODO: Implementere når data på plass
        Evaluering.ja("Person har lovlig opphold i Norge")
    else
        Evaluering.nei("Person har lovlig opphold i Norge")
}

internal fun giftEllerPartneskap(fakta: Fakta): Evaluering =
        when (fakta.personForVurdering.sivilstand) {
            SIVILSTAND.GIFT, SIVILSTAND.REGISTRERT_PARTNER, SIVILSTAND.UOPPGITT ->
                Evaluering.nei("Person er gift eller har registrert partner")
            else -> Evaluering.ja("Person er ikke gift eller har registrert partner")
        }

fun Evaluering.toJson(): String = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)