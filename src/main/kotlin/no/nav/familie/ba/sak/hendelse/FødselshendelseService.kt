package no.nav.familie.ba.sak.hendelse

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.stereotype.Service

@Service
class FødselshendelseService(private val infotrygdFeedService: InfotrygdFeedService,
                             private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
                             private val personopplysningerService: PersonopplysningerService) {

    val harSakIInfotrygdCounter: Counter = Metrics.counter("fødselshendelse.mor.eller.barn.er.tilknyttet.sak.i.infotrygd")
    val harIkkeSakIInfotrygdCounter: Counter = Metrics.counter("fødselshendelse.mor.eller.barn.er.ikke.tilknyttet.sak.i.infotrygd")

    fun fødselshendelseSkalBehandlesHosInfotrygd(morsIdent: String, barnasIdenter: List<String>): Boolean {

        val morsIdenter = personopplysningerService.hentIdenter(Ident(morsIdent))
                .filter { it.gruppe == "FOLKEREGISTERIDENT" }
                .map { it.ident }
        val alleBarnasIdenter = barnasIdenter.flatMap {
            personopplysningerService.hentIdenter(Ident(it))
                    .filter { identinfo -> identinfo.gruppe == "FOLKEREGISTERIDENT" }
                    .map { identinfo -> identinfo.ident }
        }

        val harLøpendeInfotrygdsak = infotrygdBarnetrygdClient.harLøpendeInfotrygdsak(morsIdenter, alleBarnasIdenter)
        when (harLøpendeInfotrygdsak) {
            true -> harSakIInfotrygdCounter.increment()
            false -> harIkkeSakIInfotrygdCounter.increment()
        }

        return harLøpendeInfotrygdsak
    }

    fun sendTilInfotrygdFeed(barnIdenter: List<String>) {
        infotrygdFeedService.sendTilInfotrygdFeed(barnIdenter)
    }
}