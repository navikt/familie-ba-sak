package no.nav.familie.ba.sak.integrasjoner.organisasjon

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.springframework.stereotype.Service

@Service
class OrganisasjonService(
    private val integrasjonKlient: IntegrasjonKlient,
) {
    fun hentOrganisasjon(orgnummer: String): Organisasjon {
        val organisasjon = integrasjonKlient.hentOrganisasjon(orgnummer)
        return organisasjon
    }
}
