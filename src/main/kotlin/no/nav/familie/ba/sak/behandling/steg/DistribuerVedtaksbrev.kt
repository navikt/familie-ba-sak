package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.brev.hentVedtaksbrevtype
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.dokument.domene.BrevType
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DistribuerVedtaksbrev(
        private val dokumentService: DokumentService,
        private val taskRepository: TaskRepository,
        private val featureToggleService: FeatureToggleService,
) : BehandlingSteg<DistribuerVedtaksbrevDTO> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: DistribuerVedtaksbrevDTO): StegType {
        LOG.info("Iverksetter distribusjon av vedtaksbrev med journalpostId ${data.journalpostId}")

        val toggleSuffix = dokumentService.vedtaksbrevToggelNavnSuffix(behandling)

        val loggTekst =
                if (featureToggleService.isEnabled("familie-ba-sak.bruk-ny-brevlosning.distribueringslogg-${toggleSuffix}",
                                                   false))
                    hentVedtaksbrevtype(behandling).visningsTekst
                else
                    when (behandling.resultat) {
                        BehandlingResultat.INNVILGET -> "Vedtak om innvilgelse av barnetrygd"
                        BehandlingResultat.FORTSATT_INNVILGET -> "Vedtak om forsatt innvilgelse av barnetrygd"
                        BehandlingResultat.DELVIS_INNVILGET -> "Vedtak om innvilgelse av barnetrygd"
                        BehandlingResultat.OPPHØRT -> "Vedtak er opphørt"
                        BehandlingResultat.AVSLÅTT -> "Vedtak er avslått"
                        BehandlingResultat.ENDRET -> "Vedtak er endret"
                        // TODO fikse at det er validering flere steder på hvilke resultater som er lovlige
                        else -> error("Behandlingsresultat (${behandling.resultat}) er ikke gyldig for distribusjon av vedtaksbrev.")
                    }


        dokumentService.distribuerBrevOgLoggHendelse(journalpostId = data.journalpostId,
                                                     behandlingId = data.behandlingId,
                                                     loggTekst = loggTekst,
                                                     loggBehandlerRolle = BehandlerRolle.SYSTEM,
                                                     brevType = BrevType.VEDTAK)

        val ferdigstillBehandlingTask = FerdigstillBehandlingTask.opprettTask(
                personIdent = data.personIdent,
                behandlingsId = data.behandlingId)
        taskRepository.save(ferdigstillBehandlingTask)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.DISTRIBUER_VEDTAKSBREV
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}