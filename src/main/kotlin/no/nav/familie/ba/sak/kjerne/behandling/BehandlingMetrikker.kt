package no.nav.familie.ba.sak.kjerne.behandling

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.dokument.BrevKlient
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
class BehandlingMetrikker(
        private val behandlingRepository: BehandlingRepository,
        private val vedtakRepository: VedtakRepository,
        private val vedtaksperiodeRepository: VedtaksperiodeRepository,
        private val featureToggleService: FeatureToggleService,
        private val brevKlient: BrevKlient
) {

    private val antallManuelleBehandlinger: Counter = Metrics.counter("behandling.behandlinger", "saksbehandling", "manuell")
    private val antallAutomatiskeBehandlinger: Counter =
        Metrics.counter("behandling.behandlinger", "saksbehandling", "automatisk")

    private val antallManuelleBehandlingerOpprettet: Map<BehandlingType, Counter> = initBehandlingTypeMetrikker("manuell")
    private val antallAutomatiskeBehandlingerOpprettet: Map<BehandlingType, Counter> = initBehandlingTypeMetrikker("automatisk")
    private val behandlingÅrsak: Map<BehandlingÅrsak, Counter> = initBehandlingÅrsakMetrikker()

    private val antallBehandlingResultat: Map<BehandlingResultat, Counter> =
        BehandlingResultat.values().map {
            it to Metrics.counter(
                "behandling.resultat",
                "type", it.name,
                "beskrivelse", it.displayName
            )
        }.toMap()

    private val antallBrevBegrunnelseSpesifikasjon: Map<VedtakBegrunnelseSpesifikasjon, Counter> =
            VedtakBegrunnelseSpesifikasjon.values().map {
                val tittel =
                        if (featureToggleService.isEnabled(FeatureToggleConfig.BRUK_BEGRUNNELSE_TRIGGES_AV_FRA_SANITY)) {
                            it
                                    .tilSanityBegrunnelse(brevKlient.hentSanityBegrunnelse())
                                    .navnISystem
                        } else
                            it.tittel

                it to Metrics.counter("brevbegrunnelse",
                                      "type", it.name,
                                      "beskrivelse", tittel)
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
    }

    private fun tellBehandlingstidMetrikk(behandling: Behandling) {
        val dagerSidenOpprettet = ChronoUnit.DAYS.between(behandling.opprettetTidspunkt, LocalDateTime.now())
        behandlingstid.record(dagerSidenOpprettet.toDouble())
    }

    private fun økBehandlingResultatTypeMetrikk(behandling: Behandling) {
        val behandlingResultat = behandlingRepository.finnBehandling(behandling.id).resultat
        antallBehandlingResultat[behandlingResultat]?.increment()
    }

    private fun økBegrunnelseMetrikk(behandling: Behandling) {
        if (!behandlingRepository.finnBehandling(behandling.id).erHenlagt()) {
            val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
                ?: error("Finner ikke aktivt vedtak på behandling ${behandling.id}")

            val vedtaksperiodeMedBegrunnelser = vedtaksperiodeRepository.finnVedtaksperioderFor(vedtakId = vedtak.id)

            vedtaksperiodeMedBegrunnelser.forEach {
                it.begrunnelser.forEach { vedtaksbegrunnelse: Vedtaksbegrunnelse -> antallBrevBegrunnelseSpesifikasjon[vedtaksbegrunnelse.vedtakBegrunnelseSpesifikasjon]?.increment() }
            }
        }
    }

    private fun initBehandlingTypeMetrikker(type: String): Map<BehandlingType, Counter> {
        return BehandlingType.values().map {
            it to Metrics.counter(
                "behandling.opprettet", "type",
                it.name,
                "beskrivelse",
                it.visningsnavn,
                "saksbehandling",
                type
            )
        }.toMap()
    }

    private fun initBehandlingÅrsakMetrikker(): Map<BehandlingÅrsak, Counter> {
        return BehandlingÅrsak.values().map {
            it to Metrics.counter(
                "behandling.aarsak",
                "aarsak",
                it.name,
                "beskrivelse",
                it.visningsnavn
            )
        }.toMap()
    }
}
