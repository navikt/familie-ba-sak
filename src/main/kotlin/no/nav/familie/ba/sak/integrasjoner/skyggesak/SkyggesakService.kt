package no.nav.familie.ba.sak.integrasjoner.skyggesak

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.stereotype.Service

@Service
class SkyggesakService(private val integrasjonClient: IntegrasjonClient,
                       private val personopplysningerService: PersonopplysningerService) {

    fun opprettSkyggesak(ident: String, fagsakId: Long) {
        val aktørId = personopplysningerService.hentAktivAktørId(Ident(ident))
        integrasjonClient.opprettSkyggesak(aktørId, fagsakId)
    }
}