package no.nav.familie.ba.sak.journalføring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.restDomene.RestJournalføring
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.springframework.stereotype.Component

@Component
class JuornalføringMetrikk {

    private val antallEndreBruker: Counter = Metrics.counter("journalføring.endreBruker", "journalføring", "endreBruker")
    private val antallEndreAvsender: Counter = Metrics.counter("journalføring.endreAvsender", "journalføring", "endreAvsender")
    private val antallGenerellSak: Counter = Metrics.counter("journalføring.generellSak", "journalføring", "generellSak")
    private val antallFagsak: Counter = Metrics.counter("journalføring.fagsak", "journalføring", "fagsak")

    fun oppdaterJournalføringMetrikk(journalpost: Journalpost?, updatert: RestJournalføring) {
        if (journalpost?.bruker != null && journalpost.bruker?.id != updatert.bruker.id) {
            antallEndreBruker.increment()
        }
        if(journalpost!= null && journalpost.avsenderMottaker?.navn != updatert.avsender.navn){
            antallEndreAvsender.increment()
        }
        if(updatert.knyttTilFagsak){
            antallFagsak.increment()
        }else{
            antallGenerellSak.increment()
        }
    }
}