package no.nav.familie.ba.sak.fake

import io.mockk.mockk
import no.nav.familie.ba.sak.fake.FakeØkonomiKlient.Companion.simuleringsresultater
import no.nav.familie.ba.sak.integrasjoner.økonomi.OppdragBackendKlient
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.springframework.web.client.RestClient

class FakeOppdragBackendKlient :
    OppdragBackendKlient(
        familieOppdragBackendUri = "http://familie-oppdrag-backend-fake-uri",
        restClient = mockk<RestClient>(relaxed = true),
        retryBackoffDelay = 1L,
    ) {
    override fun hentSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat = simuleringsresultater[utbetalingsoppdrag.saksnummer] ?: DetaljertSimuleringResultat(simuleringsMottakere)
}
