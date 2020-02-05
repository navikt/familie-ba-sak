package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.*
import no.nav.familie.ba.sak.mottak.NyBehandling
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
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
class BehandlingIntegrationTest(@Autowired
                                private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                                @Autowired
                                private val behandlingService: BehandlingService,
                                @Autowired
                                private val VedtakRepository: VedtakRepository) {

    companion object {
        const val STRING_LENGTH = 10
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
    @Transactional
    fun `Opprett behandling og legg til personer`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("1")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak, "sdf", BehandlingType.FØRSTEGANGSBEHANDLING, lagRandomSaksnummer())
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
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak, "sdf", BehandlingType.FØRSTEGANGSBEHANDLING, lagRandomSaksnummer())
        val vedtak = Vedtak(behandling = behandling,
                            ansvarligSaksbehandler = "ansvarligSaksbehandler",
                            vedtaksdato = LocalDate.now(),
                            stønadBrevMarkdown = "",
                            resultat = VedtakResultat.INNVILGET)
        behandlingService.lagreVedtak(vedtak)

        val hentetVedtak = VedtakRepository.findByBehandlingAndAktiv(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetVedtak?.ansvarligSaksbehandler)
    }

    @Test
    @Tag("integration")
    fun `Opprett 2 behandling vedtak og se at det siste vedtaket får aktiv satt til true`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("3")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak, "sdf", BehandlingType.FØRSTEGANGSBEHANDLING, lagRandomSaksnummer())
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
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak, "sdf", BehandlingType.FØRSTEGANGSBEHANDLING, lagRandomSaksnummer())
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
    fun `Hent HTML vedtaksbrev'`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("5")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak, "sdf", BehandlingType.FØRSTEGANGSBEHANDLING, lagRandomSaksnummer())
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
/* må skrives ny test
    @Test
    @Tag("integration")
    fun `Ikke opprett ny behandling hvis fagsaken har en behandling som ikke er iverksatt`() {
        val saksnr = lagRandomSaksnummer()
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("7")
        behandlingService.opprettNyBehandlingPåFagsak(fagsak, "sdf", BehandlingType.FØRSTEGANGSBEHANDLING, saksnr)
        Assertions.assertThrows(Exception::class.java) {
            behandlingService.opprettEllerOppdaterBehandling(NyBehandling("7",
                                                                          BehandlingType.REVURDERING,
                                                             "sdf",
                                                                          saksnr)
        }
    }
*/

}
