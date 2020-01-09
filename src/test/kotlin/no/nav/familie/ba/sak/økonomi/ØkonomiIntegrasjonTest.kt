package no.nav.familie.ba.sak.økonomi

import DevLauncher
import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.HttpTestBase
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate


@SpringBootTest(classes = [ApplicationConfig::class], properties = ["FAMILIE_OPPDRAG_API_URL=http://localhost:18085/api"])
@ActiveProfiles("dev", "mock-oauth")
@TestInstance(Lifecycle.PER_CLASS)
class ØkonomiIntegrasjonTest: HttpTestBase(
        18085
) {
    @Autowired
    lateinit var økonomiService: ØkonomiService

    @Test
    fun `Skal iverksette vedtak mot familie oppdrag`() {
        val responseBody = Ressurs.Companion.success("Oppdrag sendt ok")
        val response: MockResponse = MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(responseBody))
        mockServer.enqueue(response)

        val utbetalingsoppdrag = Utbetalingsoppdrag(
                saksbehandlerId = "saksbehandlerId",
                kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                fagSystem = "IT05",
                saksnummer = "1234",
                aktoer = "12345678910",
                utbetalingsperiode = listOf(Utbetalingsperiode(
                        erEndringPåEksisterendePeriode = false,
                        datoForVedtak = LocalDate.now(),
                        klassifisering = "BAOROSMS",
                        vedtakdatoFom = LocalDate.now(),
                        vedtakdatoTom = LocalDate.now(),
                        sats = BigDecimal(1054),
                        satsType = Utbetalingsperiode.SatsType.MND,
                        utbetalesTil = "12345678910",
                        behandlingId = 1,
                        opphør = null
                ))
        )

        val result = økonomiService.iverksettOppdrag(utbetalingsoppdrag)

        assertEquals(
                responseBody,
                result.body
        )
    }
}