package no.nav.familie.ba.sak.behandling

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.opplysningsplikt.OpplysningspliktRepository
import no.nav.familie.ba.sak.opplysningsplikt.OpplysningspliktStatus
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
class BehandlingMetrikker(
        private val behandlingResultatService: BehandlingResultatService,
        private val vedtakRepository: VedtakRepository,
        private val opplysningspliktRepository: OpplysningspliktRepository
) {

    private val antallAutomatiskeBehandlinger: Counter = Metrics.counter("behandling.automatiske.behandlinger")
    private val antallManuelleBehandlinger: Counter = Metrics.counter("behandling.manuelle.behandlinger")

    private val antallManuelleBehandlingerOpprettet: Map<BehandlingType, Counter> = initBehandlingTypeMetrikker("manuell")
    private val antallAutomatiskeBehandlingerOpprettet: Map<BehandlingType, Counter> = initBehandlingTypeMetrikker("automatisk")
    private val behandlingÅrsak: Map<BehandlingÅrsak, Counter> = initBehandlingÅrsakMetrikker()


    private val opplysningspliktStatus: Map<OpplysningspliktStatus, Counter> = initOpplysningspliktStatusMetrikker()

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

    fun tellNøkkelTallVedOpprettelseAvBehandling(behandling: Behandling) {
        if (behandling.skalBehandlesAutomatisk) {
            antallAutomatiskeBehandlingerOpprettet[behandling.type]?.increment()
            antallAutomatiskeBehandlinger.increment()
        } else {
            antallManuelleBehandlingerOpprettet[behandling.type]?.increment()
            antallManuelleBehandlinger.increment()
        }

        behandlingÅrsak[behandling.opprettetÅrsak]?.increment()
    }

    fun oppdaterBehandlingMetrikker(behandling: Behandling) {
        tellBehandlingstidMetrikk(behandling)
        økBehandlingResultatTypeMetrikk(behandling)
        økBegrunnelseMetrikk(behandling)
        økOpplysningspliktStatuseMetrikk(behandling)
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
        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
                     ?: error("Finner ikke aktivt vedtak på behandling ${behandling.id}")
        vedtak.utbetalingBegrunnelser.mapNotNull { it.vedtakBegrunnelse }
                .forEach { brevbegrunelse: VedtakBegrunnelse -> antallBrevBegrunnelser[brevbegrunelse]?.increment() }
    }

    private fun økOpplysningspliktStatuseMetrikk(behandling: Behandling) {
        val opplysningsplikt = opplysningspliktRepository.findByBehandlingId(behandling.id)
        if (opplysningsplikt != null) {
            opplysningspliktStatus[opplysningsplikt.status]?.increment()
        }
    }

    private fun initBehandlingTypeMetrikker(type: String): Map<BehandlingType, Counter> {
        return BehandlingType.values().map {
            it to Metrics.counter("behandling.opprettet.$type", "type",
                                  it.name,
                                  "beskrivelse",
                                  it.visningsnavn)
        }.toMap()
    }

    private fun initBehandlingÅrsakMetrikker(): Map<BehandlingÅrsak, Counter> {
        return BehandlingÅrsak.values().map {
            it to Metrics.counter("behandling.aarsak",
                                  "aarsak",
                                  it.name,
                                  "beskrivelse",
                                  it.visningsnavn)
        }.toMap()
    }

    private fun initOpplysningspliktStatusMetrikker(): Map<OpplysningspliktStatus, Counter> {
        return OpplysningspliktStatus.values().map {
            it to Metrics.counter("behandling.opplysningsplikt",
                                  "status",
                                  it.name,
                                  "beskrivelse",
                                  it.visningsTekst)
        }.toMap()
    }
}