package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.slot
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.VedtakOmOvergangsstønadService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.dokument.hentBrevtype
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.SendAutobrevTask
import no.nav.familie.ba.sak.task.dto.AutobrevAlderDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDate
import java.util.Properties

@DirtiesContext
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AutovedtakReduksjonSmåbarnstilleggTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val featureToggleService: FeatureToggleService,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val efSakRestClient: EfSakRestClient,
    @Autowired private val vedtakOmOvergangsstønadService: VedtakOmOvergangsstønadService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val økonomiKlient: ØkonomiKlient,
    @Autowired private val opprettTaskService: OpprettTaskService,
    @Autowired private val sendAutuBrevTask: SendAutobrevTask,
    @Autowired private val taskRepository: TaskRepository,
    @Autowired private val journalførVedtaksbrevTask: JournalførVedtaksbrevTask,
) : AbstractVerdikjedetest(efSakRestClient = efSakRestClient, økonomiKlient = økonomiKlient) {

    /**
     * 1. mocke situasjon (innvilget med småbarnstillegg), barn 3 år forrige måned
     * 2. Fyre av GET til /autobrev for å starte i gang kjørSchedulerForAutobrev()
     * 3. Verifisere at behandling har blitt automatisk behandlet, med reduksjon av småbarnstillegg
     * 4. Verifisere kall til dokdist om utsending av brev
     */

    private val barnAlder = 3L
    private val barnFødselsdato = LocalDate.now().minusYears(barnAlder).minusMonths(1)
    private val periodeMedFullOvergangsstønadFom = barnFødselsdato.plusYears(1)

    private val lastTaskSlot = slot<Task>()

    lateinit var scenario: RestScenario

    @BeforeAll
    fun init() {
        scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = barnFødselsdato.toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                        bostedsadresser = emptyList()
                    )
                )
            )
        )
    }

    @Test
    @Order(1)
    fun `Skal automatisk behandle reduksjon av småbarnstillegg pga yngste barn 3 år forrige måned`() {
        every { featureToggleService.isEnabled(any()) } returns true

        val søkersIdent = scenario.søker.ident!!

        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = listOf(
                PeriodeOvergangsstønad(
                    personIdent = søkersIdent,
                    fomDato = periodeMedFullOvergangsstønadFom,
                    tomDato = barnFødselsdato.plusYears(18), // tom lenge etter at barn har fyllt 3 år
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            )
        )
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = listOf(
                PeriodeOvergangsstønad(
                    personIdent = søkersIdent,
                    fomDato = periodeMedFullOvergangsstønadFom,
                    tomDato = barnFødselsdato.plusYears(18), // tom lenge etter at barn har fyllt 3 år
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            )
        )

        val fagsakRessurs: Ressurs<RestMinimalFagsak> = familieBaSakKlient().opprettFagsak(søkersIdent = søkersIdent)
        val fagsak: RestMinimalFagsak = fagsakRessurs.data!!
        val fagsakId: Long = fagsak.id
        val restBehandling = familieBaSakKlient().opprettBehandling(
            søkersIdent = søkersIdent,
            behandlingUnderkategori = BehandlingUnderkategori.UTVIDET
        )

        val behandling = behandlingService.hent(restBehandling.data!!.behandlingId)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = søkersIdent,
                    barnasIdenter = scenario.barna.map { it.ident!! },
                    underkategori = BehandlingUnderkategori.UTVIDET
                ),
                bekreftEndringerViaFrontend = false
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = behandling.id,
                restRegistrerSøknad = restRegistrerSøknad
            )

        restUtvidetBehandling.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.data!!.behandlingId,
                    vilkårId = it.id,
                    restPersonResultat =
                    RestPersonResultat(
                        personIdent = restPersonResultat.personIdent,
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.OPPFYLT,
                                periodeFom = barnFødselsdato
                            )
                        )
                    )
                )
            }
        }

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restUtvidetBehandling.data!!.behandlingId
        )

        val restUtvidetBehandlingEtterBehandlingsResultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restUtvidetBehandling.data!!.behandlingId
            )

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsResultat.data!!.behandlingId,
                RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse")
            )

        val vedtaksperiodeId =
            restUtvidetBehandlingEtterVurderTilbakekreving.data!!.vedtak!!.vedtaksperioderMedBegrunnelser.first()
        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperiodeId.id,
            restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                standardbegrunnelser = listOf(
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER
                )
            )
        )

        val restUtvidetBehandlingEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId)

        familieBaSakKlient().iverksettVedtak(
            behandlingId = restUtvidetBehandlingEtterSendTilBeslutter.data!!.behandlingId,
            restBeslutningPåVedtak = RestBeslutningPåVedtak(
                Beslutning.GODKJENT
            ),
            beslutterHeaders = HttpHeaders().apply {
                setBearerAuth(
                    token(
                        mapOf(
                            "groups" to listOf("SAKSBEHANDLER", "BESLUTTER"),
                            "azp" to "azp-test",
                            "name" to "Mock McMockface Beslutter",
                            "preferred_username" to "mock.mcmockface.beslutter@nav.no"
                        )
                    )
                )
            }
        )

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingService.hentAktivForFagsak(fagsakId = fagsakRessurs.data!!.id)!!,
            søkerFnr = søkersIdent,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
        )

        sendAutuBrevTask.doTask(
            Task(
                type = SendAutobrevTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    AutobrevAlderDTO(
                        fagsakId = fagsakId,
                        alder = barnAlder.toInt(),
                        årMåned = inneværendeMåned()
                    )
                ),
                properties = Properties().apply {
                    this["fagsak"] = fagsakId.toString()
                    this["callId"] = "some callId"
                }
            )
        )

        val alleBehandlinger = behandlingService.hentBehandlinger(fagsakId)
        val kanskjeDenne = behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId)
        val ellerDenne = behandlingService.hentSisteBehandlingSomErVedtatt(fagsakId)

        val behandlingEtterVurdering = behandlingService.hent(restBehandling.data!!.behandlingId)
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandlingEtterVurdering.id)

        val behandlingEtterJournalførtVedtak = stegService.håndterJournalførVedtaksbrev(
            behandling = behandlingEtterVurdering,
            journalførVedtaksbrevDTO = JournalførVedtaksbrevDTO(
                vedtakId = vedtak.id,
                task = Task(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = "")
            )
        )

        val behandlingEtterDistribuertVedtak =
            stegService.håndterDistribuerVedtaksbrev(
                behandlingEtterJournalførtVedtak,
                DistribuerDokumentDTO(
                    behandlingId = behandlingEtterJournalførtVedtak.id,
                    journalpostId = "1234",
                    personIdent = søkersIdent,
                    brevmal = hentBrevtype(
                        behandlingEtterJournalførtVedtak
                    ),
                    erManueltSendt = false
                )
            )
        val ferdigstiltBehandling = stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)

        val restMinimalFagsakEtterAvsluttetBehandling =
            fagsakService.hentMinimalFagsakForPerson(aktør = ferdigstiltBehandling.fagsak.aktør)

        println("what do I got now?")
    }
}
