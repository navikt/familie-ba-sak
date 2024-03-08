package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.web.context.WebApplicationContext

class RestValutakursValideringTest : AbstractSpringIntegrationTest() {

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var context: WebApplicationContext

    fun eger() {
        val requestBuilder = MockMvcRequestBuilders.put("http://localhost:8089/api/differanseberegning/valutakurs/1")
            .accept("application/json")
            .contentType("application/json")
            .content("""
                {
                  "id": 0,
                  "fom": "2019-03",
                  "barnIdenter": [
                    "string"
                  ],
                  "valutakursdato": "2024-03-08",
                  "valutakode": "vebeg",
                  "kurs": 0,
                  "status": "IKKE_UTFYLT"
                }
            """.trimIndent())
            .buildRequest(context)
        mockMvc.perform { requestBuilder }
    }
}