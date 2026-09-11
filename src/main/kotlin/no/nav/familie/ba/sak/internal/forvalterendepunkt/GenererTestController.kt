package no.nav.familie.ba.sak.internal.forvalterendepunkt

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.internal.TestVerktøyService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter/generer-test")
class GenererTestController(
    private val tilgangService: TilgangService,
    private val testVerktøyService: TestVerktøyService,
) {
    @GetMapping(path = ["/behandling/{behandlingId}/begrunnelsetest"])
    fun hentBegrunnelsetestPåBehandling(
        @PathVariable behandlingId: Long,
        @RequestParam("testType") testType: TestType = TestType.BEGRUNNELSETEST,
    ): String {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Hente begrunnelsetest",
        )

        return when (testType) {
            TestType.BEGRUNNELSETEST -> {
                testVerktøyService
                    .hentBegrunnelsetest(behandlingId)
                    .replace("\n", System.lineSeparator())
            }

            TestType.VEDTAKSPERIODERTEST -> {
                testVerktøyService
                    .hentVedtaksperioderTest(behandlingId)
                    .replace("\n", System.lineSeparator())
            }
        }
    }
}

enum class TestType {
    BEGRUNNELSETEST,
    VEDTAKSPERIODERTEST,
}
