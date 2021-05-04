package no.nav.familie.ba.sak.saksstatistikk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.kontrakter.felles.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import javax.persistence.PersistenceException


@Service
class SaksstatistikkConverterService(
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
    private val fagsakRepository: FagsakRepository,
    private val saksstatistikkConverter: SaksstatistikkConverter
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")


    @Transactional
    fun konverterSakerTilSisteKontrakt(): SaksstatistikkConverterResponse {
        val sakerKlarTilKonvertering =
            saksstatistikkMellomlagringRepository.finnAlleSomIkkeErResendt(SaksstatistikkMellomlagringType.SAK)

        logger.info("Starter konvertering av ${sakerKlarTilKonvertering.size} saker. dryRun=${skalSendeTilKafka()}")
        val nyesteKontraktversjon =
            hentPropertyFraMaven("familie.kontrakter.saksstatistikk") ?: error("Fant ikke nyeste versjonsnummer for kontrakt")

        var konverterteSakerPublisert = 0
        var manglendeFagsak = 0
        var sakerKonvertertCounter = mutableMapOf<String, Int>()
        sakerKlarTilKonvertering.forEach {

            val sakDvhFraMellomlagring: JsonNode = sakstatistikkObjectMapper.readTree(it.json)
            val sakId = sakDvhFraMellomlagring.path("sakId").asText()
            val fagsak = fagsakRepository.finnFagsak(sakId.toLong())
            if (fagsak != null) {
                secureLogger.info("Sak klar til konvertering: ${it.json}")
                val konvertertSak = saksstatistikkConverter.konverterSakTilSisteKontraktVersjon(it.json)
                secureLogger.info("Sak konvertert: ${sakstatistikkObjectMapper.writeValueAsString(konvertertSak)}")
                if (skalSendeTilKafka()) {
                    val nyMellomlagring = SaksstatistikkMellomlagring(
                        funksjonellId = konvertertSak.funksjonellId,
                        kontraktVersjon = nyesteKontraktversjon,
                        json = sakstatistikkObjectMapper.writeValueAsString(konvertertSak),
                        type = SaksstatistikkMellomlagringType.SAK,
                        typeId = fagsak.id
                    )
                    saksstatistikkMellomlagringRepository.save(nyMellomlagring)

                    it.konvertertTidspunkt = LocalDateTime.now()
                    saksstatistikkMellomlagringRepository.save(it)

                    konverterteSakerPublisert = konverterteSakerPublisert.inc()
                }

                if (sakerKonvertertCounter.containsKey(it.kontraktVersjon)) {
                    sakerKonvertertCounter[it.kontraktVersjon] = sakerKonvertertCounter.get(it.kontraktVersjon)!!.inc()
                } else {
                    sakerKonvertertCounter[it.kontraktVersjon] = 1
                }
            } else {
                logger.warn("Skipper konvertering av ${sakId} fordi hendelse mangler fagsak i databasen")
                manglendeFagsak = manglendeFagsak.inc()
            }

        }

        logger.info(
            "\nAntall saker klar for konvertering: ${sakerKlarTilKonvertering.size}\n" +
                    "Konvertert følgende skjemaer: $sakerKonvertertCounter  \n" +
                    "Totalt sendte meldinger: $konverterteSakerPublisert \n" +
                    "Saker som det mangler fagsak på: $manglendeFagsak \n"
        )

        return SaksstatistikkConverterResponse(
            sakerKlarTilKonvertering.size,
            konverterteSakerPublisert,
            manglendeFagsak,
            sakerKonvertertCounter
        )
    }

    data class SaksstatistikkConverterResponse(
        val antallKlarTilKonvertering: Int,
        val antallSendtTilKafka: Int,
        val antallIkkeSendt: Int,
        val antallAvHverVersjonKonvertert: Map<String, Int>
    )

    @Transactional
    fun konverterBehandlingTilSisteKontrakt(): SaksstatistikkConverterResponse {
        val behandlingerKlarTilKonvertering =
            saksstatistikkMellomlagringRepository.finnAlleSomIkkeErResendt(SaksstatistikkMellomlagringType.BEHANDLING)
        logger.info("Starter konvertering av ${behandlingerKlarTilKonvertering.size} behandlinger. enablet=${skalSendeTilKafka()}")
        val nyesteKontraktversjon =
            hentPropertyFraMaven("familie.kontrakter.saksstatistikk") ?: error("Fant ikke nyeste versjonsnummer for kontrakt")

        var konverterteBehandlingerPublisert = 0
        val resendteBehandlingerPublisert = 0
        var manglendeBehandling = 0
        val behandlingerKonvertertCounter = mutableMapOf<String, Int>()

        behandlingerKlarTilKonvertering.forEach {
            val behandlingDvhFraMellomlagring: JsonNode = sakstatistikkObjectMapper.readTree(it.json)
            val behandlingId = behandlingDvhFraMellomlagring.path("behandlingId").asText()

            try {
                val behandling = behandlingService.hent(behandlingId.toLong())

                secureLogger.info("Behandling klar til konvertering: ${it.json}")

                val behandlingDVH = saksstatistikkConverter.konverterBehandlingTilSisteKontraktversjon(it, behandling)
                secureLogger.info(
                    "Behandling konvertert til: ${
                        sakstatistikkObjectMapper.writeValueAsString(
                            behandlingDVH
                        )
                    }"
                )

                if (skalSendeTilKafka()) {

                    val saksstatistikkMellomlagring = SaksstatistikkMellomlagring(
                        funksjonellId = behandlingDVH.funksjonellId,
                        kontraktVersjon = nyesteKontraktversjon,
                        json = sakstatistikkObjectMapper.writeValueAsString(behandlingDVH),
                        type = SaksstatistikkMellomlagringType.BEHANDLING
                    )
                    saksstatistikkMellomlagringRepository.save(saksstatistikkMellomlagring)
                    konverterteBehandlingerPublisert = konverterteBehandlingerPublisert.inc()

                    it.konvertertTidspunkt = LocalDateTime.now()
                    saksstatistikkMellomlagringRepository.save(it)
                }

                if (behandlingerKonvertertCounter.containsKey(it.kontraktVersjon)) {
                    behandlingerKonvertertCounter[it.kontraktVersjon] = behandlingerKonvertertCounter.get(it.kontraktVersjon)!!.inc()
                } else {
                    behandlingerKonvertertCounter[it.kontraktVersjon] = 1
                }
            } catch (e: PersistenceException) {
                logger.warn("Skipper konvertering av ${behandlingId} fordi hendelse mangler behandling i databasen")
                manglendeBehandling = manglendeBehandling.inc()
            }
        }

        logger.info(
            "\nAntall behandlinger klar for konvertering: ${behandlingerKlarTilKonvertering.size}\n" +
                    "Konvertert følgende skjemaer: $behandlingerKonvertertCounter  \n" +
                    "Totalt sendte meldinger: $resendteBehandlingerPublisert \n" +
                    "Hendelser som det mangler behandlinger på: $manglendeBehandling \n"
        )

        return SaksstatistikkConverterResponse(
            behandlingerKlarTilKonvertering.size,
            konverterteBehandlingerPublisert,
            manglendeBehandling,
            behandlingerKonvertertCounter
        )
    }

    private fun skalSendeTilKafka(): Boolean {
        return featureToggleService.isEnabled("familie-ba-sak.skal-konvertere-saksstatistikk", false)
    }


    companion object {

        val sakstatistikkObjectMapper: ObjectMapper = objectMapper.copy()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
    }
}