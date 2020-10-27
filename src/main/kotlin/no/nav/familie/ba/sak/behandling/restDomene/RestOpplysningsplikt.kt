package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.opplysningsplikt.Opplysningsplikt
import no.nav.familie.ba.sak.opplysningsplikt.OpplysningspliktStatus

data class RestOpplysningsplikt(
        val status: OpplysningspliktStatus,
        val begrunnelse: String?
)


fun Opplysningsplikt.toRestOpplysningsplikt() = RestOpplysningsplikt(
        status = this.status,
        begrunnelse = this.begrunnelse
)