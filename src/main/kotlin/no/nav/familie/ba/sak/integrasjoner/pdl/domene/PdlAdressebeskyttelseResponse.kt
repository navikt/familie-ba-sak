package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse

class PdlAdressebeskyttelseResponse(val person: PdlAdressebeskyttelsePerson?)
class PdlAdressebeskyttelsePerson(val adressebeskyttelse: List<Adressebeskyttelse>)

class PdlAdressebeskyttelseBolkResponse(val hentPersonBolk: List<AdressebeskyttelseBolk>)

class AdressebeskyttelseBolk(
    val ident: String,
    val person: PdlAdressebeskyttelsePerson?,
    val code: String,
)
