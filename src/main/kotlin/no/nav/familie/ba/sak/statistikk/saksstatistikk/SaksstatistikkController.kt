package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.integrasjoner.statistikk.StatistikkClient
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
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
import java.time.LocalDate

@RestController
@RequestMapping("/api/saksstatistikk")
@Profile("!e2e")
@ProtectedWithClaims(issuer = "azuread")
class SaksstatistikkController(
    val saksstatistikkConverterService: SaksstatistikkConverterService,
    val fagsakRepository: FagsakRepository,
    val behandlingRepository: BehandlingRepository,
    val statistikkClient: StatistikkClient,
    val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @GetMapping(path = ["/sak/konverter"])
    @Transactional
    fun konverterOgSendSak(
        @RequestParam(required = false, defaultValue = "false")
        send: Boolean
    ): SaksstatistikkResendtResponse {
        var antallIgnorerteManglerFagsak = 0
        var antallSendteSaker = 0
        var antallFeil = 0
        val sakerKonvertertCounter = mutableMapOf<String, Int>()


        for (i in 0..3068.toLong()) {
            val sakJsonNode = statistikkClient.hentSakStatistikk(i)
            val fagsakId = sakJsonNode.path("sakId").asLong()
            val versjon = sakJsonNode.path("versjon").asText()

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

                        saksstatistikkMellomlagringRepository.saveAndFlush(nyMellomlagring)
                        logger.info("Saksstatistikk: resender offset: $i nyMellomlagring: ${nyMellomlagring.id}")
                        antallSendteSaker = antallSendteSaker.inc()
                    }

                    if (saksstatistikkMellomlagringRepository.findByOffsetVerdiAndType(i, SaksstatistikkMellomlagringType.SAK) == null){
                        saksstatistikkMellomlagringRepository.save(
                            SaksstatistikkMellomlagring(
                                offsetVerdi = i,
                                kontraktVersjon = versjon,
                                funksjonellId = "offset-$i",
                                json = sakstatistikkObjectMapper.writeValueAsString(sakJsonNode),
                                type = SaksstatistikkMellomlagringType.SAK,
                                typeId = fagsakId,
                                sendtTidspunkt = LocalDate.of(1970, 1, 1).atStartOfDay()
                            )
                        )
                    }

                } catch (e: Exception) {
                    logger.warn("Saksstatistikk: Noe gikk galt ved konvertering av offset $i", e)
                    antallFeil = antallFeil.inc()
                }

                if (sakerKonvertertCounter.containsKey(versjon)) {
                    sakerKonvertertCounter[versjon] = sakerKonvertertCounter[versjon]!!.inc()
                } else {
                    sakerKonvertertCounter[versjon] = 1
                }

            } else {
                antallIgnorerteManglerFagsak = antallIgnorerteManglerFagsak.inc()
            }
        }

        return SaksstatistikkResendtResponse(antallSendteSaker, antallIgnorerteManglerFagsak, sakerKonvertertCounter, antallFeil)
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

    @GetMapping(path = ["/behandling/konverter"])
    @Transactional
    fun konverterOgSendBehandling(
        @RequestParam(required = false, defaultValue = "false")
        send: Boolean
    ): SaksstatistikkResendtResponse {
        var antallIgnorerteManglerBehandling = 0
        var antallSendteBehandlinger = 0
        var antallFeil = 0
        val behandlingerKonvertertCounter = mutableMapOf<String, Int>()

        for (i in 0..4886.toLong()) {
            val behandlingJsonNode = statistikkClient.hentBehandlingStatistikk(i)
            val behandlingId = behandlingJsonNode.path("behandlingId").asLong()
            val versjon = behandlingJsonNode.path("versjon").asText()

            val behandling = forsøkHentBehandling(behandlingId)

            if (behandling != null) {
                try {
                    val behandlingDVH =
                        saksstatistikkConverterService.konverterBehandlingTilSisteKontraktVersjon(behandlingJsonNode)
                    secureLogger.info(
                        "Behandlingsstatistikk: Konvertert melding med offset $i: ${
                            sakstatistikkObjectMapper.writeValueAsString(behandlingDVH)
                        }"
                    )
                    if (send) {
                            val nyMellomlagring = SaksstatistikkMellomlagring(
                                funksjonellId = behandlingDVH.funksjonellId,
                                kontraktVersjon = behandlingDVH.versjon,
                                json = sakstatistikkObjectMapper.writeValueAsString(behandlingDVH),
                                type = SaksstatistikkMellomlagringType.BEHANDLING,
                                typeId = behandling.id
                            )
                            saksstatistikkMellomlagringRepository.saveAndFlush(nyMellomlagring)
                            logger.info("Behandlingsstatistikk: resender offset: $i nyMellomlagring: ${nyMellomlagring.id}")
                            antallSendteBehandlinger = antallSendteBehandlinger.inc()
                        }
                    if (saksstatistikkMellomlagringRepository.findByOffsetVerdiAndType(i, SaksstatistikkMellomlagringType.BEHANDLING) == null){
                        saksstatistikkMellomlagringRepository.save(
                            SaksstatistikkMellomlagring(
                                offsetVerdi = i,
                                kontraktVersjon = versjon,
                                funksjonellId = "offset-$i",
                                json = sakstatistikkObjectMapper.writeValueAsString(behandlingJsonNode),
                                type = SaksstatistikkMellomlagringType.BEHANDLING,
                                typeId = behandlingId,
                                sendtTidspunkt = LocalDate.of(1970, 1, 1).atStartOfDay()
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.warn("Behandlingsstatistikk: Noe gikk galt ved konvertering av offset $i", e)
                    antallFeil = antallFeil.inc()
                }

                if (behandlingerKonvertertCounter.containsKey(versjon)) {
                    behandlingerKonvertertCounter[versjon] = behandlingerKonvertertCounter[versjon]!!.inc()
                } else {
                    behandlingerKonvertertCounter[versjon] = 1
                }
            } else {
                antallIgnorerteManglerBehandling = antallIgnorerteManglerBehandling.inc()
            }
        }

        return SaksstatistikkResendtResponse(
            antallSendteBehandlinger,
            antallIgnorerteManglerBehandling,
            behandlingerKonvertertCounter,
            antallFeil
        )
    }


    @GetMapping(path = ["/behandling/konverter/{offset}"])
    fun konvertertBehandlingMeldingMedOffset(@PathVariable offset: Long): BehandlingDVH {
        val behandlingJsonNode = statistikkClient.hentBehandlingStatistikk(offset)
        val behandlingId = behandlingJsonNode.path("behandlingId").asLong()

        val behandling = forsøkHentBehandling(behandlingId)

        if (behandling != null) {
            return saksstatistikkConverterService.konverterBehandlingTilSisteKontraktVersjon(behandlingJsonNode)
        }

        error("Fant ikke behandling med id $behandlingId")
    }

    private fun forsøkHentBehandling(behandlingId: Long): Behandling? {
        return try {
            behandlingRepository.finnBehandling(behandlingId)
        } catch (e: Exception) {
            null
        }
    }

    data class SaksstatistikkResendtResponse(
        var antallSendteSaker: Int,
        val antallIgnorerte: Int,
        val antallAvHverVersjonKonvertert: Map<String, Int>,
        val antallFeil: Int,
    )
}