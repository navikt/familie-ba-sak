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
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsTidslinjeService
import no.nav.familie.ba.sak.integrasjoner.økonomi.Utbetalingstidslinje
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.OppdaterTilkjentYtelseService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.task.AktiverMinsideTask
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.tidslinje.verdier
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

@Service
class ForvalterService(
    private val økonomiService: ØkonomiService,
    private val vedtakService: VedtakService,
    private val beregningService: BeregningService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val persongrunnlagService: PersongrunnlagService,
    private val utbetalingsTidslinjeService: UtbetalingsTidslinjeService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val oppdaterTilkjentYtelseService: OppdaterTilkjentYtelseService,
    private val taskService: TaskService,
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

    @Transactional
    fun finnOgPatchAndelerTilkjentYtelseIFagsakerMedAvvik(
        fagsaker: Set<Long>,
        korrigerAndelerFraOgMedDato: LocalDate,
        dryRun: Boolean = true,
    ): List<Pair<Long, List<AndelTilkjentYtelseKorreksjonDto>?>> {
        return fagsaker.map { fagsakId ->
            val sisteIverksatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsakId)
            if (sisteIverksatteBehandling != null) {
                val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId = sisteIverksatteBehandling.id)
                val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.groupBy { Pair(it.aktør, it.type) }
                val utbetalingstidslinjer = utbetalingsTidslinjeService.genererUtbetalingstidslinjerForFagsak(fagsakId = fagsakId)
                val andelTilkjentYtelseKorreksjoner: List<AndelTilkjentYtelseKorreksjon> =
                    utbetalingstidslinjer.flatMap { utbetalingstidslinje ->
                        val andeltidslinjeForUtbetalingstidslinje = finnAndelerTilkjentYtelseForUtbetalingstidslinje(utbetalingstidslinje = utbetalingstidslinje, andelerTilkjentYtelsePerAktørOgType = andelerTilkjentYtelse)
                        utbetalingstidslinje.tidslinje
                            // Ser kun på andeler som løper fra og med korrigerAndelerFraOgMedDato. Tanken er at vi ikke ønsker å korrigere bakover i tid.
                            .beskjærFraOgMed(korrigerAndelerFraOgMedDato)
                            .kombinerMed(andeltidslinjeForUtbetalingstidslinje) { utbetalingsperiode, andelTilkjentYtelse ->
                                // Dersom verken utbetalingsperiode eller andelTilkjentYtelse er null betyr det at de overlapper.
                                // Dersom de har ulike verdier for enten periodeId, forrigePeriodeId eller kildeBehandlingId oppretter vi en AndelTilkjentYtelseKorreksjon
                                // Korreksjonen inneholder andelen med feil samt en korrigert versjon av andelen.
                                if (utbetalingsperiode != null && andelTilkjentYtelse != null) {
                                    if (utbetalingsperiode.periodeId != andelTilkjentYtelse.periodeOffset || utbetalingsperiode.forrigePeriodeId != andelTilkjentYtelse.forrigePeriodeOffset || utbetalingsperiode.behandlingId != andelTilkjentYtelse.kildeBehandlingId) {
                                        return@kombinerMed AndelTilkjentYtelseKorreksjon(andelTilkjentYtelse, andelTilkjentYtelse.copy(id = 0, periodeOffset = utbetalingsperiode.periodeId, forrigePeriodeOffset = utbetalingsperiode.forrigePeriodeId, kildeBehandlingId = utbetalingsperiode.behandlingId))
                                    }
                                }
                                return@kombinerMed null
                            }.filtrerIkkeNull()
                            .tilPerioderIkkeNull()
                            .verdier()
                    }

                if (!dryRun) {
                    // Sletter andeler med feil og oppretter korrigerte andeler.
                    // Slettede andeler lagres i ny tabell PatchetAndelTilkjentYtelse slik at vi har mulighet til å finne ut hvordan de så ut før endringen.
                    oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedKorrigerteAndeler(
                        tilkjentYtelse = tilkjentYtelse,
                        andelTilkjentYtelseKorreksjoner = andelTilkjentYtelseKorreksjoner,
                    )
                }
                return@map Pair(fagsakId, andelTilkjentYtelseKorreksjoner.tilAndelerTilkjentYtelseKorreksjonerDto())
            }
            return@map Pair(fagsakId, null)
        }
    }

    @Transactional
    fun finnFagsakSomSkalHaMinsideAktivertOgLagTask(
        antallFagsaker: Int,
        dryRun: Boolean = true,
    ) {
        val fagsakerSomSkalHaMinsideAktivert =
            fagsakRepository.finnLøpendeFagsakSomIkkeHarFåttMinsideAktivert(PageRequest.of(0, antallFagsaker)).content
        logger.info("Fant ${fagsakerSomSkalHaMinsideAktivert.size} fagsaker som ikke har fått minside aktivert")

        if (!dryRun) {
            logger.info("Oppretter AktiverMinsideTask for ${fagsakerSomSkalHaMinsideAktivert.size} fagsaker")
            fagsakerSomSkalHaMinsideAktivert.forEach { fagsak ->
                taskService.save(
                    AktiverMinsideTask.opprettTask(fagsak.aktør),
                )
            }
        }
    }

    private fun finnAndelerTilkjentYtelseForUtbetalingstidslinje(
        utbetalingstidslinje: Utbetalingstidslinje,
        andelerTilkjentYtelsePerAktørOgType: Map<Pair<Aktør, YtelseType>, List<AndelTilkjentYtelse>>,
    ): Tidslinje<AndelTilkjentYtelse> =
        andelerTilkjentYtelsePerAktørOgType.entries
            .single { (_, andeler) ->

                val førsteOffset =
                    andeler
                        .firstOrNull { it.erAndelSomSkalSendesTilOppdrag() && it.periodeOffset != null }
                        ?.periodeOffset
                if (førsteOffset != null) {
                    return@single utbetalingstidslinje.erTidslinjeForPeriodeId(førsteOffset)
                }
                return@single false
            }.value
            .map { Periode(verdi = it, fom = it.stønadFom.førsteDagIInneværendeMåned(), tom = it.stønadTom.sisteDagIInneværendeMåned()) }
            .tilTidslinje()
}

interface FagsakMedFlereMigreringer {
    val fagsakId: Long
    val fødselsnummer: String
}

data class AndelTilkjentYtelseKorreksjon(
    val andelMedFeil: AndelTilkjentYtelse,
    val korrigertAndel: AndelTilkjentYtelse,
)

fun List<AndelTilkjentYtelseKorreksjon>.tilAndelerTilkjentYtelseKorreksjonerDto() =
    this.map {
        AndelTilkjentYtelseKorreksjonDto(
            andelMedFeil = it.andelMedFeil.tilAndelTilkjentYtelseDto(),
            korrigertAndel = it.korrigertAndel.tilAndelTilkjentYtelseDto(),
        )
    }

fun AndelTilkjentYtelse.tilAndelTilkjentYtelseDto() =
    AndelTilkjentYtelseDto(
        id = this.id,
        stønadFom = this.stønadFom,
        stønadTom = this.stønadTom,
        beløp = this.kalkulertUtbetalingsbeløp,
        periodeId = this.periodeOffset,
        forrigePeriodeId = this.forrigePeriodeOffset,
        kildeBehandlingId = this.kildeBehandlingId,
    )

data class AndelTilkjentYtelseKorreksjonDto(
    val andelMedFeil: AndelTilkjentYtelseDto,
    val korrigertAndel: AndelTilkjentYtelseDto,
)

data class AndelTilkjentYtelseDto(
    val id: Long,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val beløp: Int,
    val periodeId: Long?,
    val forrigePeriodeId: Long?,
    val kildeBehandlingId: Long?,
)
