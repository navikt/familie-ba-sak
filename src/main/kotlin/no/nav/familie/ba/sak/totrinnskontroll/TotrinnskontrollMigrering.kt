package no.nav.familie.ba.sak.totrinnskontroll

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TotrinnskontrollMigrering(
        private val behandlingRepository: BehandlingRepository,
        private val totrinnskontrollService: TotrinnskontrollService,
        private val vedtakService: VedtakService
) {

    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    fun migrerTotrinnskontroll() {
        LOG.info("Flytter data fra totrinnskontroll til ny tabell")

        var migrerteTotrinnskontrollBehandlinger = 0
        val behandlinger = behandlingRepository.findAll()
        behandlinger.forEach {behandling ->
            runCatching {
                val verdi = behandling.steg.rekkefølge >= StegType.BESLUTTE_VEDTAK.rekkefølge
            }.onFailure { LOG.warn("If for behandling $it feilet.", it) }

            runCatching {
                if (behandling.steg.rekkefølge >= StegType.BESLUTTE_VEDTAK.rekkefølge) {
                    val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                    val aktivTotrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id)

                    if (vedtak != null && aktivTotrinnskontroll == null) {
                        val godkjent = behandling.status == BehandlingStatus.FERDIGSTILT

                        totrinnskontrollService.lagreEllerOppdater(Totrinnskontroll(
                                behandling = behandling,
                                saksbehandler = vedtak.ansvarligSaksbehandler,
                                beslutter = vedtak.ansvarligBeslutter,
                                godkjent = godkjent
                        ))
                        migrerteTotrinnskontrollBehandlinger++
                    }
                }
            }.onFailure { LOG.info("Migrering for behandling $behandling feilet.", it) }
        }

        LOG.info("Fant ${behandlinger.size} behandlinger og flyttet data fra totrinnskontroll " +
                 "til ny tabell for $migrerteTotrinnskontrollBehandlinger behandlinger")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)
    }
}