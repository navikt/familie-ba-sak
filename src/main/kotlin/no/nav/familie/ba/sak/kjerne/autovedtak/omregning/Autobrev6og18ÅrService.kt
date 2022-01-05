package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakUtils
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class Autobrev6og18ÅrService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val taskRepository: TaskRepositoryWrapper,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val autovedtakService: AutovedtakService
) {

    @Transactional
    fun opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO) {

        logger.info("opprettOmregningsoppgaveForBarnIBrytingsalder for fagsak ${autobrev6og18ÅrDTO.fagsakId}")

        val behandling =
            behandlingService.hentAktivForFagsak(autobrev6og18ÅrDTO.fagsakId) ?: error("Fant ikke aktiv behandling")

        if (behandling.fagsak.status != FagsakStatus.LØPENDE) {
            logger.info("Fagsak ${behandling.fagsak.id} har ikke status løpende, og derfor prosesseres den ikke videre.")
            return
        }

        if (behandlingService.harBehandlingsårsakAlleredeKjørt(
                fagsakId = autobrev6og18ÅrDTO.fagsakId,
                behandlingÅrsak = finnBehandlingÅrsakForAlder(autobrev6og18ÅrDTO.alder),
                måned = YearMonth.now()
            )
        ) {
            logger.info("Fagsak ${behandling.fagsak.id} ${autobrev6og18ÅrDTO.alder} års omregningsbrev brev allerede sendt")
            return
        }

        if (barnAlleredeBegrunnetPåFagsak(autobrev6og18ÅrDTO)) {
            logger.info("Fagsak ${behandling.fagsak.id} ${autobrev6og18ÅrDTO.alder} års omregningsbrev brev allerede sendt")
            return
        }

        if (!barnMedAngittAlderInneværendeMånedEksisterer(
                behandlingId = behandling.id,
                alder = autobrev6og18ÅrDTO.alder
            )
        ) {
            logger.warn("Fagsak ${behandling.fagsak.id} har ikke noe barn med alder ${autobrev6og18ÅrDTO.alder} ")
            return
        }

        if (barnetrygdOpphører(autobrev6og18ÅrDTO, behandling)) {
            logger.info("Fagsak ${behandling.fagsak.id} har ikke barn under 18 år og vil opphøre.")
            return
        }

        if (behandling.status != BehandlingStatus.AVSLUTTET) {
            error("Kan ikke opprette ny behandling for fagsak ${behandling.fagsak.id} ettersom den allerede har en åpen behanding.")
        }

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                fagsak = behandling.fagsak,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = finnBehandlingÅrsakForAlder(
                    autobrev6og18ÅrDTO.alder
                )
            )

        vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(
            vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingEtterBehandlingsresultat.id),
            vedtakBegrunnelseSpesifikasjon = AutobrevUtils.hentGjeldendeVedtakbegrunnelseReduksjonForAlder(
                autobrev6og18ÅrDTO.alder
            )
        )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat
            )

        opprettTaskJournalførVedtaksbrev(vedtakId = opprettetVedtak.id)
    }

    private fun barnetrygdOpphører(
        autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO,
        behandling: Behandling
    ) =
        autobrev6og18ÅrDTO.alder == Alder.ATTEN.år &&
            !barnUnder18årInneværendeMånedEksisterer(behandlingId = behandling.id)

    private fun finnBehandlingÅrsakForAlder(alder: Int): BehandlingÅrsak =
        when (alder) {
            Alder.SEKS.år -> BehandlingÅrsak.OMREGNING_6ÅR
            Alder.ATTEN.år -> BehandlingÅrsak.OMREGNING_18ÅR
            else -> throw Feil("Alder må være oppgitt til enten 6 eller 18 år.")
        }

    private fun barnAlleredeBegrunnetPåFagsak(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO): Boolean {
        val vedtaksBegrunnelserForReduksjon =
            AutobrevUtils.hentStandardbegrunnelserReduksjonForAlder(autobrev6og18ÅrDTO.alder)

        return FagsakUtils.fagsakBegrunnetMedBegrunnelse(
            vedtaksperiodeMedBegrunnelser = behandlingService.hentBehandlinger(fagsakId = autobrev6og18ÅrDTO.fagsakId)
                .filter { it.status == BehandlingStatus.AVSLUTTET }
                .flatMap { behandling ->
                    val vedtak = vedtakService.hentAktivForBehandlingThrows(behandling.id)
                    vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
                },
            standardbegrunnelser = vedtaksBegrunnelserForReduksjon,
            måned = YearMonth.now()
        )
    }

    private fun barnMedAngittAlderInneværendeMånedEksisterer(behandlingId: Long, alder: Int): Boolean =
        barnMedAngittAlderInneværendeMåned(behandlingId, alder).isNotEmpty()

    private fun barnMedAngittAlderInneværendeMåned(behandlingId: Long, alder: Int): List<Person> =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.personer
            ?.filter { it.type == PersonType.BARN && it.fyllerAntallÅrInneværendeMåned(alder) }?.toList() ?: listOf()

    private fun barnUnder18årInneværendeMånedEksisterer(behandlingId: Long): Boolean =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.personer
            ?.any { it.type == PersonType.BARN && it.erYngreEnnInneværendeMåned(Alder.ATTEN.år) } ?: false

    private fun opprettTaskJournalførVedtaksbrev(vedtakId: Long) {
        val task = Task(
            JournalførVedtaksbrevTask.TASK_STEP_TYPE,
            "$vedtakId"
        )
        taskRepository.save(task)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(Autobrev6og18ÅrService::class.java)
    }
}

enum class Alder(val år: Int) {
    SEKS(år = 6),
    ATTEN(år = 18)
}
