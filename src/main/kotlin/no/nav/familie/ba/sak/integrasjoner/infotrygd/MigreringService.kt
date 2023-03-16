package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.commons.foedselsnummer.FoedselsNr
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.del
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonInfo
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.MigreringResponseDto
import no.nav.familie.ba.sak.integrasjoner.migrering.MigreringRestClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
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
private val SISTE_DATO_FORRIGE_SATS = LocalDate.of(2023, 2, 28)

@Service
class MigreringService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
    private val env: EnvService,
    private val fagsakService: FagsakService,
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val personidentService: PersonidentService,
    private val stegService: StegService,
    private val taskRepository: TaskRepositoryWrapper,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val vilkårService: VilkårService,
    private val migreringRestClient: MigreringRestClient,
    private val kompetanseService: KompetanseService,
    private val persongrunnlagService: PersongrunnlagService,
    private val institusjonService: InstitusjonService
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
            if (erIdentHistorisk(personIdent)) {
                secureLog.warn("Personident $personIdent er historisk, og kan ikke brukes til å migrere$personIdent")
                kastOgTellMigreringsFeil(MigreringsfeilType.IDENT_IKKE_LENGER_AKTIV)
            }

            val løpendeInfotrygdsak = hentLøpendeSakFraInfotrygd(personIdent)
            val underkategori = kastFeilEllerHentUnderkategori(løpendeInfotrygdsak)
            kastfeilHvisIkkeEnDelytelseIInfotrygd(løpendeInfotrygdsak)

            secureLog.info("Migrering: fant løpende sak for $personIdent sak=${løpendeInfotrygdsak.id} stønad=${løpendeInfotrygdsak.stønad?.id}")

            val barnasIdenter = finnBarnMedLøpendeStønad(løpendeInfotrygdsak)

            secureLog.info("barnasIdenter=$barnasIdenter")
            if (løpendeInfotrygdsak.erEnslingMindreårig(personIdent, barnasIdenter)) {
                secureLog.info("Migrering: $personIdent er lik barn registert på stønad=${løpendeInfotrygdsak.stønad?.id}")
                kastOgTellMigreringsFeil(MigreringsfeilType.ENSLIG_MINDREÅRIG)
            }

            // Vi ønsker at steg'ene selv lagrer aktører. De blir cachet i appen så det blir ikke gjort nytt kall mot PDL
            val barnasAktør = personidentService.hentOgLagreAktørIder(barnasIdenter, false)
            kastFeilVedDobbeltforekomstViaHistoriskIdent(barnasAktør, barnasIdenter)

            val fagsak = try {
                if (løpendeInfotrygdsak.erInstitusjon()) {
                    løpendeInfotrygdsak.validerInstitusjonSak()
                    val tssEksternId = løpendeInfotrygdsak.stønad?.mottakerNummer!!.toString()

                    val orgNr =
                        institusjonService.hentOrgnummerForTssEksternId(tssEksternId)

                    val instInfo = InstitusjonInfo(orgNummer = orgNr, tssEksternId = tssEksternId)
                    fagsakService.hentEllerOpprettFagsakForPersonIdent(
                        fødselsnummer = personIdent,
                        institusjon = instInfo,
                        fagsakType = FagsakType.INSTITUSJON
                    )
                        .also { kastFeilDersomAlleredeMigrert(it) }
                } else {
                    fagsakService.hentEllerOpprettFagsakForPersonIdent(fødselsnummer = personIdent)
                        .also { kastFeilDersomAlleredeMigrert(it) }
                }
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
                        barnasIdenter = barnasIdenter,
                        kategori = if (erEøsSak(løpendeInfotrygdsak)) BehandlingKategori.EØS else BehandlingKategori.NASJONAL,
                        fagsakId = fagsak.id
                    )
                )
            }.getOrElse {
                secureLog.info("Kan ikke opprette behandling ${it.message}", it)
                kastOgTellMigreringsFeil(MigreringsfeilType.KAN_IKKE_OPPRETTE_BEHANDLING)
            }

            val migreringsdato = virkningsdatoFra(infotrygdKjøredato(YearMonth.now()))

            vilkårService.hentVilkårsvurdering(behandlingId = behandling.id)?.apply {
                forsøkSettPerioderFomTilpassetInfotrygdKjøreplan(this, migreringsdato)
                if (løpendeInfotrygdsak.undervalg == "MD") leggTilVilkårsvurderingDeltBostedPåBarna()
            } ?: kastOgTellMigreringsFeil(MigreringsfeilType.MANGLER_VILKÅRSVURDERING)
            // Lagre ned migreringsdato
            behandlingService.lagreNedMigreringsdato(migreringsdato, behandling)

            val behandlingEtterVilkårsvurdering =
                stegService.håndterVilkårsvurdering(behandling) // Se funksjonen lagVilkårsvurderingForMigreringsbehandling i VilkårService

            val førsteAndelerTilkjentYtelse = finnFørsteAndelerTilkjentYtelse(behandling.id)

            sammenlignBeregnetYtelseMedNåværendeFraInfotrygd(
                førsteAndelerTilkjentYtelse,
                løpendeInfotrygdsak,
                personIdent,
                barnasIdenter
            )

            if (løpendeInfotrygdsak.undervalg == "EU") {
                kompetanseService.hentKompetanser(BehandlingId(behandling.id)).forEach { kompetanse ->
                    kompetanseService.oppdaterKompetanse(
                        BehandlingId(behandling.id),
                        kompetanse.copy(resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND)
                    )
                }
            }

            sammenlingBarnInfotrygdMedBarnBAsak(behandling, barnasIdenter, personIdent)

            iverksett(behandlingEtterVilkårsvurdering)

            when (underkategori) {
                BehandlingUnderkategori.ORDINÆR -> migrertCounter.increment()
                BehandlingUnderkategori.UTVIDET -> migrertUtvidetCounter.increment()
                BehandlingUnderkategori.INSTITUSJON -> throw Feil("Institusjon bruker samme underkategori som Ordinær")
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

            secureLog.info("Ukjent feil ved migrering ${e.message}", e)
            kastOgTellMigreringsFeil(MigreringsfeilType.UKJENT)
        }
    }

    fun sammenlingBarnInfotrygdMedBarnBAsak(
        behandling: Behandling,
        barnasIdenterInfotrygd: List<String>,
        søkersIdent: String
    ) {
        val barna = persongrunnlagService.hentBarna(behandling).map { it.aktør.aktivFødselsnummer() }

        barnasIdenterInfotrygd.forEach { identFraInfotrygd ->
            if (erIdentHistorisk(identFraInfotrygd)) {
                val alleIdenterTilPerson = personidentService.hentIdenter(identFraInfotrygd, false)
                    .filter { it.gruppe == "FOLKEREGISTERIDENT" }.map { it.ident }

                if (!alleIdenterTilPerson.any { barna.contains(it) }) {
                    secureLog.warn("Migrering stoppes fordi liste med barn fra saken i infotrygd ikke stemmer med ba-sak.  basak=$barna , barnasIdenterInfotrygd=$barnasIdenterInfotrygd, identSomManglerIInfotrygd=$alleIdenterTilPerson")
                    kastOgTellMigreringsFeil(MigreringsfeilType.DIFF_BARN_INFOTRYGD_OG_BA_SAK)
                }

                secureLog.info("Barnets ident i infotrygd $identFraInfotrygd er historisk og får ny aktiv ident $alleIdenterTilPerson i ba-sak")
            }
        }
    }

    private fun erEøsSak(løpendeInfotrygdsak: Sak): Boolean {
        return løpendeInfotrygdsak.undervalg == "EU"
    }

    private fun kastFeilVedDobbeltforekomstViaHistoriskIdent(barnasAktør: List<Aktør>, barnasIdenter: List<String>) {
        secureLog.info("barnasAktør=$barnasAktør")
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

    private fun erIdentHistorisk(personIdent: String): Boolean {
        val søkerIdenter = personidentService.hentIdenter(personIdent = personIdent, historikk = true)
            .filter { it.gruppe == "FOLKEREGISTERIDENT" }

        return søkerIdenter.single { it.ident == personIdent }.historisk
    }

    private fun kastFeilDersomAlleredeMigrert(fagsak: Fagsak) {
        val aktivBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsak.id)
        if (aktivBehandling != null) {
            val behandlinger = behandlingHentOgPersisterService.hentBehandlinger(fagsakId = fagsak.id)
                .sortedBy { it.opprettetTidspunkt }

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
        val (ferdigBehandledeSaker, åpneSaker) = infotrygdBarnetrygdClient.hentSaker(listOf(personIdent)).bruker
            .filter { it.resultat != "HB" } // Filterer bort henlagte behandlinger
            .partition { it.status == "FB" }

        if (åpneSaker.isNotEmpty()) {
            kastOgTellMigreringsFeil(MigreringsfeilType.ÅPEN_SAK_INFOTRYGD)
        }

        var ikkeOpphørteSaker = ferdigBehandledeSaker.sortedByDescending { it.iverksattdato }
            .filter {
                it.stønad != null && (it.stønad!!.opphørsgrunn == "0" || it.stønad!!.opphørsgrunn.isNullOrEmpty())
            }

        if (ikkeOpphørteSaker.size > 1) {
            ikkeOpphørteSaker = ikkeOpphørteSaker.filter {
                it.stønad!!.opphørtFom != null && it.stønad!!.opphørtFom == "000000"
            }
            if (ikkeOpphørteSaker.size > 1) {
                kastOgTellMigreringsFeil(MigreringsfeilType.FLERE_LØPENDE_SAKER_INFOTRYGD)
            }
            logger.info("Filtrerte bort saker med opphørtFom satt men med opphørsgrunn 0")
            secureLog.info("Filtrerte bort saker med opphørtFom satt men med opphørsgrunn 0 for ident=$personIdent")
        }

        if (ikkeOpphørteSaker.isEmpty()) {
            kastOgTellMigreringsFeil(MigreringsfeilType.INGEN_LØPENDE_SAK_INFOTRYGD)
        }
        return ikkeOpphørteSaker.first()
    }

    private fun kastFeilEllerHentUnderkategori(sak: Sak): BehandlingUnderkategori {
        return when {
            (sak.valg == "OR" && sak.undervalg in listOf("OS", "MD", "EU")) -> {
                BehandlingUnderkategori.ORDINÆR
            }

            (sak.valg == "UT" && sak.undervalg in listOf("EF", "MD", "EU")) -> {
                BehandlingUnderkategori.UTVIDET
            }

            (sak.erInstitusjon()) -> {
                BehandlingUnderkategori.ORDINÆR
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

            sak.stønad!!.delytelse.filter { it.tom == null }.size == 0 -> {
                if (sak.stønad!!.antallBarn == 0 && sak.stønad!!.barn.isEmpty()) {
                    kastOgTellMigreringsFeil(MigreringsfeilType.DELYTELSE_OG_ANTALLBARN_NULL)
                }
                return
            }

            else -> {
                kastOgTellMigreringsFeil(MigreringsfeilType.UGYLDIG_ANTALL_DELYTELSER_I_INFOTRYGD)
            }
        }
    }

    private fun finnBarnMedLøpendeStønad(løpendeSak: Sak): List<String> {
        val (barnOver18, barnUnder18) = løpendeSak.stønad!!.barn
            .filter { it.barnetrygdTom == NULLDATO }
            .map { it.barnFnr!! }
            .partition { ident -> FoedselsNr(ident).foedselsdato.isSameOrBefore(LocalDate.now().minusYears(18L)) }

        if (barnOver18.size > 0) {
            secureLog.warn("Det er barn på stønaden i infotrygd som er over 18 år. Disse vil bli ignorert.  $barnOver18 sak=$løpendeSak")
        }

        if (barnUnder18.isEmpty()) {
            secureLog.info("Fant ingen barn med løpende stønad på sak ${løpendeSak.saksblokk} ${løpendeSak.saksnr} på bruker i Infotrygd.")
            kastOgTellMigreringsFeil(MigreringsfeilType.INGEN_BARN_MED_LØPENDE_STØNAD_I_INFOTRYGD)
        } else if (barnUnder18.distinct().size != løpendeSak.stønad!!.antallBarn) {
            secureLog.info(
                "${MigreringsfeilType.OPPGITT_ANTALL_BARN_ULIKT_ANTALL_BARNIDENTER.beskrivelse}: " +
                    "barnasIdenter.size=${barnUnder18.size} stønad.antallBarn=${løpendeSak.stønad!!.antallBarn}"
            )
            kastOgTellMigreringsFeil(MigreringsfeilType.OPPGITT_ANTALL_BARN_ULIKT_ANTALL_BARNIDENTER)
        }

        return barnUnder18.distinct()
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
                env.erDev() -> if (this.isBefore(kjøredato)) førsteDagIInneværendeMåned() else førsteDagINesteMåned()
                env.erPreprod() -> LocalDate.of(2022, 1, 1)
                this.isBefore(kjøredato) -> førsteDagIInneværendeMåned()
                this.isAfter(kjøredato.plusDays(1)) -> førsteDagINesteMåned()
                else -> {
                    kastOgTellMigreringsFeil(
                        MigreringsfeilType.IKKE_GYLDIG_KJØREDATO,
                        "Kjøring pågår. Vent med migrering til etter ${kjøredato.plusDays(2)}"
                    )
                }
            }.minusMonths(1)
        }
    }

    fun infotrygdKjøredato(yearMonth: YearMonth): LocalDate {
        when (yearMonth.year) {
            2022 ->
                return when (yearMonth.month) {
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

            2023 -> {
                return when (yearMonth.month) {
                    JANUARY -> 18
                    FEBRUARY -> 15
                    MARCH -> 20
                    APRIL -> 17
                    MAY -> 15
                    JUNE -> 19
                    JULY -> 18
                    AUGUST -> 18
                    SEPTEMBER -> 18
                    OCTOBER -> 18
                    NOVEMBER -> 17
                    DECEMBER -> 4
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
        val andelerTilkjentYtelse =
            tilkjentYtelseRepository.findByBehandlingOptional(behandlingId)?.andelerTilkjentYtelse
                ?: kastOgTellMigreringsFeil(MigreringsfeilType.MANGLER_ANDEL_TILKJENT_YTELSE)
        val førsteUtbetalingsMåned = andelerTilkjentYtelse.minOfOrNull { it.stønadFom }
            ?: kastOgTellMigreringsFeil(MigreringsfeilType.MANGLER_ANDEL_TILKJENT_YTELSE)

        return andelerTilkjentYtelse.filter { it.stønadFom == førsteUtbetalingsMåned }
    }

    private fun sammenlignBeregnetYtelseMedNåværendeFraInfotrygd(
        førsteAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        infotrygdSak: Sak,
        fnr: String,
        barnasIdenter: List<String>
    ) {
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

        val førsteUtbetalingsbeløp = kalkulerFørsteUtbetalingsbeløpSomFørSatsendring(førsteAndelerTilkjentYtelse)
        if (førsteUtbetalingsbeløp != beløpFraInfotrygd) {
            val beløpfeilType = if (infotrygdSak.undervalg == "MD") {
                MigreringsfeilType.BEREGNET_DELT_BOSTED_BELØP_ULIKT_BELØP_FRA_INFOTRYGD
            } else {
                MigreringsfeilType.BEREGNET_BELØP_FOR_UTBETALING_ULIKT_BELØP_FRA_INFOTRYGD
            }
            secureLog.info(
                "Ulikt beløp ba-sak og infotrygd migrering. Andeler fra og med ${førsteAndelerTilkjentYtelse.first().stønadFom}: " +
                    "$førsteAndelerTilkjentYtelse"
            )
            secureLog.info("Beløp fra infotrygd sammsvarer ikke med beløp fra ba-sak for ${infotrygdSak.valg} ${infotrygdSak.undervalg} fnr=$fnr baSak=$førsteUtbetalingsbeløp infotrygd=$beløpFraInfotrygd")
            kastOgTellMigreringsFeil(beløpfeilType)
        }
    }

    fun kalkulerFørsteUtbetalingsbeløpSomFørSatsendring(atys: List<AndelTilkjentYtelse>): Int {
        return atys.sumOf { aty ->
            when (aty.type) {
                YtelseType.ORDINÆR_BARNETRYGD -> {
                    val beløp = if (aty.sats == SatsService.finnSisteSatsFor(SatsType.TILLEGG_ORBA).beløp) {
                        SatsService.finnAlleSatserFor(SatsType.TILLEGG_ORBA).find {
                            it.gyldigTom == SISTE_DATO_FORRIGE_SATS
                        }!!.beløp
                    } else if (aty.sats == SatsService.finnSisteSatsFor(SatsType.ORBA).beløp) {
                        SatsService.finnAlleSatserFor(SatsType.ORBA).find {
                            it.gyldigTom == SISTE_DATO_FORRIGE_SATS
                        }!!.beløp
                    } else {
                        aty.sats
                    }
                    if (aty.prosent == BigDecimal(50)) {
                        beløp.toBigDecimal().del(2.toBigDecimal(), 0).toInt()
                    } else {
                        beløp
                    }
                }

                YtelseType.SMÅBARNSTILLEGG -> {
                    val beløp = SatsService.finnAlleSatserFor(SatsType.SMA).find {
                        it.gyldigTom == SISTE_DATO_FORRIGE_SATS
                    }!!.beløp
                    if (aty.prosent == BigDecimal(50)) {
                        beløp.toBigDecimal().del(2.toBigDecimal(), 0).toInt()
                    } else {
                        beløp
                    }
                }

                YtelseType.UTVIDET_BARNETRYGD -> {
                    val beløp = SatsService.finnAlleSatserFor(SatsType.UTVIDET_BARNETRYGD).find {
                        it.gyldigTom == SISTE_DATO_FORRIGE_SATS
                    }!!.beløp
                    if (aty.prosent == BigDecimal(50)) {
                        beløp.toBigDecimal().del(2.toBigDecimal(), 0).toInt()
                    } else {
                        beløp
                    }
                }

                else -> Integer.valueOf(0)
            }
        }
    }

    private fun iverksett(behandling: Behandling) {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id) ?: kastOgTellMigreringsFeil(
            MigreringsfeilType.IVERKSETT_BEHANDLING_UTEN_VEDTAK
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
    AKTIV_BEHANDLING("Personen har en behandling i BA-sak"),
    ALLEREDE_MIGRERT("Saken er migrert"),
    BEREGNET_BELØP_FOR_UTBETALING_ULIKT_BELØP_FRA_INFOTRYGD("Saken løper med feil beløp i Infotrygd. Må rettes i Infotrygd før migrering."),
    BEREGNET_DELT_BOSTED_BELØP_ULIKT_BELØP_FRA_INFOTRYGD("Saken løper med feil beløp i Infotrygd. Må rettes i Infotrygd før migrering."),
    DIFF_BARN_INFOTRYGD_OG_BA_SAK("Antall barn er ulikt i BA-sak og Infotrygd. Kontroller hvilke barn som skal ha barnetrygd. Dersom saken ligger riktig i Infotrygd, kan den migreres manuelt. Er det feil i Infotrygd må det vurderes om saken kan rettes i Infotrygd før migrering. Hvis den ikke kan rettes i Infotrygd, meld saken i Porten. Velg \"Meld sak om Infotrygd\""),
    FAGSAK_AVSLUTTET_UTEN_MIGRERING("Saken er migrert"),
    FLERE_LØPENDE_SAKER_INFOTRYGD("Personen har en åpen behandling i Infotrygd som må lukkes før migrering."),
    HISTORISK_IDENT_REGNET_SOM_EKSTRA_BARN_I_INFOTRYGD("Saken kan ikke migreres. Meld saken i Porten. Velg '\"Meld sak om Infotrygd\"."),
    IDENT_IKKE_LENGER_AKTIV("Saken kan ikke migreres. Meld saken i Porten. Velg \"Meld sak om Infotrygd\"."),
    IDENT_BARN_IKKE_LENGER_AKTIV("Saken kan ikke migreres. Meld saken i Porten. "),
    INSTITUSJON_MANGLER_INFO("Saken kan ikke migreres. Meld saken i Porten. Velg \"Meld sak om Infotrygd\"."),
    IKKE_GYLDIG_KJØREDATO("Kjøring pågår. Vent med migrering til etter kjøring."),
    IKKE_STØTTET_SAKSTYPE("Denne saken må migreres manuelt."),
    INGEN_BARN_MED_LØPENDE_STØNAD_I_INFOTRYGD("Ingen barn med stønad i Infotrygd. Saken kan ikke migreres. Meld saken i Porten. Velg \"Meld sak om Infotrygd\"."),
    INGEN_LØPENDE_SAK_INFOTRYGD("Saken kan ikke migreres. Meld saken i Porten Velg \"Meld sak om Infotrygd\"."),
    ENSLIG_MINDREÅRIG("Saken kan ikke migreres. Må behandles i Infotrygd"),
    IVERKSETT_BEHANDLING_UTEN_VEDTAK("Saken kan ikke migreres. Meld saken i Porten, velg \"Meld sak om Barnetrygd\"."),
    KAN_IKKE_OPPRETTE_BEHANDLING("Saken kan ikke migreres. Meld saken i Porten. Velg \"Meld sak om Infotrygd\"."),
    KUN_ETT_MIGRERINGFORSØK_PER_DAG("Migrering allerede påbegynt. Vent minst en dag før du prøver igjen."),
    MANGLER_ANDEL_TILKJENT_YTELSE("Saken må migreres manuelt"),
    MANGLER_VILKÅRSVURDERING("Saken må migreres manuelt"),
    MIGRERING_ALLEREDE_PÅBEGYNT("Saken har allerede en påbegynt manuell migrering. Denne må ferdigbehandles eller henlegges."),
    OPPGITT_ANTALL_BARN_ULIKT_ANTALL_BARNIDENTER("Saken må migreres manuelt"),
    SMÅBARNSTILLEGG_BA_SAK_IKKE_INFOTRYGD("Småbarnstillegg må rettes i Infotrygd før migrering."),
    SMÅBARNSTILLEGG_INFOTRYGD_IKKE_BA_SAK("Småbarnstillegg må rettes i Infotrygd før migrering."),
    UGYLDIG_ANTALL_DELYTELSER_I_INFOTRYGD("Saken kan ikke migreres. Meld saken i Porten. Velg \"Meld sak om Infotrygd\"."),
    UKJENT("Saken kan ikke migreres. Meld saken i Porten. Velg \"Meld sak om Infotrygd\"."),
    ÅPEN_SAK_INFOTRYGD("Personen har en åpen behandling i Infotrygd som må lukkes før migrering"),
    DELYTELSE_OG_ANTALLBARN_NULL("Saken trenger ikke å migreres da den ikke har aktiv ytelse med barn i Infotrygd") // Disse kan man nok la være å migrere
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

fun Sak.erInstitusjon() = this.valg == "OR" && this.undervalg == "IB"
fun Sak.validerInstitusjonSak() {
    if (this.stønad?.mottakerNummer == null || this.stønad?.status != "04") { // I følge Infotrygd-Tore så skal Institusjonsstønader ha status 04
        kastOgTellMigreringsFeil(feiltype = MigreringsfeilType.INSTITUSJON_MANGLER_INFO)
    }
}

fun Sak.erEnslingMindreårig(personIdent: String, barnasIdenter: List<String>) =
    personIdent in barnasIdenter && !this.erInstitusjon()