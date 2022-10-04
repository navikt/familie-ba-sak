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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

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
        val behandlinger = finnBehandlingerMedNullEllerDuplikatOffset()
        val input = RettOffsetIAndelTilkjentYtelseDto(
            simuler = true,
            behandlinger = behandlinger
        )
        task.doTask(
            Task(
                type = RettOffsetIAndelTilkjentYtelseTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(input)
            )
        )
        throw RuntimeException("Kaster exception her for å sikre at ingenting frå transaksjonen blir committa")
    }

    @PostMapping("/rett-offset")
    @Transactional
    fun rettOffsetfeil() {
        val input = RettOffsetIAndelTilkjentYtelseDto(
            simuler = false,
            behandlinger = finnBehandlingerMedNullEllerDuplikatOffset()
        )
        task.doTask(
            Task(
                type = RettOffsetIAndelTilkjentYtelseTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(input)
            )
        )
    }

    @PostMapping("/rett-offset-for-behandling")
    @Transactional
    fun rettOffsetfeilForBehandlinger(@RequestBody(required = true) behandlinger: List<Long>) {
        val input = RettOffsetIAndelTilkjentYtelseDto(
            simuler = false,
            behandlinger = behandlinger.toSet(),
            ignorerValidering = true
        )
        task.doTask(
            Task(
                type = RettOffsetIAndelTilkjentYtelseTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(input)
            )
        )
    }

    @PostMapping("/simuler-offset-for-alle-behandlinger")
    @Transactional
    fun simulerOffsetfeilForAlleBehandlinger(@RequestBody(required = true) behandlinger: List<Long>) {
        val input = RettOffsetIAndelTilkjentYtelseDto(
            simuler = true,
            behandlinger = finnAlleBehandlingerEndretEtterFeilOppsto()
        )
        task.kjørOffsetFix(
            Task(
                type = RettOffsetIAndelTilkjentYtelseTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(input)
            )
        )
    }

    private fun finnAlleBehandlingerEndretEtterFeilOppsto(): Set<Long> {
        val ugyldigeResultater = listOf(
            Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD,
            Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET,
            Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE,
            Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
            Behandlingsresultat.AVSLÅTT,
            Behandlingsresultat.FORTSATT_INNVILGET
        )

        val behandlingerEtterDato = behandlingRepository.finnBehandlingerOpprettetEtterDatoForOffsetFeil(
            ugyldigeResultater = ugyldigeResultater,
            startDato = LocalDate.of(2022, 9, 5),
            sluttDato = LocalDate.of(2022, 9, 30)
        )
        logger.warn(
            "Behandlinger opprettet f.o.m. 5. september 2022 (${behandlingerEtterDato.size} stk): ${
            behandlingerEtterDato.joinToString(separator = ",")
            }"
        )

        return behandlingerEtterDato.toSet()
    }

    private fun finnBehandlingerMedNullEllerDuplikatOffset(): Set<Long> {
        val ugyldigeResultater = listOf(
            Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD,
            Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET,
            Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE,
            Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
            Behandlingsresultat.AVSLÅTT,
            Behandlingsresultat.FORTSATT_INNVILGET
        )
        val behandlingerMedDuplikateOffset =
            behandlingRepository.finnBehandlingerMedDuplikateOffsetsForAndelTilkjentYtelse(ugyldigeResultater = ugyldigeResultater)
        val behandlingerMedNullOffsets = behandlingRepository.finnBehandlingerMedFeilNullOffsetsForAndelTilkjentYtelse(
            ugyldigeResultater = ugyldigeResultater
        )

        logger.warn(
            "Behandlinger med duplikate offset (${behandlingerMedDuplikateOffset.size} stk): ${
            behandlingerMedDuplikateOffset.joinToString(separator = ",")
            }"
        )

        logger.warn(
            "Behandlinger med feilaktig null-offset (${behandlingerMedNullOffsets.size} stk): ${
            behandlingerMedNullOffsets.joinToString(separator = ",")
            }"
        )

        return (behandlingerMedDuplikateOffset + behandlingerMedNullOffsets).toSet()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RettOffsetController::class.java)
    }
}
