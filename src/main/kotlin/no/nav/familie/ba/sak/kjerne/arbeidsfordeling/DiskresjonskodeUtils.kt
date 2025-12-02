package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService.IdentMedAdressebeskyttelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING

fun finnPersonMedStrengesteAdressebeskyttelse(
    personer: List<IdentMedAdressebeskyttelse>,
): String? =
    personer
        .sortedByDescending { it.personType == PersonType.SØKER }
        .maxByOrNull { it.adressebeskyttelsegradering.strenghet() }
        ?.takeIf { it.adressebeskyttelsegradering.strenghet() > 0 }
        ?.ident

private fun ADRESSEBESKYTTELSEGRADERING?.strenghet(): Int =
    when (this) {
        ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG -> 3
        ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND -> 2
        ADRESSEBESKYTTELSEGRADERING.FORTROLIG -> 1
        else -> 0
    }
