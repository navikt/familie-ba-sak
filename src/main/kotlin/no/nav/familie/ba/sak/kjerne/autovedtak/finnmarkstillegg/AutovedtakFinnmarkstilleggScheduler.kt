package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import no.nav.familie.ba.sak.config.FeatureToggle.KJØRING_AUTOVEDTAK_FINNMARKSTILLEGG
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene.FinnmarkstilleggKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene.FinnmarkstilleggKjøringRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringScheduler.Companion.CRON_HVERT_10_MIN_UKEDAG
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AutovedtakFinnmarkstilleggScheduler(
    private val leaderClientService: LeaderClientService,
    private val fagsakRepository: FagsakRepository,
    private val unleashService: UnleashNextMedContextService,
    private val opprettTaskService: OpprettTaskService,
    private val finnmarkstilleggKjøringRepository: FinnmarkstilleggKjøringRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val pdlRestClient: SystemOnlyPdlRestClient,
) : ApplicationListener<ContextClosedEvent> {
    @Volatile private var isShuttingDown = false

    @Scheduled(cron = CRON_HVERT_10_MIN_UKEDAG)
    fun triggAutovedtakFinnmarkstillegg() {
        if (unleashService.isEnabled(KJØRING_AUTOVEDTAK_FINNMARKSTILLEGG)) {
            opprettTaskerForAutovedtakFinnmarkstillegg(1000)
        }
    }

    private fun opprettTaskerForAutovedtakFinnmarkstillegg(antallFagsaker: Int) {
        if (!isShuttingDown && leaderClientService.isLeader()) {
            var antallBehandlingerStartet = 0
            var startSide = 0
            while (antallBehandlingerStartet < antallFagsaker) {
                val page = fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(Pageable.ofSize(antallFagsaker))
                val fagsaker = page.toList()

                val fagsakerMedPersonidenter =
                    fagsaker
                        .filter {
                            finnmarkstilleggKjøringRepository.findByFagsakId(it) == null
                        }.onEach {
                            finnmarkstilleggKjøringRepository.save(FinnmarkstilleggKjøring(fagsakId = it))
                        }.mapNotNull { fagsakId ->
                            behandlingHentOgPersisterService
                                .hentSisteBehandlingSomErIverksatt(fagsakId = fagsakId)
                                ?.let { fagsakId to it }
                        }.mapNotNull { (fagsakId, behandling) ->
                            persongrunnlagService
                                .hentAktiv(behandlingId = behandling.id)
                                ?.let { fagsakId to it.personer.map { person -> person.aktør.aktivFødselsnummer() } }
                        }.toMap()

                val personerSomBorIFinnmarkEllerNordTroms =
                    fagsakerMedPersonidenter.values
                        .flatten()
                        .distinct()
                        .chunked(1000)
                        .flatMap { personer ->
                            pdlRestClient
                                .hentBostedsadresseOgDeltBostedForPersoner(personer)
                                .filterValues { it.nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms() }
                                .keys
                        }

                val fagsakerDerMinstEnAktørBorIFinnmarkEllerNordTroms =
                    fagsakerMedPersonidenter
                        .filterValues { personerSomBorIFinnmarkEllerNordTroms.intersect(it).isNotEmpty() }
                        .keys

                antallBehandlingerStartet +=
                    fagsakerDerMinstEnAktørBorIFinnmarkEllerNordTroms
                        .onEach { opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(it) }
                        .size

                if (++startSide >= page.totalPages) break
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AutovedtakFinnmarkstilleggScheduler::class.java)
    }

    override fun onApplicationEvent(event: ContextClosedEvent) {
        isShuttingDown = true
    }
}
