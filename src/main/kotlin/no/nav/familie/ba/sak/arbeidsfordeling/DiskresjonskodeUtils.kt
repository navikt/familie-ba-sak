package no.nav.familie.ba.sak.arbeidsfordeling

import no.nav.familie.ba.sak.integrasjoner.domene.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService.IdentMedAdressebeskyttelse

fun finnPersonMedStrengesteAdressebeskyttelse(personer: List<IdentMedAdressebeskyttelse>): String? {
    return personer.fold(null,
                         fun(person: IdentMedAdressebeskyttelse?,
                             neste: IdentMedAdressebeskyttelse): IdentMedAdressebeskyttelse? {
                             return when {
                                 person?.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG -> {
                                     person
                                 }
                                 neste.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG -> {
                                     neste
                                 }
                                 person?.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.FORTROLIG -> {
                                     person
                                 }
                                 neste.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.FORTROLIG
                                 -> {
                                     neste
                                 }
                                 else -> null
                             }
                         })?.ident
}