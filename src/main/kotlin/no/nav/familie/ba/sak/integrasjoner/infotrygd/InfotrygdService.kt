package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.stereotype.Service

@Service
class InfotrygdService(
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val integrasjonClient: IntegrasjonClient,
    private val personopplysningerService: PersonopplysningerService
) {

    fun hentInfotrygdsakerForSøker(aktør: Aktør): InfotrygdSøkResponse<Sak> {
        return infotrygdBarnetrygdClient.hentSaker(listOf(aktør.aktivFødselsnummer()), emptyList())
    }

    fun hentMaskertRestInfotrygdsakerVedManglendeTilgang(aktør: Aktør): RestInfotrygdsaker? {
        val harTilgang =
            integrasjonClient.sjekkTilgangTilPersoner(listOf(aktør.aktivFødselsnummer())).first().harTilgang
        return if (!harTilgang) {
            RestInfotrygdsaker(
                adressebeskyttelsegradering = personopplysningerService.hentAdressebeskyttelseSomSystembruker(aktør),
                harTilgang = false
            )
        } else null
    }

    fun hentInfotrygdstønaderForSøker(ident: String, historikk: Boolean = false): InfotrygdSøkResponse<Stønad> {
        val søkerIdenter = personopplysningerService.hentIdenter(Ident(ident))
            .filter { it.gruppe == "FOLKEREGISTERIDENT" }
            .map { it.ident }
        return infotrygdBarnetrygdClient.hentStønader(søkerIdenter, emptyList(), historikk)
    }

    fun hentMaskertRestInfotrygdstønaderVedManglendeTilgang(aktør: Aktør): RestInfotrygdstønader? {
        val harTilgang =
            integrasjonClient.sjekkTilgangTilPersoner(listOf(aktør.aktivFødselsnummer())).first().harTilgang
        return if (!harTilgang) {
            RestInfotrygdstønader(
                adressebeskyttelsegradering = personopplysningerService.hentAdressebeskyttelseSomSystembruker(
                    aktør
                ),
                harTilgang = false
            )
        } else null
    }

    fun harÅpenSakIInfotrygd(søkerIdenter: List<String>, barnasIdenter: List<String> = emptyList()): Boolean {
        return infotrygdBarnetrygdClient.harÅpenSakIInfotrygd(søkerIdenter, barnasIdenter)
    }

    fun harLøpendeSakIInfotrygd(søkerIdenter: List<String>, barnasIdenter: List<String> = emptyList()): Boolean {
        return infotrygdBarnetrygdClient.harLøpendeSakIInfotrygd(søkerIdenter, barnasIdenter)
    }
}
