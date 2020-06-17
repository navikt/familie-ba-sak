package no.nav.familie.ba.sak.hendelse

import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.Ident
import org.springframework.stereotype.Service

@Service
class FødselshendelseService(private val infotrygdFeedService: InfotrygdFeedService,
                             private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
                             private val integrasjonClient: IntegrasjonClient) {

    fun fødselshendelseSkalBehandlesHosInfotrygd(søkersIdent: String, barnasIdenter: List<String>): Boolean {

        // Siden vi sender til ba-sak uansett, dersom søker har sak i ba-sak eller ikke har en sak i noen av fagsystemene,
        // holder det å sjekke om søker har en sak i infotrygd for å avgjøre hvor vi skal sende hendelsen videre.

        // Tjenesten mot infotrygd-replika sjekker om søker eller barn finnes I DET HELE TATT, m.a.o. alle tidligere og
        // avsluttede søknader.

        val søkersIdenter = integrasjonClient.hentIdenter(Ident(søkersIdent))
                .filter { it.gruppe == "FOLKEREGISTERIDENT" }
                .map { it.ident }
        val alleBarnasIdenter = barnasIdenter.flatMap {
            integrasjonClient.hentIdenter(Ident(it))
                    .filter { identinfo -> identinfo.gruppe == "FOLKEREGISTERIDENT" }
                    .map { identinfo -> identinfo.ident }
        }

        return infotrygdBarnetrygdClient.finnesHosInfotrygd(søkersIdenter, alleBarnasIdenter)
    }

    fun sendTilInfotrygdFeed(barnIdenter: List<String>) {
        infotrygdFeedService.sendTilInfotrygdFeed(barnIdenter)
    }
}