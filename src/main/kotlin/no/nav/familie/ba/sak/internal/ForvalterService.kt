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
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.utbetalingsperioder
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
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
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
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
    fun patchAndelerISisteIverksatteBehandlingMedFeilPeriodeIdEllerKildeBehandlingIdForFagsaker(
        fagsaker: List<Long>,
        dryRun: Boolean,
    ): List<AndelKorreksjonResultat> = fagsaker.map { fagsak -> patchAndelerISisteIverksatteBehandlingMedFeilPeriodeIdEllerKildeBehandlingIdForFagsak(fagsak, dryRun) }

    @Transactional
    fun patchAndelerISisteIverksatteBehandlingMedFeilPeriodeIdEllerKildeBehandlingIdForFagsak(
        fagsakId: Long,
        dryRun: Boolean,
    ): AndelKorreksjonResultat {
        val sisteIverksatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsakId) ?: throw Feil("Finner ikke siste iverksatte behandling for fagsak $fagsakId")
        val perioderForKjeder =
            tilkjentYtelseRepository
                .findByFagsak(fagsakId)
                .fold(mutableMapOf<Long, List<Tidslinje<Utbetalingsperiode>>>()) { kjederForFagsak, tilkjentYtelse ->
                    val kjederForUtbetalingsperioder =
                        tilkjentYtelse
                            .utbetalingsperioder()
                            .sortedBy { it.periodeId }
                            .fold(mutableMapOf<Long, MutableList<Periode<Utbetalingsperiode>>>()) { kjeder, utbetalingsperiode ->
                                kjeder.apply {
                                    val kjede = getOrDefault(utbetalingsperiode.forrigePeriodeId, mutableListOf()) + Periode(utbetalingsperiode, utbetalingsperiode.vedtakdatoFom, utbetalingsperiode.vedtakdatoTom)
                                    put(utbetalingsperiode.periodeId, kjede.toMutableList())
                                    remove(utbetalingsperiode.forrigePeriodeId)
                                }
                            }.mapValues { (_, kjede) -> Pair(kjede.tilTidslinje(), kjede.minOfOrNull { it.verdi.forrigePeriodeId ?: -1 }) }
                    kjederForFagsak.apply {
                        kjederForUtbetalingsperioder.forEach { periodeId, (tidslinje, forrigePeriodeId) ->
                            val kjedeForFagsak = kjederForFagsak.getOrDefault(forrigePeriodeId, mutableListOf()) + tidslinje
                            put(periodeId, kjedeForFagsak.toMutableList())
                            remove(forrigePeriodeId)
                        }
                    }
                }.mapValues { (_, tidslinjerForKjede) ->
                    tidslinjerForKjede.kombiner().tilPerioderIkkeNull()
                }

        // Tidslinje per kjede med gjeldende/siste periodeId for hver periode
        val gjeldeneTidslinjePerKjede =
            perioderForKjeder.mapValues { (_, perioder) ->
                perioder
                    .map { periode -> Periode(periode.verdi.maxBy { utbetalingsperiode -> utbetalingsperiode.periodeId }, periode.fom, periode.tom) }
                    .tilTidslinje()
            }

        // Alle periodeId'er per kjede. Inneholder alle periodeId'er og ikke bare gjeldende/siste for hver periode
        val kjedeIderPerKjede =
            perioderForKjeder.mapValues { (_, perioder) ->
                perioder
                    .flatMap { periode -> periode.verdi.map { utbetalingsperiode -> utbetalingsperiode.periodeId } }
                    .toSet()
                    .sorted()
            }

        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = sisteIverksatteBehandling.id)
        val andelTidslinjer =
            andelerTilkjentYtelse.groupBy { Pair(it.aktør, it.type) }.mapNotNull { (_, andeler) ->
                val perioder = andeler.map { Periode(it, it.stønadFom.førsteDagIInneværendeMåned(), it.stønadTom.sisteDagIInneværendeMåned()) }
                val sistePeriodeId = perioder.maxOf { it.verdi.periodeOffset ?: -1 }
                if (sistePeriodeId != -1L) {
                    sistePeriodeId to perioder.tilTidslinje()
                } else {
                    null
                }
            }

        val andelerMedFeilPeriodeIdEllerKildeBehandlingId =
            andelTidslinjer.flatMap { (periodeId, andelTidslinje) ->
                // Finner ut hvilken kjede andelene tilhører.
                val sistePeriodeIdIKjedenTilAndeler = kjedeIderPerKjede.entries.single { it.value.contains(periodeId) }.key
                andelTidslinje
                    .kombinerMed(gjeldeneTidslinjePerKjede.getOrDefault(sistePeriodeIdIKjedenTilAndeler, tomTidslinje())) { andel, utbetalingsperiode ->
                        // Dersom det finnes en andel for perioden, ta med andelen og utbetalingsperioden dersom andelen ikke har korrekt periodeId og kildeBehandlingId.
                        if (andel != null && utbetalingsperiode != null && (andel.periodeOffset != utbetalingsperiode.periodeId || andel.kildeBehandlingId != utbetalingsperiode.behandlingId)) {
                            Pair(andel, utbetalingsperiode)
                        } else {
                            null
                        }
                    }.tilPerioderIkkeNull()
                    .verdier()
            }

        val nyeAndeler =
            andelerMedFeilPeriodeIdEllerKildeBehandlingId.map { (andel, utbetalingsperiode) ->
                andel.copy(
                    id = 0,
                    periodeOffset = utbetalingsperiode.periodeId,
                    forrigePeriodeOffset = utbetalingsperiode.forrigePeriodeId,
                    kildeBehandlingId = utbetalingsperiode.behandlingId,
                )
            }

        val andelerSomSkalSlettes = andelerMedFeilPeriodeIdEllerKildeBehandlingId.map { (andel, _) -> andel }

        if (andelerSomSkalSlettes.size != nyeAndeler.size) throw Feil("Det må være like mange nye/korrigerte andeler som det er andeler vi sletter")

        if (!dryRun) {
            andelTilkjentYtelseRepository.deleteAll(andelerSomSkalSlettes)
            andelTilkjentYtelseRepository.saveAll(nyeAndeler)
        }

        return AndelKorreksjonResultat(andelerSomSkalSlettes.tilAndelTilkjentYtelseDto(), nyeAndeler.tilAndelTilkjentYtelseDto())
    }
}

fun Collection<AndelTilkjentYtelse>.tilAndelTilkjentYtelseDto(): List<AndelTilkjentYtelseDto> =
    this.map {
        AndelTilkjentYtelseDto(
            fom = it.stønadFom,
            tom = it.stønadTom,
            periodeId = it.periodeOffset,
            forrigePeriodeId = it.forrigePeriodeOffset,
            kildeBehandlingId = it.kildeBehandlingId,
        )
    }

data class AndelTilkjentYtelseDto(
    val fom: YearMonth,
    val tom: YearMonth?,
    val periodeId: Long?,
    val forrigePeriodeId: Long?,
    val kildeBehandlingId: Long?,
)

data class AndelKorreksjonResultat(
    val andelerSomSlettes: List<AndelTilkjentYtelseDto>,
    val andelerSomOppretets: List<AndelTilkjentYtelseDto>,
)

interface FagsakMedFlereMigreringer {
    val fagsakId: Long
    val fødselsnummer: String
}
