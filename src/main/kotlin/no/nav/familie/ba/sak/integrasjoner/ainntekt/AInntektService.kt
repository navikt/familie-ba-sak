package no.nav.familie.ba.sak.integrasjoner.ainntekt

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.PersonIdent
import org.springframework.stereotype.Service

@Service
class AInntektService(
    private val integrasjonClient: IntegrasjonClient,
) {
    fun hentAInntektUrl(personIdent: PersonIdent) = integrasjonClient.hentAInntektUrl(personIdent)
}
