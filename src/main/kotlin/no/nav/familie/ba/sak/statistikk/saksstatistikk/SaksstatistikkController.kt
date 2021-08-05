package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.integrasjoner.statistikk.StatistikkClient
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/saksstatistikk")
@Profile("!e2e")
@ProtectedWithClaims(issuer = "azuread")
class SaksstatistikkController(val fagsakRepository: FagsakRepository,
val statistikkClient: StatistikkClient) {


    @GetMapping(path = ["/sak/konverter"])
    fun konverterSlettedeMeldinger(): SaksstatistikkConverterResponse {
        var antallIgnorerteManglerFagsak: Int = 0
        var antallSendteSaker: Int = 0

        for (i in 2000..2555.toLong()) {
            val sakJsonNode = statistikkClient.hentSakStatistikk(i)
            val fagsakId = sakJsonNode.path("sakId").asLong()

            val fagsak = fagsakRepository.finnFagsak(fagsakId)

            if (fagsak != null) {
                antallSendteSaker = antallSendteSaker.inc()
            } else {
                antallIgnorerteManglerFagsak = antallIgnorerteManglerFagsak.inc()
            }
        }

        return SaksstatistikkConverterResponse(antallSendteSaker, antallIgnorerteManglerFagsak)
    }

    data class SaksstatistikkConverterResponse(var antallSendteSaker: Int, val antallIgnorerteManglerFagsak: Int)
}