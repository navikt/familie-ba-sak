package no.nav.familie.ba.sak.hendelse

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.personinfo.Ident
import org.springframework.stereotype.Service

@Service
class FødselshendelseService(private val infotrygdFeedService: InfotrygdFeedService,
                             private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
                             private val integrasjonClient: IntegrasjonClient) {

    val finnesHosInfotrygdCounter: Counter = Metrics.counter("fødselshendelse.mor.eller.barn.finnes.i.infotrygd")
    val finnesIkkeHosInfotrygdCounter: Counter = Metrics.counter("fødselshendelse.mor.eller.barn.finnes.ikke.i.infotrygd")

    fun fødselshendelseSkalBehandlesHosInfotrygd(morsIdent: String, barnasIdenter: List<String>): Boolean {

        val morsIdenter = integrasjonClient.hentIdenter(Ident(morsIdent))
                .filter { it.gruppe == "FOLKEREGISTERIDENT" }
                .map { it.ident }
        val alleBarnasIdenter = barnasIdenter.flatMap {
            integrasjonClient.hentIdenter(Ident(it))
                    .filter { identinfo -> identinfo.gruppe == "FOLKEREGISTERIDENT" }
                    .map { identinfo -> identinfo.ident }
        }

        val finnesHosInfotrygd = !infotrygdBarnetrygdClient.finnesIkkeHosInfotrygd(morsIdenter, alleBarnasIdenter)
        when (finnesHosInfotrygd) {
            true -> finnesHosInfotrygdCounter.increment()
            false -> finnesIkkeHosInfotrygdCounter.increment()
        }

        return finnesHosInfotrygd
    }

    fun sendTilInfotrygdFeed(barnIdenter: List<String>) {
        infotrygdFeedService.sendTilInfotrygdFeed(barnIdenter)
    }
}