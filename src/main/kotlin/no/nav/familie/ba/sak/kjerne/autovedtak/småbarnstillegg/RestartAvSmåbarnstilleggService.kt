package no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.erAlleredeBegrunnetMedBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class RestartAvSmåbarnstilleggService(
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val opprettTaskService: OpprettTaskService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val behandlingService: BehandlingService,
) {

    /**
     * Opprette "vurder livshendelse"-oppgave når det oppstår en restart av småbarnstillegg, første dag hver måned
     */
    @Scheduled(cron = "0 0 7 1 * *")
    @Transactional
    fun scheduledFinnRestartetSmåbarnstilleggOgOpprettOppgave() {
        finnAlleFagsakerMedRestartetSmåbarnstillegg().forEach { fagsakId ->
            val sisteIverksatteBehandling = behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsakId)

            if (sisteIverksatteBehandling != null) {
                opprettTaskService.opprettOppgaveTask(
                    behandlingId = sisteIverksatteBehandling.id,
                    oppgavetype = Oppgavetype.VurderLivshendelse,
                    beskrivelse = "Småbarnstillegg har startet opp igjen denne måneden"
                )
            }
        }
    }

    private fun finnAlleFagsakerMedRestartetSmåbarnstillegg(): List<Long> {
        return fagsakRepository.finnAlleFagsakerMedOppstartSmåbarnstilleggIMåned(
            iverksatteLøpendeBehandlinger = behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker()
        ).filter { fagsakId ->
            !periodeMedRestartetSmåbarnstilleggErAlleredeBegrunnet(fagsakId = fagsakId)
        }
    }

    private fun periodeMedRestartetSmåbarnstilleggErAlleredeBegrunnet(fagsakId: Long): Boolean {
        val vedtaksperioderForVedtatteBehandlinger = behandlingService.hentBehandlinger(fagsakId = fagsakId)
            .filter { behandling ->
                behandling.erVedtatt()
            }
            .flatMap { behandling ->
                val vedtak = vedtakService.hentAktivForBehandlingThrows(behandling.id)
                vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
            }

        val standardbegrunnelser = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGET_SMÅBARNSTILLEGG)

        return vedtaksperioderForVedtatteBehandlinger.erAlleredeBegrunnetMedBegrunnelse(
            standardbegrunnelser = standardbegrunnelser,
            måned = YearMonth.now()
        )
    }
}