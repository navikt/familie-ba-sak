package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBrevkode
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.OmregningBrevData
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class AutovedtakBrevService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val autovedtakService: AutovedtakService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val taskRepository: TaskRepositoryWrapper,
    private val infotrygdService: InfotrygdService,
    private val behandlingRepository: BehandlingRepository,
) : AutovedtakBehandlingService<OmregningBrevData> {
    override fun kjørBehandling(
        behandlingsdata: OmregningBrevData,
    ): String {
        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = behandlingsdata.behandlingsårsak,
                fagsakId = behandlingsdata.fagsakId,
            )

        vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(
            vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingEtterBehandlingsresultat.id),
            standardbegrunnelse = behandlingsdata.standardbegrunnelse,
        )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat,
            )

        opprettTaskJournalførVedtaksbrev(vedtakId = opprettetVedtak.id)

        return AutovedtakStegService.BEHANDLING_FERDIG
    }

    fun skalAutobrevBehandlingOpprettes(
        fagsakId: Long,
        behandlingsårsak: BehandlingÅrsak,
        standardbegrunnelser: List<Standardbegrunnelse>,
    ): Boolean {
        if (!behandlingsårsak.erOmregningsårsak()) {
            throw Feil("Sjekk om autobrevbehandling skal opprettes sjekker på årsak som ikke er omregning.")
        }

        if (harSendtBrevFraInfotrygd(fagsakId, behandlingsårsak)) {
            return false
        }

        if (behandlingService.harBehandlingsårsakAlleredeKjørt(
                fagsakId = fagsakId,
                behandlingÅrsak = behandlingsårsak,
                måned = YearMonth.now(),
            )
        ) {
            logger.info("Brev for ${behandlingsårsak.visningsnavn} har allerede kjørt for $fagsakId")
            return false
        }

        val tidligereVedtatteBehandlinger =
            behandlingHentOgPersisterService
                .hentBehandlinger(fagsakId = fagsakId)
                .filter { behandling -> behandling.erVedtatt() }

        val standardbegrunnelserIPeriodeFraTidligereBehandlinger =
            tidligereVedtatteBehandlinger
                .flatMap {
                    behandlingRepository.hentBegrunnelserPåBehandlingIPeriode(
                        it.id,
                        YearMonth.now().førsteDagIInneværendeMåned(),
                    )
                }.toSet()
                .mapNotNull { begrunnelse -> enumValues<Standardbegrunnelse>().find { it.name == begrunnelse } }

        val erBarnAlleredeBegrunnet =
            standardbegrunnelserIPeriodeFraTidligereBehandlinger.any { it in standardbegrunnelser }

        if (erBarnAlleredeBegrunnet) {
            logger.info("Begrunnelser $standardbegrunnelser for ${behandlingsårsak.visningsnavn} har allerede kjørt for $fagsakId")
            return false
        }

        return true
    }

    fun harSendtBrevFraInfotrygd(
        fagsakId: Long,
        behandlingsårsak: BehandlingÅrsak,
    ): Boolean {
        val personidenter = fagsakService.hentAktør(fagsakId).personidenter
        val harSendtBrev =
            infotrygdService.harSendtbrev(personidenter.map { it.fødselsnummer }, behandlingsårsak.tilBrevkoder())
        return if (harSendtBrev) {
            logger.info("Har sendt autobrev fra infotrygd, dropper å lage behandling for å sende brev fra ba-sak. fagsakId=$fagsakId behandlingsårsak=$behandlingsårsak")
            true
        } else {
            logger.info("Har ikke sendt autobrev fra infotrygd, lager ny behandling og sender brev på vanlig måte. fagsakId=$fagsakId behandlingsårsak=$behandlingsårsak")
            false
        }
    }

    private fun opprettTaskJournalførVedtaksbrev(vedtakId: Long) {
        val task =
            Task(
                JournalførVedtaksbrevTask.TASK_STEP_TYPE,
                "$vedtakId",
            )
        taskRepository.save(task)
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakBrevService::class.java)
    }

    override fun skalAutovedtakBehandles(behandlingsdata: OmregningBrevData): Boolean = true
}

private fun BehandlingÅrsak.tilBrevkoder(): List<InfotrygdBrevkode> =
    when (this) {
        BehandlingÅrsak.OMREGNING_18ÅR -> {
            listOf(
                InfotrygdBrevkode.BREV_BATCH_OMREGNING_BARN_18_ÅR,
                InfotrygdBrevkode.BREV_MANUELL_OMREGNING_BARN_18_ÅR,
            )
        }

        BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG -> {
            listOf(
                InfotrygdBrevkode.BREV_BATCH_OPPHØR_SMÅBARNSTILLLEGG,
                InfotrygdBrevkode.BREV_MANUELL_OPPHØR_SMÅBARNSTILLLEGG,
            )
        }

        else -> {
            emptyList()
        }
    }
