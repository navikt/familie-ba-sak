package no.nav.familie.ba.sak.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForIverksettingFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForSimuleringFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.pakkInnForUtbetaling
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.grupperAndeler
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
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.utbetalingsoppdrag
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
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
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

    fun identifiserUtbetalingerOver100Prosent(callId: String) {
        MDC.put(MDCConstants.MDC_CALL_ID, callId)

        runBlocking {
            finnOgLoggUtbetalingerOver100Prosent(callId)
        }

        logger.info("Ferdig med å kjøre identifiserUtbetalingerOver100Prosent")
    }

    @OptIn(InternalCoroutinesApi::class) // for å få lov til å hente CancellationException
    suspend fun finnOgLoggUtbetalingerOver100Prosent(callId: String) {
        var slice = fagsakRepository.finnLøpendeFagsaker(PageRequest.of(0, 10000))
        val scope = CoroutineScope(Dispatchers.Default.limitedParallelism(10))
        val deffereds = mutableListOf<Deferred<Unit>>()

        // coroutineScope {
        while (slice.pageable.isPaged) {
            val sideNr = slice.number
            val fagsaker = slice.get().toList()
            logger.info("Starter kjøring av identifiserUtbetalingerOver100Prosent side=$sideNr")
            deffereds.add(
                scope.async {
                    MDC.put(MDCConstants.MDC_CALL_ID, callId)
                    sjekkChunkMedFagsakerOmDeHarUtbetalingerOver100Prosent(fagsaker)
                    logger.info("Avslutter kjøring av identifiserUtbetalingerOver100Prosent side=$sideNr")
                },
            )

            slice = fagsakRepository.finnLøpendeFagsaker(slice.nextPageable())
        }
        deffereds.forEach {
            if (it.isCancelled) {
                logger.warn("Async jobb med status kansellert. Se securelog")
                secureLogger.warn(
                    "Async jobb kansellert med: ${it.getCancellationException().message} ${
                        it.getCancellationException().stackTraceToString()
                    }",
                )
            }

            it.await()
        }

        logger.info("Alle async jobber er kjørt. Totalt antall sider=${deffereds.size}")
    }

    private fun sjekkChunkMedFagsakerOmDeHarUtbetalingerOver100Prosent(fagsaker: List<Long>) {
        fagsaker.forEach { fagsakId ->
            val sisteIverksatteBehandling =
                behandlingRepository.finnSisteIverksatteBehandling(fagsakId = fagsakId)
            if (sisteIverksatteBehandling != null) {
                try {
                    tilkjentYtelseValideringService.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
                        sisteIverksatteBehandling,
                    )
                } catch (e: UtbetalingsikkerhetFeil) {
                    val arbeidsfordelingService =
                        arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = sisteIverksatteBehandling.id)
                    secureLogger.warn("Over 100% utbetaling for fagsak=$fagsakId, enhet=${arbeidsfordelingService.behandlendeEnhetId}, melding=${e.message}")
                }
            } else {
                logger.warn("Skipper sjekk 100% for fagsak $fagsakId pga manglende sisteIverksettBehandling")
            }
        }
    }

    fun sjekkOmTilkjentYtelseForBehandlingHarUkorrektOpphørsdato(behandlingId: Long): Boolean {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId)
        return opphørsdatoErKorrekt(tilkjentYtelse)
    }

    fun identifiserBehandlingerSomKanKrevePatching(): List<Long> {
        val tilkjenteYtelserMedOpphørSomKanVæreFeil =
            tilkjentYtelseRepository.findTilkjentYtelseMedFeilUtbetalingsoppdrag()
        logger.info("Behandlinger som potensielt har feil: ${tilkjenteYtelserMedOpphørSomKanVæreFeil.map { it.behandling.id }}")

        return tilkjenteYtelserMedOpphørSomKanVæreFeil
            .filter { !opphørsdatoErKorrekt(it) }
            .map { it.behandling.id }
    }

    private fun opphørsdatoErKorrekt(tilkjentYtelse: TilkjentYtelse): Boolean {
        val utbetalingsoppdrag = tilkjentYtelse.utbetalingsoppdrag() ?: return true
        logger.info("Sjekker behandling for korrekt opphørsdato ${tilkjentYtelse.behandling.id}")
        try {
            val grupperteNyeAndeler = grupperAndeler(
                beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId = tilkjentYtelse.behandling.id)
                    .pakkInnForUtbetaling(AndelTilkjentYtelseForSimuleringFactory()),
            )

            val forrigeIverksatteBehandling =
                behandlingRepository.finnIverksatteBehandlinger(tilkjentYtelse.behandling.fagsak.id)
                    .filter { it.id != tilkjentYtelse.behandling.id && it.aktivertTidspunkt < tilkjentYtelse.behandling.aktivertTidspunkt }
                    .maxByOrNull { it.aktivertTidspunkt }!!

            val grupperteForrigeAndeler = grupperAndeler(
                beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId = forrigeIverksatteBehandling.id)
                    .pakkInnForUtbetaling(AndelTilkjentYtelseForSimuleringFactory()),
            )

            val sisteBeståendeAndelPerKjede =
                ØkonomiUtils.sisteBeståendeAndelPerKjede(grupperteForrigeAndeler, grupperteNyeAndeler)

            // Finner andeler som skal opphøres slik vi gjorde før
            val andelerTilOpphør = grupperteForrigeAndeler
                .mapValues { (person, forrigeAndeler) ->
                    forrigeAndeler.filter {
                        sisteBeståendeAndelPerKjede[person] == null ||
                            it.stønadFom > sisteBeståendeAndelPerKjede[person]!!.stønadTom
                    }
                }
                .filter { (_, andelerSomOpphøres) -> andelerSomOpphøres.isNotEmpty() }
                .mapValues { andelForKjede -> andelForKjede.value.sortedBy { it.stønadFom } }
                .map { (_, kjedeEtterFørsteEndring) ->
                    kjedeEtterFørsteEndring.last() to kjedeEtterFørsteEndring.minOf { it.stønadFom }
                }

            secureLogger.info("Andeler som som skal opphøres: ${andelerTilOpphør.map { "PeriodeId: ${it.first.periodeOffset} ForrigePeriodeId: ${it.first.forrigePeriodeOffset} Opphørsdato: ${it.second}" }} for behandling ${tilkjentYtelse.behandling.id}")
            val utbetalingsperioderMedOpphør = utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør != null }
            secureLogger.info("Utbetalingsperioder med opphør: $utbetalingsperioderMedOpphør for behandling ${tilkjentYtelse.behandling.id}")

            // Finner ut hvilken opphørsAndel som tilhører hvilken utbetalingsperiodeMedOpphør
            val alleUtbetalingsperioderMedOpphørHarKorrektOpphørsdato = utbetalingsperioderMedOpphør
                .all { periodeMedOpphør ->
                    val andelerTilPersonMedOpphør =
                        andelerTilOpphør.filter { andelForPerson -> andelForPerson.first.periodeOffset == periodeMedOpphør.periodeId }
                    if (andelerTilPersonMedOpphør.size != 1) {
                        secureLogger.info("Mer enn 1 eller ingen andeler med samme periodeOffsett som opphørsperioden $periodeMedOpphør for behandling ${tilkjentYtelse.behandling.id}")
                        false
                    } else {
                        secureLogger.info("Andel fra forrige med korrekt opphørsdato: ${andelerTilPersonMedOpphør.first().second.førsteDagIInneværendeMåned()}. Opphørsperiode sendt til økonomi med opphørsdato: ${periodeMedOpphør.opphør!!.opphørDatoFom} for behandling ${tilkjentYtelse.behandling.id}")
                        andelerTilPersonMedOpphør.first().second
                            .førsteDagIInneværendeMåned() == periodeMedOpphør.opphør!!.opphørDatoFom
                    }
                }
            return alleUtbetalingsperioderMedOpphørHarKorrektOpphørsdato
        } catch (e: Exception) {
            secureLogger.warn(
                "opphørsdatoErKorrekt kaster feil: ${e.message} for behandling ${tilkjentYtelse.behandling.id}",
                e,
            )
            return false
        }
    }
}
