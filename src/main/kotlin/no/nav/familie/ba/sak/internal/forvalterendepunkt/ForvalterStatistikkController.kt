package no.nav.familie.ba.sak.internal.forvalterendepunkt

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.statistikk.stønadsstatistikk.StønadsstatistikkService
import no.nav.familie.eksterne.kontrakter.UtbetalingsperiodeDVHV2
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter/statistikk")
class ForvalterStatistikkController(
    private val stønadsstatistikkService: StønadsstatistikkService,
    private val persongrunnlagService: PersongrunnlagService,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val tilgangService: TilgangService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    @PostMapping("/send-behandlingsstatistikk-til-dvh")
    fun sendBehandlingsstatistikkTilDvh(
        @RequestBody behandlingId: Long,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Sende behandlingsstatistikk for behandling til Datavarehus",
        )
        saksstatistikkEventPublisher.publiserBehandlingsstatistikk(behandlingId)
        return ResponseEntity.ok("Sendt behandlingsstatistikk for behandling $behandlingId til Datavarehus")
    }

    @GetMapping("/stonadstatistikk-utbetalingsperioder/{behandlingId}")
    fun hentStønadstatistikkUtbetalingsperioder(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<List<UtbetalingsperiodeDVHV2>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Hente utbetalingsperioder til datavarehus for behandling",
        )
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)
        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId)
        val utbetalingsperioder = stønadsstatistikkService.hentUtbetalingsperioderTilDatavarehus(behandling = behandling, persongrunnlag = persongrunnlag)

        return ResponseEntity.ok(utbetalingsperioder)
    }
}
