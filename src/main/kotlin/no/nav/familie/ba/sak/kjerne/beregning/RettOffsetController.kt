package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.security.token.support.core.api.ProtectedWithClaims
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

    @PostMapping("/simuler")
    @Transactional
    fun simuler() {
        val input = RettOffsetIAndelTilkjentYtelseDto(
            simuler = true,
            behandlinger = behandlingRepository.finnBehandlingerMedDuplikateOffsetsForAndelTilkjentYtelse()
        )
        task.doTask(
            Task(
                type = RettOffsetIAndelTilkjentYtelseTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(input)
            )
        )
        throw RuntimeException("Kaster exception her for å sikre at ingenting frå transaksjonen blir committa")
    }
}
