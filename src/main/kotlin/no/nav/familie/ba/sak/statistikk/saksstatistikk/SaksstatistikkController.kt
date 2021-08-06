package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.integrasjoner.statistikk.StatistikkClient
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/saksstatistikk")
@Profile("!e2e")
@ProtectedWithClaims(issuer = "azuread")
class SaksstatistikkController(
    val saksstatistikkConverterService: SaksstatistikkConverterService,
    val fagsakRepository: FagsakRepository,
    val statistikkClient: StatistikkClient,
    val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @GetMapping(path = ["/sak/konverter"])
    @Transactional
    fun konvertertOgSendSak(
        @RequestParam(required = false, defaultValue = "false")
        send: Boolean
    ): SaksstatistikkConverterResponse {
        var antallIgnorerteManglerFagsak: Int = 0
        var antallSendteSaker: Int = 0


        for (i in 2000..2050.toLong()) {

            val sakJsonNode = statistikkClient.hentSakStatistikk(i)
            val fagsakId = sakJsonNode.path("sakId").asLong()

            val fagsak = fagsakRepository.finnFagsak(fagsakId)

            if (fagsak != null) {
                try {
                    val sakDVH = saksstatistikkConverterService.konverterSakTilSisteKontraktVersjon(sakJsonNode)
                    secureLogger.info(
                        "Saksstatistikk: Konvertert melding med offset $i: ${
                            sakstatistikkObjectMapper.writeValueAsString(
                                sakDVH
                            )
                        }"
                    )
                    if (send) {
                        val nyMellomlagring = SaksstatistikkMellomlagring(
                            funksjonellId = sakDVH.funksjonellId,
                            kontraktVersjon = sakDVH.versjon,
                            json = sakstatistikkObjectMapper.writeValueAsString(sakDVH),
                            type = SaksstatistikkMellomlagringType.SAK,
                            typeId = fagsak.id
                        )
                        logger.info("Saksstatistikk: sender offset $i")

                        saksstatistikkMellomlagringRepository.save(nyMellomlagring)

                    }
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


    @GetMapping(path = ["/sak/konverter/{offset}"])
    fun konvertertSakMeldingMedOffset(@PathVariable offset: Long): SakDVH {
        val sakJsonNode = statistikkClient.hentSakStatistikk(offset)
        val fagsakId = sakJsonNode.path("sakId").asLong()

        val fagsak = fagsakRepository.finnFagsak(fagsakId)

        if (fagsak != null) {
            return saksstatistikkConverterService.konverterSakTilSisteKontraktVersjon(sakJsonNode)
        }

        error("Fant ikke fagsak med id $fagsakId")
    }

    data class SaksstatistikkConverterResponse(var antallSendteSaker: Int, val antallIgnorerteManglerFagsak: Int)
}