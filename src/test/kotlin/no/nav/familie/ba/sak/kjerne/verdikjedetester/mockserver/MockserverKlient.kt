package no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver

import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import org.springframework.web.client.getForObject
import org.springframework.web.client.postForEntity

class MockserverKlient(
        private val mockServerUrl: String,
        restOperations: RestOperations,
) : AbstractRestClient(restOperations, "mock-server") {

    val restOperations = RestTemplateBuilder().additionalInterceptors(
            MdcValuesPropagatingClientInterceptor()).build()

    fun hentOppgaveOpprettetMedCallid(callId: String): String {
        return restOperations.getForObject("$mockServerUrl/rest/api/oppgave/cache/$callId")
    }

    fun clearOppaveCache() {
        restOperations.delete("$mockServerUrl/rest/api/oppgave/cache/clear")
    }

    fun clearFerdigstillJournapostCache() {
        restOperations.delete("$mockServerUrl/rest/api/dokarkiv/internal/ferdigstill/clear")
    }

    fun lagScenario(restScenario: RestScenario): RestScenario {
        return restOperations.postForEntity<RestScenario>("$mockServerUrl/rest/scenario", restScenario).body
               ?: error("Klarte ikke lage scenario med data $restScenario")
    }
}