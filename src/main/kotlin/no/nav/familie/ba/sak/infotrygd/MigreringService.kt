package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår.*
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MigreringService(private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
                       private val fagsakService: FagsakService,
                       private val stegService: StegService,
                       private val vedtakService: VedtakService,
                       private val taskRepository: TaskRepository,
                       private val vilkårService: VilkårService,
                       private val vilkårsvurderingService: VilkårsvurderingService,
                       private val totrinnskontrollService: TotrinnskontrollService,
                       private val loggService: LoggService) {

    fun migrer(personIdent: String) {
        fagsakService.hentEllerOpprettFagsakForPersonIdent(personIdent)

        val løpendeSak = hentLøpendeSakFraInfotrygd(personIdent)

        kastFeilDersomSakIkkeErOrdinær(løpendeSak)

        val barnasIdenter = løpendeSak.stønadList
                .first().barn
                .filter { it.barnetrygdTom == "000000" }
                .map { it.barnFnr!! }

        val behandling = stegService.håndterNyBehandling(NyBehandling(søkersIdent = personIdent,
                                                     behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                                                     kategori = BehandlingKategori.NASJONAL,
                                                     underkategori = BehandlingUnderkategori.ORDINÆR,
                                                     behandlingÅrsak = BehandlingÅrsak.MIGRERING,
                                                     skalBehandlesAutomatisk = false,
                                                     barnasIdenter = barnasIdenter))

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(behandling, false)

        vilkårsvurdering.settOppfylt(BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER)

        vilkårsvurdering.settAllePeriodeFomTilInneværendeMåned()

        vilkårsvurderingService.oppdater(vilkårsvurdering)

        stegService.håndterVilkårsvurdering(behandling)


        // TODO sammenlign tilkjent ytelse med beløp fra infotrygd. Dersom dette ikke er likt, avbryt med feil.

        if (behandling.resultat == BehandlingResultat.INNVILGET) {
            iverksett(behandling)
        } else {
            throw Feil(message = "Migrering mislyktes",
                       frontendFeilmelding = "Migrering mislyktes")
        }
    }

    private fun Vilkårsvurdering.settOppfylt(vararg vilkår: Vilkår) {
        this.personResultater.forEach {
            it.vilkårResultater.forEach {
                if (vilkår.contains(it.vilkårType)) {
                    it.resultat = Resultat.OPPFYLT
                    it.begrunnelse = "Migrering"
                }
            }
        }
    }

    private fun Vilkårsvurdering.settAllePeriodeFomTilInneværendeMåned() {
        this.personResultater.forEach {
            it.vilkårResultater.forEach {
                it.periodeFom = LocalDate.now().førsteDagIInneværendeMåned()
            }
        }
    }

    private fun hentLøpendeSakFraInfotrygd(personIdent: String): Sak {
        val ikkeOpphørteSaker = infotrygdBarnetrygdClient.hentSaker(listOf(personIdent)).bruker.sortedByDescending { it.iverksattdato }
                .filter {
                    it.stønadList.isNotEmpty() && it.stønadList.first().opphørsgrunn.isNullOrBlank()
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

    private fun iverksett(behandling: Behandling) {  // TODO Kan bruke denne hvis beslutter skal settes til System
        // er totrinnskontrollen kun for show? Fra funksjonell side er ikke dette nødvendig. Hvis det kan fjernes, fjern det.
        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling)
        totrinnskontrollService.besluttTotrinnskontroll(behandling, SikkerhetContext.SYSTEM_NAVN, SikkerhetContext.SYSTEM_FORKORTELSE, Beslutning.GODKJENT)
        loggService.opprettBeslutningOmVedtakLogg(behandling, Beslutning.GODKJENT)
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                     ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")
        val task = IverksettMotOppdragTask.opprettTask(behandling, vedtak, SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }
}