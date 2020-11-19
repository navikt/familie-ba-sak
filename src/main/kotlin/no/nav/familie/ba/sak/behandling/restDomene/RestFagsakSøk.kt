package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.pdl.internal.ADRESSEBESKYTTELSEGRADERING

data class RestSøkParam(
        var personIdent: String
)

enum class FagsakDeltagerRolle {
    BARN,
    FORELDER,
    UKJENT
}

data class RestFagsakDeltager(
        var navn: String? = null,
        var ident: String = "",
        var rolle: FagsakDeltagerRolle,
        var kjønn: Kjønn? = Kjønn.UKJENT,
        var fagsakId: Long? = null,
        var adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
        var harTilgang: Boolean = true
)

data class RestPågåendeSakRequest(
        val søkersIdent: String,
        val barnasIdenter: List<String> = emptyList()
)

data class RestPågåendeSakResponse(
    val harPågåendeSakIBaSak: Boolean,
    val harPågåendeSakIInfotrygd: Boolean
)
