package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.SKAL_MIGRERE_ORDINÆR_DELT_BOSTED
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.SKAL_MIGRERE_UTVIDET_DELT_BOSTED
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.MigreringResponseDto
import no.nav.familie.ba.sak.integrasjoner.migrering.MigreringRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
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
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.kontrakter.ba.infotrygd.Sak
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
import javax.validation.ConstraintViolationException

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
    private val featureToggleService: FeatureToggleService
) {

    private val logger = LoggerFactory.getLogger(MigreringService::class.java)
    private val secureLog = LoggerFactory.getLogger("secureLogger")
    private val migrertCounter = Metrics.counter("migrering.ok")
    private val migrertUtvidetCounter = Metrics.counter("migrering.utvidet.ok")
    private val migrertAvSaksbehandler = Metrics.counter("migrering.saksbehandler")
    private val migrertAvSaksbehandlerNotificationFeil = Metrics.counter("migrering.saksbehandler.send.feilet")

    @Transactional
    fun migrer(personIdent: String): MigreringResponseDto {
        try {
            validerAtIdentErAktiv(personIdent)

            val løpendeInfotrygdsak = hentLøpendeSakFraInfotrygd(personIdent)

            if (løpendeInfotrygdsak.undervalg == "MD" && erToggleForDeltBostedAvskrudd(løpendeInfotrygdsak)) {
                secureLog.warn("Migrering: Kan ikke migrere ${løpendeInfotrygdsak.valg}-saker med delt bosted")
                kastOgTellMigreringsFeil(MigreringsfeilType.IKKE_STØTTET_SAKSTYPE)
            }
            val underkategori = kastFeilEllerHentUnderkategori(løpendeInfotrygdsak)
            kastfeilHvisIkkeEnDelytelseIInfotrygd(løpendeInfotrygdsak)

            secureLog.info("Migrering: fant løpende sak for $personIdent sak=${løpendeInfotrygdsak.id} stønad=${løpendeInfotrygdsak.stønad?.id}")

            val barnasIdenter = finnBarnMedLøpendeStønad(løpendeInfotrygdsak)
            if (personIdent in barnasIdenter) {
                secureLog.info("Migrering: $personIdent er lik barn registert på stønad=${løpendeInfotrygdsak.stønad?.id}")
                kastOgTellMigreringsFeil(MigreringsfeilType.INSTITUSJON)
            }

            // Vi ønsker at steg'ene selv lagrer aktører. De blir cachet i appen så det blir ikke gjort nytt kall mot PDL
            val personAktør = personidentService.hentOgLagreAktør(personIdent, false)
            val barnasAktør = personidentService.hentOgLagreAktørIder(barnasIdenter, false)
            kastFeilVedDobbeltforekomstViaHistoriskIdent(barnasAktør, barnasIdenter)

            try {
                fagsakService.hentEllerOpprettFagsakForPersonIdent(personIdent)
                    .also { kastFeilDersomAlleredeMigrert(it) }
            } catch (exception: Exception) {
                if (exception is ConstraintViolationException) {
                    logger.warn("Migrering: Klarte ikke å opprette fagsak på grunn av krasj i databasen, prøver igjen senere. Feilmelding: ${exception.message}.")
                    kastOgTellMigreringsFeil(MigreringsfeilType.UKJENT)
                }

                throw exception
            }

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

            val migreringsdato = virkningsdatoFra(infotrygdKjøredato(YearMonth.now()))

            vilkårService.hentVilkårsvurdering(behandlingId = behandling.id)?.apply {
                forsøkSettPerioderFomTilpassetInfotrygdKjøreplan(this, migreringsdato)
                if (løpendeInfotrygdsak.undervalg == "MD") leggTilVilkårsvurderingDeltBostedPåBarna()
                vilkårsvurderingService.oppdater(this)
            } ?: kastOgTellMigreringsFeil(MigreringsfeilType.MANGLER_VILKÅRSVURDERING)
            // Lagre ned migreringsdato
            behandlingService.lagreNedMigreringsdato(migreringsdato, behandling)

            val behandlingEtterVilkårsvurdering =
                stegService.håndterVilkårsvurdering(behandling) // Se funksjonen lagVilkårsvurderingForMigreringsbehandling i VilkårService

            val førsteAndelerTilkjentYtelse = finnFørsteAndelerTilkjentYtelse(behandling.id)

            sammenlignBeregnetYtelseMedNåværendeFraInfotrygd(førsteAndelerTilkjentYtelse, løpendeInfotrygdsak, personIdent, barnasIdenter)

            iverksett(behandlingEtterVilkårsvurdering)

            when (underkategori) {
                BehandlingUnderkategori.ORDINÆR -> migrertCounter.increment()
                BehandlingUnderkategori.UTVIDET -> migrertUtvidetCounter.increment()
            }
            val migreringResponseDto = MigreringResponseDto(
                fagsakId = behandlingEtterVilkårsvurdering.fagsak.id,
                behandlingId = behandlingEtterVilkårsvurdering.id,
                infotrygdStønadId = løpendeInfotrygdsak.stønad?.id,
                infotrygdSakId = løpendeInfotrygdsak.id,
                virkningFom = førsteAndelerTilkjentYtelse.first().stønadFom,
                infotrygdTkNr = løpendeInfotrygdsak.tkNr,
                infotrygdIverksattFom = løpendeInfotrygdsak.stønad?.iverksattFom,
                infotrygdVirkningFom = løpendeInfotrygdsak.stønad?.virkningFom,
                infotrygdRegion = løpendeInfotrygdsak.region
            )
            secureLog.info("Ferdig migrert $personIdent. Response til familie-ba-migrering: $migreringResponseDto")

            if (!SikkerhetContext.erSystemKontekst() && !env.erDev()) {
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

    private fun kastFeilVedDobbeltforekomstViaHistoriskIdent(barnasAktør: List<Aktør>, barnasIdenter: List<String>) {
        val dobbeltforekomster =
            barnasAktør.filter { barnasAktør.count { barn -> barn.aktørId == it.aktørId } > 1 }.toSet()

        if (dobbeltforekomster.isNotEmpty()) {
            secureLog.warn(
                "Kan ikke migrere fordi barnasIdenter $barnasIdenter inneholder en eller flere historiske identer tilhørende samme barn" +
                    " som en annen ident. Fant følgende dobbeltforekomster: $dobbeltforekomster"
            )
            kastOgTellMigreringsFeil(MigreringsfeilType.HISTORISK_IDENT_REGNET_SOM_EKSTRA_BARN_I_INFOTRYGD)
        }
    }

    private fun erToggleForDeltBostedAvskrudd(infotrygdsak: Sak): Boolean {
        return when (infotrygdsak.valg) {
            "OR" -> !featureToggleService.isEnabled(SKAL_MIGRERE_ORDINÆR_DELT_BOSTED, false)
            "UT" -> !featureToggleService.isEnabled(SKAL_MIGRERE_UTVIDET_DELT_BOSTED, false)
            else -> true
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

            // Ikke migrer hvis det er en annen dagsaktuell migreringsebehandling. Infotrygd leser ikke fra feed før om kvelden.
            // For å forhindre dobbelt migrerte saker, som at saksbehandler migrer og henlegger sak samme dag, og den automatisk migreres senere
            if (behandlinger.any { it.type == BehandlingType.MIGRERING_FRA_INFOTRYGD && it.opprettetTidspunkt.toLocalDate() == LocalDate.now() }) {
                kastOgTellMigreringsFeil(MigreringsfeilType.KUN_ETT_MIGRERINGFORSØK_PER_DAG)
            }
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
            (sak.valg == "OR" && sak.undervalg in listOf("OS", "MD")) -> {
                BehandlingUnderkategori.ORDINÆR
            }
            (sak.valg == "UT" && sak.undervalg in listOf("EF", "MD")) -> {
                BehandlingUnderkategori.UTVIDET
            }
            else -> {
                kastOgTellMigreringsFeil(MigreringsfeilType.IKKE_STØTTET_SAKSTYPE)
            }
        }
    }

    private fun kastfeilHvisIkkeEnDelytelseIInfotrygd(sak: Sak) {
        when {
            sak.valg == "OR" && sak.stønad!!.delytelse.filter { it.tom == null }.size == 1 -> {
                return
            }
            sak.valg == "UT" && sak.stønad!!.delytelse.filter { it.tom == null }.size == 1 -> {
                return
            }
            sak.valg == "UT" && sak.stønad!!.delytelse.filter { it.tom == null }.size == 2 -> {
                return
            }
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

    private fun forsøkSettPerioderFomTilpassetInfotrygdKjøreplan(
        vilkårsvurdering: Vilkårsvurdering,
        migreringsdato: LocalDate
    ) {
        vilkårsvurdering.personResultater.forEach { personResultat ->
            personResultat.vilkårResultater.forEach {
                it.periodeFom = it.periodeFom ?: migreringsdato
            }
        }
    }

    private fun virkningsdatoFra(kjøredato: LocalDate): LocalDate {
        LocalDate.now().run {
            return when {
                env.erPreprod() -> LocalDate.of(2022, 1, 1)
                this.isBefore(kjøredato) -> this.førsteDagIInneværendeMåned()
                this.isAfter(kjøredato.plusDays(1)) -> this.førsteDagINesteMåned()
                env.erDev() -> this.førsteDagINesteMåned()
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

    private fun Vilkårsvurdering.leggTilVilkårsvurderingDeltBostedPåBarna() {
        this.personResultater.filter { !it.erSøkersResultater() }.forEach {
            val vilkårBorMedSøker = it.vilkårResultater.find { it.vilkårType == Vilkår.BOR_MED_SØKER }
            vilkårBorMedSøker?.utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED)
        }
    }

    private fun finnFørsteAndelerTilkjentYtelse(behandlingId: Long): List<AndelTilkjentYtelse> {
        val andelerTilkjentYtelse = tilkjentYtelseRepository.findByBehandlingOptional(behandlingId)?.andelerTilkjentYtelse
            ?: kastOgTellMigreringsFeil(MigreringsfeilType.MANGLER_ANDEL_TILKJENT_YTELSE)
        val førsteUtbetalingsMåned = andelerTilkjentYtelse.minOfOrNull { it.stønadFom }
            ?: kastOgTellMigreringsFeil(MigreringsfeilType.MANGLER_ANDEL_TILKJENT_YTELSE)

        return andelerTilkjentYtelse.filter { it.stønadFom == førsteUtbetalingsMåned }
    }

    private fun sammenlignBeregnetYtelseMedNåværendeFraInfotrygd(
        førsteAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        infotrygdSak: Sak,
        fnr: String,
        barnasIdenter: List<String>,
    ) {
        val førsteUtbetalingsbeløp = førsteAndelerTilkjentYtelse.sumOf { it.kalkulertUtbetalingsbeløp }
        val delytelserInfotrygd = infotrygdSak.stønad!!.delytelse.filter { it.tom == null }
        val beløpFraInfotrygd = delytelserInfotrygd.sumOf { it.beløp }.toInt()

        val (søkersAndeler, barnasAndeler) = førsteAndelerTilkjentYtelse.partition { it.erSøkersAndel() }

        if (barnasIdenter.size != barnasAndeler.groupBy { it.aktør.aktørId }.size) {
            secureLog.info(
                "Migrering ble stoppet fordi det var barn på stønaden i Infotrygd det ikke ble tilkjent ytelse for:\n" +
                    "${barnasIdenter.filterNot { barnasAndeler.personidenter.contains(it) }}"
            )
            kastOgTellMigreringsFeil(MigreringsfeilType.DIFF_BARN_INFOTRYGD_OG_BA_SAK)
        }
        if (søkersAndeler.any { it.erSmåbarnstillegg() }) {
            delytelserInfotrygd.find { it.typeDelytelse == "SM" }
                ?: kastOgTellMigreringsFeil(MigreringsfeilType.SMÅBARNSTILLEGG_BA_SAK_IKKE_INFOTRYGD)
        }
        if (delytelserInfotrygd.any { it.typeDelytelse == "SM" }) {
            søkersAndeler.find { it.erSmåbarnstillegg() }
                ?: kastOgTellMigreringsFeil(MigreringsfeilType.SMÅBARNSTILLEGG_INFOTRYGD_IKKE_BA_SAK)
        }

        if (førsteUtbetalingsbeløp != beløpFraInfotrygd) {
            val beløpfeilType = if (infotrygdSak.undervalg == "MD")
                MigreringsfeilType.BEREGNET_DELT_BOSTED_BELØP_ULIKT_BELØP_FRA_INFOTRYGD else
                MigreringsfeilType.BEREGNET_BELØP_FOR_UTBETALING_ULIKT_BELØP_FRA_INFOTRYGD
            secureLog.info(
                "Ulikt beløp ba-sak og infotrygd migrering. Andeler fra og med ${førsteAndelerTilkjentYtelse.first().stønadFom}: " +
                    "$førsteAndelerTilkjentYtelse"
            )
            secureLog.info("Beløp fra infotrygd sammsvarer ikke med beløp fra ba-sak for ${infotrygdSak.valg} ${infotrygdSak.undervalg} fnr=$fnr baSak=$førsteUtbetalingsbeløp infotrygd=$beløpFraInfotrygd")
            kastOgTellMigreringsFeil(
                beløpfeilType,
                beløpfeilType.beskrivelse +
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
        if (env.erPreprod()) {
            vedtak.vedtaksdato = LocalDate.of(2022, 1, 1).atStartOfDay()
        }
        vedtakService.oppdater(vedtak)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)
        val task = IverksettMotOppdragTask.opprettTask(behandling, vedtak, SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }
}

private val List<AndelTilkjentYtelse>.personidenter: List<String>
    get() {
        return flatMap { it.aktør.personidenter.map { it.fødselsnummer } }
    }

enum class MigreringsfeilType(val beskrivelse: String) {
    AKTIV_BEHANDLING("Det finnes allerede en aktiv behandling på personen som ikke er migrering"),
    ALLEREDE_MIGRERT("Personen er allerede migrert"),
    BEREGNET_BELØP_FOR_UTBETALING_ULIKT_BELØP_FRA_INFOTRYGD("Beregnet beløp var ulikt beløp fra Infotrygd"),
    BEREGNET_DELT_BOSTED_BELØP_ULIKT_BELØP_FRA_INFOTRYGD("Beløp beregnet for delt bosted var ulikt beløp fra Infotrygd"),
    DIFF_BARN_INFOTRYGD_OG_BA_SAK("Antall barn på tilkjent ytelse samsvarer ikke med antall barn på stønaden fra infotrygd"),
    DIFF_BARN_INFOTRYGD_OG_PDL("Kan ikke migrere fordi barn fra PDL ikke samsvarer med løpende barnetrygdbarn fra Infotrygd"),
    FAGSAK_AVSLUTTET_UTEN_MIGRERING("Personen er allerede migrert"),
    FLERE_DELYTELSER_I_INFOTRYGD("Finnes flere delytelser på sak"),
    FLERE_LØPENDE_SAKER_INFOTRYGD("Fant mer enn én aktiv sak på bruker i infotrygd"),
    HISTORISK_IDENT_REGNET_SOM_EKSTRA_BARN_I_INFOTRYGD("Listen med barn fra Infotrygd har identer tilhørende samme barn"),
    IDENT_IKKE_LENGER_AKTIV("Ident ikke lenger aktiv"),
    IKKE_GYLDIG_KJØREDATO("Ikke gyldig kjøredato"),
    IKKE_STØTTET_GRADERING("Personen har ikke støttet gradering"),
    IKKE_STØTTET_SAKSTYPE("Kan kun migrere ordinære(OR OS) og utvidet(UT EF) saker"),
    INGEN_BARN_MED_LØPENDE_STØNAD_I_INFOTRYGD("Fant ingen barn med løpende stønad på sak"),
    INGEN_LØPENDE_SAK_INFOTRYGD("Personen har ikke løpende sak i infotrygd"),
    INSTITUSJON("Midlertidig ignoerert fordi det er en institusjon"),
    IVERKSETT_BEHANDLING_UTEN_VEDTAK("Fant ikke aktivt vedtak på behandling"),
    KAN_IKKE_OPPRETTE_BEHANDLING("Kan ikke opprette behandling"),
    KUN_ETT_MIGRERINGFORSØK_PER_DAG("Migrering allerede påbegynt i dag. Vent minst en dag før man prøver igjen"),
    MANGLER_ANDEL_TILKJENT_YTELSE("Fant ingen andeler tilkjent ytelse på behandlingen"),
    MANGLER_FØRSTE_UTBETALINGSPERIODE("Tilkjent ytelse er null"),
    MANGLER_VILKÅRSVURDERING("Fant ikke vilkårsvurdering."),
    MER_ENN_ETT_BARN_PÅ_SAK_AV_TYPE_UT_MD("Migrering av sakstype UT-MD er begrenset til saker med ett barn"),
    MIGRERING_ALLEREDE_PÅBEGYNT("Migrering allerede påbegynt"),
    SMÅBARNSTILLEGG_BA_SAK_IKKE_INFOTRYGD("Uoverensstemmelse angående småbarnstillegg"),
    SMÅBARNSTILLEGG_INFOTRYGD_IKKE_BA_SAK("Uoverensstemmelse angående småbarnstillegg"),
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
