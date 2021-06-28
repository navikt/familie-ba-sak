package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.kontrakter.felles.infotrygdsak.InfotrygdSak


internal fun morHarLøpendeUtbetalingerIBA(fagsak: Fagsak): Boolean {
    return fagsak.status == FagsakStatus.LØPENDE ?: false
}

internal fun morHarLøpendeUtbetalingerIInfotrygd(infotrygdsak: InfotrygdSak): Boolean {
    return false
}


internal fun morHarSakerMenIkkeLøpendeUtbetalingerIBA(fagsak: Fagsak): Boolean {
    return fagsak.status != FagsakStatus.LØPENDE
}

internal fun morHarSakerMenIkkeLøpendeIInfotrygd(fagsak: Fagsak): Boolean {
    return false
}

internal fun morHarBarnDerFarHarLøpendeUtbetalingIInfotrygd(fagsak: Fagsak): Boolean {
    return false
}