package no.nav.familie.ba.sak.bisys

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.postForEntity
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.YearMonth

@ActiveProfiles("postgres")
class BisysControllerTest: WebSpringAuthTestRunner() {

    @Test
    fun `hent-utvidet-barnetrygd skal returnere korrekte dummyverdier`() {
        val header = HttpHeaders()
        header.contentType = MediaType.APPLICATION_JSON
        header.setBearerAuth(token(emptyMap()).toString())
        val requestEntity = HttpEntity<String>(objectMapper.writeValueAsString(BisysUtvidetBarnetrygdRequest(
            "1234",
            LocalDate.now()
        )), header)

        val typetResponse = restTemplate.postForEntity<BisysUtvidetBarnetrygdResponse>(
            hentUrl("/api/bisys/hent-utvidet-barnetrygd"),
            requestEntity)

        assertEquals(BisysStønadstype.UTVIDET, typetResponse.body!!.perioder[0].stønadstype)
        assertEquals(YearMonth.of(2020, 1), typetResponse.body!!.perioder[0].fomMåned)
        assertEquals(YearMonth.of(2020, 1), typetResponse.body!!.perioder[0].tomMåned)
        assertEquals(1024, typetResponse.body!!.perioder[0].beløp)

        // Sjekk at JSON-strengen er på avtalt format.
        val stringResponse = restTemplate.postForEntity<String>(
            hentUrl("/api/bisys/hent-utvidet-barnetrygd"),
            requestEntity)

        assertEquals("""{"perioder":[{"stønadstype":"UTVIDET","fomMåned":"2020-01","tomMåned":"2020-01","beløp":1024}]}""", stringResponse.body!!)
    }
}