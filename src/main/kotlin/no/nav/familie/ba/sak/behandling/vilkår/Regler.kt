package no.nav.familie.ba.sak.behandling.vilkår

import io.micrometer.core.instrument.Counter
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.GrUkjentBosted
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personinfo.SIVILSTAND
import no.nav.nare.core.evaluations.Evaluering
import io.micrometer.core.instrument.Metrics

internal fun barnUnder18År(fakta: Fakta): Evaluering =
    if (fakta.alder < 18)
        evalueringJa("Barn er under 18 år", Vilkår.UNDER_18_ÅR)
    else
        evalueringNei("Barn er ikke under 18 år", Vilkår.UNDER_18_ÅR)

internal fun harEnSøker(fakta: Fakta): Evaluering {
    val subVilkårEnSøker = Vilkår.BOR_MED_SØKER.spesifikasjon.children
            .first { it.beskrivelse.contains("Har eksakt en søker") }

    val barn = fakta.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker

    return if (søker.size == 1)
        evalueringJa("Søknad har eksakt en søker", Vilkår.BOR_MED_SØKER.name, subVilkårEnSøker.beskrivelse)
    else
        evalueringNei("Søknad har mer enn en eller ingen søker", Vilkår.BOR_MED_SØKER.name, subVilkårEnSøker.beskrivelse)
}

internal fun søkerErMor(fakta: Fakta): Evaluering {
    val subVilkårErMor = Vilkår.BOR_MED_SØKER.spesifikasjon.children
            .first { it.beskrivelse.contains("søker må være mor") }

    val barn = fakta.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker

    return if (søker.isEmpty())
        Evaluering.nei(("Ingen søker"))
    else if (søker.first().kjønn == Kjønn.KVINNE)
        evalueringJa("Søker er mor", Vilkår.BOR_MED_SØKER.name, subVilkårErMor.beskrivelse)
    else
        evalueringNei("Søker er ikke mor", Vilkår.BOR_MED_SØKER.name, subVilkårErMor.beskrivelse)

}

internal fun barnBorMedSøker(fakta: Fakta): Evaluering {
    val barn = fakta.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker

    return if (søker.isEmpty())
        Evaluering.nei(("Ingen søker"))
    else if (søker.first().bostedsadresse != null &&
             søker.first().bostedsadresse !is GrUkjentBosted &&
             søker.first().bostedsadresse == barn.bostedsadresse)
        evalueringJa("Barnet bor med mor", Vilkår.BOR_MED_SØKER)
    else
        evalueringNei("Barnet bor ikke med mor", Vilkår.BOR_MED_SØKER)
}

internal fun bosattINorge(fakta: Fakta): Evaluering {
    return if (fakta.personForVurdering.id !== null) //TODO: Implementere når data på plass
        Evaluering.ja("Person bosatt i Norge")
    else Evaluering.nei("Person ikke bosatt i Norge")
}

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

fun evalueringJa(begrunnelse: String, vilkår: String, beskrivelse: String): Evaluering {
    hentMetricCounter("suksess", vilkår, beskrivelse).increment()
    return Evaluering.ja(begrunnelse)
}

fun evalueringJa(begrunnelse: String, vilkår: Vilkår): Evaluering {
    hentMetricCounter("suksess", vilkår.name, vilkår.spesifikasjon.beskrivelse).increment()
    return Evaluering.ja(begrunnelse)
}

fun evalueringNei(begrunnelse: String, vilkår: String, beskrivelse: String): Evaluering {
    hentMetricCounter("feil", vilkår, beskrivelse).increment()
    return Evaluering.nei(begrunnelse)
}

fun evalueringNei(begrunnelse: String, vilkår: Vilkår): Evaluering {
    hentMetricCounter("feil", vilkår.name, vilkår.spesifikasjon.beskrivelse).increment()
    return Evaluering.nei(begrunnelse)
}

private fun hentMetricCounter(type: String, vilkår: String, beskrivelse: String) : Counter =
    Metrics.counter("behandling.vilkår.$type",
                    "vilkår",
                    vilkår,
                    "beskrivelse",
                    beskrivelse)

fun Evaluering.toJson(): String = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)