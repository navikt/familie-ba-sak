package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår.*
import no.nav.familie.ba.sak.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.fpsak.tidsserie.LocalDateSegment
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month.*
import java.time.YearMonth

@Service
class MigreringService(private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
                       private val fagsakService: FagsakService,
                       private val stegService: StegService,
                       private val vedtakService: VedtakService,
                       private val taskRepository: TaskRepository,
                       private val vilkårService: VilkårService,
                       private val vilkårsvurderingService: VilkårsvurderingService,
                       private val behandlingRepository: BehandlingRepository,
                       private val tilkjentYtelseRepository: TilkjentYtelseRepository,
                       private val totrinnskontrollService: TotrinnskontrollService,
                       private val loggService: LoggService) {

    fun migrer(personIdent: String) {
        fagsakService.hentEllerOpprettFagsakForPersonIdent(personIdent)

        val løpendeSak = hentLøpendeSakFraInfotrygd(personIdent)

        kastFeilDersomSakIkkeErOrdinær(løpendeSak)

        val barnasIdenter = finnBarnMedLøpendeStønad(løpendeSak)

        var behandling = stegService.håndterNyBehandling(NyBehandling(søkersIdent = personIdent,
                                                                      behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                                                                      kategori = BehandlingKategori.NASJONAL,
                                                                      underkategori = BehandlingUnderkategori.ORDINÆR,
                                                                      behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                                                                      skalBehandlesAutomatisk = false,
                                                                      barnasIdenter = barnasIdenter))

        vilkårService.initierVilkårsvurderingForBehandling(behandling, false).also { vilkårsvurdering ->
            vilkårsvurdering.settOppfylt(BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER)
            vilkårsvurdering.forsøkSettPerioderFomTilpassetInfotrygdKjøreplan()
            vilkårsvurderingService.oppdater(vilkårsvurdering)
        }
        stegService.håndterVilkårsvurdering(behandling)

        sammenlignTilkjentYtelseMedBeløpFraInfotrygd(behandling, løpendeSak)

        behandling = behandlingRepository.finnBehandling(behandling.id)

        if (behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET) {
            iverksett(behandling)
        } else {
            throw Feil(message = "Migrering mislyktes",
                       frontendFeilmelding = "Migrering mislyktes")
        }
    }

    private fun hentLøpendeSakFraInfotrygd(personIdent: String): Sak {
        val ikkeOpphørteSaker = infotrygdBarnetrygdClient.hentSaker(listOf(personIdent)).bruker.sortedByDescending { it.iverksattdato }
                .filter {
                    it.stønad != null && it.stønad!!.opphørsgrunn.isNullOrBlank()
                }

        if (ikkeOpphørteSaker.size > 1) {
            throw Feil(message = "Fikk uventet resultat. Fant mer enn én aktiv sak på person i infotrygd",
                       frontendFeilmelding = "Fikk uventet resultat. Fant mer enn én aktiv sak på person i infotrygd",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)
        }

        if (ikkeOpphørteSaker.isEmpty()) {
            throw FunksjonellFeil("Personen har ikke løpende sak i infotrygd.",
                                  "Personen har ikke løpende sak i infotrygd.",
                                  HttpStatus.INTERNAL_SERVER_ERROR)

        }

        return ikkeOpphørteSaker.first()
    }

    private fun kastFeilDersomSakIkkeErOrdinær(sak: Sak) {
        if (!(sak.valg == "OR" && sak.undervalg == "OS")) {
            throw FunksjonellFeil("Kan kun migrere ordinære saker (OR, OS)",
                                  "Kan kun migrere ordinære saker (OR, OS)",
                                  HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun finnBarnMedLøpendeStønad(løpendeSak: Sak): List<String> {
        val barnasIdenter = løpendeSak.stønad!!.barn
                .filter { it.barnetrygdTom == "000000" }
                .map { it.barnFnr!! }

        if (barnasIdenter.isEmpty())
            throw Feil("Fant ingen barn med løpende stønad på sak ${løpendeSak.saksblokk}${løpendeSak.saksnr} for person i Infotrygd.",
                       "Fant ingen barn med løpende stønad på sak ${løpendeSak.saksblokk}${løpendeSak.saksnr} for person i Infotrygd.")
        return barnasIdenter
    }

    private fun Vilkårsvurdering.settOppfylt(vararg vilkår: Vilkår) {
        this.personResultater.forEach { personResultat ->
            personResultat.vilkårResultater.forEach {
                if (vilkår.contains(it.vilkårType)) {
                    it.resultat = Resultat.OPPFYLT
                    it.begrunnelse = "Migrering"
                }
            }
        }
    }

    private fun Vilkårsvurdering.forsøkSettPerioderFomTilpassetInfotrygdKjøreplan() {
        val inneværendeMåned = YearMonth.now()
        this.personResultater.forEach { personResultat ->
            personResultat.vilkårResultater.forEach {
                it.periodeFom = virkningsdatoFra(infotrygdKjøredato(inneværendeMåned))
            }
        }
    }

    private fun virkningsdatoFra(kjøredato: LocalDate): LocalDate {
        LocalDate.now().run {
            return when {
                this.isBefore(kjøredato) -> this.førsteDagIInneværendeMåned()
                this.isAfter(kjøredato.plusDays(1)) -> this.førsteDagINesteMåned()
                else -> throw FunksjonellFeil("Migrering er midlertidig deaktivert frem til ${kjøredato.plusDays(2)}",
                                              "Migrering er midlertidig deaktivert frem til ${kjøredato.plusDays(2)}")
            }
        }
    }

    private fun infotrygdKjøredato(yearMonth: YearMonth): LocalDate {
        yearMonth.run {
            if (this.year != 2021) throw Feil("Migrering mislyktes: Kopien av Infotrygds kjøreplan er utdatert.",
                                              "Migrering mislyktes: Kopien av Infotrygds kjøreplan er utdatert.")
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

    private fun sammenlignTilkjentYtelseMedBeløpFraInfotrygd(behandling: Behandling,
                                                             løpendeSak: Sak) {
        tilkjentYtelseRepository.findByBehandlingOptional(behandling.id)?.andelerTilkjentYtelse?.let {
            if (it.isEmpty()) throw Feil("Migrering mislyktes. Andeler tilkjent ytelse mangler.")

            val førsteUtbetalingsperiode = beregnUtbetalingsperioderUtenKlassifisering(it)
                    .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                    .first()
            val tilkjentBeløp = førsteUtbetalingsperiode.value
            val beløpFraInfotrygd = løpendeSak.stønad!!.delytelse?.beløp?.toInt()

            if (tilkjentBeløp != beløpFraInfotrygd) throw Feil("Migrering mislyktes: Avvik i beregnet beløp ($tilkjentBeløp =/= $beløpFraInfotrygd)")

        } ?: throw Feil("Migrering mislyktes: Tilkjent ytelse er null.")
    }

    private fun iverksett(behandling: Behandling) {
        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling)
        totrinnskontrollService.besluttTotrinnskontroll(behandling, SikkerhetContext.SYSTEM_NAVN, SikkerhetContext.SYSTEM_FORKORTELSE, Beslutning.GODKJENT)
        loggService.opprettBeslutningOmVedtakLogg(behandling, Beslutning.GODKJENT)
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                     ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")
        vedtak.vedtaksdato = LocalDateTime.now()
        vedtakService.oppdater(vedtak)
        val task = IverksettMotOppdragTask.opprettTask(behandling, vedtak, SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }
}