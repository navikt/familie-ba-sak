package no.nav.familie.ba.sak.ekstern.skatteetaten

import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.skatteetaten.model.Person
import no.nav.familie.ba.skatteetaten.model.PersonerResponse
import org.springframework.stereotype.Service
import java.time.Year

@Service
class SkatteetatenService(private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient) {

    fun finnPersonerMedUtvidetBarnetrygd(år: String): PersonerResponse {
        val personerFraInfotrygd = infotrygdBarnetrygdClient.hentPersonerMedUtvidetBarnetrygd(Year.parse(år))
        val personerFraBaSak: MutableList<Person> = mutableListOf() // Todo
        return PersonerResponse(personerFraBaSak.apply { addAll(personerFraInfotrygd.brukere) })
    }
}