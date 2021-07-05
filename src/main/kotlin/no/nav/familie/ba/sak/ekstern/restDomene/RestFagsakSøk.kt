package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING

data class RestSøkParam(
        val personIdent: String,
        val barnasIdenter: List<String> = emptyList()
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