package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus

internal fun morHarLøpendeUtbetalingerIBA(fagsak: Fagsak?): Boolean {
    return fagsak?.status == FagsakStatus.LØPENDE

}


internal fun morHarSakerMenIkkeLøpendeUtbetalingerIBA(fagsak: Fagsak?): Boolean {
    return fagsak != null && fagsak.status != FagsakStatus.LØPENDE
}