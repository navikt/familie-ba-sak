package no.nav.familie.ba.sak.kjerne.autobrev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakBegrunnelseRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
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
        private val behandlingService: BehandlingService,
        private val stegService: StegService,
        private val vedtakService: VedtakService,
        private val taskRepository: TaskRepository,
        private val vedtaksperiodeService: VedtaksperiodeService,
        private val vedtakBegrunnelseRepository: VedtakBegrunnelseRepository
) {

    @Transactional
    fun opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO) {

        logger.info("opprettOmregningsoppgaveForBarnIBrytingsalder for fagsak ${autobrev6og18ÅrDTO.fagsakId}")

        val behandling = behandlingService.hentAktivForFagsak(autobrev6og18ÅrDTO.fagsakId) ?: error("Fant ikke aktiv behandling")

        if (behandling.fagsak.status != FagsakStatus.LØPENDE) {
            logger.info("Fagsak ${behandling.fagsak.id} har ikke status løpende, og derfor prosesseres den ikke videre.")
            return
        }

        if (brevAlleredeSendt(autobrev6og18ÅrDTO)) {
            logger.info("Fagsak ${behandling.fagsak.id} ${autobrev6og18ÅrDTO.alder} års omregningsbrev brev allerede sendt")
            return
        }

        if (!barnMedAngittAlderInneværendeMånedEksisterer(behandlingId = behandling.id, alder = autobrev6og18ÅrDTO.alder)) {
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

        val opprettetBehandling =
                stegService.håndterNyBehandling(nyBehandling = opprettNyOmregningBehandling(behandling = behandling,
                                                                                            behandlingÅrsak = finnBehandlingÅrsakForAlder(
                                                                                                    autobrev6og18ÅrDTO.alder)))

        stegService.håndterVilkårsvurdering(behandling = opprettetBehandling)

        vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(
                vedtak = vedtakService.hentAktivForBehandlingThrows(opprettetBehandling.id),
                vedtakBegrunnelseSpesifikasjon = finnVedtakbegrunnelseForAlder(autobrev6og18ÅrDTO.alder)
        )

        val opprettetVedtak = vedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(opprettetBehandling)

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

    private fun finnVedtakbegrunnelseForAlder(alder: Int): VedtakBegrunnelseSpesifikasjon =
            when (alder) {
                Alder.seks.år -> VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR
                Alder.atten.år -> VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR
                else -> throw Feil("Alder må være oppgitt til enten 6 eller 18 år.")
            }

    private fun brevAlleredeSendt(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO): Boolean {
        val brevSendtMedNyModell = behandlingService.hentBehandlinger(fagsakId = autobrev6og18ÅrDTO.fagsakId)
                .filter { it.status == BehandlingStatus.AVSLUTTET }
                .any { behandling ->
                    val vedtak = vedtakService.hentAktivForBehandlingThrows(behandling.id)
                    val vedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

                    vedtaksperioderMedBegrunnelser.any { vedtaksperiodeMedBegrunnelser ->
                        vedtaksperiodeMedBegrunnelser.begrunnelser.map { it.vedtakBegrunnelseSpesifikasjon }
                                .contains(finnVedtakbegrunnelseForAlder(autobrev6og18ÅrDTO.alder))
                    }
                }

        val brevSendtMedGammelModell = vedtakBegrunnelseRepository.finnForFagsakMedBegrunnelseGyldigFom(
                fagsakId = autobrev6og18ÅrDTO.fagsakId,
                vedtakBegrunnelse = finnVedtakbegrunnelseForAlder(autobrev6og18ÅrDTO.alder),
                fom = autobrev6og18ÅrDTO.årMåned.toLocalDate()
        ).isNotEmpty()

        return brevSendtMedNyModell || brevSendtMedGammelModell
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
        val task = Task(JournalførVedtaksbrevTask.TASK_STEP_TYPE,
                               "$vedtakId")
        taskRepository.save(task)
    }

    fun Person.fyllerAntallÅrInneværendeMåned(år: Int): Boolean {
        return this.fødselsdato.isSameOrAfter(now().minusYears(år.toLong()).førsteDagIInneværendeMåned()) &&
               this.fødselsdato.isSameOrBefore(now().minusYears(år.toLong()).sisteDagIMåned())
    }

    fun Person.erYngreEnnInneværendeMåned(år: Int): Boolean {
        return this.fødselsdato.isAfter(now().minusYears(år.toLong()).sisteDagIMåned())
    }

    companion object {

        private val logger = LoggerFactory.getLogger(Autobrev6og18ÅrService::class.java)
    }
}

enum class Alder(val år: Int) {
    seks(år = 6),
    atten(år = 18)
}