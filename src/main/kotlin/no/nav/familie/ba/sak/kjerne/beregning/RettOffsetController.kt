package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/periodeoffset")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class RettOffsetController(val task: RettOffsetIAndelTilkjentYtelseTask) {

    @GetMapping("/simuler")
    fun simuler() {
        val input = RettOffsetIAndelTilkjentYtelseDto(simuler = true, kunSiste = true)
        task.doTask(
            Task(
                type = RettOffsetIAndelTilkjentYtelseTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(input)
            )
        )
    }
}
