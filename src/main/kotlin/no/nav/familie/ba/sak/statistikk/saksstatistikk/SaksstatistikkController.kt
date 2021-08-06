package no.nav.familie.ba.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ba.sak.integrasjoner.statistikk.StatistikkClient
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/saksstatistikk")
@Profile("!e2e")
@ProtectedWithClaims(issuer = "azuread")
class SaksstatistikkController(
        val saksstatistikkConverterService: SaksstatistikkConverterService,
        val fagsakRepository: FagsakRepository,
        val statistikkClient: StatistikkClient
) {



    @GetMapping(path = ["/sak/konverter"])
    fun konverterSlettedeMeldinger(): SaksstatistikkConverterResponse {
        var antallIgnorerteManglerFagsak: Int = 0
        var antallSendteSaker: Int = 0

        for (i in 2000..4722.toLong()) {
            val sakJsonNode = statistikkClient.hentSakStatistikk(i)
            val fagsakId = sakJsonNode.path("sakId").asLong()

            val fagsak = fagsakRepository.finnFagsak(fagsakId)

            if (fagsak != null) {
                try {
                    saksstatistikkConverterService.konverterSakTilSisteKontraktVersjon(sakJsonNode)
                } catch (e: Exception) {
                    RuntimeException("Noe gikk galt ved konvertering av offset $i", e)
                }

                antallSendteSaker = antallSendteSaker.inc()
            } else {
                antallIgnorerteManglerFagsak = antallIgnorerteManglerFagsak.inc()
            }
        }

        return SaksstatistikkConverterResponse(antallSendteSaker, antallIgnorerteManglerFagsak)
    }


    @GetMapping(path = ["/sak/resend/{offset}"])
    fun resendMeldingMedOffset(@PathVariable offset: Long): JsonNode {
        val sakJsonNode = statistikkClient.hentSakStatistikk(offset)
        val fagsakId = sakJsonNode.path("sakId").asLong()

        val fagsak = fagsakRepository.finnFagsak(fagsakId)

        if (fagsak != null) {

        }

        error("Fant ikke fagsak med id $fagsakId")
    }

    data class SaksstatistikkConverterResponse(var antallSendteSaker: Int, val antallIgnorerteManglerFagsak: Int)
}