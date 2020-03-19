package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType


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