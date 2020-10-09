package no.nav.familie.ba.sak.behandling

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.*
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
class BehandlingMetrikker(
        private val behandlingResultatService: BehandlingResultatService,
        private val vedtakService: VedtakService
) {

    private val antallBehandlingResultatTyper: Map<BehandlingResultatType, Counter> =
            BehandlingResultatType.values().map {
                it to Metrics.counter("behandling.resultat",
                                      "type", it.name,
                                      "beskrivelse", it.displayName)
            }.toMap()

    private val antallBrevBegrunnelser: Map<VedtakBegrunnelse, Counter> =
            VedtakBegrunnelse.values().map {
                it to Metrics.counter("brevbegrunnelse",
                                      "type", it.name,
                                      "beskrivelse", it.tittel)
            }.toMap()

    private val behandlingstid: DistributionSummary = Metrics.summary("behandling.tid")

    fun oppdaterBehandlingMetrikker(behandling: Behandling) {
        tellBehandlingstidMetrikk(behandling)
        økBehandlingResultatTypeMetrikk(behandling)
        økBegrunnelseMetrikk(behandling)
    }

    private fun tellBehandlingstidMetrikk(behandling: Behandling) {
        val dagerSidenOpprettet = ChronoUnit.DAYS.between(behandling.opprettetTidspunkt, LocalDateTime.now())
        behandlingstid.record(dagerSidenOpprettet.toDouble())
    }

    private fun økBehandlingResultatTypeMetrikk(behandling: Behandling) {
        val behandlingResultatType = behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandling)
        antallBehandlingResultatTyper[behandlingResultatType]?.increment()
    }

    private fun økBegrunnelseMetrikk(behandling: Behandling) {
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                     ?: error("Finner ikke aktivt vedtak på behandling ${behandling.id}")
        vedtak.utbetalingBegrunnelser.mapNotNull { it.behandlingresultatOgVilkårBegrunnelse }
                .forEach { brevbegrunelse: VedtakBegrunnelse -> antallBrevBegrunnelser[brevbegrunelse]?.increment() }
    }
}