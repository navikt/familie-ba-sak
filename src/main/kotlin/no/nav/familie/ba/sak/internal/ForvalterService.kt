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
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
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
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLogg
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLoggRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class ForvalterService(
    private val økonomiService: ØkonomiService,
    private val vedtakService: VedtakService,
    private val beregningService: BeregningService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val stegService: StegService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val taskRepository: TaskRepositoryWrapper,
    private val autovedtakService: AutovedtakService,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val infotrygdService: InfotrygdService,
    private val persongrunnlagService: PersongrunnlagService,
    private val pdlIdentRestClient: PdlIdentRestClient,
    private val personidentService: PersonidentService,
    private val aktørIdRepository: AktørIdRepository,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val aktørMergeLoggRepository: AktørMergeLoggRepository,
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

    fun finnÅpneFagsakerMedFlereMigreringsbehandlingerOgLøpendeSakIInfotrygd(fraÅrMåned: YearMonth): List<Pair<Long, String>> {
        val løpendeFagsakerMedFlereMigreringsbehandlinger = fagsakRepository.finnFagsakerMedFlereMigreringsbehandlinger(
            fraÅrMåned.førsteDagIInneværendeMåned().atStartOfDay(),
        )

        return løpendeFagsakerMedFlereMigreringsbehandlinger.filter { infotrygdService.harLøpendeSakIInfotrygd(listOf(it.fødselsnummer)) }
            .map { Pair(it.fagsakId, it.fødselsnummer) }
    }

    fun finnÅpneFagsakerMedFlereMigreringsbehandlinger(fraÅrMåned: YearMonth): List<Pair<Long, String>> {
        return fagsakRepository.finnFagsakerMedFlereMigreringsbehandlinger(
            fraÅrMåned.førsteDagIInneværendeMåned().atStartOfDay(),
        ).map { Pair(it.fagsakId, it.fødselsnummer) }
    }

    @Transactional
    fun patchIdentForBarnPåFagsak(dto: PatchIdentForBarnPåFagsak) {
        secureLogger.info("Patcher barnets ident på fagsak $dto")

        if (dto.gammelIdent == dto.nyIdent) {
            throw IllegalArgumentException("ident som skal patches er lik ident som det skal patches til")
        }

        val aktørForBarnSomSkalPatches = persongrunnlagService.hentSøkerOgBarnPåFagsak(fagsakId = dto.fagsakId)
            ?.singleOrNull { it.type == PersonType.BARN && it.aktør.aktivFødselsnummer() == dto.gammelIdent.ident }?.aktør ?: error("Fant ikke ident som skal patches som barn på fagsak=${dto.fagsakId}")

        if (dto.skalSjekkeAtGammelIdentErHistoriskAvNyIdent) {
            val identer = pdlIdentRestClient.hentIdenter(personIdent = dto.nyIdent.ident, historikk = true)
            if (identer.none { it.ident == dto.gammelIdent.ident && it.historisk }) {
                error("Ident som skal patches finnes ikke som historisk ident av ny ident")
            }
        }

        val nyAktør = personidentService.hentOgLagreAktør(ident = dto.nyIdent.ident, lagre = true)

        val aktivBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = dto.fagsakId) ?: error("Fant ingen aktiv behandling for fagsak")

        aktørIdRepository.patchAndelTilkjentYteleseMedNyAktør(
            gammelAktørId = aktørForBarnSomSkalPatches.aktørId,
            nyAktørId = nyAktør.aktørId,
            behandlingId = aktivBehandling.id,
        )

        patchPersonForBarnMedNyAktørId(
            aktørForBarnSomSkalPatches = aktørForBarnSomSkalPatches,
            nyAktør = nyAktør,
            behandling = aktivBehandling,
        )

        patchPersonResultatMedNyAktør(
            aktørForBarnSomSkalPatches = aktørForBarnSomSkalPatches,
            nyAktør = nyAktør,
            behandling = aktivBehandling,
        )

        aktørIdRepository.patchPeriodeOvergangstønadtMedNyAktør(
            gammelAktørId = aktørForBarnSomSkalPatches.aktørId,
            nyAktørId = nyAktør.aktørId,
            behandlingId = aktivBehandling.id,
        )

        aktørMergeLoggRepository.save(
            AktørMergeLogg(
                fagsakId = dto.fagsakId,
                historiskAktørId = aktørForBarnSomSkalPatches.aktørId,
                nyAktørId = nyAktør.aktørId,

                mergeTidspunkt = LocalDateTime.now(),
            ),
        )
    }

    private fun patchPersonForBarnMedNyAktørId(
        aktørForBarnSomSkalPatches: Aktør,
        nyAktør: Aktør,
        behandling: Behandling,
    ) {
        val personForBarnet = persongrunnlagService.hentBarna(behandling)
            .singleOrNull { barn -> barn.aktør.aktivFødselsnummer() == aktørForBarnSomSkalPatches.aktivFødselsnummer() }

        personForBarnet?.let { person ->
            secureLogger.info("Patcher person sin aktørId=${aktørForBarnSomSkalPatches.aktørId} med ${nyAktør.aktørId} for personId=${personForBarnet.id}")
            aktørIdRepository.patchPersonMedNyAktør(
                gammelAktørId = aktørForBarnSomSkalPatches.aktørId,
                nyAktørId = nyAktør.aktørId,
                personId = person.id,
            )
        }
    }

    private fun patchPersonResultatMedNyAktør(
        aktørForBarnSomSkalPatches: Aktør,
        nyAktør: Aktør,
        behandling: Behandling,
    ) {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)

        vilkårsvurdering?.let { vilkårsvurderingId ->
            secureLogger.info("Patcher person_resultat sin aktørId=${aktørForBarnSomSkalPatches.aktørId} med ${nyAktør.aktørId} for vilkårsvurderingId=${vilkårsvurderingId.id}")
            aktørIdRepository.patchPersonResultatMedNyAktør(
                gammelAktørId = aktørForBarnSomSkalPatches.aktørId,
                nyAktørId = nyAktør.aktørId,
                vilkårsvurderingId = vilkårsvurderingId.id,
            )
        }
    }
}

data class PatchIdentForBarnPåFagsak(
    val fagsakId: Long,
    val gammelIdent: PersonIdent,
    val nyIdent: PersonIdent,
    /*
    Sjekker at gammel ident er historisk av ny. Hvis man ønsker å patche med en ident hvor den gamle ikke er
    historisk av ny, så settes denne til false. OBS: Du må da være sikker på at identen man ønsker å patche til er
    samme person. Dette kan skje hvis identen ikke er merget av folketrygden.
     */
    val skalSjekkeAtGammelIdentErHistoriskAvNyIdent: Boolean = true,
)

interface FagsakMedFlereMigreringer {
    val fagsakId: Long
    val fødselsnummer: String
}
