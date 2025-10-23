package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnEøsFagsakerMedBarnSomBorIFinnmarkNordTromsTask.TASK_STEP_TYPE,
    beskrivelse = "Finn EØS-fagsaker der minst ett barn har bostedsadresse eller delt bosted i Finnmark/Nord-Troms",
    maxAntallFeil = 1,
)
class FinnEøsFagsakerMedBarnSomBorIFinnmarkNordTromsTask(
    private val systemOnlyPdlRestKlient: SystemOnlyPdlRestKlient,
    private val persongrunnlagService: PersongrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val startside = task.payload.toInt()

        val sisteVedtatteBehandlingPerEøsFagsak =
            behandlingHentOgPersisterService
                .hentSisteVedtatteBehandlingerFraLøpendeEøsFagsaker(PageRequest.of(startside, PAGE_SIZE))
                .toList()

        val eøsFagsakerMedBarnasIdenter =
            sisteVedtatteBehandlingPerEøsFagsak
                .associate {
                    (it.fagsakId to it.behandlingId) to
                        persongrunnlagService.hentBarna(it.behandlingId).map { barna -> barna.aktør.aktivFødselsnummer() }
                }

        val barnSomBorIFinnmarkNordTroms =
            eøsFagsakerMedBarnasIdenter
                .flatMap { it.value }
                .chunked(1000)
                .flatMap { identer ->
                    systemOnlyPdlRestKlient
                        .hentBostedsadresseOgDeltBostedForPersoner(identer)
                        .filterValues { Adresser.opprettFra(it).harAdresserSomErRelevantForFinnmarkstillegg() }
                        .keys
                }.toSet()

        val eøsFagsakerDerMinstEttBarnBorIFinnmark =
            eøsFagsakerMedBarnasIdenter
                .filterValues { barnasIdenter -> barnasIdenter.intersect(barnSomBorIFinnmarkNordTroms).isNotEmpty() }
                .keys
                .map {
                    it.first to arbeidsfordelingService.hentArbeidsfordelingPåBehandling(it.second)
                }

        eøsFagsakerDerMinstEttBarnBorIFinnmark
            .forEach { (fagsakId, behandlendeEnhet) ->
                logger.warn("EØS-fagsak har barn med bostedsadresse eller delt bosted i Finnmark/Nord-Troms, fagsak id = $fagsakId, behandlende enhet = ${behandlendeEnhet.behandlendeEnhetId}")
            }
    }

    companion object {
        const val PAGE_SIZE = 1000
        const val TASK_STEP_TYPE = "finnEØSFagsakerMedBarnSomBorIFinnmarkNordTromsTask"
        private val logger = LoggerFactory.getLogger(this::class.java)

        fun opprettTask(
            startside: Int,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = startside.toString(),
            )
    }
}
