package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBrevkode
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.erAlleredeBegrunnetMedBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class AutobrevService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val autovedtakService: AutovedtakService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val taskRepository: TaskRepositoryWrapper,
    private val infotrygdService: InfotrygdService
) {

    fun skalAutobrevBehandlingOpprettes(
        fagsakId: Long,
        behandlingsårsak: BehandlingÅrsak,
        standardbegrunnelser: List<VedtakBegrunnelseSpesifikasjon>
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
                måned = YearMonth.now()
            )
        ) {
            logger.info("Brev for ${behandlingsårsak.visningsnavn} har allerede kjørt for $fagsakId")
            return false
        }

        val vedtaksperioderForVedtatteBehandlinger = behandlingService.hentBehandlinger(fagsakId = fagsakId)
            .filter { behandling ->
                behandling.erVedtatt()
            }
            .flatMap { behandling ->
                val vedtak = vedtakService.hentAktivForBehandlingThrows(behandling.id)
                vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
            }

        if (barnAlleredeBegrunnet(
                vedtaksperioderMedBegrunnelser = vedtaksperioderForVedtatteBehandlinger,
                standardbegrunnelser = standardbegrunnelser
            )
        ) {
            logger.info("Begrunnelser $standardbegrunnelser for ${behandlingsårsak.visningsnavn} har allerede kjørt for $fagsakId")
            return false
        }

        return true
    }

    fun harSendtBrevFraInfotrygd(fagsakId: Long, behandlingsårsak: BehandlingÅrsak): Boolean {
        val personidenter = fagsakService.hentAktør(fagsakId).personidenter
        val harSendtBrev =
            infotrygdService.harSendtbrev(personidenter.map { it.fødselsnummer }, behandlingsårsak.tilBrevkoder())
        return if (harSendtBrev) {
            logger.info("Har sendt autobrev fra infotrygd, dropper å lage behandling for å sende brev fra ba-sak")
            true
        } else {
            logger.info("Har ikke sendt autobrev fra infotrygd, lager ny behandling og sender brev på vanlig måte")
            false
        }
    }

    fun opprettOgKjørOmregningsbehandling(
        behandling: Behandling,
        behandlingsårsak: BehandlingÅrsak,
        standardbegrunnelse: VedtakBegrunnelseSpesifikasjon
    ) {
        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                fagsak = behandling.fagsak,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = behandlingsårsak
            )

        vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(
            vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingEtterBehandlingsresultat.id),
            vedtakBegrunnelseSpesifikasjon = standardbegrunnelse
        )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat
            )

        opprettTaskJournalførVedtaksbrev(vedtakId = opprettetVedtak.id)
    }

    private fun barnAlleredeBegrunnet(
        vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
        standardbegrunnelser: List<VedtakBegrunnelseSpesifikasjon>
    ): Boolean {
        return vedtaksperioderMedBegrunnelser.erAlleredeBegrunnetMedBegrunnelse(
            standardbegrunnelser = standardbegrunnelser,
            måned = YearMonth.now()
        )
    }

    private fun opprettTaskJournalførVedtaksbrev(vedtakId: Long) {
        val task = Task(
            JournalførVedtaksbrevTask.TASK_STEP_TYPE,
            "$vedtakId"
        )
        taskRepository.save(task)
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutobrevService::class.java)
    }
}

private fun BehandlingÅrsak.tilBrevkoder(): List<InfotrygdBrevkode> {
    return when (this) {
        BehandlingÅrsak.OMREGNING_6ÅR -> listOf(
            InfotrygdBrevkode.BREV_BATCH_OMREGNING_BARN_6_ÅR,
            InfotrygdBrevkode.BREV_MANUELL_OMREGNING_BARN_6_ÅR
        )
        BehandlingÅrsak.OMREGNING_18ÅR -> listOf(
            InfotrygdBrevkode.BREV_BATCH_OMREGNING_BARN_18_ÅR,
            InfotrygdBrevkode.BREV_MANUELL_OMREGNING_BARN_18_ÅR
        )
        BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG -> listOf(
            InfotrygdBrevkode.BREV_BATCH_OPPHØR_SMÅBARNSTILLLEGG,
            InfotrygdBrevkode.BREV_MANUELL_OPPHØR_SMÅBARNSTILLLEGG
        )
        else -> emptyList()
    }
}
