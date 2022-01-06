package no.nav.familie.ba.sak.integrasjoner.skyggesak

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Service

@Service
class SkyggesakService(
    private val integrasjonClient: IntegrasjonClient
) {

    fun opprettSkyggesak(aktør: Aktør, fagsakId: Long) {
        integrasjonClient.opprettSkyggesak(aktør, fagsakId)
    }
}
