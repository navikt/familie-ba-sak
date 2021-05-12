package no.nav.familie.ba.sak.saksstatistikk

import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import no.nav.familie.ba.sak.vedtak.producer.KafkaProducer
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

@RestController
@RequestMapping("/api/saksstatistikk")
@Profile("!e2e")
@ProtectedWithClaims(issuer = "azuread")
class SaksstatistikkKafkaController(
    val kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry,
    val saksstatistikkConverterService: SaksstatistikkConverterService
) : SmartLifecycle {

    private var startSak: Boolean = false
    private var startBehandling: Boolean = false


    @GetMapping(path = ["/behandling/start"])
    fun startBehandlingConsumer(): String {
        startBehandling = true
        this.start()
        return "OK"
    }

    @GetMapping(path = ["/stop"])
    fun stopBehandlingConsumer(): String {
        startBehandling = false
        startSak = false
        this.stop()
        return "OK"
    }

    @GetMapping(path = ["/sak/start"])
    fun startSakConsumer(): String {
        startSak = true
        this.start()
        return "OK"
    }

    @PostMapping(path = ["/sak/konverter"])
    fun konverterSak(): SaksstatistikkConverterService.SaksstatistikkConverterResponse {
        return saksstatistikkConverterService.konverterSakerTilSisteKontrakt()
    }

    @PostMapping(path = ["/behandling/konverter"])
    fun konverterBehandling(): SaksstatistikkConverterService.SaksstatistikkConverterResponse {
        return saksstatistikkConverterService.konverterBehandlingTilSisteKontrakt()
    }

    override fun start() {
        if (startSak) {
            kafkaListenerEndpointRegistry.getListenerContainer(SaksstatistikkSakConsumer.SAK_CLIENT_ID).start()
        }

        if (startBehandling) {
            kafkaListenerEndpointRegistry.getListenerContainer(SaksstatistikkBehandlingConsumer.BEHANDLING_CLIENT_ID).start()
        }
    }

    override fun stop() {
        kafkaListenerEndpointRegistry.stop()
    }

    override fun isRunning(): Boolean {
        return kafkaListenerEndpointRegistry.isRunning
    }
}