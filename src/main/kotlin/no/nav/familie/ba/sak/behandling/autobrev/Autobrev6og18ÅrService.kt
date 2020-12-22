package no.nav.familie.ba.sak.behandling.autobrev

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.SendAutobrev6og18ÅrTask
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class Autobrev6og18ÅrService(
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
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

        // Finne ut om fagsak har behandling som ikke er fullført -> hvis ja, feile task og logge feil og øke metrikk
        //  hvis tasken forsøker for siste gang -> opprett oppgave for å håndtere videre manuelt
        // TODO: trenger vi denne sjekken når den allerede gjøres i behnadlingsservice l.75
        if (behandling.status != BehandlingStatus.AVSLUTTET) {
            error("Kan ikke opprette ny behandling for fagsak ${behandling.fagsak.id} ettersom den allerede har en åpen behanding.")
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

        val behandlingÅrsak = if (autobrev6og18ÅrDTO.alder == 6) {
            BehandlingÅrsak.OMREGNING_6ÅR
        } else {
            BehandlingÅrsak.OMREGNING_18ÅR
        }

        stegService.håndterNyBehandling(nyBehandling = opprettNyOmregningBehandling(behandling = behandling,
                                                                                    behandlingÅrsak = behandlingÅrsak))

        val opprettetBehandling = behandlingService.hentAktivForFagsak(autobrev6og18ÅrDTO.fagsakId)
                                  ?: error("Aktiv behandling finnes ikke for fagsak")
        stegService.håndterVilkårsvurdering(behandling = opprettetBehandling)

        val vedtak = vedtakService.opprettVedtakOgTotrinnskontrollForAutomatiskBehandling(behandling)
        opprettTaskJournalførVedtaksbrev(vedtakId = vedtak.id)

        SendAutobrev6og18ÅrTask.LOG.info("SendAutobrev6og18ÅrTask for fagsak ${behandling.fagsak.id}")
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
                    //TODO: Diskuter med Henning: ser ut å være tilpasset behov for førstegangsbehandling Fødselshendelse
                    // men at flaget passer dårlig sammen med omregning. Hvordan skal vi løse det?
                    // Satte den temporært til false for å komme videre uten alt for mye kodeendringer.
                         skalBehandlesAutomatisk = false
            )

    fun Person.fyllerAntallÅrInneværendeMåned(år: Int): Boolean {
        return this.fødselsdato.isAfter(LocalDate.now().minusYears(år.toLong()).førsteDagIInneværendeMåned()) &&
               this.fødselsdato.isBefore(LocalDate.now().minusYears(år.toLong()).sisteDagIMåned())
    }

    fun Person.erYngreEnnInneværendeMåned(år: Int): Boolean {
        return this.fødselsdato.isAfter(LocalDate.now().minusYears(år.toLong()).sisteDagIMåned())
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