package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.ekstern.restDomene.RestValutakurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.logging.Level
import java.util.logging.Logger

@ExtendWith(SpringExtension::class)
@WebMvcTest(ValutakursController::class)
class RestValutakursValideringTestV2 {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @Throws(Exception::class)
    fun testValidInput() {
        val logger: Logger = Logger.getLogger(MockMvc::class.java.name)
        logger.setLevel(Level.OFF)

        val restValutakursOK: RestValutakurs =
            RestValutakurs(
                id = 1,
                fom = null,
                tom = null,
                barnIdenter = listOf("12345678901"),
                valutakursdato = null,
                valutakode = "EUR",
                kurs = null,
            )

        val restValutakursIkkeOK = restValutakursOK.copy(valutakode = "eur")

        mockMvc.perform(
            post("/api/differanseberegning/valutakurs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(restValutakursOK)),
        ).andExpect(status().isOk())

        mockMvc.perform(
            post("/api/differanseberegning/valutakurs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(restValutakursIkkeOK)),
        ).andExpect(status().is4xxClientError())
    }
}
