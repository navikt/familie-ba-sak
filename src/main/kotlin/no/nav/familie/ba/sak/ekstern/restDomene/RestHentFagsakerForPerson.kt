package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType

data class RestHentFagsakerForPerson(val personIdent: String, val fagsakTyper: List<FagsakType> = listOf(FagsakType.NORMAL, FagsakType.INSTITUSJON, FagsakType.BARN_ENSLIG_MINDREÅRIG))
