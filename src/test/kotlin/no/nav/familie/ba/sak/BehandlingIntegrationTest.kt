package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.Beregning
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.*
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.ba.sak.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
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
class BehandlingIntegrationTest(
        @Autowired
        private var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

        @Autowired
        private var behandlingService: BehandlingService,

        @Autowired
        private var fagsakService: FagsakService,

        @Autowired
        private var beregning: Beregning,

        @Autowired
        private var behandlingVedtakRepository: BehandlingVedtakRepository
) {
    val STRING_LENGTH = 10
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
        val behandling = behandlingService.nyBehandling("1", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING, "sdf", lagRandomSaksnummer())
        Assertions.assertEquals(1, behandlingService.hentBehandlinger(behandling.fagsak.id).size)
    }

    @Test
    @Tag("integration")
    @Transactional
    fun `Opprett behandling og legg til personer`() {
        val behandling = behandlingService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING, "sdf", lagRandomSaksnummer())
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandling.id)

        val søker = Person(personIdent = PersonIdent("0"), type = PersonType.SØKER, personopplysningGrunnlag = personopplysningGrunnlag, fødselsdato = LocalDate.now())
        val barn = Person(personIdent = PersonIdent("12345678910"), type = PersonType.BARN, personopplysningGrunnlag = personopplysningGrunnlag, fødselsdato = LocalDate.now())
        personopplysningGrunnlag.leggTilPerson(søker)
        personopplysningGrunnlag.leggTilPerson(barn)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val hentetPersonopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
        Assertions.assertNotNull(hentetPersonopplysningGrunnlag)
        Assertions.assertEquals(2, hentetPersonopplysningGrunnlag?.personer?.size)
        Assertions.assertEquals(1, hentetPersonopplysningGrunnlag?.barna?.size)
    }

    @Test
    @Tag("integration")
    fun `Opprett behandling vedtak`() {
        val behandling = behandlingService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING, "sdf", lagRandomSaksnummer())
        val behandlingVedtak = BehandlingVedtak(behandling = behandling, ansvarligSaksbehandler = "ansvarligSaksbehandler", vedtaksdato = LocalDate.now(), stønadFom = LocalDate.now(), stønadTom = LocalDate.now().plusDays(1), stønadBrevMarkdown = "")
        behandlingService.lagreBehandlingVedtak(behandlingVedtak)

        val hentetBehandlingVedtak = behandlingVedtakRepository.findByBehandlingAndAktiv(behandling.id)
        Assertions.assertNotNull(hentetBehandlingVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetBehandlingVedtak?.ansvarligSaksbehandler)
    }

    @Test
    @Tag("integration")
    fun `Opprett 2 behandling vedtak og se at det siste vedtaket får aktiv satt til true`() {
        val behandling = behandlingService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING, "sdf", lagRandomSaksnummer())
        val behandlingVedtak = BehandlingVedtak(behandling = behandling, ansvarligSaksbehandler = "ansvarligSaksbehandler", vedtaksdato = LocalDate.now(), stønadFom = LocalDate.now(), stønadTom = LocalDate.now().plusDays(1), stønadBrevMarkdown = "")
        behandlingService.lagreBehandlingVedtak(behandlingVedtak)

        val behandling2Vedtak = BehandlingVedtak(behandling = behandling, ansvarligSaksbehandler = "ansvarligSaksbehandler2", vedtaksdato = LocalDate.now(), stønadFom = LocalDate.now(), stønadTom = LocalDate.now().plusDays(1), stønadBrevMarkdown = "")
        behandlingService.lagreBehandlingVedtak(behandling2Vedtak)

        val hentetBehandlingVedtak = behandlingService.hentBehandlingVedtakHvisEksisterer(behandling.id)
        Assertions.assertNotNull(hentetBehandlingVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler2", hentetBehandlingVedtak?.ansvarligSaksbehandler)
    }

    @Test
    @Tag("integration")
    fun `Opprett nytt behandling vedtak på aktiv behandling`() {
        val behandling = behandlingService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING, "sdf", lagRandomSaksnummer())
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandling.id)

        val søker = Person(personIdent = PersonIdent("123456789010"), type = PersonType.SØKER, personopplysningGrunnlag = personopplysningGrunnlag, fødselsdato = LocalDate.now())
        personopplysningGrunnlag.leggTilPerson(søker)

        personopplysningGrunnlag.leggTilPerson(Person(personIdent = PersonIdent("123456789011"), type = PersonType.BARN, personopplysningGrunnlag = personopplysningGrunnlag, fødselsdato = LocalDate.now()))
        personopplysningGrunnlag.setAktiv(true)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        behandlingService.nyttVedtakForAktivBehandling(
                fagsakId = behandling.fagsak.id ?: 1L,
                nyttVedtak = NyttVedtak("sakstype", arrayOf(BarnBeregning(fødselsnummer = "123456789011", beløp = 1054, stønadFom = LocalDate.now()))),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val hentetBehandlingVedtak = behandlingService.hentBehandlingVedtakHvisEksisterer(behandling.id)
        Assertions.assertNotNull(hentetBehandlingVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetBehandlingVedtak?.ansvarligSaksbehandler)
    }

    @Test
    @Tag("integration")
    fun `Iverksett vedtak på aktiv behandling`() {
        val økonomiKlientMock: ØkonomiKlient = Mockito.mock(ØkonomiKlient::class.java)
        val økonomiService = ØkonomiService(fagsakService, økonomiKlientMock, beregning, behandlingService)

        val behandling = behandlingService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING, "sdf", lagRandomSaksnummer())
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandling.id)

        val søker = Person(personIdent = PersonIdent("123456789010"), type = PersonType.SØKER, personopplysningGrunnlag = personopplysningGrunnlag, fødselsdato = LocalDate.now())
        personopplysningGrunnlag.leggTilPerson(søker)

        personopplysningGrunnlag.leggTilPerson(Person(personIdent = PersonIdent("123456789011"), type = PersonType.BARN, personopplysningGrunnlag = personopplysningGrunnlag, fødselsdato = LocalDate.now()))
        personopplysningGrunnlag.setAktiv(true)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        behandlingService.nyttVedtakForAktivBehandling(
                fagsakId = behandling.fagsak.id ?: 1L,
                nyttVedtak = NyttVedtak("sakstype", arrayOf(BarnBeregning(fødselsnummer = "123456789011", beløp = 1054, stønadFom = LocalDate.now()))),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val hentetBehandlingVedtak = behandlingService.hentBehandlingVedtakHvisEksisterer(behandling.id)
        Assertions.assertNotNull(hentetBehandlingVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetBehandlingVedtak?.ansvarligSaksbehandler)

        Mockito.`when`(økonomiKlientMock.iverksettOppdrag(MockitoHelper.anyObject()))
                .thenReturn(ResponseEntity.ok(Ressurs.success("Oppdrag sendt ok")))
        økonomiService.iverksettVedtak(behandlingVedtak = hentetBehandlingVedtak!!, saksbehandlerId = "ansvarligSaksbehandler")

        val oppdatertBehandlingVedtak = behandlingService.hentAktivVedtakForBehandling(behandling.id)
        Assertions.assertEquals(BehandlingVedtakStatus.SENDT_TIL_IVERKSETTING, oppdatertBehandlingVedtak?.status)
    }

    object MockitoHelper {
        fun <T> anyObject(): T {
            Mockito.any<T>()
            return uninitialized()
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> uninitialized(): T =  null as T
    }

    @Test
    @Tag("integration")
    fun `Hent HTML vedtaksbrev'`() {
        val behandling = behandlingService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING, "sdf", lagRandomSaksnummer())
        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertNotNull(behandling.id)

        behandlingService.nyttVedtakForAktivBehandling(
                fagsakId = behandling.fagsak.id ?: 1L,
                nyttVedtak = NyttVedtak("sakstype", arrayOf(BarnBeregning(fødselsnummer = "123456789011", beløp = 1054, stønadFom = LocalDate.now()))),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val htmlvedtaksbrevRess = behandlingService.hentHtmlVedtakForBehandling(behandling.id!!);
        Assertions.assertEquals(Ressurs.Status.SUKSESS, htmlvedtaksbrevRess.status)
        assert(htmlvedtaksbrevRess.data!!.equals("<HTML>HTML_MOCKUP</HTML>"))
    }
}
