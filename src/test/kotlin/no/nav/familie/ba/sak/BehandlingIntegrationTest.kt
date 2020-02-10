package no.nav.familie.ba.sak

import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.DokGenService
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.*
import no.nav.familie.ba.sak.behandling.domene.vedtak.*
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.kontrakter.felles.Ressurs
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
import java.util.*
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
                fagsakService)
    }

    private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')

    fun lagRandomSaksnummer(): String {
        return ThreadLocalRandom.current()
                .ints(STRING_LENGTH.toLong(), 0, charPool.size)
                .asSequence()
                .map(charPool::get)
                .joinToString("")
    }

    @Test
    @Tag("integration")
    fun `Kjør flyway migreringer og sjekk at behandlingslagerservice klarer å lese å skrive til postgresql`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("1")
        behandlingService.opprettNyBehandlingPåFagsak(fagsak, "sdf", BehandlingType.FØRSTEGANGSBEHANDLING, lagRandomSaksnummer())
        Assertions.assertEquals(1, behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    @Tag("integration")
    fun `Test at opprettEllerOppdaterBehandling kjører uten feil`() {

        val fagsak = behandlingService.opprettEllerOppdaterBehandlingFraHendelse(
                PersonIdent("4975"),
                {fagsak -> lagNyBehandling(fagsak,BehandlingType.FØRSTEGANGSBEHANDLING)},
                {grunnlag -> grunnlag.leggTilPersoner("4975","4976", "4977")}
        )
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
                                                                       lagRandomSaksnummer())
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
                                                                       lagRandomSaksnummer())
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
                                                                       lagRandomSaksnummer())
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
    @Tag("integration")
    fun `Opprett nytt behandling vedtak på aktiv behandling`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("4")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       lagRandomSaksnummer())
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandling.id)

        val søker = Person(personIdent = PersonIdent("4"),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = LocalDate.now())
        personopplysningGrunnlag.leggTilPerson(søker)

        personopplysningGrunnlag.leggTilPerson(Person(personIdent = PersonIdent("12345678911"),
                                                      type = PersonType.BARN,
                                                      personopplysningGrunnlag = personopplysningGrunnlag,
                                                      fødselsdato = LocalDate.now()))
        personopplysningGrunnlag.aktiv = true
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        behandlingService.nyttVedtakForAktivBehandling(
                fagsakId = behandling.fagsak.id ?: 1L,
                nyttVedtak = NyttVedtak("sakstype",
                                        arrayOf(BarnBeregning(fødselsnummer = "123456789011",
                                                              beløp = 1054,
                                                              stønadFom = LocalDate.now())),
                                        resultat = VedtakResultat.INNVILGET),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val hentetVedtak = behandlingService.hentVedtakHvisEksisterer(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetVedtak?.ansvarligSaksbehandler)
    }

    @Test
    @Tag("integration")
    fun `Hent HTML vedtaksbrev`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("5")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       lagRandomSaksnummer())
        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertNotNull(behandling.id)

        behandlingService.nyttVedtakForAktivBehandling(
                fagsakId = behandling.fagsak.id ?: 1L,
                nyttVedtak = NyttVedtak("sakstype",
                                        arrayOf(BarnBeregning(fødselsnummer = "123456789011",
                                                              beløp = 1054,
                                                              stønadFom = LocalDate.now())),
                                        resultat = VedtakResultat.INNVILGET),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
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

        val fagsak1 = behandlingService.opprettEllerOppdaterBehandlingFraHendelse(
                PersonIdent(morId),
                { fagsak -> lagNyBehandling(fagsak,BehandlingType.FØRSTEGANGSBEHANDLING)},
                { grunnlag -> grunnlag.leggTilPersoner(morId,barn1Id)}
        )

        val fagsak2 = behandlingService.opprettEllerOppdaterBehandlingFraHendelse(
                PersonIdent(morId),
                { fagsak -> lagNyBehandling(fagsak,BehandlingType.FØRSTEGANGSBEHANDLING)},
                { grunnlag -> grunnlag.leggTilPersoner(morId,barn2Id)}
        )

        // skal ikke føre til flere barn på persongrunnlaget.
        behandlingService.opprettEllerOppdaterBehandlingFraHendelse(
                PersonIdent(morId),
                { fagsak -> lagNyBehandling(fagsak,BehandlingType.FØRSTEGANGSBEHANDLING)},
                { grunnlag -> grunnlag.leggTilPersoner(morId,barn1Id, barn2Id)}
        )

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
        val personIdent = PersonIdent(morId)

        val type = BehandlingType.FØRSTEGANGSBEHANDLING
        behandlingService.opprettBehandling(
                personIdent,
                { fagsak -> lagNyBehandling(fagsak, type) },
                { grunnlag -> grunnlag.leggTilPersoner(morId,barnId) })

        Assertions.assertThrows(Exception::class.java) {
            behandlingService.opprettBehandling(
                    personIdent,
                    { fagsak -> lagNyBehandling(fagsak, BehandlingType.REVURDERING) },
                    { grunnlag -> grunnlag.leggTilPersoner(morId,barnId) })
        }
    }

    @Test
    @Tag("integration")
    fun `Opphør migrert behandling`() {

        val søkerFnr = tilfeldigFødselsnummer()
        val barn1Fnr = tilfeldigFødselsnummer()
        val barn2Fnr = tilfeldigFødselsnummer()

        val personIdent = PersonIdent(søkerFnr)
        val fagsak = behandlingService.opprettEllerOppdaterBehandlingFraHendelse(
                personIdent,
                { fagsak -> lagNyBehandling(fagsak, BehandlingType.MIGRERING) },
                { grunnlag -> grunnlag.leggTilPersoner(søkerFnr,barn1Fnr,barn2Fnr) })

        val behandling = behandlingService.hentBehandlingHvisEksisterer(fagsak.id);

        val barnasBeregning = arrayOf(
                BarnBeregning(barn1Fnr, 1054, LocalDate.now()),
                BarnBeregning(barn2Fnr, 1054, LocalDate.now())
        )
        val nyttVedtak = NyttVedtak("sakstype", barnasBeregning, VedtakResultat.INNVILGET)

        behandlingService.nyttVedtakForAktivBehandling(fagsak.id!!, nyttVedtak, "saksbehandler1")

        behandlingService.opphørBehandlingOgVedtak("saksbehandler2",
                                                   tilfeldigsSaksnummer(),
                                                   behandling?.id!!,
                                                   BehandlingType.MIGRERING_OPPHØRT,
                                                   { a: Vedtak -> Unit }).data!!.behandling.id;

        val aktivBehandling = behandlingService.hentBehandlingHvisEksisterer(fagsak.id);

        Assertions.assertEquals(BehandlingType.MIGRERING_OPPHØRT, aktivBehandling!!.type)
        Assertions.assertNotEquals(behandling.id!!, aktivBehandling.id)
    }

    private fun lagNyBehandling(fagsak: Fagsak, type: BehandlingType): Behandling {
        return Behandling(fagsak = fagsak,
                          journalpostID = "jounalpostId",
                          type = type,
                          saksnummer = tilfeldigsSaksnummer())
    }

    private fun PersonopplysningGrunnlag.leggTilPersoner(søkerId: String, vararg barnId: String) :PersonopplysningGrunnlag {
        this.leggTilPerson(PersonType.SØKER, PersonIdent(søkerId), LocalDate.now())

        barnId.forEach {
            this.leggTilPerson(PersonType.BARN, PersonIdent(it), LocalDate.now())
        }

        return this
    }



    private var løpenummer = 0
    private fun tilfeldigFødselsnummer() =
            Base64.getEncoder().encodeToString((System.currentTimeMillis() + (løpenummer++)).toString().toByteArray())

    private fun tilfeldigsSaksnummer() = UUID.randomUUID().toString().substring(0, 18)

}
