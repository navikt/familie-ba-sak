package no.nav.familie.ba.sak.behandling.vilkår

import io.micrometer.core.instrument.Counter
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.GrUkjentBosted
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personinfo.SIVILSTAND
import no.nav.nare.core.evaluations.Evaluering
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import java.time.LocalDate

internal fun barnUnder18År(fakta: Fakta): Evaluering =
        if (fakta.alder < 18)
            evalueringJa("Barn er under 18 år", Vilkår.UNDER_18_ÅR, fakta.personForVurdering.type)
        else
            evalueringNei("Barn er ikke under 18 år", Vilkår.UNDER_18_ÅR, fakta.personForVurdering.type)

internal fun harEnSøker(fakta: Fakta): Evaluering {
    val barn = fakta.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker
    val metricBeskrivelse = barn.type.name.plus("Har eksakt en søker")

    return if (søker.size == 1)
        evalueringJa("Søknad har eksakt en søker", Vilkår.BOR_MED_SØKER, metricBeskrivelse)
    else
        evalueringNei("Søknad har mer enn en eller ingen søker", Vilkår.BOR_MED_SØKER, metricBeskrivelse)
}

internal fun søkerErMor(fakta: Fakta): Evaluering {
    val barn = fakta.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker
    val metricBeskrivelse = barn.type.name.plus("søker må være mor")

    return when {
        søker.isEmpty() -> evalueringNei("Ingen søker", Vilkår.BOR_MED_SØKER, metricBeskrivelse)
        søker.first().kjønn == Kjønn.KVINNE -> evalueringJa("Søker er mor", Vilkår.BOR_MED_SØKER, metricBeskrivelse)
        else -> evalueringNei("Søker er ikke mor", Vilkår.BOR_MED_SØKER, barn.type)
    }
}

internal fun barnBorMedSøker(fakta: Fakta): Evaluering {
    val barn = fakta.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker

    return if (søker.isEmpty())
        Evaluering.nei(("Ingen søker"))
    else if (søker.first().bostedsadresse != null &&
             søker.first().bostedsadresse !is GrUkjentBosted &&
             søker.first().bostedsadresse == barn.bostedsadresse)
        evalueringJa("Barnet bor med mor", Vilkår.BOR_MED_SØKER, barn.type)
    else
        evalueringNei("Barnet bor ikke med mor", Vilkår.BOR_MED_SØKER, barn.type)
}

internal fun bosattINorge(fakta: Fakta): Evaluering =
        // En person med registrert bostedsadresse er bosatt i Norge.
        // En person som mangler registrert bostedsadresse er utflyttet.
        // See: https://navikt.github.io/pdl/#_utflytting
        fakta.personForVurdering.bostedsadresse
                ?.let { evalueringJa("Person bosatt i Norge", Vilkår.BOSATT_I_RIKET, fakta.personForVurdering.type) }
                ?: evalueringNei("Person er ikke bosatt i Norge", Vilkår.BOSATT_I_RIKET, fakta.personForVurdering.type)

internal fun lovligOpphold(fakta: Fakta): Evaluering {
    if (fakta.personForVurdering.type == PersonType.BARN) {
        Evaluering.kanskje("Ikke separat oppholdsvurdering for barnet ved automatisk vedtak.")
    }

    return with(finnNåværendeMedlemskap(fakta)) {
        when {
            contains(Medlemskap.NORDEN) -> evalueringJa("Er nordisk statsborger.",
                                                        Vilkår.LOVLIG_OPPHOLD,
                                                        PersonType.SØKER)
            contains(Medlemskap.EØS) -> Evaluering.kanskje("Er EØS borger.")
            contains(Medlemskap.TREDJELANDSBORGER) -> Evaluering.kanskje("Tredjelandsborger med lovlig opphold.")
            else -> Evaluering.kanskje("Person har lovlig opphold.")
        }
    }
}

internal fun giftEllerPartnerskap(fakta: Fakta): Evaluering =
        when (fakta.personForVurdering.sivilstand) {
            SIVILSTAND.GIFT, SIVILSTAND.REGISTRERT_PARTNER, SIVILSTAND.UOPPGITT ->
                evalueringNei("Person er gift eller har registrert partner", Vilkår.GIFT_PARTNERSKAP, fakta.personForVurdering.type)
            else -> evalueringJa("Person er ikke gift eller har registrert partner", Vilkår.GIFT_PARTNERSKAP, fakta.personForVurdering.type)
        }

fun finnNåværendeMedlemskap(fakta: Fakta): List<Medlemskap> =
        fakta.personForVurdering.statsborgerskap?.filter {
            it.gyldigPeriode?.fom?.isBefore(LocalDate.now()) ?: true &&
            it.gyldigPeriode?.tom?.isAfter(LocalDate.now()) ?: true
        }
                ?.map { it.medlemskap } ?: error("Person har ikke noe statsborgerskap.")

private fun evalueringJa(begrunnelse: String, vilkår: Vilkår, beskrivelse: String): Evaluering {
    hentMetricCounter("suksess", vilkår.name, beskrivelse).increment()
    return Evaluering.ja(begrunnelse)
}

private fun evalueringJa(begrunnelse: String, vilkår: Vilkår, personType: PersonType): Evaluering {
    val beskrivelse = personType.name.plus(vilkår.spesifikasjon.beskrivelse)

    hentMetricCounter("suksess", vilkår.name, beskrivelse).increment()
    return Evaluering.ja(begrunnelse)
}

private fun evalueringNei(begrunnelse: String, vilkår: Vilkår, beskrivelse: String): Evaluering {
    hentMetricCounter("feil", vilkår.name, beskrivelse).increment()
    return Evaluering.nei(begrunnelse)
}

private fun evalueringNei(begrunnelse: String, vilkår: Vilkår, personType: PersonType): Evaluering {
    val beskrivelse = personType.name.plus(vilkår.spesifikasjon.beskrivelse)

    hentMetricCounter("feil", vilkår.name, beskrivelse).increment()
    return Evaluering.nei(begrunnelse)
}

private fun hentMetricCounter(type: String, vilkår: String, beskrivelse: String): Counter =
        Metrics.counter("behandling.vilkår.$type",
                        "vilkår",
                        vilkår,
                        "beskrivelse",
                        beskrivelse)

fun Evaluering.toJson(): String = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)