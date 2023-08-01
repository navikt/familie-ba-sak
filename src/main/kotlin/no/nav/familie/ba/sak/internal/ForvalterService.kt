package no.nav.familie.ba.sak.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForIverksettingFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class ForvalterService(
    private val økonomiService: ØkonomiService,
    private val vedtakService: VedtakService,
    private val beregningService: BeregningService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val stegService: StegService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val taskRepository: TaskRepositoryWrapper,
    private val autovedtakService: AutovedtakService,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
) {
    private val logger = LoggerFactory.getLogger(ForvalterService::class.java)

    @Transactional
    fun lagOgSendUtbetalingsoppdragTilØkonomiForBehandling(behandlingId: Long) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        val forrigeBehandlingSendtTilØkonomi =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling)
        val erBehandlingOpprettetEtterDenneSomErSendtTilØkonomi = forrigeBehandlingSendtTilØkonomi != null &&
            forrigeBehandlingSendtTilØkonomi.aktivertTidspunkt.isAfter(behandling.aktivertTidspunkt)

        if (tilkjentYtelse.utbetalingsoppdrag != null) {
            throw Feil("Behandling $behandlingId har allerede opprettet utbetalingsoppdrag")
        }
        if (erBehandlingOpprettetEtterDenneSomErSendtTilØkonomi) {
            throw Feil("Det finnes en behandling opprettet etter $behandlingId som er sendt til økonomi")
        }

        økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId),
            saksbehandlerId = "VL",
            andelTilkjentYtelseForUtbetalingsoppdragFactory = AndelTilkjentYtelseForIverksettingFactory(),
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun kopierEndretUtbetalingFraForrigeBehandling(
        sisteVedtatteBehandling: Behandling,
        nestSisteVedtatteBehandling: Behandling,
    ) {
        endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
            behandling = sisteVedtatteBehandling,
            forrigeBehandling = nestSisteVedtatteBehandling,
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun kjørForenkletSatsendringFor(fagsakId: Long) {
        val fagsak = fagsakService.hentPåFagsakId(fagsakId)

        val nyBehandling = stegService.håndterNyBehandling(
            NyBehandling(
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SATSENDRING,
                søkersIdent = fagsak.aktør.aktivFødselsnummer(),
                skalBehandlesAutomatisk = true,
                fagsakId = fagsakId,
            ),
        )

        val behandlingEtterVilkårsvurdering =
            stegService.håndterVilkårsvurdering(nyBehandling)

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterVilkårsvurdering,
            )
        behandlingService.oppdaterStatusPåBehandling(nyBehandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)
        val task =
            IverksettMotOppdragTask.opprettTask(nyBehandling, opprettetVedtak, SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }

    @Transactional(readOnly = true)
    suspend fun identifiserUtbetalingerOver100Prosent(callId: String) {
        var slice: Slice<Long> = fagsakRepository.finnLøpendeFagsaker(PageRequest.of(0, 200))
        val scope = CoroutineScope(Dispatchers.Default)
        var sideNr: Int = 0
        val deffereds = mutableListOf(
            scope.async {
                sjekkChunkMedFagsakerOmDeHarUtbetalingerOver100Prosent(callId, slice, sideNr)
            },
        )

        while (slice.hasNext()) {
            slice = fagsakRepository.finnLøpendeFagsaker(slice.nextPageable())
            sideNr = sideNr.inc()
            deffereds.add(
                scope.async {
                    sjekkChunkMedFagsakerOmDeHarUtbetalingerOver100Prosent(callId, slice, sideNr)
                },
            )
        }

        deffereds.awaitAll()
        logger.info("Ferdig med å kjøre identifiserUtbetalingerOver100Prosent")
    }

    private fun sjekkChunkMedFagsakerOmDeHarUtbetalingerOver100Prosent(callId: String, slice: Slice<Long>, sideNr: Int) {
        MDC.put(MDCConstants.MDC_CALL_ID, callId)
        logger.info("identifiserUtbetalingerOver100Prosent side $sideNr")
        slice.get().toList().forEach { fagsakId ->
            val sisteIverksatteBehandling =
                behandlingRepository.finnSisteIverksatteBehandling(fagsakId = fagsakId)!!
            try {
                tilkjentYtelseValideringService.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
                    sisteIverksatteBehandling,
                )
            } catch (e: UtbetalingsikkerhetFeil) {
                val arbeidsfordelingService =
                    arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = sisteIverksatteBehandling.id)
                secureLogger.warn("Over 100% utbetaling for fagsak=$fagsakId, enhet=${arbeidsfordelingService.behandlendeEnhetId}, melding=${e.message}")
            }
        }
    }
}
