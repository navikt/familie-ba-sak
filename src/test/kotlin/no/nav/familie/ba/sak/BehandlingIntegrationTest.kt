package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.behandling.BehandlingslagerService
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.*
import no.nav.familie.ba.sak.behandling.domene.vedtak.BarnBeregning
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.NyttVedtak
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.util.DbContainerInitializer
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
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import javax.transaction.Transactional
import kotlin.streams.asSequence


@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres")
@Tag("integration")
class BehandlingIntegrationTest(
        @Autowired
        private var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

        @Autowired
        private var behandlingslagerService: BehandlingslagerService,

        @Autowired
        private var behandlingVedtakRepository: BehandlingVedtakRepository
) {

    val STRING_LENGTH = 10
    private val charPool : List<Char> = ('A'..'Z') + ('0'..'9')

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
        behandlingslagerService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING,"sdf", lagRandomSaksnummer())
        Assertions.assertEquals(1, behandlingslagerService.hentAlleBehandlinger().size)
    }

    @Test
    @Tag("integration")
    @Transactional
    fun `Opprett behandling og legg til personer`() {
        val behandling = behandlingslagerService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING,"sdf", lagRandomSaksnummer())
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandling.id)

        val søker = Person( personIdent = PersonIdent("0"), type = PersonType.SØKER)
        val barn = Person( personIdent = PersonIdent("12345678910"), type = PersonType.BARN )
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
        val behandling = behandlingslagerService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING,"sdf", lagRandomSaksnummer())
        val behandlingVedtak = BehandlingVedtak( behandling = behandling, ansvarligSaksbehandler = "ansvarligSaksbehandler", vedtaksdato = LocalDate.now(), stønadFom = LocalDate.now(), stønadTom = LocalDate.now().plusDays(1), stønadBrevMarkdown = "")
        behandlingslagerService.lagreBehandlingVedtak(behandlingVedtak)

        val hentetBehandlingVedtak = behandlingVedtakRepository.finnBehandlingVedtak(behandling.id)
        Assertions.assertNotNull(hentetBehandlingVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetBehandlingVedtak?.ansvarligSaksbehandler)
    }

    @Test
    @Tag("integration")
    fun `Opprett 2 behandling vedtak og se at det siste vedtaket får aktiv satt til true`() {
        val behandling = behandlingslagerService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING,"sdf", lagRandomSaksnummer())
        val behandlingVedtak = BehandlingVedtak( behandling = behandling, ansvarligSaksbehandler = "ansvarligSaksbehandler", vedtaksdato = LocalDate.now(), stønadFom = LocalDate.now(), stønadTom = LocalDate.now().plusDays(1), stønadBrevMarkdown = "")
        behandlingslagerService.lagreBehandlingVedtak(behandlingVedtak)

        val behandling2Vedtak = BehandlingVedtak( behandling = behandling, ansvarligSaksbehandler = "ansvarligSaksbehandler2", vedtaksdato = LocalDate.now(), stønadFom = LocalDate.now(), stønadTom = LocalDate.now().plusDays(1), stønadBrevMarkdown = "")
        behandlingslagerService.lagreBehandlingVedtak(behandling2Vedtak)

        val hentetBehandlingVedtak = behandlingslagerService.hentBehandlingVedtakHvisEksisterer(behandling.id)
        Assertions.assertNotNull(hentetBehandlingVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler2", hentetBehandlingVedtak?.ansvarligSaksbehandler)
    }

    @Test
    @Tag("integration")
    fun `Opprett nytt behandling vedtak på aktiv behandling`() {
        val behandling = behandlingslagerService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING,"sdf", lagRandomSaksnummer())
        Assertions.assertNotNull(behandling.fagsak.id)

        behandlingslagerService.nyttVedtakForAktivBehandling(
                fagsakId = behandling.fagsak.id?:1L,
                nyttVedtak = NyttVedtak("sakstype", arrayOf(BarnBeregning(fødselsnummer = "123456789010", beløp = 1054, stønadFom = LocalDate.now()))),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val hentetBehandlingVedtak = behandlingslagerService.hentBehandlingVedtakHvisEksisterer(behandling.id)
        Assertions.assertNotNull(hentetBehandlingVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetBehandlingVedtak?.ansvarligSaksbehandler)
    }
}