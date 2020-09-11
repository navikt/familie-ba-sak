package no.nav.familie.ba.sak.behandling.steg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedClient
import no.nav.familie.ba.sak.infotrygd.domene.InfotrygdVedtakFeedDto
import no.nav.familie.ba.sak.logg.LoggService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class FerdigstillBehandling(
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val behandlingResultatService: BehandlingResultatService,
        private val loggService: LoggService
) : BehandlingSteg<String> {

    private val antallBehandlingResultatTyper: Map<BehandlingResultatType, Counter> = BehandlingResultatType.values().map {
        it to Metrics.counter("behandling.resultat", "type",
                              it.name,
                              "beskrivelse",
                              it.displayName)
    }.toMap()

    private val behandlingstid: DistributionSummary = Metrics.summary("behandling.tid")

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: String): StegType {
        LOG.info("Forsøker å ferdigstille behandling ${behandling.id}")

        val fagsak = behandling.fagsak
        val behandlingResultatType = behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandling.id)

        if (behandling.status !== BehandlingStatus.IVERKSETTER_VEDTAK) {
            error("Prøver å ferdigstille behandling ${behandling.id}, men status er ${behandling.status}")
        }

        loggService.opprettFerdigstillBehandling(behandling)
        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.AVSLUTTET)

        oppdaterFagsakStatus(fagsak = fagsak)

        val dagerSidenOpprettet = ChronoUnit.DAYS.between(behandling.opprettetTidspunkt, LocalDateTime.now())
        behandlingstid.record(dagerSidenOpprettet.toDouble())
        antallBehandlingResultatTyper[behandlingResultatType]?.increment()

        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun oppdaterFagsakStatus(fagsak: Fagsak) {
        val gjeldendeBehandlinger =
                behandlingService.oppdaterGjeldendeBehandlingForFremtidigUtbetaling(fagsak.id, LocalDate.now())
        if (gjeldendeBehandlinger.isNotEmpty()) {
            fagsakService.oppdaterStatus(fagsak, FagsakStatus.LØPENDE)
        } else {
            fagsakService.oppdaterStatus(fagsak, FagsakStatus.AVSLUTTET)
        }
    }

    override fun stegType(): StegType {
        return StegType.FERDIGSTILLE_BEHANDLING
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}