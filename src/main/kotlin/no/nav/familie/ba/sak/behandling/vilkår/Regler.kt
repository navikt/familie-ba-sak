package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.GrUkjentBosted
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingMetrics.Companion.økTellerForLovligOpphold
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.nare.core.evaluations.Evaluering
import java.time.LocalDate

internal fun barnUnder18År(fakta: Fakta): Evaluering =
        if (fakta.alder < 18)
            Evaluering.ja("Barn er under 18 år")
        else
            Evaluering.nei("Barn er ikke under 18 år")

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

    return when {
        søker.isEmpty() -> Evaluering.nei("Ingen søker")
        søker.first().kjønn == Kjønn.KVINNE -> Evaluering.ja("Søker er mor")
        else -> Evaluering.nei("Søker er ikke mor")
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
        Evaluering.ja("Barnet bor med mor")
    else
        Evaluering.nei("Barnet bor ikke med mor")
}

internal fun bosattINorge(fakta: Fakta): Evaluering =
// En person med registrert bostedsadresse er bosatt i Norge.
// En person som mangler registrert bostedsadresse er utflyttet.
        // See: https://navikt.github.io/pdl/#_utflytting
        fakta.personForVurdering.bostedsadresse
                ?.let { Evaluering.ja("Person bosatt i Norge") }
        ?: Evaluering.nei("Person er ikke bosatt i Norge")

internal fun lovligOpphold(fakta: Fakta): Evaluering {
    if (fakta.behandlingOpprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE &&
        fakta.personForVurdering.type == PersonType.BARN) {
        return Evaluering.ja("Ikke separat oppholdsvurdering for barnet ved automatisk vedtak.")
    }

    val nåværendeMedlemskap = finnNåværendeMedlemskap(fakta.personForVurdering.statsborgerskap)

    return when (finnSterkesteMedlemskap(nåværendeMedlemskap)) {
            Medlemskap.NORDEN -> Evaluering.ja("Er nordisk statsborger.")
            //TODO: Implementeres av TEA-1532
            Medlemskap.EØS -> {
                sjekkLovligOppholdForEØSBorger(fakta)
            }
            Medlemskap.TREDJELANDSBORGER -> {
                val nåværendeOpphold = fakta.personForVurdering.opphold?.singleOrNull { it.gjeldendeNå() }
                if (nåværendeOpphold == null || nåværendeOpphold.type == OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER) {
                    økTellerForLovligOpphold(LovligOppholdAvslagÅrsaker.TREDJELANDSBORGER, fakta.personForVurdering.type)
                    Evaluering.nei("${fakta.personForVurdering.type} har ikke lovlig opphold")
                } else Evaluering.ja("Er tredjelandsborger med lovlig opphold")
            }
            Medlemskap.UKJENT, Medlemskap.STATSLØS -> {
                val nåværendeOpphold = fakta.personForVurdering.opphold?.singleOrNull { it.gjeldendeNå() }
                if (nåværendeOpphold == null || nåværendeOpphold.type == OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER){
                    økTellerForLovligOpphold(LovligOppholdAvslagÅrsaker.STATSLØS, fakta.personForVurdering.type)
                    Evaluering.nei("${fakta.personForVurdering.type} er statsløs eller mangler statsborgerskap, og har ikke lovlig opphold")
                }
                else Evaluering.ja("Er statsløs eller mangler statsborgerskap med lovlig opphold")
            }
            else -> Evaluering.kanskje("Kan ikke avgjøre om personen har lovlig opphold.")
    }
}

internal fun giftEllerPartnerskap(fakta: Fakta): Evaluering =
        when (fakta.personForVurdering.sivilstand) {
            SIVILSTAND.UOPPGITT ->
                if (fakta.behandlingOpprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE)
                    Evaluering.ja("Person mangler informasjon om sivilstand.")
                else
                    Evaluering.kanskje("Person mangler informasjon om sivilstand.")
            SIVILSTAND.GIFT, SIVILSTAND.REGISTRERT_PARTNER ->
                Evaluering.nei("Person er gift eller har registrert partner")
            else -> Evaluering.ja("Person er ikke gift eller har registrert partner")
        }

fun finnNåværendeMedlemskap(statsborgerskap: List<GrStatsborgerskap>?): List<Medlemskap> =
        statsborgerskap?.filter {
            it.gyldigPeriode?.fom?.isBefore(LocalDate.now()) ?: true &&
            it.gyldigPeriode?.tom?.isAfter(LocalDate.now()) ?: true
        }
                ?.map { it.medlemskap } ?: emptyList()

fun finnSterkesteMedlemskap(medlemskap: List<Medlemskap>): Medlemskap? {
    return with(medlemskap) {
        when {
            contains(Medlemskap.NORDEN) -> Medlemskap.NORDEN
            contains(Medlemskap.EØS) -> Medlemskap.EØS
            contains(Medlemskap.TREDJELANDSBORGER) -> Medlemskap.TREDJELANDSBORGER
            contains(Medlemskap.STATSLØS) -> Medlemskap.STATSLØS
            contains(Medlemskap.UKJENT) -> Medlemskap.UKJENT
            else -> null
        }
    }
}


fun Evaluering.toJson(): String = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

private fun sjekkLovligOppholdForEØSBorger(fakta: Fakta): Evaluering {
    return if (personHarLøpendeArbeidsforhold(fakta.personForVurdering)) {
        Evaluering.ja("Mor er EØS-borger og har et løpende arbeidsforhold i Norge.")
    } else {
        if (annenForelderEksistererOgBorMedMor(fakta)) {
            with(statsborgerskapAnnenForelder(fakta)) {
                when {
                    contains(Medlemskap.NORDEN) -> Evaluering.ja("Annen forelder er norsk eller nordisk statsborger.")
                    contains(Medlemskap.EØS) -> {
                        if (personHarLøpendeArbeidsforhold(hentAnnenForelder(fakta).first())) {
                            Evaluering.ja("Annen forelder er fra EØS og har et løpende arbeidsforhold i Norge.")
                        } else {
                            sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(fakta)
                        }
                    }
                    contains(Medlemskap.TREDJELANDSBORGER) -> Evaluering.nei("Annen forelder er tredjelandsborger.")
                    contains(Medlemskap.UKJENT) -> Evaluering.nei("Annen forelder er uten statsborgerskap.")
                    else -> {
                        Evaluering.nei("Statsborgerskap for annen forelder kan ikke avgjøres.")
                    }
                }
            }
        } else {
            sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(fakta)
        }
    }
}

private fun sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(fakta: Fakta): Evaluering {
    // Regelflytramme. Utkommentert pga. at SonarCube flagger dette som en bug. Rammen skal benyttes når reglene er implementert.
    /*if (morHarBoddINorgeIMerEnn5År()) {
        if (morHarJobbetINorgeSiste5År()) {
            // Evaluering.ja("Mor har bodd i Norge i mer enn 5 år og jobbet i Norge siste 5 år.")
        } else {
            // Evaluering.nei("Mor har ikke jobbet kontinuerlig i Norge siste 5 år.")
        }
    } else {
        // Evaluering.nei("Mor har ikke bodd i Norge sammenhengende i mer enn 5 år.")
    }*/
    return Evaluering.kanskje("ikke implementert")
}

fun personHarLøpendeArbeidsforhold(personForVurdering: Person): Boolean = personForVurdering.arbeidsforhold?.any {
    it.periode?.tom == null || it.periode.tom >= LocalDate.now()
} ?: false

fun annenForelderEksistererOgBorMedMor(fakta: Fakta): Boolean {
    val annenForelder = hentAnnenForelder(fakta).firstOrNull()
    return if (annenForelder == null) {
        false
    } else {
        fakta.personForVurdering.bostedsadresse != null
        && fakta.personForVurdering.bostedsadresse !is GrUkjentBosted
        && fakta.personForVurdering.bostedsadresse == annenForelder.bostedsadresse
    }
}

fun statsborgerskapAnnenForelder(fakta: Fakta): List<Medlemskap> {
    val annenForelder =
            hentAnnenForelder(fakta).first()
    return finnNåværendeMedlemskap(annenForelder.statsborgerskap)
}

private fun hentAnnenForelder(fakta: Fakta) = fakta.personForVurdering.personopplysningGrunnlag.personer.filter {
    it.type == PersonType.ANNENPART
}

fun morHarBoddINorgeIMerEnn5År(): Boolean = true // ikke implementert
fun morHarJobbetINorgeSiste5År(): Boolean = true // ikke implementert