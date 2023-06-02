package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.ba.sak.integrasjoner.pdl.AdressebeskyttelseBolk
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse

class PdlAdressebeskyttelseResponse(val person: PdlAdressebeskyttelsePerson?)
class PdlAdressebeskyttelsePerson(val adressebeskyttelse: List<Adressebeskyttelse>)

class PdlAdressebeskyttelseBolkResponse(val hentPersonBolk: PdlAdressebeskyttelsePersonBolk?)

class PdlAdressebeskyttelsePersonBolk(val adressebeskyttelse: List<AdressebeskyttelseBolk>)
