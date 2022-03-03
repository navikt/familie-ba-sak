package no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.erAlleredeBegrunnetMedBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class RestartAvSmåbarnstilleggService(
    private val fagsakRepository: FagsakRepository,
    private val opprettTaskService: OpprettTaskService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val behandlingService: BehandlingService,
) {

    /**
     * Første dag hver måned sjekkes det om noen fagsaker har oppstart av småbarnstillegg inneværende måned, etter å ha
     * hatt et opphold. Hvis perioden ikke allerede er begrunnet, skal det opprettes en "vurder livshendelse"-oppgave
     */
    @Scheduled(cron = "0 0 7 1 * *")
    @Transactional
    fun scheduledFinnRestartetSmåbarnstilleggOgOpprettOppgave() {
        finnAlleFagsakerMedRestartetSmåbarnstilleggIMåned().forEach { fagsakId ->
            logger.info("Oppretter 'vurder livshendelse'-oppgave på fagsak $fagsakId fordi småbarnstillegg har startet opp igjen denne måneden")

            val sisteIverksatteBehandling = behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsakId)

            if (sisteIverksatteBehandling != null) {
                opprettTaskService.opprettOppgaveTask(
                    behandlingId = sisteIverksatteBehandling.id,
                    oppgavetype = Oppgavetype.VurderLivshendelse,
                    beskrivelse = "Småbarnstillegg: endring i overgangsstønad må behandles manuelt"
                )
            }
        }
    }

    fun finnAlleFagsakerMedRestartetSmåbarnstilleggIMåned(måned: YearMonth = YearMonth.now()): List<Long> {
        return behandlingService.partitionByIverksatteBehandlinger {
            fagsakRepository.finnAlleFagsakerMedOppstartSmåbarnstilleggIMåned(
                iverksatteLøpendeBehandlinger = it,
                stønadFom = måned
            )
        }.filter { fagsakId ->
            !periodeMedRestartetSmåbarnstilleggErAlleredeBegrunnet(fagsakId = fagsakId, måned = måned)
        }
    }

    private fun periodeMedRestartetSmåbarnstilleggErAlleredeBegrunnet(fagsakId: Long, måned: YearMonth): Boolean {
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
            måned = måned
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RestartAvSmåbarnstilleggService::class.java)
    }
}
