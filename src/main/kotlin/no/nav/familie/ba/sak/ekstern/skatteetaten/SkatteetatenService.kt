package no.nav.familie.ba.sak.ekstern.skatteetaten

import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioder
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioderResponse
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerson
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPersonerResponse
import org.springframework.stereotype.Service

@Service
class SkatteetatenService(private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient) {

    fun finnPersonerMedUtvidetBarnetrygd(år: String): SkatteetatenPersonerResponse {
        val personerFraInfotrygd = infotrygdBarnetrygdClient.hentPersonerMedUtvidetBarnetrygd(år)
        val personerFraBaSak = emptyList<SkatteetatenPerson>() // Todo
        return SkatteetatenPersonerResponse(personerFraInfotrygd.brukere + personerFraBaSak)
    }

    fun finnPerioderMedUtvidetBarnetrygd(personer: List<String>, år: String): SkatteetatenPerioderResponse {
        val perioderFraInfotrygd = personer.mapNotNull { infotrygdBarnetrygdClient.hentPerioderMedUtvidetBarnetrygd(it, år) }
        val perioderFraBaSak = emptyList<SkatteetatenPerioder>() // Todo
        return SkatteetatenPerioderResponse(perioderFraInfotrygd + perioderFraBaSak)
    }
}