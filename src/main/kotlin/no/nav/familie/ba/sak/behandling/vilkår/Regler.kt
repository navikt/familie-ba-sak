package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.GrUkjentBosted
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
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

    return with(finnNåværendeMedlemskap(fakta)) {
        when {
            contains(Medlemskap.NORDEN) -> Evaluering.ja("Er nordisk statsborger.")
            //TODO: Implementeres av TEA-1532
            contains(Medlemskap.EØS) -> Evaluering.kanskje("Er EØS borger.")
            contains(Medlemskap.TREDJELANDSBORGER) -> {
                val nåværendeOpphold = fakta.personForVurdering.opphold?.singleOrNull { it.gjeldendeNå() }
                if (nåværendeOpphold == null || nåværendeOpphold.type == OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER) {
                    økTellerForLovligOpphold(LovligOppholdAvslagÅrsaker.TREDJELANDSBORGER, fakta.personForVurdering.type)
                    Evaluering.nei("${fakta.personForVurdering.type} har ikke lovlig opphold")
                } else Evaluering.ja("Er tredjelandsborger med lovlig opphold")
            }
            //TODO: Implementeres av TEA-1534
            else -> Evaluering.kanskje("Kan ikke avgjøre om personen har lovlig opphold.")
        }
    }
}

internal fun giftEllerPartnerskap(fakta: Fakta): Evaluering =
        when (fakta.personForVurdering.sivilstand) {
            SIVILSTAND.UOPPGITT ->
                Evaluering.kanskje("Person mangler informasjon om sivilstand.")
            SIVILSTAND.GIFT, SIVILSTAND.REGISTRERT_PARTNER, SIVILSTAND.UOPPGITT ->
                Evaluering.nei("Person er gift eller har registrert partner")
            else -> Evaluering.ja("Person er ikke gift eller har registrert partner")
        }

fun finnNåværendeMedlemskap(fakta: Fakta): List<Medlemskap> =
        fakta.personForVurdering.statsborgerskap?.filter { statsborgerskap ->
            statsborgerskap.gyldigPeriode?.fom?.isBefore(LocalDate.now()) ?: true &&
            statsborgerskap.gyldigPeriode?.tom?.isAfter(LocalDate.now()) ?: true
        }
                ?.map { it.medlemskap } ?: error("Person har ikke noe statsborgerskap.")


fun Evaluering.toJson(): String = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)