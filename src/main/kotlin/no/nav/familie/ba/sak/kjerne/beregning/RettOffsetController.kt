package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
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
    val behandlingRepository: BehandlingRepository,
    val taskRepository: TaskRepository,
    val beregningService: BeregningService
) {

    @PostMapping("/simuler-offset-fix")
    @Transactional
    fun simuler() {
        val behandlinger = finnBehandlingerMedNullEllerDuplikatOffset()
        val input = RettOffsetIAndelTilkjentYtelseDto(
            simuler = true,
            behandlinger = behandlinger
        )
        task.simulerEllerRettOffsettForBehandlingerMedNullEllerDuplikatOffset(input)
        throw RuntimeException("Kaster exception her for å sikre at ingenting frå transaksjonen blir committa")
    }

    @PostMapping("/rett-offset")
    @Transactional
    fun rettOffsetfeil() {
        val input = RettOffsetIAndelTilkjentYtelseDto(
            simuler = false,
            behandlinger = finnBehandlingerMedNullEllerDuplikatOffset()
        )
        task.simulerEllerRettOffsettForBehandlingerMedNullEllerDuplikatOffset(input)
    }

    @PostMapping("/rett-offset-for-behandling")
    @Transactional
    fun rettOffsetfeilForBehandlinger(@RequestBody(required = true) behandlinger: List<Long>) {
        val input = RettOffsetIAndelTilkjentYtelseDto(
            simuler = false,
            behandlinger = behandlinger.toSet(),
            ignorerValidering = true
        )
        task.simulerEllerRettOffsettForBehandlingerMedNullEllerDuplikatOffset(input)
    }

    @PostMapping("/simuler-offset-for-alle-behandlinger")
    @Transactional
    fun simulerOffsetfeilForAlleBehandlinger() {
        val alleBehandlingerEndretEtterFeilOppsto = finnAlleBehandlingerEndretEtterFeilOppsto()
        var antallTaskerOpprettet: Int = 0
        alleBehandlingerEndretEtterFeilOppsto.chunked(500).forEach {
            val input = RettOffsetIAndelTilkjentYtelseDto(
                simuler = true,
                behandlinger = it.toSet()
            )

            taskRepository.save(
                Task(
                    type = RettOffsetIAndelTilkjentYtelseTask.TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(input)
                )
            )
            antallTaskerOpprettet = antallTaskerOpprettet++
            logger.info("Opprettet RettOffsetIAndelTilkjentYtelseTask nr $antallTaskerOpprettet med ${it.size} behandlinger av totalt ${alleBehandlingerEndretEtterFeilOppsto.size}")
        }
    }

    @PostMapping("/rett-offset-for-liste-behandlinger")
    @Transactional
    fun rettOffsetfeilForListeBehandlinger(@RequestBody(required = true) behandlinger: List<Long>) {
        var antallTaskerOpprettet = 0
        behandlinger.chunked(500).forEach {
            val input = RettOffsetIAndelTilkjentYtelseDto(
                simuler = false,
                behandlinger = it.toSet()
            )

            taskRepository.save(
                Task(
                    type = RettOffsetIAndelTilkjentYtelseTask.TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(input)
                )
            )
            antallTaskerOpprettet = antallTaskerOpprettet++
            logger.info("Opprettet RettOffsetIAndelTilkjentYtelseTask nr $antallTaskerOpprettet med ${it.size} behandlinger av totalt ${behandlinger.size}")
        }
    }

    @PostMapping("/rett-offset-for-alle-behandlinger")
    @Transactional
    fun rettOffsetfeilForAlleBehandlinger() {
        val alleBehandlingerEndretEtterFeilOppsto = finnAlleBehandlingerEndretEtterFeilOppsto()
        var antallTaskerOpprettet = 0
        alleBehandlingerEndretEtterFeilOppsto.chunked(500).forEach {
            val input = RettOffsetIAndelTilkjentYtelseDto(
                simuler = false,
                behandlinger = it.toSet()
            )

            taskRepository.save(
                Task(
                    type = RettOffsetIAndelTilkjentYtelseTask.TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(input)
                )
            )
            antallTaskerOpprettet = antallTaskerOpprettet++
            logger.info("Opprettet RettOffsetIAndelTilkjentYtelseTask nr $antallTaskerOpprettet med ${it.size} behandlinger av totalt ${alleBehandlingerEndretEtterFeilOppsto.size}")
        }
    }

    @PostMapping("/revisjon-av-utbetalingsoppdrag")
    fun kjørRevisjonPåUtbetalingsoppdrag(@RequestBody(required = true) behandlinger: List<Long>) {
        logger.info("Starter revisjon av ${behandlinger.size} behandlinger:  $behandlinger")

        behandlinger.forEach { behandlingId ->
            val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
            val utbetalingsperioder = tilkjentYtelse.utbetalingsperioder()

            if (utbetalingsperioder.isEmpty()) {
                logger.warn("Behandling $behandlingId mangler utbettalingsoppdrag")
            }

            if (harFeilUtbetalingsoppdragMhpAndeler(tilkjentYtelse)) {
                logger.warn("Revisjon av utbetalingsoppdrag for behandling $behandlingId feilet")
                secureLogger.warn(
                    "Revisjon av utbetalingsoppdrag for behandling $behandlingId feilet\n" +
                        "Utbetalingsperioder: \n" +
                        "${utbetalingsperioder.map { objectMapper.writeValueAsString(it) + "\n" }}" +
                        "Andeler: \n" +
                        "${tilkjentYtelse.andelerTilkjentYtelse.map { it.toString() + "\n" }}"
                )
            }
        }

        logger.info("Avsluttet revisjon av ${behandlinger.size} behandlinger:  $behandlinger")
    }

    private fun finnAlleBehandlingerEndretEtterFeilOppsto(): Set<Long> {
        val ugyldigeResultater = listOf(
            HENLAGT_TEKNISK_VEDLIKEHOLD,
            HENLAGT_SØKNAD_TRUKKET,
            HENLAGT_AUTOMATISK_FØDSELSHENDELSE,
            HENLAGT_FEILAKTIG_OPPRETTET,
            AVSLÅTT,
            FORTSATT_INNVILGET
        )

        val behandlingerEtterDato = behandlingRepository.finnBehandlingerOpprettetEtterDatoForOffsetFeil(
            ugyldigeResultater = ugyldigeResultater,
            startDato = LocalDate.of(2022, 9, 5).atStartOfDay()
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
            HENLAGT_TEKNISK_VEDLIKEHOLD,
            HENLAGT_SØKNAD_TRUKKET,
            HENLAGT_AUTOMATISK_FØDSELSHENDELSE,
            HENLAGT_FEILAKTIG_OPPRETTET,
            AVSLÅTT,
            FORTSATT_INNVILGET
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
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

internal fun harFeilUtbetalingsoppdragMhpAndeler(tilkjentYtelse: TilkjentYtelse): Boolean {
    val utbetalingsperioder = tilkjentYtelse.utbetalingsperioder()

    val offsetForAndeler = tilkjentYtelse.andelerTilkjentYtelse.map { it.periodeOffset }

    val utbetalingsperioderMedOpphør = utbetalingsperioder
        .filter { it.opphør?.opphørDatoFom != null || it.erEndringPåEksisterendePeriode }
        .map { it.periodeId }

    val utbetalingsperioderSomOpprettes = utbetalingsperioder
        .filter { it.opphør == null && !it.erEndringPåEksisterendePeriode }
        .map { it.periodeId }

    val opphørtPeriodeFinnesIAndel = offsetForAndeler.intersect(utbetalingsperioderMedOpphør).isNotEmpty()
    val nyePerioderManglerIAndel = utbetalingsperioderSomOpprettes.any { !offsetForAndeler.contains(it) }

    val harEnFeil = opphørtPeriodeFinnesIAndel || nyePerioderManglerIAndel
    return harEnFeil
}

fun TilkjentYtelse.utbetalingsperioder() = this.utbetalingsoppdrag?.let {
    objectMapper.readValue(it, Utbetalingsoppdrag::class.java)
}?.utbetalingsperiode ?: emptyList()
