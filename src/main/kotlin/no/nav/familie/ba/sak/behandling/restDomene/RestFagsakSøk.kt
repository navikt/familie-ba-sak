package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn


data class RestSøkParam(
        var personIdent: String
)

enum class FagsakDeltagerRolle {
    BARN, FORELDER, UKJENT
}

data class RestFagsakDeltager(
        var navn: String?= null,
        var ident: String,
        var rolle: FagsakDeltagerRolle,
        var kjønn: Kjønn?= Kjønn.UKJENT,
        var fagsakId: Long?= null
)