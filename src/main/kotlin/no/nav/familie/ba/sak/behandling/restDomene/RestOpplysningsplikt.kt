package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.opplysningsplikt.OpplysningspliktStatus

data class RestOpplysningsplikt(
        val status: OpplysningspliktStatus,
        val begrunnelse: String?
)
