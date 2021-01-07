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
import no.nav.familie.ba.sak.behandling.vedtak.UtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
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
        private val taskRepository: TaskRepository
) {

    @Transactional
    fun opprettOmregningsoppgaveForBarnIBrytingsAlder(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO) {

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

        if (autobrev6og18ÅrDTO.alder == Alder.atten.år &&
            !barnUnder18årInneværendeMånedEksisterer(behandlingId = behandling.id)) {

            LOG.info("Fagsak ${behandling.fagsak.id} har ikke noe barn med alder under 18 år")
            return
        }

        if (behandling.status != BehandlingStatus.AVSLUTTET) {
            error("Kan ikke opprette ny behandling for fagsak ${behandling.fagsak.id} ettersom den allerede har en åpen behanding.")
        }

        val opprettetBehandling =
                stegService.håndterNyBehandling(nyBehandling = opprettNyOmregningBehandling(behandling = behandling,
                                                                                            behandlingÅrsak = finnBehandlingÅrsak(
                                                                                                    autobrev6og18ÅrDTO)))

        stegService.håndterVilkårsvurdering(behandling = opprettetBehandling)

        leggTilUtbetalingBegrunnelse(behandlingId = opprettetBehandling.id,
                                     begrunnelseType = VedtakBegrunnelseType.INNVILGELSE,
                                     vedtakBegrunnelse = finnVedtakbegrunnelse(autobrev6og18ÅrDTO),
                                     målform = persongrunnlagService.hentSøker(opprettetBehandling.id)?.målform ?: Målform.NB)

        val opprettetVedtak = vedtakService.opprettVedtakOgTotrinnskontrollForAutomatiskBehandling(opprettetBehandling)

        opprettTaskJournalførVedtaksbrev(vedtakId = opprettetVedtak.id)
    }

    private fun finnBehandlingÅrsak(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO): BehandlingÅrsak =
            if (autobrev6og18ÅrDTO.alder == 6) {
                BehandlingÅrsak.OMREGNING_6ÅR
            } else {
                BehandlingÅrsak.OMREGNING_18ÅR
            }

    private fun finnVedtakbegrunnelse(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO): VedtakBegrunnelse =
            if (autobrev6og18ÅrDTO.alder == 6) {
                VedtakBegrunnelse.REDUKSJON_UNDER_6_ÅR
            } else {
                VedtakBegrunnelse.REDUKSJON_UNDER_18_ÅR
            }

    private fun leggTilUtbetalingBegrunnelse(behandlingId: Long,
                                             begrunnelseType: VedtakBegrunnelseType,
                                             vedtakBegrunnelse: VedtakBegrunnelse,
                                             målform: Målform): Vedtak {

        val opprettetVedtak = vedtakService.hentAktivForBehandling(behandlingId = behandlingId)
                     ?: error("Fant ikke aktivt vedtak på behandling $behandlingId")

        return behandlingService.hentAndelTilkjentYtelseInneværendeMåned(behandlingId).let {
                vedtakService.leggTilUtbetalingBegrunnelse(UtbetalingBegrunnelse(vedtak = opprettetVedtak,
                                                                                 fom = it.stønadFom.førsteDagIInneværendeMåned(),
                                                                                 tom = it.stønadTom.sisteDagIInneværendeMåned(),
                                                                                 begrunnelseType = begrunnelseType,
                                                                                 vedtakBegrunnelse = vedtakBegrunnelse,
                                                                                 brevBegrunnelse = vedtakBegrunnelse.hentBeskrivelse(
                                                                                         målform = målform)))
            }
    }


    private fun brevAlleredeSendt(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO): Boolean {
        // TODO: Det trenges en modellavklaring, hvordan persisterer vi informasjon om at denne omregningen gjelder inneværende måned:
        // På vedtaket, beregning.opprettetDato eller skal modellen utvides?
        return false
        //behandlingService.hentBehandlinger(autobrev6og18ÅrDTO.fagsakId)
        //.filter { it.opprettetÅrsak == BehandlingÅrsak.OMREGNING_6ÅR }
        //.any {innværendemåned}
    }

    private fun barnMedAngittAlderInneværendeMånedEksisterer(behandlingId: Long, alder: Int): Boolean =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.personer
                    ?.any { it.type == PersonType.BARN && it.fyllerAntallÅrInneværendeMåned(alder) } ?: false

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

    fun Person.fyllerAntallÅrInneværendeMåned(år: Int): Boolean {
        return this.fødselsdato.isAfter(now().minusYears(år.toLong()).førsteDagIInneværendeMåned()) &&
               this.fødselsdato.isBefore(now().minusYears(år.toLong()).sisteDagIMåned())
    }

    fun Person.erYngreEnnInneværendeMåned(år: Int): Boolean {
        return this.fødselsdato.isAfter(now().minusYears(år.toLong()).sisteDagIMåned())
    }

    private fun opprettTaskJournalførVedtaksbrev(vedtakId: Long) {
        val task = Task.nyTask(JournalførVedtaksbrevTask.TASK_STEP_TYPE,
                               "$vedtakId")
        taskRepository.save(task)
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