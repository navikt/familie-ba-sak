package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.MigreringResponseDto
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
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
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.fpsak.tidsserie.LocalDateSegment
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month.APRIL
import java.time.Month.AUGUST
import java.time.Month.DECEMBER
import java.time.Month.JULY
import java.time.Month.JUNE
import java.time.Month.MAY
import java.time.Month.NOVEMBER
import java.time.Month.OCTOBER
import java.time.Month.SEPTEMBER
import java.time.YearMonth

private const val NULLDATO = "000000"

@Service
class MigreringService(
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val stegService: StegService,
    private val vedtakService: VedtakService,
    private val taskRepository: TaskRepositoryWrapper,
    private val vilkårService: VilkårService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val env: EnvService,
    private val pdlRestClient: PdlRestClient
) {

    private val alleredeMigrertPersonFeilmelding = "Personen er allerede migrert."

    @Transactional
    fun migrer(personIdent: String): MigreringResponseDto {
        val løpendeSak = hentLøpendeSakFraInfotrygd(personIdent)

        kastFeilDersomSakIkkeErOrdinær(løpendeSak)

        val barnasIdenter = finnBarnMedLøpendeStønad(løpendeSak)
        validerAtBarnErIRelasjonMedPersonident(personIdent, barnasIdenter)

        fagsakService.hentEllerOpprettFagsakForPersonIdent(personIdent)
            .also { kastFeilDersomAlleredeMigrert(it) }

        val behandling = stegService.håndterNyBehandling(
            NyBehandling(
                søkersIdent = personIdent,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingÅrsak = BehandlingÅrsak.MIGRERING,
                skalBehandlesAutomatisk = true,
                barnasIdenter = barnasIdenter
            )
        )

        vilkårService.hentVilkårsvurdering(behandlingId = behandling.id)?.apply {
            forsøkSettPerioderFomTilpassetInfotrygdKjøreplan(this)
            vilkårsvurderingService.oppdater(this)
        } ?: error("Fant ikke vilkårsvurdering.")

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)

        sammenlignTilkjentYtelseMedBeløpFraInfotrygd(behandlingEtterVilkårsvurdering, løpendeSak)

        iverksett(behandlingEtterVilkårsvurdering)

        return MigreringResponseDto(behandlingEtterVilkårsvurdering.fagsak.id, behandlingEtterVilkårsvurdering.id)
    }

    private fun validerAtBarnErIRelasjonMedPersonident(personIdent: String, barnasIdenter: List<String>) {
        val listeBarnFraPdl = pdlRestClient.hentForelderBarnRelasjon(personIdent)
            .filter { it.relatertPersonsRolle == FORELDERBARNRELASJONROLLE.BARN }.map { it.relatertPersonsIdent }
        if (barnasIdenter.size != listeBarnFraPdl.size || !listeBarnFraPdl.containsAll(barnasIdenter)) {
            throw error("Kan ikke migrere fordi barn fra PDL ikke samsvarer løpende barnetrygdbarn fra Infotrygd.")
        }
    }

    private fun kastFeilDersomAlleredeMigrert(fagsak: Fagsak) {
        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id)
        if (aktivBehandling != null) {
            val behandlinger = behandlingRepository.finnBehandlinger(fagsak.id).sortedBy { it.opprettetTidspunkt }

            behandlinger.findLast { it.erMigrering() && !it.erHenlagt() }?.apply {
                when (fagsak.status) {
                    FagsakStatus.OPPRETTET -> throw FunksjonellFeil("Migrering allerede påbegynt.")
                    FagsakStatus.LØPENDE -> throw FunksjonellFeil(alleredeMigrertPersonFeilmelding)
                    FagsakStatus.AVSLUTTET -> {
                        behandlinger.find { it.erTekniskOpphør() && it.opprettetTidspunkt.isAfter(this.opprettetTidspunkt) }
                            ?: throw FunksjonellFeil(alleredeMigrertPersonFeilmelding)
                    }
                }
            } ?: throw FunksjonellFeil("Det finnes allerede en aktiv behandling på personen som ikke er migrering.")
        }
    }

    private fun hentLøpendeSakFraInfotrygd(personIdent: String): Sak {
        val (ferdigBehandledeSaker, åpneSaker) = infotrygdBarnetrygdClient.hentSaker(listOf(personIdent)).bruker.partition { it.status == "FB" }
        if (åpneSaker.isNotEmpty()) throw FunksjonellFeil("Bruker har åpen behandling i Infotrygd")

        val ikkeOpphørteSaker = ferdigBehandledeSaker.sortedByDescending { it.iverksattdato }
            .filter {
                it.stønad != null && (it.stønad!!.opphørsgrunn == "0" || it.stønad!!.opphørsgrunn.isNullOrEmpty())
            }

        if (ikkeOpphørteSaker.size > 1) {
            throw FunksjonellFeil(
                melding = "Fant mer enn én aktiv sak på bruker i infotrygd",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
            )
        }

        if (ikkeOpphørteSaker.isEmpty()) {
            throw FunksjonellFeil(
                melding = "Personen har ikke løpende sak i infotrygd.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
        return ikkeOpphørteSaker.first()
    }

    private fun kastFeilDersomSakIkkeErOrdinær(sak: Sak) {
        if (!(sak.valg == "OR" && sak.undervalg == "OS")) {
            throw FunksjonellFeil(
                melding = "Kan kun migrere ordinære saker (OR, OS)",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
        when (val antallBeløp = sak.stønad!!.delytelse.size) {
            1 -> return
            else -> throw FunksjonellFeil(
                melding = "Kan kun migrere ordinære saker med nøyaktig ett utbetalingsbeløp. Fant $antallBeløp.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    private fun finnBarnMedLøpendeStønad(løpendeSak: Sak): List<String> {
        val barnasIdenter = løpendeSak.stønad!!.barn
            .filter { it.barnetrygdTom == NULLDATO }
            .map { it.barnFnr!! }

        if (barnasIdenter.isEmpty())
            throw FunksjonellFeil("Fant ingen barn med løpende stønad på sak ${løpendeSak.saksblokk}${løpendeSak.saksnr} på bruker i Infotrygd.")
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
                this.isBefore(kjøredato) -> this.førsteDagIInneværendeMåned()
                this.isAfter(kjøredato.plusDays(1)) -> this.førsteDagINesteMåned()
                env.erDev() || env.erE2E() || env.erPreprod() -> this.førsteDagINesteMåned()
                else -> throw FunksjonellFeil(
                    "Migrering er midlertidig deaktivert frem til ${kjøredato.plusDays(2)} da det kolliderer med Infotrygds kjøredato",
                    "Migrering er midlertidig deaktivert frem til ${kjøredato.plusDays(2)} da det kolliderer med Infotrygds kjøredato"
                )
            }.minusMonths(1)
        }
    }

    private fun infotrygdKjøredato(yearMonth: YearMonth): LocalDate {
        yearMonth.run {
            if (this.year != 2021) throw Feil(
                "Migrering feilet: Kopien av Infotrygds kjøreplan er utdatert.",
                "Migrering feilet: Kopien av Infotrygds kjøreplan er utdatert.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
            )
            return when (this.month) {
                APRIL -> 19
                MAY -> 14
                JUNE -> 17
                JULY -> 19
                AUGUST -> 18
                SEPTEMBER -> 17
                OCTOBER -> 18
                NOVEMBER -> 17
                DECEMBER -> 6
                else -> 19
            }.run { yearMonth.atDay(this) }
        }
    }

    private fun sammenlignTilkjentYtelseMedBeløpFraInfotrygd(
        behandling: Behandling,
        løpendeSak: Sak
    ) {
        tilkjentYtelseRepository.findByBehandlingOptional(behandling.id)?.andelerTilkjentYtelse?.let { andelerTilkjentYtelse: MutableSet<AndelTilkjentYtelse> ->
            if (andelerTilkjentYtelse.isEmpty()) throw Feil(
                "Migrering feilet: Fant ingen andeler tilkjent ytelse på behandlingen",
                "Migrering feilet: Fant ingen andeler tilkjent ytelse på behandlingen",
                HttpStatus.INTERNAL_SERVER_ERROR
            )

            val førsteUtbetalingsperiode = beregnUtbetalingsperioderUtenKlassifisering(andelerTilkjentYtelse)
                .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                .first()
            val tilkjentBeløp = førsteUtbetalingsperiode.value
            val beløpFraInfotrygd =
                løpendeSak.stønad!!.delytelse.singleOrNull()?.beløp?.toInt() ?: error("Finnes flere delytelser på sak")

            if (tilkjentBeløp != beløpFraInfotrygd) throw Feil(
                "Migrering feilet: Nytt, beregnet beløp var ulikt beløp fra Infotrygd " +
                    "($tilkjentBeløp =/= $beløpFraInfotrygd)",
                "Migrering feilet: Nytt, beregnet beløp var ulikt beløp fra Infotrygd " +
                    "($tilkjentBeløp =/= $beløpFraInfotrygd)",
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        } ?: throw Feil(
            "Migrering feilet: Tilkjent ytelse er null.",
            "Migrering feilet: Tilkjent ytelse er null.",
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    private fun iverksett(behandling: Behandling) {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
            ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")
        vedtakService.oppdater(vedtak)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)
        val task = IverksettMotOppdragTask.opprettTask(behandling, vedtak, SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }
}
