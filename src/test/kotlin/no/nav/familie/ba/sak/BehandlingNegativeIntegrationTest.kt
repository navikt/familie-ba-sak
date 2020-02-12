package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.BarnBeregning
import no.nav.familie.ba.sak.behandling.domene.vedtak.NyBeregning
import no.nav.familie.ba.sak.behandling.domene.vedtak.NyttVedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakResultat
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

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen-negative")
@Tag("integration")
class BehandlingNegativeIntegrationTest(@Autowired
                                        private val behandlingService: BehandlingService) {

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Test
    @Tag("integration")
    fun `Hent HTML vedtaksbrev Negative'`() {
        val failRess = behandlingService.hentHtmlVedtakForBehandling(100)
        Assertions.assertEquals(Ressurs.Status.FEILET, failRess.status)

        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("6")
        val behandling =
                behandlingService.opprettNyBehandlingPåFagsak(fagsak, "sdf", BehandlingType.FØRSTEGANGSBEHANDLING, "sak1")
        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertNotNull(behandling.id)
    }

    @Test
    @Tag("integration")
    fun `Oppdater avslag vedtak med beregning`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("777")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       "123")
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandling.id)

        val søker = Person(personIdent = PersonIdent("777"),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = LocalDate.now())
        personopplysningGrunnlag.leggTilPerson(søker)
        val barn = Person(personIdent = PersonIdent("12345678910"),
                          type = PersonType.BARN,
                          personopplysningGrunnlag = personopplysningGrunnlag,
                          fødselsdato = LocalDate.now())
        personopplysningGrunnlag.leggTilPerson(søker)
        personopplysningGrunnlag.leggTilPerson(barn)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        behandlingService.nyttVedtakForAktivBehandling(
                fagsakId = behandling.fagsak.id ?: 1L,
                nyttVedtak = NyttVedtak(resultat = VedtakResultat.AVSLÅTT),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )
        val hentetVedtak = behandlingService.hentVedtakHvisEksisterer(behandling.id)

        val fagsakRes = behandlingService.oppdaterAktivVedtakMedBeregning(
                fagsakId = behandling.fagsak.id ?: 1L,
                nyBeregning = NyBeregning(
                        arrayOf(
                                BarnBeregning(
                                        fødselsnummer = "123456789011",
                                        beløp = 1054,
                                        stønadFom = LocalDate.now()
                                ))
                )
        )

        Assertions.assertEquals(Ressurs.Status.FEILET, fagsakRes.status)
    }
}
