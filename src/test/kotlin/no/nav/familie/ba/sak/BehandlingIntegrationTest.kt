package no.nav.familie.ba.sak

import io.mockk.*
import io.mockk.impl.annotations.MockK
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.DokGenService
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.*
import no.nav.familie.ba.sak.behandling.domene.vedtak.*
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.mottak.NyBehandling
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.task.OpphørVedtak
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.ba.sak.vilkår.vilkårsvurderingKomplettForBarnOgSøker
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.util.concurrent.ThreadLocalRandom
import javax.transaction.Transactional
import kotlin.streams.asSequence


@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen")
@Tag("integration")
class BehandlingIntegrationTest {

    companion object {
        const val STRING_LENGTH = 10
    }

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Autowired
    lateinit var vedtakBarnRepository: VedtakBarnRepository

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    lateinit var personRepository: PersonRepository

    @Autowired
    lateinit var dokGenService: DokGenService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var vilkårService: VilkårService

    @MockK
    lateinit var integrasjonTjeneste: IntegrasjonTjeneste

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                vedtakRepository,
                vedtakBarnRepository,
                personopplysningGrunnlagRepository,
                personRepository,
                dokGenService,
                fagsakService,
                vilkårService,
                integrasjonTjeneste)
    }

    @Test
    @Tag("integration")
    fun `Kjør flyway migreringer og sjekk at behandlingslagerservice klarer å lese å skrive til postgresql`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("1")
        behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                      "sdf",
                                                      BehandlingType.FØRSTEGANGSBEHANDLING,
                                                      lagRandomSaksnummer(),
                                                      BehandlingKategori.NATIONAL,
                                                      BehandlingUnderkategori.ORDINÆR)
        Assertions.assertEquals(1, behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    @Tag("integration")
    fun `Test at opprettEllerOppdaterBehandling kjører uten feil`() {
        every {
            integrasjonTjeneste.hentPersoninfoFor(any())
        } returns Personinfo(LocalDate.now())

        val nyBehandling = NyBehandling(BehandlingKategori.NATIONAL,
                                        BehandlingUnderkategori.ORDINÆR,
                                        "4975",
                                        arrayOf("4976", "4977"),
                                        BehandlingType.FØRSTEGANGSBEHANDLING,
                                        "asd")
        val fagsak = behandlingService.opprettEllerOppdaterBehandlingFraHendelse(nyBehandling)
        Assertions.assertEquals(1, behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    @Tag("integration")
    @Transactional
    fun `Opprett behandling og legg til personer`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("1")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       lagRandomSaksnummer(),
                                                                       BehandlingKategori.NATIONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandling.id)

        val søker = Person(personIdent = PersonIdent("1"),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = LocalDate.now())
        val barn = Person(personIdent = PersonIdent("12345678910"),
                          type = PersonType.BARN,
                          personopplysningGrunnlag = personopplysningGrunnlag,
                          fødselsdato = LocalDate.now())
        personopplysningGrunnlag.leggTilPerson(søker)
        personopplysningGrunnlag.leggTilPerson(barn)
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
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("2")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       lagRandomSaksnummer(),
                                                                       BehandlingKategori.NATIONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)
        val vedtak = Vedtak(behandling = behandling,
                            ansvarligSaksbehandler = "ansvarligSaksbehandler",
                            vedtaksdato = LocalDate.now(),
                            stønadBrevMarkdown = "",
                            resultat = VedtakResultat.INNVILGET)
        behandlingService.lagreVedtak(vedtak)

        val hentetVedtak = vedtakRepository.findByBehandlingAndAktiv(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetVedtak?.ansvarligSaksbehandler)
    }

    @Test
    @Tag("integration")
    fun `Opprett 2 behandling vedtak og se at det siste vedtaket får aktiv satt til true`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("3")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       lagRandomSaksnummer(),
                                                                       BehandlingKategori.NATIONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)
        val vedtak = Vedtak(behandling = behandling,
                            ansvarligSaksbehandler = "ansvarligSaksbehandler",
                            vedtaksdato = LocalDate.now(),
                            stønadBrevMarkdown = "",
                            resultat = VedtakResultat.INNVILGET)
        behandlingService.lagreVedtak(vedtak)

        val aktivtVedtak = Vedtak(behandling = behandling,
                                  ansvarligSaksbehandler = "ansvarligSaksbehandler2",
                                  vedtaksdato = LocalDate.now(),
                                  stønadBrevMarkdown = "",
                                  resultat = VedtakResultat.INNVILGET)
        behandlingService.lagreVedtak(aktivtVedtak)

        val hentetVedtak = behandlingService.hentVedtakHvisEksisterer(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler2", hentetVedtak?.ansvarligSaksbehandler)
    }

    @Test
    @Tag("integrasion")
    fun `Opprett nytt vedtak på aktiv behandling`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("2")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       lagRandomSaksnummer(),
                                                                       BehandlingKategori.NATIONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id!!, "2", "12345678911")
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        behandlingService.nyttVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling,
                nyttVedtak = NyttVedtak(resultat = VedtakResultat.INNVILGET,
                                        samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker("2",
                                                                                                      listOf("12345678911"))),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val hentetVedtak = behandlingService.hentVedtakHvisEksisterer(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetVedtak?.ansvarligSaksbehandler)
        Assertions.assertEquals("", hentetVedtak?.stønadBrevMarkdown)
    }

    @Test
    @Tag("integration")
    fun `Hent HTML vedtaksbrev`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("5")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       lagRandomSaksnummer(),
                                                                       BehandlingKategori.NATIONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)
        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertNotNull(behandling.id)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id!!, "3", "12345678912")
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        behandlingService.nyttVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling,
                nyttVedtak = NyttVedtak(resultat = VedtakResultat.INNVILGET,
                                        samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker("3",
                                                                                                      listOf("12345678912"))),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        behandlingService.oppdaterAktivVedtakMedBeregning(
                vedtak = vedtak!!,
                personopplysningGrunnlag = personopplysningGrunnlag,
                nyBeregning = NyBeregning(
                        arrayOf(BarnBeregning(fødselsnummer = "3",
                                              beløp = 1054,
                                              stønadFom = LocalDate.now()))
                )
        )

        val htmlvedtaksbrevRess = behandlingService.hentHtmlVedtakForBehandling(behandling.id!!)
        Assertions.assertEquals(Ressurs.Status.SUKSESS, htmlvedtaksbrevRess.status)
        assert(htmlvedtaksbrevRess.data!! == "<HTML>HTML_MOCKUP</HTML>")
    }

    @Test
    @Tag("integration")
    fun `Legg til barn fra ny behandling på eksisterende behandling dersom behandling er OPPRETTET`() {
        val morId = "10000010000"
        val barn1Id = "10000010001"
        val barn2Id = "10000010002"

        every {
            integrasjonTjeneste.hentPersoninfoFor(any())
        } returns Personinfo(LocalDate.now())

        val fagsak1 = behandlingService.opprettEllerOppdaterBehandlingFraHendelse(NyBehandling(BehandlingKategori.NATIONAL,
                                                                                               BehandlingUnderkategori.ORDINÆR,
                                                                                               morId,
                                                                                               arrayOf(barn1Id),
                                                                                               BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                                               null))
        val fagsak2 = behandlingService.opprettEllerOppdaterBehandlingFraHendelse(NyBehandling(BehandlingKategori.NATIONAL,
                                                                                               BehandlingUnderkategori.ORDINÆR,
                                                                                               morId,
                                                                                               arrayOf(barn2Id),
                                                                                               BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                                               null))

        // skal ikke føre til flere barn på persongrunnlaget.
        behandlingService.opprettEllerOppdaterBehandlingFraHendelse(NyBehandling(BehandlingKategori.NATIONAL,
                                                                                 BehandlingUnderkategori.ORDINÆR, morId,
                                                                                 arrayOf(barn1Id, barn2Id),
                                                                                 BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                                 null))

        Assertions.assertTrue(fagsak1.id == fagsak2.id)

        val behandlinger = behandlingService.hentBehandlinger(fagsak1.id)
        Assertions.assertEquals(1, behandlinger.size)

        val grunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlinger.first()!!.id)

        Assertions.assertTrue(grunnlag!!.personer.any { it.personIdent.ident == morId })
        Assertions.assertTrue(grunnlag.personer.any { it.personIdent.ident == barn1Id })
        Assertions.assertTrue(grunnlag.personer.any { it.personIdent.ident == barn2Id })
        Assertions.assertEquals(3, grunnlag.personer.size)
    }

    @Test
    @Tag("integration")
    fun `Ikke opprett ny behandling hvis fagsaken har en behandling som ikke er iverksatt`() {
        val morId = "765"
        val barnId = "766"

        every {
            integrasjonTjeneste.hentPersoninfoFor(any())
        } returns Personinfo(LocalDate.now())

        behandlingService.opprettBehandling(NyBehandling(BehandlingKategori.NATIONAL,
                                                         BehandlingUnderkategori.ORDINÆR,
                                                         morId,
                                                         arrayOf(barnId),
                                                         BehandlingType.FØRSTEGANGSBEHANDLING,
                                                         null))
        Assertions.assertThrows(Exception::class.java) {
            behandlingService.opprettBehandling(NyBehandling(BehandlingKategori.NATIONAL,
                                                             BehandlingUnderkategori.ORDINÆR,
                                                             morId,
                                                             arrayOf(barnId),
                                                             BehandlingType.REVURDERING,
                                                             null))
        }
    }

    @Test
    @Tag("integration")
    fun `Opphør migrert vedtak via task`() {

        val søkerFnr = "01010199990"
        val barn1Fnr = "01010199991"
        val barn2Fnr = "01010199992"

        every {
            integrasjonTjeneste.hentPersoninfoFor(any())
        } returns Personinfo(LocalDate.now())

        val nyBehandling =
                NyBehandling(BehandlingKategori.NATIONAL,
                             BehandlingUnderkategori.ORDINÆR,
                             søkerFnr,
                             arrayOf(barn1Fnr, barn2Fnr),
                             BehandlingType.MIGRERING_FRA_INFOTRYGD,
                             "journalpostId")

        val fagsak = behandlingService.opprettBehandling(nyBehandling)

        val behandling = behandlingService.hentBehandlingHvisEksisterer(fagsak.id)

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling?.id)
        Assertions.assertNotNull(personopplysningGrunnlag)

        val barnasBeregning = arrayOf(
                BarnBeregning(barn1Fnr, 1054, LocalDate.now()),
                BarnBeregning(barn2Fnr, 1054, LocalDate.now())
        )
        val nyttVedtak = NyttVedtak(VedtakResultat.INNVILGET,
                                    samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker("01010199990",
                                                                                                  listOf("01010199991",
                                                                                                         "01010199992")))
        val nyBeregning = NyBeregning(barnasBeregning)

        behandlingService.nyttVedtakForAktivBehandling(behandling!!, personopplysningGrunnlag!!, nyttVedtak, "saksbehandler1")

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        behandlingService.oppdaterAktivVedtakMedBeregning(vedtak!!, personopplysningGrunnlag, nyBeregning)

        val task = OpphørVedtak.opprettTaskOpphørVedtak(
                behandling,
                vedtak, "saksbehandler",
                BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT
        )

        val taskRepository: TaskRepository = mockk()
        val slot = slot<Task>()

        every { taskRepository.save(capture(slot)) } answers { slot.captured }

        OpphørVedtak(
                behandlingService,
                taskRepository
        ).doTask(task)

        verify(exactly = 1) {
            taskRepository.save(any())
            Assertions.assertEquals("iverksettMotOppdrag", slot.captured.taskStepType)
        }

        val aktivBehandling = behandlingService.hentBehandlingHvisEksisterer(fagsak.id);

        Assertions.assertEquals(BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT, aktivBehandling!!.type)
        Assertions.assertNotEquals(behandling.id!!, aktivBehandling.id)
    }


    @Test
    @Tag("integration")
    fun `Hent behandlinger for løpende fagsaker til konsistensavstemming mot økonomi`() {
        //Lag fagsak med behandling og personopplysningsgrunnlag og Iverksett.
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("2")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       lagRandomSaksnummer(),
                                                                       BehandlingKategori.NATIONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)
        val vedtak = Vedtak(behandling = behandling,
                            ansvarligSaksbehandler = "ansvarligSaksbehandler",
                            vedtaksdato = LocalDate.now(),
                            stønadBrevMarkdown = "",
                            resultat = VedtakResultat.INNVILGET)
        behandlingService.lagreVedtak(vedtak)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSATT)
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id!!, "5", "12345678914")
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val oppdragIdListe = behandlingService.hentAktiveBehandlingerForLøpendeFagsaker()

        Assertions.assertEquals(1, oppdragIdListe.size)
        Assertions.assertEquals(behandling.id!!, oppdragIdListe[0].behandlingsId)
        Assertions.assertEquals("5", oppdragIdListe[0].personIdent)
    }

    @Test
    @Tag("integration")
    fun `Opprett nytt avslag vedtak`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("777")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       lagRandomSaksnummer(),
                                                                       BehandlingKategori.NATIONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id!!, "6", "12345678915")
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val fagsakRes = behandlingService.nyttVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                nyttVedtak = NyttVedtak(resultat = VedtakResultat.AVSLÅTT,
                                        samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker("6",
                                                                                                      listOf("12345678915"))),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )
        Assertions.assertEquals(behandling.fagsak.id, fagsakRes.data?.id)

        val hentetVedtak = behandlingService.hentVedtakHvisEksisterer(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetVedtak?.ansvarligSaksbehandler)
        Assertions.assertNotEquals("", hentetVedtak?.stønadBrevMarkdown)
    }
}

fun lagTestPersonopplysningGrunnlag(behandlingId: Long,
                                    søkerPersonIdent: String,
                                    barnPersonIdent: String): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId)
    val søker = Person(personIdent = PersonIdent(søkerPersonIdent),
                       type = PersonType.SØKER,
                       personopplysningGrunnlag = personopplysningGrunnlag,
                       fødselsdato = LocalDate.now())
    val barn = Person(personIdent = PersonIdent(barnPersonIdent),
                      type = PersonType.BARN,
                      personopplysningGrunnlag = personopplysningGrunnlag,
                      fødselsdato = LocalDate.now())

    personopplysningGrunnlag.leggTilPerson(søker)
    personopplysningGrunnlag.leggTilPerson(barn)

    return personopplysningGrunnlag
}

private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')

fun lagRandomSaksnummer(): String {
    return ThreadLocalRandom.current()
            .ints(BehandlingIntegrationTest.STRING_LENGTH.toLong(), 0, charPool.size)
            .asSequence()
            .map(charPool::get)
            .joinToString("")
}