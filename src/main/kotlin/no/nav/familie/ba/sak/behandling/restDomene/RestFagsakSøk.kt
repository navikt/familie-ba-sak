package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent

data class RestFunnetFagsak(
    val fagsakId: Long,
    val personType: PersonType
)

data class RestFagsakSøk(
        val personIdent: String,
        val navn: String,
        val kjønn: Kjønn,
        val fagsaker: List<RestFunnetFagsak>
)