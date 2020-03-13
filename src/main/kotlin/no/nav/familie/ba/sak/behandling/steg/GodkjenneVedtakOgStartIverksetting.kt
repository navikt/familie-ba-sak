package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.ToTrinnKontrollService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdrag
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class GodkjenneVedtakOgStartIverksetting(
        private val toTrinnKontrollService: ToTrinnKontrollService,
        private val vedtakService: VedtakService,
        private val taskRepository: TaskRepository,
        private val loggService: LoggService
) : BehandlingSteg<String> {

    override fun utførSteg(behandling: Behandling, data: String): Behandling {
        if (behandling.status != BehandlingStatus.SENDT_TIL_BESLUTTER) {
            error("Kan ikke iverksette et vedtak som ikke er foreslått av en saksbehandler")
        }

        if (behandling.status == BehandlingStatus.LAGT_PA_KO_FOR_SENDING_MOT_OPPDRAG
            || behandling.status == BehandlingStatus.SENDT_TIL_IVERKSETTING) {
            error("Behandlingen er allerede sendt til oppdrag og venter på kvittering")
        } else if (behandling.status == BehandlingStatus.IVERKSATT || behandling.status == BehandlingStatus.FERDIGSTILT) {
            error("Behandlingen er allerede iverksatt/ferdigstilt")
        }

        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()
        toTrinnKontrollService.valider2trinnVedIverksetting(behandling, saksbehandlerId)

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                     ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        opprettTaskIverksettMotOppdrag(behandling, vedtak, saksbehandlerId)

        loggService.opprettGodkjentVedtakLogg(behandling)

        return behandling
    }

    override fun stegType(): StegType {
        return StegType.GODKJENNE_VEDTAK
    }

    override fun nesteSteg(behandling: Behandling): StegType {
        return StegType.FERDIGSTILLE_BEHANDLING
    }

    private fun opprettTaskIverksettMotOppdrag(behandling: Behandling, vedtak: Vedtak, saksbehandlerId: String) {
        val task = IverksettMotOppdrag.opprettTask(behandling, vedtak, saksbehandlerId)
        taskRepository.save(task)
    }
}