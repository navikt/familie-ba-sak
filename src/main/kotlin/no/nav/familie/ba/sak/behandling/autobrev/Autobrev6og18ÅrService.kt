package no.nav.familie.ba.sak.behandling.autobrev

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.UtbetalingBegrunnelseRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate.now

@Service
class Autobrev6og18ÅrService(
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val persongrunnlagService: PersongrunnlagService,
        private val behandlingService: BehandlingService,
        private val stegService: StegService,
        private val vedtakService: VedtakService,
        private val taskRepository: TaskRepository,
        private val utbetalingBegrunnelseRepository: UtbetalingBegrunnelseRepository
) {

    @Transactional
    fun opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO) {

        val behandling = behandlingService.hentAktivForFagsak(autobrev6og18ÅrDTO.fagsakId) ?: error("Fant ikke aktiv behandling")

        if (behandling.fagsak.status != FagsakStatus.LØPENDE) {
            LOG.info("Fagsak ${behandling.fagsak.id} har ikke status løpende, og derfor prosesseres den ikke videre.")
            return
        }

        if (brevAlleredeSendt(autobrev6og18ÅrDTO)) {
            LOG.info("Fagsak ${behandling.fagsak.id} ${autobrev6og18ÅrDTO.alder}års omregningsbrev brev allerede sendt")
            return
        }

        if (!barnMedAngittAlderInneværendeMånedEksisterer(behandlingId = behandling.id, alder = autobrev6og18ÅrDTO.alder)) {
            LOG.warn("Fagsak ${behandling.fagsak.id} har ikke noe barn med alder ${autobrev6og18ÅrDTO.alder} ")
            return
        }

        if (barnetrygdOpphører(autobrev6og18ÅrDTO, behandling)) {
            LOG.info("Fagsak ${behandling.fagsak.id} har ikke barn under 18 år og vil opphøre.")
            return
        }

        if (behandling.status != BehandlingStatus.AVSLUTTET) {
            error("Kan ikke opprette ny behandling for fagsak ${behandling.fagsak.id} ettersom den allerede har en åpen behanding.")
        }

        val opprettetBehandling =
                stegService.håndterNyBehandling(nyBehandling = opprettNyOmregningBehandling(behandling = behandling,
                                                                                            behandlingÅrsak = finnBehandlingÅrsakForAlder(
                                                                                                    autobrev6og18ÅrDTO.alder)))

        stegService.håndterVilkårsvurdering(behandling = opprettetBehandling)

        vedtakService.leggTilUtbetalingBegrunnelsePåInneværendeUtbetalinsperiode(behandlingId = opprettetBehandling.id,
                                                                   begrunnelseType = VedtakBegrunnelseType.REDUKSJON,
                                                                   vedtakBegrunnelse = finnVedtakbegrunnelseForAlder(autobrev6og18ÅrDTO.alder),
                                                                   målform = persongrunnlagService.hentSøker(opprettetBehandling.id)?.målform ?: Målform.NB,
                                                                   barnasFødselsdatoer = barnMedAngittAlderInneværendeMåned(behandlingId = opprettetBehandling.id,
                                                                                              alder = autobrev6og18ÅrDTO.alder))

        val opprettetVedtak = vedtakService.opprettVedtakOgTotrinnskontrollForAutomatiskBehandling(opprettetBehandling)

        opprettTaskJournalførVedtaksbrev(vedtakId = opprettetVedtak.id)
    }

    private fun barnetrygdOpphører(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO,
                                   behandling: Behandling) =
            autobrev6og18ÅrDTO.alder == Alder.atten.år &&
            !barnUnder18årInneværendeMånedEksisterer(behandlingId = behandling.id)

    private fun finnBehandlingÅrsakForAlder(alder: Int): BehandlingÅrsak =
            when (alder) {
                Alder.seks.år -> BehandlingÅrsak.OMREGNING_6ÅR
                Alder.atten.år -> BehandlingÅrsak.OMREGNING_18ÅR
                else -> throw Feil("Alder må være oppgitt til enten 6 eller 18 år.")
            }

    private fun finnVedtakbegrunnelseForAlder(alder: Int): VedtakBegrunnelse =
            when (alder) {
                Alder.seks.år -> VedtakBegrunnelse.REDUKSJON_UNDER_6_ÅR
                Alder.atten.år -> VedtakBegrunnelse.REDUKSJON_UNDER_18_ÅR
                else -> throw Feil("Alder må være oppgitt til enten 6 eller 18 år.")
            }

    private fun brevAlleredeSendt(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO): Boolean {
        return utbetalingBegrunnelseRepository.finnForFagsakMedBegrunnelseGyldigFom(
                fagsakId = autobrev6og18ÅrDTO.fagsakId,
                vedtakBegrunnelse = finnVedtakbegrunnelseForAlder(autobrev6og18ÅrDTO.alder),
                fom = autobrev6og18ÅrDTO.årMåned.toLocalDate()
        ) != null
    }

    private fun barnMedAngittAlderInneværendeMånedEksisterer(behandlingId: Long, alder: Int): Boolean =
            barnMedAngittAlderInneværendeMåned(behandlingId, alder).isNotEmpty()

    private fun barnMedAngittAlderInneværendeMåned(behandlingId: Long, alder: Int): List<Person> =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.personer
                    ?.filter { it.type == PersonType.BARN && it.fyllerAntallÅrInneværendeMåned(alder) }?.toList() ?: listOf()

    private fun barnUnder18årInneværendeMånedEksisterer(behandlingId: Long): Boolean =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.personer
                    ?.any { it.type == PersonType.BARN && it.erYngreEnnInneværendeMåned(Alder.atten.år) } ?: false

    private fun opprettNyOmregningBehandling(behandling: Behandling, behandlingÅrsak: BehandlingÅrsak): NyBehandling =
            NyBehandling(søkersIdent = behandling.fagsak.hentAktivIdent().ident,
                         behandlingType = BehandlingType.REVURDERING,
                         kategori = behandling.kategori,
                         underkategori = behandling.underkategori,
                         behandlingÅrsak = behandlingÅrsak,
                         skalBehandlesAutomatisk = true
            )


    private fun opprettTaskJournalførVedtaksbrev(vedtakId: Long) {
        val task = Task.nyTask(JournalførVedtaksbrevTask.TASK_STEP_TYPE,
                               "$vedtakId")
        taskRepository.save(task)
    }

    fun Person.fyllerAntallÅrInneværendeMåned(år: Int): Boolean {
        return this.fødselsdato.isAfter(now().minusYears(år.toLong()).førsteDagIInneværendeMåned()) &&
               this.fødselsdato.isBefore(now().minusYears(år.toLong()).sisteDagIMåned())
    }

    fun Person.erYngreEnnInneværendeMåned(år: Int): Boolean {
        return this.fødselsdato.isAfter(now().minusYears(år.toLong()).sisteDagIMåned())
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

enum class Alder(val år: Int) {
    seks(år = 6),
    atten(år = 18)
}