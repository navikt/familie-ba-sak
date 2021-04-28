package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import org.springframework.stereotype.Service

@Service
class InfotrygdService(private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
                       private val integrasjonClient: IntegrasjonClient,
                       private val personopplysningerService: PersonopplysningerService) {
    fun hentInfotrygdsakerForSøker(ident: String): InfotrygdSøkResponse<Sak> {
        return infotrygdBarnetrygdClient.hentSaker(listOf(ident), emptyList())
    }

    fun hentMaskertRestInfotrygdsakerVedManglendeTilgang(personIdent: String): RestInfotrygdsaker? {
        val harTilgang = integrasjonClient.sjekkTilgangTilPersoner(listOf(personIdent)).first().harTilgang
        return if (!harTilgang) {
            RestInfotrygdsaker(
                    adressebeskyttelsegradering = personopplysningerService.hentAdressebeskyttelseSomSystembruker(personIdent),
                    harTilgang = false
            )
        } else null
    }

    fun hentInfotrygdstønaderForSøker(ident: String): InfotrygdSøkResponse<Stønad> {
        return infotrygdBarnetrygdClient.hentStønader(listOf(ident), emptyList())
    }

    fun hentMaskertRestInfotrygdstønaderVedManglendeTilgang(personIdent: String): RestInfotrygdstønader? {
        val harTilgang = integrasjonClient.sjekkTilgangTilPersoner(listOf(personIdent)).first().harTilgang
        return if (!harTilgang) {
            RestInfotrygdstønader(
                    adressebeskyttelsegradering = personopplysningerService.hentAdressebeskyttelseSomSystembruker(personIdent),
                    harTilgang = false
            )
        } else null
    }

    fun harLøpendeEllerÅpenSakIInfotrygd(søkerIdenter: List<String>): Boolean {
        return infotrygdBarnetrygdClient.harLøpendeSakIInfotrygd(søkerIdenter, emptyList())
               || infotrygdBarnetrygdClient.harÅpenSakIInfotrygd(søkerIdenter, emptyList())
    }
}