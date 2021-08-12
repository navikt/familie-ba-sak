package no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.kjerne.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall.FiltreringsregelIkkeOppfylt
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall.FiltreringsregelOppfylt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import java.time.LocalDate

enum class Filtreringsregel(val vurder: FiltreringsreglerFakta.() -> Evaluering) {
    MOR_GYLDIG_FNR(vurder = { morHarGyldigFnr(this) }),
    BARN_GYLDIG_FNR(vurder = { barnHarGyldigFnr(this) }),
    MOR_LEVER(vurder = { morLever(this) }),
    BARN_LEVER(vurder = { barnLever(this) }),
    MER_ENN_5_MND_SIDEN_FORRIGE_BARN(vurder = { merEnn5mndEllerMindreEnnFemDagerSidenForrigeBarn(this) }),
    MOR_ER_OVER_18_ÅR(vurder = { morErOver18år(this) }),
    MOR_HAR_IKKE_VERGE(vurder = { morHarIkkeVerge(this) }),
}

fun evaluerFiltreringsregler(fakta: FiltreringsreglerFakta) = Filtreringsregel.values()
        .fold(mutableListOf<Evaluering>()) { acc, filtreringsregel ->
            if (acc.any { it.resultat == Resultat.IKKE_OPPFYLT }) {
                acc.add(Evaluering(
                        resultat = Resultat.IKKE_VURDERT,
                        identifikator = filtreringsregel.name,
                        begrunnelse = "Ikke vurdert",
                        evalueringÅrsaker = emptyList()
                ))
            } else {
                acc.add(filtreringsregel.vurder(fakta).copy(identifikator = filtreringsregel.name))
            }

            acc
        }

fun morHarGyldigFnr(fakta: FiltreringsreglerFakta): Evaluering {
    val erMorFnrGyldig = (!erBostNummer(fakta.mor.personIdent.ident) && !erFDatnummer(fakta.mor.personIdent.ident))

    return if (erMorFnrGyldig) Evaluering.oppfylt(FiltreringsregelOppfylt.MOR_HAR_GYLDIG_FNR) else Evaluering.ikkeOppfylt(
            FiltreringsregelIkkeOppfylt.MOR_HAR_UGYLDIG_FNR)
}

fun barnHarGyldigFnr(fakta: FiltreringsreglerFakta): Evaluering {
    val erbarnFnrGyldig =
            fakta.barnaFraHendelse.all { (!erBostNummer(it.personIdent.ident) && !erFDatnummer(it.personIdent.ident)) }

    return if (erbarnFnrGyldig) Evaluering.oppfylt(FiltreringsregelOppfylt.BARN_HAR_GYLDIG_FNR) else Evaluering.ikkeOppfylt(
            FiltreringsregelIkkeOppfylt.BARN_HAR_UGYLDIG_FNR)
}

fun morErOver18år(fakta: FiltreringsreglerFakta): Evaluering = if (fakta.mor.hentAlder() >= 18) Evaluering.oppfylt(
        FiltreringsregelOppfylt.MOR_ER_OVER_18_ÅR) else Evaluering.ikkeOppfylt(FiltreringsregelIkkeOppfylt.MOR_ER_UNDER_18_ÅR)

fun morLever(fakta: FiltreringsreglerFakta): Evaluering = if (fakta.morLever) Evaluering.oppfylt(FiltreringsregelOppfylt.MOR_LEVER) else Evaluering.ikkeOppfylt(
        FiltreringsregelIkkeOppfylt.MOR_LEVER_IKKE)

fun barnLever(fakta: FiltreringsreglerFakta): Evaluering = if (fakta.barnaLever) Evaluering.oppfylt(FiltreringsregelOppfylt.BARNET_LEVER) else Evaluering.ikkeOppfylt(
        FiltreringsregelIkkeOppfylt.BARNET_LEVER_IKKE)

fun morHarIkkeVerge(fakta: FiltreringsreglerFakta): Evaluering = if (!fakta.morHarVerge) Evaluering.oppfylt(FiltreringsregelOppfylt.MOR_ER_MYNDIG) else Evaluering.ikkeOppfylt(
        FiltreringsregelIkkeOppfylt.MOR_ER_UMYNDIG)

fun merEnn5mndEllerMindreEnnFemDagerSidenForrigeBarn(fakta: FiltreringsreglerFakta): Evaluering {
    return when (fakta.barnaFraHendelse.all { barnFraHendelse ->
        fakta.restenAvBarna.all {
            barnFraHendelse.fødselsdato.isAfter(it.fødselsdato.plusMonths(5)) ||
            barnFraHendelse.fødselsdato.isBefore(it.fødselsdato.plusDays(6))
        }
    }) {
        true -> Evaluering.oppfylt(FiltreringsregelOppfylt.MER_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL)
        false -> Evaluering.ikkeOppfylt(FiltreringsregelIkkeOppfylt.MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL)
    }
}

internal fun erFDatnummer(personIdent: String): Boolean {
    return personIdent.substring(6).toInt() == 0
}

internal fun erBostNummer(personIdent: String): Boolean {
    return personIdent.substring(2, 3).toInt() > 1
}