package no.nav.familie.ba.sak.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.utbetalingsland
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
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
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val persongrunnlagService: PersongrunnlagService,
    private val kompetanseRepository: KompetanseRepository,
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
) {
    private val logger = LoggerFactory.getLogger(ForvalterService::class.java)

    @Transactional
    fun lagOgSendUtbetalingsoppdragTilØkonomiForBehandling(behandlingId: Long) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        val forrigeBehandlingSendtTilØkonomi =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling)
        val erBehandlingOpprettetEtterDenneSomErSendtTilØkonomi =
            forrigeBehandlingSendtTilØkonomi != null &&
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
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun kjørForenkletSatsendringFor(fagsakId: Long) {
        val fagsak = fagsakService.hentPåFagsakId(fagsakId)

        val nyBehandling =
            stegService.håndterNyBehandling(
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

        finnOgLoggUtbetalingerOver100Prosent(callId)

        logger.info("Ferdig med å kjøre identifiserUtbetalingerOver100Prosent")
    }

    @OptIn(
        InternalCoroutinesApi::class,
        ExperimentalCoroutinesApi::class,
    ) // for å få lov til å hente CancellationException
    fun finnOgLoggUtbetalingerOver100Prosent(callId: String) {
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

        runBlocking {
            deffereds.awaitAll()
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
        val løpendeFagsakerMedFlereMigreringsbehandlinger =
            fagsakRepository.finnFagsakerMedFlereMigreringsbehandlinger(
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

    fun settFomPåVilkårTilPersonsFødselsdato(behandlingId: Long): Vilkårsvurdering {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val personerPåBehandling = persongrunnlagService.hentSøkerOgBarnPåBehandling(behandlingId)

        if (!behandling.aktiv || !behandling.erVedtatt()) {
            throw Feil("Behandlingen er ikke aktiv eller ikke vedtatt, så den burde ikke patches.")
        }

        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandlingId)
                ?: throw Feil("Det er ingen vilkårsvurdering for behandling: $behandlingId")

        vilkårsvurdering.personResultater.forEach { personResultat ->
            personResultat.vilkårResultater.forEach vilkårresultatLoop@{ vilkårResultat ->
                if (vilkårResultat.vilkårType == UNDER_18_ÅR) return@vilkårresultatLoop

                val person =
                    personerPåBehandling?.singleOrNull { it.aktør == personResultat.aktør }
                        ?: throw Feil("Finner ikke person på behandling med aktørId ${personResultat.aktør.aktørId}.")

                validerKunEttVilkårResultatFørFødselsdato(personResultat, vilkårResultat.vilkårType, person.fødselsdato)

                val vilkårFomErFørFødselsdato =
                    vilkårResultat.periodeFom?.isBefore(person.fødselsdato)
                        ?: throw Feil("Vilkår ${vilkårResultat.id} har ingen fom-dato og kan ikke patches.")

                val periodeFomOgFødselsdatoErISammeMåned = vilkårResultat.periodeFom!!.toYearMonth() == person.fødselsdato.toYearMonth()
                if (vilkårFomErFørFødselsdato && periodeFomOgFødselsdatoErISammeMåned) {
                    secureLogger.info(
                        "Vilkårresultat ${vilkårResultat.vilkårType} med id=${vilkårResultat.id} på behandling $behandlingId har periodeFom ${vilkårResultat.periodeFom} som er før personens fødselsdato ${person.fødselsdato}. " +
                            "Setter den til personens fødselsdato.",
                    )
                    vilkårResultat.periodeFom = person.fødselsdato
                } else if (vilkårFomErFørFødselsdato) {
                    throw Feil("Vilkår ${vilkårResultat.id} har fom-dato før ${person.fødselsdato}, men de er ikke i samme måned.")
                }
            }
        }

        vilkårsvurdering.personResultater.forEach { personResultat ->
            val person =
                personerPåBehandling?.singleOrNull { it.aktør == personResultat.aktør }
                    ?: throw Feil("Finner ikke person på behandling.")

            if (personResultat.vilkårResultater.any { it.periodeFom?.isBefore(person.fødselsdato) == true && it.vilkårType != UNDER_18_ÅR }) {
                throw Feil("Er fortsatt vilkår som starter før fødselsdato på barn.")
            }
        }
        return vilkårsvurderingService.oppdater(vilkårsvurdering)
    }

    private fun validerKunEttVilkårResultatFørFødselsdato(
        personResultat: PersonResultat,
        vilkårType: Vilkår,
        fødselsdato: LocalDate,
    ) {
        val vilkårResultatAvSammeTypeFørFødselsdatoForPerson =
            personResultat.vilkårResultater
                .filter {
                    it.vilkårType == vilkårType &&
                        it.periodeFom?.isBefore(fødselsdato) ?: true
                }
        if (vilkårResultatAvSammeTypeFørFødselsdatoForPerson.size > 1) {
            throw Feil("Det finnes flere vilkårresultater som begynner før fødselsdato til person: $this")
        }
    }

    fun finnUtenlandskePeriodebeløpSomSkalKorrigeres(): Pair<List<UtenlandskPeriodebeløpEndring>, List<Kompetanse>> {
        val utenlandskePeriodebeløpMedFeilUtbetalingsland = utenlandskPeriodebeløpRepository.hentUtenlandskePeriodebeløpMedFeilUtbetalingsland()
        val behandlinger = utenlandskePeriodebeløpMedFeilUtbetalingsland.map { it.behandlingId }.toSet()
        val sekundærlandsKompetanser = kompetanseRepository.hentSekundærlandsKompetanserForBehandlinger(behandlinger).groupBy { it.behandlingId }

        val utenlandskePeriodebeløpPerBehandling = utenlandskePeriodebeløpMedFeilUtbetalingsland.groupBy { it.behandlingId }

        val kompetanserMedFeil: MutableList<Kompetanse> = mutableListOf()

        val korrigerteUtenlandskePeriodebeløp =
            utenlandskePeriodebeløpPerBehandling.entries.flatMap { (behandlingId, utenlandskePeriodebeløp) ->
                utenlandskePeriodebeløp.tilSeparateTidslinjerForBarna().outerJoin(sekundærlandsKompetanser[behandlingId]!!.tilSeparateTidslinjerForBarna()) { upb, kompetanse ->
                    try {
                        val utbetalingsland = kompetanse?.utbetalingsland()
                        when {
                            kompetanse == null -> null
                            upb == null -> null
                            utbetalingsland == null -> throw Feil("Skal ikke kunne skje")
                            upb.utbetalingsland != utbetalingsland ->
                                UtenlandskPeriodebeløpEndring(id = upb.id, behandlingId = behandlingId, utbetalingslandOriginalt = upb.utbetalingsland, utbetalingslandKorrigert = utbetalingsland)

                            else -> null
                        }
                    } catch (e: Exception) {
                        kompetanserMedFeil.add(kompetanse!!)
                        null
                    }
                }.flatMap { (_, tidslinjer) -> tidslinjer.perioder().mapNotNull { periode -> periode.innhold } }
            }
        return Pair(korrigerteUtenlandskePeriodebeløp, kompetanserMedFeil)
    }
}

data class UtenlandskPeriodebeløpEndring(
    val id: Long,
    val behandlingId: Long,
    val utbetalingslandOriginalt: String?,
    val utbetalingslandKorrigert: String,
)

interface FagsakMedFlereMigreringer {
    val fagsakId: Long
    val fødselsnummer: String
}
