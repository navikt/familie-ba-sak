package no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

class MockserverKlient(
    private val mockServerUrl: String,
    private val restClient: RestClient,
) {
    fun lagScenario(restScenario: RestScenario): RestScenario {
        val scenario =
            restClient.post().body(restScenario).retrieve().body<RestScenario>()
                ?: error("Klarte ikke lage scenario med data $restScenario")
        logger.info("Laget scenario: ${scenario.convertDataClassToJson()}")

        return scenario
    }

    companion object {
        val logger = LoggerFactory.getLogger(MockserverKlient::class.java)
    }
}
