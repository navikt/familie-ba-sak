package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/periodeoffset")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class RettOffsetController(
    val task: RettOffsetIAndelTilkjentYtelseTask,
    val behandlingRepository: BehandlingRepository
) {

    @PostMapping("/simuler-offset-fix")
    @Transactional
    fun simuler() {
        val henlagteResultater = listOf(Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD,
            Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET,
            Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE,
            Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET
        )
        val behandlingerMedDuplikateOffset = behandlingRepository.finnBehandlingerMedDuplikateOffsetsForAndelTilkjentYtelse(ugyldigeResultater = henlagteResultater)
        val behandlingerMedNullOffsets = behandlingRepository.finnBehandlingerMedFeilNullOffsetsForAndelTilkjentYtelse(
            ugyldigeResultater = henlagteResultater + listOf(
                Behandlingsresultat.AVSLÅTT,
                Behandlingsresultat.FORTSATT_INNVILGET,
            )
        )

        logger.warn(
            "Behandlinger med duplikate offset: ${
                behandlingerMedDuplikateOffset.joinToString(separator = ",")
            }"
        )

        logger.warn(
            "Behandlinger med feilaktig null-offset: ${
                behandlingerMedNullOffsets.joinToString(separator = ",")
            }"
        )

        val input = RettOffsetIAndelTilkjentYtelseDto(
            simuler = true,
            behandlinger = (behandlingerMedDuplikateOffset + behandlingerMedNullOffsets).toSet()
        )
        task.doTask(
            Task(
                type = RettOffsetIAndelTilkjentYtelseTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(input)
            )
        )
        throw RuntimeException("Kaster exception her for å sikre at ingenting frå transaksjonen blir committa")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RettOffsetController::class.java)
    }
}
