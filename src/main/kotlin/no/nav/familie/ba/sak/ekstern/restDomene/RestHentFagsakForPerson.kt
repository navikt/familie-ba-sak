package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.fagsak.FagsakEier

data class RestHentFagsakForPerson(val personIdent: String, val fagsakEier: FagsakEier = FagsakEier.OMSORGSPERSON)
