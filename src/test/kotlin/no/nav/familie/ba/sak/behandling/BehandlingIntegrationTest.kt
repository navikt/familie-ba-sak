package no.nav.familie.ba.sak.behandling

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import no.nav.familie.ba.sak.behandling.beregning.BarnBeregning
import no.nav.familie.ba.sak.behandling.beregning.NyBeregning
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.NyFagsak
import no.nav.familie.ba.sak.behandling.vedtak.*
import no.nav.familie.ba.sak.behandling.vilkår.vilkårsvurderingKomplettForBarnOgSøker
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.mottak.NyBehandling
import no.nav.familie.ba.sak.mottak.NyBehandlingHendelse
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.task.OpphørVedtakTask
import no.nav.familie.ba.sak.task.OpphørVedtakTask.Companion.opprettOpphørVedtakTask
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.ba.sak.util.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.util.randomFnr
import no.nav.familie.ba.sak.økonomi.OppdragId
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import javax.transaction.Transactional


@SpringBootTest(properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen", "mock-oauth")
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class BehandlingIntegrationTest {

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Autowired
    lateinit var vedtakService: VedtakService

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var integrasjonTjeneste: IntegrasjonTjeneste

    @MockK(relaxed = true)
    lateinit var taskRepository: TaskRepository

    @MockK(relaxed = true)
    lateinit var featureToggleService: FeatureToggleService

    lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                personopplysningGrunnlagRepository,
                fagsakService,
                integrasjonTjeneste,
                featureToggleService,
                taskRepository)

        stubFor(get(urlEqualTo("/api/aktoer/v1"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(mapOf("aktørId" to "1"))))))
        stubFor(get(urlEqualTo("/api/personopplysning/v1/info"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(Personinfo(LocalDate.of(2019,
                                                                                                                              1,
                                                                                                                              1)))))))
        stubFor(get(urlEqualTo("/api/personopplysning/v1/info/BAR"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(Personinfo(LocalDate.of(2019,
                                                                                                                              1,
                                                                                                                              1)))))))
    }

    @Test
    @Tag("integration")
    fun `Kjør flyway migreringer og sjekk at behandlingslagerservice klarer å lese å skrive til postgresql`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        opprettNyOrdinærBehandling(fagsak, behandlingService)
        Assertions.assertEquals(1, behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    @Tag("integration")
    fun `Test at opprettEllerOppdaterBehandling kjører uten feil`() {
        val fnr = randomFnr()

        val nyBehandling = NyBehandlingHendelse(
                fnr,
                listOf(randomFnr(), randomFnr()))
        val fagsak = behandlingService.opprettEllerOppdaterBehandlingFraHendelse(nyBehandling)
        Assertions.assertEquals(1, behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    @Tag("integration")
    @Transactional
    fun `Opprett behandling og legg til personer`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = opprettNyOrdinærBehandling(fagsak, behandlingService)
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)

        val søker = Person(personIdent = PersonIdent(fnr),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = LocalDate.now())
        val barn = Person(personIdent = PersonIdent(barnFnr),
                          type = PersonType.BARN,
                          personopplysningGrunnlag = personopplysningGrunnlag,
                          fødselsdato = LocalDate.now())
        personopplysningGrunnlag.personer.add(søker)
        personopplysningGrunnlag.personer.add(barn)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val hentetPersonopplysningGrunnlag =
                personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
        Assertions.assertNotNull(hentetPersonopplysningGrunnlag)
        Assertions.assertEquals(2, hentetPersonopplysningGrunnlag?.personer?.size)
        Assertions.assertEquals(1, hentetPersonopplysningGrunnlag?.barna?.size)
    }

    @Test
    @Tag("integration")
    fun `Opprett behandling vedtak`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = opprettNyOrdinærBehandling(fagsak, behandlingService)

        opprettNyttInvilgetVedtak(behandling)

        val hentetVedtak = vedtakRepository.findByBehandlingAndAktiv(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetVedtak?.ansvarligSaksbehandler)
    }

    @Test
    @Tag("integration")
    fun `Opprett 2 behandling vedtak og se at det siste vedtaket får aktiv satt til true`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = opprettNyOrdinærBehandling(fagsak, behandlingService)

        opprettNyttInvilgetVedtak(behandling, saksbehandler = "ansvarligSaksbehandler1")
        opprettNyttInvilgetVedtak(behandling, saksbehandler = "ansvarligSaksbehandler2")

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler2", hentetVedtak?.ansvarligSaksbehandler)
    }

    @Test
    @Tag("integration")
    fun `Opprett nytt vedtak på aktiv behandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = opprettNyOrdinærBehandling(fagsak, behandlingService)

        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, fnr, barnFnr)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        vedtakService.nyttVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling,
                nyttVedtak = NyttVedtak(resultat = VedtakResultat.INNVILGET,
                                        samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker(
                                                fnr,
                                                listOf(barnFnr)),
                                        begrunnelse = ""),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetVedtak?.ansvarligSaksbehandler)
        Assertions.assertEquals("", hentetVedtak?.stønadBrevMarkdown)
    }

    @Test
    @Tag("integration")
    fun `Legg til barn fra ny behandling på eksisterende behandling dersom behandling er OPPRETTET`() {
        val morId = randomFnr()
        val barn1Id = randomFnr()
        val barn2Id = randomFnr()

        val fagsak1 = behandlingService.opprettEllerOppdaterBehandlingFraHendelse(NyBehandlingHendelse(morId, listOf(barn1Id)))
        val fagsak2 =
                behandlingService.opprettEllerOppdaterBehandlingFraHendelse(NyBehandlingHendelse(morId, listOf(barn2Id)))

        // skal ikke føre til flere barn på persongrunnlaget.
        behandlingService.opprettEllerOppdaterBehandlingFraHendelse(NyBehandlingHendelse(morId, listOf(barn1Id, barn2Id)))

        Assertions.assertTrue(fagsak1.id == fagsak2.id)

        val behandlinger = behandlingService.hentBehandlinger(fagsak1.id)
        Assertions.assertEquals(1, behandlinger.size)

        val grunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlinger.first().id)

        Assertions.assertTrue(grunnlag!!.personer.any { it.personIdent.ident == morId })
        Assertions.assertTrue(grunnlag.personer.any { it.personIdent.ident == barn1Id })
        Assertions.assertTrue(grunnlag.personer.any { it.personIdent.ident == barn2Id })
        Assertions.assertEquals(3, grunnlag.personer.size)
    }

    @Test
    @Tag("integration")
    fun `Ikke opprett ny behandling hvis fagsaken har en behandling som ikke er iverksatt`() {
        val morId = randomFnr()
        val barnId = randomFnr()

        fagsakService.nyFagsak(NyFagsak(personIdent = morId))
        behandlingService.opprettBehandling(NyBehandling(BehandlingKategori.NASJONAL,
                                                         BehandlingUnderkategori.ORDINÆR,
                                                         morId,
                                                         listOf(barnId),
                                                         BehandlingType.FØRSTEGANGSBEHANDLING,
                                                         null))
        Assertions.assertThrows(Exception::class.java) {
            behandlingService.opprettBehandling(NyBehandling(BehandlingKategori.NASJONAL,
                                                             BehandlingUnderkategori.ORDINÆR,
                                                             morId,
                                                             listOf(barnId),
                                                             BehandlingType.REVURDERING,
                                                             null))
        }
    }

    @Test
    @Tag("integration")
    fun `Opphør migrert vedtak via task`() {

        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        fagsakService.nyFagsak(NyFagsak(personIdent = søkerFnr))
        val nyBehandling =
                NyBehandling(BehandlingKategori.NASJONAL,
                             BehandlingUnderkategori.ORDINÆR,
                             søkerFnr,
                             listOf(barn1Fnr, barn2Fnr),
                             BehandlingType.MIGRERING_FRA_INFOTRYGD,
                             "journalpostId")

        val fagsak = behandlingService.opprettBehandling(nyBehandling)

        val behandling = behandlingService.hentAktivForFagsak(fagsak.id)

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling!!.id)
        Assertions.assertNotNull(personopplysningGrunnlag)

        val barnasBeregning = listOf(
                BarnBeregning(barn1Fnr,
                              1054,
                              LocalDate.of(2020, 1, 1),
                              Ytelsetype.ORDINÆR_BARNETRYGD),
                BarnBeregning(barn2Fnr,
                              1054,
                              LocalDate.of(2020, 1, 1),
                              Ytelsetype.ORDINÆR_BARNETRYGD)
        )
        val nyttVedtak = NyttVedtak(VedtakResultat.INNVILGET,
                                    samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker(
                                            søkerFnr,
                                            listOf(barn1Fnr,
                                                   barn2Fnr)),
                                    begrunnelse = "")
        val nyBeregning = NyBeregning(barnasBeregning)

        vedtakService.nyttVedtakForAktivBehandling(behandling, personopplysningGrunnlag!!, nyttVedtak, "saksbehandler1")

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        vedtakService.oppdaterAktivVedtakMedBeregning(vedtak!!, personopplysningGrunnlag, nyBeregning)

        val task = opprettOpphørVedtakTask(
                behandling,
                vedtak, "saksbehandler",
                BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT,
                LocalDate.now()
        )

        val taskRepository: TaskRepository = mockk()
        val slot = slot<Task>()

        every { taskRepository.save(capture(slot)) } answers { slot.captured }

        OpphørVedtakTask(
                vedtakService,
                taskRepository
        ).doTask(task)

        verify(exactly = 1) {
            taskRepository.save(any())
            Assertions.assertEquals("iverksettMotOppdrag", slot.captured.taskStepType)
        }

        val aktivBehandling = behandlingService.hentAktivForFagsak(fagsak.id)

        Assertions.assertEquals(BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT, aktivBehandling!!.type)
        Assertions.assertNotEquals(behandling.id, aktivBehandling.id)
    }


    @Test
    @Tag("integration")
    fun `Hent behandlinger for løpende fagsaker til konsistensavstemming mot økonomi`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        //Lag fagsak med behandling og personopplysningsgrunnlag og Iverksett.
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = opprettNyOrdinærBehandling(fagsak, behandlingService)

        val vedtak = Vedtak(behandling = behandling,
                            ansvarligSaksbehandler = "ansvarligSaksbehandler",
                            vedtaksdato = LocalDate.of(2020, 1, 1),
                            stønadBrevMarkdown = "",
                            resultat = VedtakResultat.INNVILGET,
                            begrunnelse = "")
        vedtakService.lagre(vedtak)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FERDIGSTILT)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, fnr, barnFnr)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        fagsak.status = FagsakStatus.LØPENDE
        fagsakService.lagre(fagsak)

        val oppdragIdListe = behandlingService.hentAktiveBehandlingerForLøpendeFagsaker()

        Assertions.assertTrue(oppdragIdListe.contains(OppdragId(fnr, behandling.id)))
    }

    @Test
    @Tag("integration")
    fun `Opprett nytt avslag vedtak`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = opprettNyOrdinærBehandling(fagsak, behandlingService)

        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, fnr, barnFnr)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val fagsakRes = vedtakService.nyttVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                nyttVedtak = NyttVedtak(resultat = VedtakResultat.AVSLÅTT,
                                        samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker(
                                                fnr,
                                                listOf(barnFnr)),
                                        begrunnelse = ""),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )
        Assertions.assertEquals(behandling.fagsak.id, fagsakRes.data?.id)

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetVedtak?.ansvarligSaksbehandler)
        Assertions.assertNotEquals("", hentetVedtak?.stønadBrevMarkdown)
    }

    private fun opprettNyttInvilgetVedtak(behandling: Behandling, saksbehandler: String = "ansvarligSaksbehandler"): Vedtak {
        vedtakService.lagre(Vedtak(behandling = behandling,
                                   ansvarligSaksbehandler = saksbehandler,
                                   vedtaksdato = LocalDate.now(),
                                   stønadBrevMarkdown = "",
                                   resultat = VedtakResultat.INNVILGET,
                                   begrunnelse = "")
        )

        return vedtakService.hentAktivForBehandling(behandling.id)!!
    }

    @Test
    @Tag("integration")
    fun `Opprett nytt opphørt vedtak`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = opprettNyOrdinærBehandling(fagsak, behandlingService)

        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, fnr, "12345678915")
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val fagsakRes = vedtakService.nyttVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                nyttVedtak = NyttVedtak(resultat = VedtakResultat.OPPHØRT,
                                        samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker(fnr,
                                                                                                      listOf("12345678915")),
                                        begrunnelse = ""),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )
        Assertions.assertEquals(behandling.fagsak.id, fagsakRes.data?.id)

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetVedtak?.ansvarligSaksbehandler)
        Assertions.assertNotEquals("", hentetVedtak?.stønadBrevMarkdown)
    }
}

fun opprettNyOrdinærBehandling(fagsak: Fagsak, behandlingService: BehandlingService) =
        behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                      null,
                                                      BehandlingType.FØRSTEGANGSBEHANDLING,
                                                      BehandlingKategori.NASJONAL,
                                                      BehandlingUnderkategori.ORDINÆR)