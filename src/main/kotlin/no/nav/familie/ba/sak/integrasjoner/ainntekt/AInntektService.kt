package no.nav.familie.ba.sak.integrasjoner.ainntekt

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.kontrakter.felles.PersonIdent
import org.springframework.stereotype.Service

@Service
class AInntektService(
    private val integrasjonKlient: IntegrasjonKlient,
) {
    fun hentAInntektUrl(personIdent: PersonIdent) = integrasjonKlient.hentAInntektUrl(personIdent)
}
