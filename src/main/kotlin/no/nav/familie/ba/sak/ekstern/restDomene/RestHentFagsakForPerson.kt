package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.fagsak.FagsakEier

data class RestHentFagsakForPerson(val personIdent: String, val erEnsligMindreårig: Boolean = false, var erPåInstitusjon: Boolean = false)

val RestHentFagsakForPerson.fagsakEier: FagsakEier
    get() = if (erEnsligMindreårig || erPåInstitusjon) FagsakEier.BARN else FagsakEier.OMSORGSPERSON
