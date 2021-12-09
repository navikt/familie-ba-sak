package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.dto.AutobrevAlderDTO
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate.now

@Service
class AutobrevService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val taskRepository: TaskRepositoryWrapper,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val autovedtakService: AutovedtakService
) {

    @Transactional
    fun opprettOmregningsoppgaveForBarnIBrytingsalder(autobrevAlderDTO: AutobrevAlderDTO) {

        logger.info("opprettOmregningsoppgaveForBarnIBrytingsalder for fagsak ${autobrevAlderDTO.fagsakId}")

        val behandling =
            behandlingService.hentAktivForFagsak(autobrevAlderDTO.fagsakId) ?: error("Fant ikke aktiv behandling")

        if (behandling.fagsak.status != FagsakStatus.LØPENDE) {
            logger.info("Fagsak ${behandling.fagsak.id} har ikke status løpende, og derfor prosesseres den ikke videre.")
            return
        }

        if (brevAlleredeSendt(autobrevAlderDTO)) {
            logger.info("Fagsak ${behandling.fagsak.id} ${autobrevAlderDTO.alder} års omregningsbrev brev allerede sendt")
            return
        }

        if (!barnMedAngittAlderInneværendeEllerForrigeMånedEksisterer(
                behandlingId = behandling.id,
                alder = autobrevAlderDTO.alder
            )
        ) {
            logger.warn("Fagsak ${behandling.fagsak.id} har ikke noe barn med alder ${autobrevAlderDTO.alder} ")
            return
        }

        if (barnetrygdOpphører(autobrevAlderDTO, behandling)) {
            logger.info("Fagsak ${behandling.fagsak.id} har ikke barn under 18 år og vil opphøre.")
            return
        }

        if (
            (autobrevAlderDTO.alder == 3 && !behandling.erSmåbarnstillegg()) ||
            alderEr3ÅrOgHarSmåbarnstilleggMenHarFortsattBarnUnder3År(autobrevAlderDTO, behandling)
        ) {
            logger.info("Fagsak ${behandling.fagsak.id} er ikke småbarnstillegg, eller er småbarnstillegg men har fortsatt andre barn under 3 år")
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
                    autobrevAlderDTO.alder
                )
            )

        vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(
            vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingEtterBehandlingsresultat.id),
            vedtakBegrunnelseSpesifikasjon = AutobrevUtils.hentGjeldendeVedtakbegrunnelseReduksjonForAlder(
                autobrevAlderDTO.alder
            )
        )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat
            )

        opprettTaskJournalførVedtaksbrev(vedtakId = opprettetVedtak.id)
    }

    private fun barnetrygdOpphører(
        autobrevAlderDTO: AutobrevAlderDTO,
        behandling: Behandling
    ) =
        autobrevAlderDTO.alder == Alder.ATTEN.år &&
            !barnUnder18årInneværendeMånedEksisterer(behandlingId = behandling.id)

    private fun finnBehandlingÅrsakForAlder(alder: Int): BehandlingÅrsak =
        when (alder) {
            Alder.TRE.år -> BehandlingÅrsak.SMÅBARNSTILLEGG
            Alder.SEKS.år -> BehandlingÅrsak.OMREGNING_6ÅR
            Alder.ATTEN.år -> BehandlingÅrsak.OMREGNING_18ÅR
            else -> throw Feil("Alder må være oppgitt til enten 3, 6 eller 18 år.")
        }

    private fun brevAlleredeSendt(autobrevAlderDTO: AutobrevAlderDTO): Boolean =
        behandlingService.hentBehandlinger(fagsakId = autobrevAlderDTO.fagsakId)
            .filter { it.status == BehandlingStatus.AVSLUTTET }
            .any { behandling ->
                val vedtak = vedtakService.hentAktivForBehandlingThrows(behandling.id)
                val vedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

                val vedtaksBegrunnelserForReduksjon =
                    AutobrevUtils.hentStandardbegrunnelserReduksjonForAlder(autobrevAlderDTO.alder)
                vedtaksperioderMedBegrunnelser.any { vedtaksperiodeMedBegrunnelser ->
                    vedtaksperiodeMedBegrunnelser.begrunnelser.map { it.vedtakBegrunnelseSpesifikasjon }
                        .any { vedtaksBegrunnelserForReduksjon.contains(it) }
                }
            }

    private fun barnMedAngittAlderInneværendeEllerForrigeMånedEksisterer(behandlingId: Long, alder: Int): Boolean =
        when (alder) {
            3 -> barnMedAngittAlderForrigeMåned(behandlingId, alder).isNotEmpty()
            else -> barnMedAngittAlderInneværendeMåned(behandlingId, alder).isNotEmpty()
        }

    private fun barnMedAngittAlderInneværendeMåned(behandlingId: Long, alder: Int): List<Person> =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.personer
            ?.filter { it.type == PersonType.BARN && it.fyllerAntallÅrInneværendeMåned(alder) }?.toList() ?: emptyList()

    private fun barnMedAngittAlderForrigeMåned(behandlingId: Long, alder: Int): List<Person> =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.personer
            ?.filter { it.type == PersonType.BARN && it.fylteAntallÅrForrigeMåned(alder) }?.toList() ?: emptyList()

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

    fun Person.fyllerAntallÅrInneværendeMåned(år: Int): Boolean {
        return this.fødselsdato.isSameOrAfter(now().minusYears(år.toLong()).førsteDagIInneværendeMåned()) &&
            this.fødselsdato.isSameOrBefore(now().minusYears(år.toLong()).sisteDagIMåned())
    }

    fun Person.fylteAntallÅrForrigeMåned(år: Int): Boolean {
        return this.fødselsdato.isSameOrAfter(
            now().minusYears(år.toLong()).minusMonths(1).førsteDagIInneværendeMåned()
        ) &&
            this.fødselsdato.isSameOrBefore(now().minusYears(år.toLong()).minusMonths(1).sisteDagIMåned())
    }

    fun Person.erYngreEnnInneværendeMåned(år: Int): Boolean {
        return this.fødselsdato.isAfter(now().minusYears(år.toLong()).sisteDagIMåned())
    }

    private fun alderEr3ÅrOgHarSmåbarnstilleggMenHarFortsattBarnUnder3År(
        autobrevAlderDTO: AutobrevAlderDTO,
        behandling: Behandling
    ): Boolean {
        if (autobrevAlderDTO.alder != 3) {
            return false
        }
        if (!behandling.erSmåbarnstillegg()) {
            return false
        }
        val alleBarnUnder3år: List<Person> =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)?.personer
                ?.filter {
                    it.type == PersonType.BARN &&
                        it.fødselsdato.isSameOrAfter(now().minusYears(3).sisteDagIMåned())
                }?.toList() ?: emptyList()
        return alleBarnUnder3år.isNotEmpty()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(AutobrevService::class.java)
    }
}

enum class Alder(val år: Int) {
    TRE(år = 3),
    SEKS(år = 6),
    ATTEN(år = 18)
}
