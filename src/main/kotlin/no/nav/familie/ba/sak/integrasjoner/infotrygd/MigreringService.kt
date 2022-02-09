package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.MigreringResponseDto
import no.nav.familie.ba.sak.integrasjoner.migrering.MigreringRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.skyggesak.SkyggesakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.fpsak.tidsserie.LocalDateSegment
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month.APRIL
import java.time.Month.AUGUST
import java.time.Month.DECEMBER
import java.time.Month.FEBRUARY
import java.time.Month.JANUARY
import java.time.Month.JULY
import java.time.Month.JUNE
import java.time.Month.MARCH
import java.time.Month.MAY
import java.time.Month.NOVEMBER
import java.time.Month.OCTOBER
import java.time.Month.SEPTEMBER
import java.time.YearMonth

private const val NULLDATO = "000000"

@Service
class MigreringService(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val env: EnvService,
    private val fagsakService: FagsakService,
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val pdlRestClient: PdlRestClient,
    private val personidentService: PersonidentService,
    private val personopplysningerService: PersonopplysningerService,
    private val stegService: StegService,
    private val taskRepository: TaskRepositoryWrapper,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val vilkårService: VilkårService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val migreringRestClient: MigreringRestClient,
    private val skyggesakService: SkyggesakService
) {

    private val logger = LoggerFactory.getLogger(MigreringService::class.java)
    private val secureLog = LoggerFactory.getLogger("secureLogger")
    private val migrertCounter = Metrics.counter("migrering.ok")
    private val migrertAvSaksbehandler = Metrics.counter("migrering.saksbehandler")
    private val migrertAvSaksbehandlerNotificationFeil = Metrics.counter("migrering.saksbehandler.send.feilet")

    @Transactional
    fun migrer(personIdent: String): MigreringResponseDto {
        try {
            validerAtIdentErAktiv(personIdent)

            val løpendeSak = hentLøpendeSakFraInfotrygd(personIdent)

            val underkategori = kastFeilEllerHentUnderkategori(løpendeSak)
            kastfeilHvisIkkeEnDelytelseIInfotrygd(løpendeSak)

            secureLog.info("Migrering: fant løpende sak for $personIdent sak=${løpendeSak.id} stønad=${løpendeSak.stønad?.id}")

            val barnasIdenter = finnBarnMedLøpendeStønad(løpendeSak)

            val personAktør = personidentService.hentOgLagreAktør(personIdent)
            val barnasAktør = personidentService.hentOgLagreAktørIder(barnasIdenter)

            validerStøttetGradering(personAktør) // Midlertidig skrudd av støtte for kode 6 inntil det kan behandles

            validerAtBarnErIRelasjonMedPersonident(personAktør, barnasAktør)

            val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(personIdent)
                .also { kastFeilDersomAlleredeMigrert(it) }

            val behandling = runCatching {
                stegService.håndterNyBehandling(
                    NyBehandling(
                        søkersIdent = personIdent,
                        behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                        behandlingÅrsak = BehandlingÅrsak.MIGRERING,
                        skalBehandlesAutomatisk = true,
                        underkategori = underkategori,
                        barnasIdenter = barnasIdenter
                    )
                )
            }.getOrElse { kastOgTellMigreringsFeil(MigreringsfeilType.KAN_IKKE_OPPRETTE_BEHANDLING, it.message, it) }

            vilkårService.hentVilkårsvurdering(behandlingId = behandling.id)?.apply {
                forsøkSettPerioderFomTilpassetInfotrygdKjøreplan(this)
                vilkårsvurderingService.oppdater(this)
            } ?: kastOgTellMigreringsFeil(MigreringsfeilType.MANGLER_VILKÅRSVURDERING)

            val behandlingEtterVilkårsvurdering =
                stegService.håndterVilkårsvurdering(behandling) // Se funksjonen lagVilkårsvurderingForMigreringsbehandling i VilkårService

            val førsteUtbetalingsperiode = finnFørsteUtbetalingsperiode(behandling.id)

            sammenlignFørsteUtbetalingsbeløpMedBeløpFraInfotrygd(førsteUtbetalingsperiode.value, løpendeSak.stønad!!)

            iverksett(behandlingEtterVilkårsvurdering)
            migrertCounter.increment()
            val migreringResponseDto = MigreringResponseDto(
                fagsakId = behandlingEtterVilkårsvurdering.fagsak.id,
                behandlingId = behandlingEtterVilkårsvurdering.id,
                infotrygdStønadId = løpendeSak.stønad?.id,
                infotrygdSakId = løpendeSak.id,
                virkningFom = førsteUtbetalingsperiode.fom.toYearMonth(),
                infotrygdTkNr = løpendeSak.tkNr,
                infotrygdIverksattFom = løpendeSak.stønad?.iverksattFom,
                infotrygdVirkningFom = løpendeSak.stønad?.virkningFom,
                infotrygdRegion = løpendeSak.region
            )
            secureLog.info("Ferdig migrert $personIdent. Response til familie-ba-migrering: $migreringResponseDto")

            if (!SikkerhetContext.erSystemKontekst() && !env!!.erDev()) {
                logger.info("Sender manuelt trigget migrering til familie-ba-migrering")
                Result.runCatching {
                    migrertAvSaksbehandler.increment()
                    migreringRestClient.migrertAvSaksbehandler(personIdent, migreringResponseDto)
                }.onFailure {
                    // logg og fortsett siden dette ikke er kritisk
                    logger.warn("Klarte ikke sende migrert av saksbehandler til familie-ba-migrering. $migreringResponseDto")
                    migrertAvSaksbehandlerNotificationFeil.increment()
                }
            }
            return migreringResponseDto
        } catch (e: Exception) {
            if (e is KanIkkeMigrereException) throw e
            kastOgTellMigreringsFeil(MigreringsfeilType.UKJENT, e.message, e)
        }
    }

    private fun validerStøttetGradering(personAktør: Aktør) {
        val adressebeskyttelse = personopplysningerService.hentAdressebeskyttelseSomSystembruker(personAktør)
        if (adressebeskyttelse == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG) {
            kastOgTellMigreringsFeil(MigreringsfeilType.IKKE_STØTTET_GRADERING)
        }
    }

    private fun validerAtIdentErAktiv(personIdent: String) {
        val søkerIdenter = personidentService.hentIdenter(personIdent = personIdent, historikk = true)
            .filter { it.gruppe == "FOLKEREGISTERIDENT" }

        if (søkerIdenter.single { it.ident == personIdent }.historisk) {
            secureLog.warn("Personident $personIdent er historisk, og kan ikke brukes til å migrere$søkerIdenter")
            kastOgTellMigreringsFeil(MigreringsfeilType.IDENT_IKKE_LENGER_AKTIV)
        }
    }

    private fun validerAtBarnErIRelasjonMedPersonident(personAktør: Aktør, barnasAktør: List<Aktør>) {
        val barnasIdenter = barnasAktør.map { it.aktivFødselsnummer() }

        val listeBarnFraPdl = pdlRestClient.hentForelderBarnRelasjon(personAktør)
            .filter { it.relatertPersonsRolle == FORELDERBARNRELASJONROLLE.BARN }
            .map { it.relatertPersonsIdent }
        if (!listeBarnFraPdl.containsAll(barnasIdenter)) {
            secureLog.info(
                "Kan ikke migrere person ${personAktør.aktivFødselsnummer()} fordi barn fra PDL IKKE inneholder alle løpende barnetrygdbarn fra Infotrygd.\n" +
                    "Barn fra PDL: ${listeBarnFraPdl}\n Barn fra Infotrygd: $barnasIdenter"
            )
            kastOgTellMigreringsFeil(MigreringsfeilType.DIFF_BARN_INFOTRYGD_OG_PDL)
        }
    }

    private fun kastFeilDersomAlleredeMigrert(fagsak: Fagsak) {
        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id)
        if (aktivBehandling != null) {
            val behandlinger = behandlingRepository.finnBehandlinger(fagsak.id).sortedBy { it.opprettetTidspunkt }

            behandlinger.findLast { it.erMigrering() && !it.erHenlagt() }?.apply {
                when (fagsak.status) {
                    FagsakStatus.OPPRETTET -> {
                        kastOgTellMigreringsFeil(MigreringsfeilType.MIGRERING_ALLEREDE_PÅBEGYNT)
                    }
                    FagsakStatus.LØPENDE -> {
                        kastOgTellMigreringsFeil(MigreringsfeilType.ALLEREDE_MIGRERT)
                    }
                    FagsakStatus.AVSLUTTET -> {
                        behandlinger.find { it.opprettetTidspunkt.isAfter(this.opprettetTidspunkt) }
                            ?: kastOgTellMigreringsFeil(MigreringsfeilType.FAGSAK_AVSLUTTET_UTEN_MIGRERING)
                    }
                }
            } ?: kastOgTellMigreringsFeil(MigreringsfeilType.AKTIV_BEHANDLING)
        }
    }

    private fun hentLøpendeSakFraInfotrygd(personIdent: String): Sak {
        val (ferdigBehandledeSaker, åpneSaker) = infotrygdBarnetrygdClient.hentSaker(listOf(personIdent)).bruker.partition { it.status == "FB" }
        if (åpneSaker.isNotEmpty()) {
            kastOgTellMigreringsFeil(MigreringsfeilType.ÅPEN_SAK_INFOTRYGD)
        }

        val ikkeOpphørteSaker = ferdigBehandledeSaker.sortedByDescending { it.iverksattdato }
            .filter {
                it.stønad != null && (it.stønad!!.opphørsgrunn == "0" || it.stønad!!.opphørsgrunn.isNullOrEmpty())
            }

        if (ikkeOpphørteSaker.size > 1) {
            kastOgTellMigreringsFeil(MigreringsfeilType.FLERE_LØPENDE_SAKER_INFOTRYGD)
        }

        if (ikkeOpphørteSaker.isEmpty()) {
            kastOgTellMigreringsFeil(MigreringsfeilType.INGEN_LØPENDE_SAK_INFOTRYGD)
        }
        return ikkeOpphørteSaker.first()
    }

    private fun kastFeilEllerHentUnderkategori(sak: Sak): BehandlingUnderkategori {

        return when {
            (sak.valg == "OR" && sak.undervalg == "OS") -> {
                BehandlingUnderkategori.ORDINÆR
            }
            (sak.valg == "UT" && sak.undervalg == "EF" && !env.erProd()) -> {
                BehandlingUnderkategori.UTVIDET
            }
            else -> {
                kastOgTellMigreringsFeil(MigreringsfeilType.IKKE_STØTTET_SAKSTYPE)
            }
        }
    }

    private fun kastfeilHvisIkkeEnDelytelseIInfotrygd(sak: Sak) {
        when (sak.stønad!!.delytelse.filter { it.tom == null }.size) {
            1 -> return
            else -> {
                kastOgTellMigreringsFeil(MigreringsfeilType.UGYLDIG_ANTALL_DELYTELSER_I_INFOTRYGD)
            }
        }
    }

    private fun finnBarnMedLøpendeStønad(løpendeSak: Sak): List<String> {
        val barnasIdenter = løpendeSak.stønad!!.barn
            .filter { it.barnetrygdTom == NULLDATO }
            .map { it.barnFnr!! }

        if (barnasIdenter.isEmpty()) {
            kastOgTellMigreringsFeil(
                MigreringsfeilType.INGEN_BARN_MED_LØPENDE_STØNAD_I_INFOTRYGD,
                "Fant ingen barn med løpende stønad på sak ${løpendeSak.saksblokk}${løpendeSak.saksnr} på bruker i Infotrygd."
            )
        }
        return barnasIdenter
    }

    private fun forsøkSettPerioderFomTilpassetInfotrygdKjøreplan(vilkårsvurdering: Vilkårsvurdering) {
        val inneværendeMåned = YearMonth.now()
        vilkårsvurdering.personResultater.forEach { personResultat ->
            personResultat.vilkårResultater.forEach {
                it.periodeFom = it.periodeFom ?: virkningsdatoFra(infotrygdKjøredato(inneværendeMåned))
            }
        }
    }

    private fun virkningsdatoFra(kjøredato: LocalDate): LocalDate {
        LocalDate.now().run {
            return when {
                env?.erPreprod() ?: false -> LocalDate.of(2022, 1, 1)
                this.isBefore(kjøredato) -> this.førsteDagIInneværendeMåned()
                this.isAfter(kjøredato.plusDays(1)) -> this.førsteDagINesteMåned()
                env!!.erDev() -> this.førsteDagINesteMåned()
                else -> {
                    kastOgTellMigreringsFeil(
                        MigreringsfeilType.IKKE_GYLDIG_KJØREDATO,
                        "Migrering er midlertidig deaktivert frem til ${kjøredato.plusDays(2)} da det kolliderer med Infotrygds kjøredato"
                    )
                }
            }.minusMonths(1)
        }
    }

    private fun infotrygdKjøredato(yearMonth: YearMonth): LocalDate {
        yearMonth.run {
            if (this.year == 2021 || this.year == 2022) {
                return when (this.month) {
                    JANUARY -> 18
                    FEBRUARY -> 15
                    MARCH -> 18
                    APRIL -> 19
                    MAY -> 16
                    JUNE -> 17
                    JULY -> 18
                    AUGUST -> 18
                    SEPTEMBER -> 19
                    OCTOBER -> 18
                    NOVEMBER -> 17
                    DECEMBER -> 5
                }.run { yearMonth.atDay(this) }
            }
        }
        kastOgTellMigreringsFeil(
            MigreringsfeilType.IKKE_GYLDIG_KJØREDATO,
            "Kopien av Infotrygds kjøreplan er utdatert."
        )
    }

    private fun finnFørsteUtbetalingsperiode(behandlingId: Long): LocalDateSegment<Int> {
        return tilkjentYtelseRepository.findByBehandlingOptional(behandlingId)?.andelerTilkjentYtelse
            ?.let { andelerTilkjentYtelse: MutableSet<AndelTilkjentYtelse> ->
                if (andelerTilkjentYtelse.isEmpty()) {
                    kastOgTellMigreringsFeil(MigreringsfeilType.MANGLER_ANDEL_TILKJENT_YTELSE)
                }

                val førsteUtbetalingsperiode = beregnUtbetalingsperioderUtenKlassifisering(andelerTilkjentYtelse)
                    .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                    .first()
                førsteUtbetalingsperiode
            } ?: kastOgTellMigreringsFeil(MigreringsfeilType.MANGLER_FØRSTE_UTBETALINGSPERIODE)
    }

    private fun sammenlignFørsteUtbetalingsbeløpMedBeløpFraInfotrygd(
        førsteUtbetalingsbeløp: Int?,
        infotrygdStønad: Stønad,
    ) {
        val beløpFraInfotrygd =
            infotrygdStønad.delytelse.singleOrNull { it.tom == null }?.beløp?.toInt()
                ?: kastOgTellMigreringsFeil(MigreringsfeilType.FLERE_DELYTELSER_I_INFOTRYGD)

        if (førsteUtbetalingsbeløp != beløpFraInfotrygd) {
            kastOgTellMigreringsFeil(
                MigreringsfeilType.BEREGNET_BELØP_FOR_UTBETALING_ULIKT_BELØP_FRA_INFOTRYGD,
                MigreringsfeilType.BEREGNET_BELØP_FOR_UTBETALING_ULIKT_BELØP_FRA_INFOTRYGD.beskrivelse +
                    "($førsteUtbetalingsbeløp(ba-sak) ≠ $beløpFraInfotrygd(infotrygd))"
            )
        }
    }

    private fun iverksett(behandling: Behandling) {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id) ?: kastOgTellMigreringsFeil(
            MigreringsfeilType.IVERKSETT_BEHANDLING_UTEN_VEDTAK,
            "${MigreringsfeilType.IVERKSETT_BEHANDLING_UTEN_VEDTAK.beskrivelse} ${behandling.id}"
        )
        if (env!!.erPreprod()) {
            vedtak.vedtaksdato = LocalDate.of(2022, 1, 1).atStartOfDay()
        }
        vedtakService.oppdater(vedtak)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)
        val task = IverksettMotOppdragTask.opprettTask(behandling, vedtak, SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }
}

enum class MigreringsfeilType(val beskrivelse: String) {
    AKTIV_BEHANDLING("Det finnes allerede en aktiv behandling på personen som ikke er migrering"),
    ALLEREDE_MIGRERT("Personen er allerede migrert"),
    BEREGNET_BELØP_FOR_UTBETALING_ULIKT_BELØP_FRA_INFOTRYGD("Beregnet beløp var ulikt beløp fra Infotryg"),
    DIFF_BARN_INFOTRYGD_OG_PDL("Kan ikke migrere fordi barn fra PDL ikke samsvarer med løpende barnetrygdbarn fra Infotrygd"),
    FAGSAK_AVSLUTTET_UTEN_MIGRERING("Personen er allerede migrert"),
    FLERE_DELYTELSER_I_INFOTRYGD("Finnes flere delytelser på sak"),
    FLERE_LØPENDE_SAKER_INFOTRYGD("Fant mer enn én aktiv sak på bruker i infotrygd"),
    IDENT_IKKE_LENGER_AKTIV("Ident ikke lenger aktiv"),
    IKKE_GYLDIG_KJØREDATO("Ikke gyldig kjøredato"),
    IKKE_STØTTET_GRADERING("Personen har ikke støttet gradering"),
    IKKE_STØTTET_SAKSTYPE("Kan kun migrere ordinære saker (OR, OS)"),
    INGEN_BARN_MED_LØPENDE_STØNAD_I_INFOTRYGD("Fant ingen barn med løpende stønad på sak"),
    INGEN_LØPENDE_SAK_INFOTRYGD("Personen har ikke løpende sak i infotrygd"),
    IVERKSETT_BEHANDLING_UTEN_VEDTAK("Fant ikke aktivt vedtak på behandling"),
    KAN_IKKE_OPPRETTE_BEHANDLING("Kan ikke opprette behandling"),
    MANGLER_ANDEL_TILKJENT_YTELSE("Fant ingen andeler tilkjent ytelse på behandlingen"),
    MANGLER_FØRSTE_UTBETALINGSPERIODE("Tilkjent ytelse er null"),
    MANGLER_VILKÅRSVURDERING("Fant ikke vilkårsvurdering."),
    MIGRERING_ALLEREDE_PÅBEGYNT("Migrering allerede påbegynt"),
    UGYLDIG_ANTALL_DELYTELSER_I_INFOTRYGD("Kan kun migrere ordinære saker med nøyaktig ett utbetalingsbeløp"),
    UKJENT("Ukjent migreringsfeil"),
    ÅPEN_SAK_INFOTRYGD("Bruker har åpen behandling i Infotrygd"),
}

open class KanIkkeMigrereException(
    open val feiltype: MigreringsfeilType = MigreringsfeilType.UKJENT,
    open val melding: String? = null,
    open val throwable: Throwable? = null
) : RuntimeException(melding, throwable)

val migreringsFeilCounter = mutableMapOf<String, Counter>()
fun kastOgTellMigreringsFeil(
    feiltype: MigreringsfeilType,
    melding: String? = null,
    throwable: Throwable? = null
): Nothing =
    throw KanIkkeMigrereException(feiltype, melding, throwable).also {
        if (migreringsFeilCounter[feiltype.name] == null) {
            migreringsFeilCounter[feiltype.name] = Metrics.counter("migrering.feil", "type", feiltype.name)
        }

        migreringsFeilCounter[feiltype.name]?.increment()
    }
