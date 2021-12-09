package no.nav.familie.ba.sak.integrasjoner.skyggesak

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Service

@Service
class SkyggesakService(
    private val integrasjonClient: IntegrasjonClient,
    private val personopplysningerService: PersonopplysningerService
) {

    fun opprettSkyggesak(aktør: Aktør, fagsakId: Long) {
        integrasjonClient.opprettSkyggesak(aktør, fagsakId)
    }
}
