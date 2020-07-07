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
    val beskrivelse = barn.type.name.plus(subVilkårErMor.beskrivelse)

    return if (søker.isEmpty())
        Evaluering.nei(("Ingen søker"))
    else if (søker.first().kjønn == Kjønn.KVINNE)
        evalueringJa("Søker er mor", Vilkår.BOR_MED_SØKER.name, beskrivelse)
    else
        evalueringNei("Søker er ikke mor", Vilkår.BOR_MED_SØKER.name, beskrivelse)
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
                ?.let { Evaluering.ja("Person bosatt i Norge") }
        ?: Evaluering.nei("Person er ikke bosatt i Norge")

internal fun lovligOpphold(fakta: Fakta): Evaluering =
        with(finnNåværendeMedlemskap(fakta)) {
            when {
                contains(Medlemskap.NORDEN) -> evalueringJa("Person er nordisk statsborger.",
                                                            Vilkår.LOVLIG_OPPHOLD,
                                                            fakta.personForVurdering.type)
                contains(Medlemskap.EØS) -> evalueringJa("Person er EØS borger.",
                                                         Vilkår.LOVLIG_OPPHOLD,
                                                         fakta.personForVurdering.type)
                contains(Medlemskap.TREDJELANDSBORGER) -> evalueringJa("Person har lovig opphold.",
                                                                       Vilkår.LOVLIG_OPPHOLD,
                                                                       fakta.personForVurdering.type)
                else -> evalueringJa("Person har lovlig opphold.", Vilkår.LOVLIG_OPPHOLD, fakta.personForVurdering.type)
            }
        }


internal fun giftEllerPartneskap(fakta: Fakta): Evaluering =
        when (fakta.personForVurdering.sivilstand) {
            SIVILSTAND.GIFT, SIVILSTAND.REGISTRERT_PARTNER, SIVILSTAND.UOPPGITT ->
                Evaluering.nei("Person er gift eller har registrert partner")
            else -> Evaluering.ja("Person er ikke gift eller har registrert partner")
        }

fun finnNåværendeMedlemskap(fakta: Fakta): List<Medlemskap> =
        fakta.personForVurdering.statsborgerskap?.filter {
            it.gyldigPeriode?.fom?.isBefore(LocalDate.now()) ?: true &&
            it.gyldigPeriode?.tom?.isAfter(LocalDate.now()) ?: true
        }
                ?.map { it.medlemskap } ?: error("Person har ikke noe statsborgerskap.")

private fun evalueringJa(begrunnelse: String, vilkår: String, beskrivelse: String): Evaluering {
    hentMetricCounter("suksess", vilkår, beskrivelse).increment()
    return Evaluering.ja(begrunnelse)
}

private fun evalueringJa(begrunnelse: String, vilkår: Vilkår, personType: PersonType): Evaluering {
    val beskrivelse = personType.name.plus(vilkår.spesifikasjon.beskrivelse)

    hentMetricCounter("suksess", vilkår.name, beskrivelse).increment()
    return Evaluering.ja(begrunnelse)
}

private fun evalueringNei(begrunnelse: String, vilkår: String, beskrivelse: String): Evaluering {
    hentMetricCounter("feil", vilkår, beskrivelse).increment()
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

