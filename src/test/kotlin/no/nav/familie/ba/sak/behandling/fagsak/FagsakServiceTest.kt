package no.nav.familie.ba.sak.behandling.fagsak

import io.mockk.every
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.RegistrerPersongrunnlagDTO
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.FAMILIERELASJONSROLLE
import no.nav.familie.ba.sak.pdl.internal.Familierelasjon
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.pdl.internal.Personident
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.vedtak.producer.MockKafkaProducer
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("dev", "mock-pdl", "mock-arbeidsfordeling")
@Tag("integration")
@TestInstance(Lifecycle.PER_CLASS)
class FagsakServiceTest(
        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val stegService: StegService,

        @Autowired
        private val personopplysningerService: PersonopplysningerService,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService
) {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    /*
    This is a complicated test against following family relationship:
    søker3-----------
    (no case)       | (medmor)
                    barn2
                    | (medmor)
    søker1-----------
                    | (mor)
                    barn1
                    | (far)
    søker2-----------
                    | (far)
                    barn3

     We tests three search:
     1) search for søker1, one participant (søker1) should be returned
     2) search for barn1, three participants (barn1, søker1, søker2) should be returned
     3) search for barn2, three participants (barn2, søker3, søker1) should be returned, where fagsakId of søker3 is null
     */
    @Test
    fun `test å søke fagsak med fnr`() {
        val søker1Fnr = randomFnr()
        val søker2Fnr = randomFnr()
        val søker3Fnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val barn3Fnr = randomFnr()

        every {
            personopplysningerService.hentPersoninfoMedRelasjoner(eq(barn1Fnr))
        } returns PersonInfo(fødselsdato = LocalDate.of(2018, 5, 1), kjønn = Kjønn.KVINNE, navn = "barn1")

        every {
            personopplysningerService.hentPersoninfoMedRelasjoner(eq(barn2Fnr))
        } returns PersonInfo(fødselsdato = LocalDate.of(2019, 5, 1),
                             kjønn = Kjønn.MANN,
                             navn = "barn2",
                             familierelasjoner = setOf(Familierelasjon(Personident(søker1Fnr),
                                                                       FAMILIERELASJONSROLLE.MEDMOR,
                                                                       "søker1",
                                                                       LocalDate.of(1990, 2, 19)),
                                                       Familierelasjon(Personident(søker3Fnr),
                                                                       FAMILIERELASJONSROLLE.MEDMOR,
                                                                       "søker3",
                                                                       LocalDate.of(1990, 1, 10))))

        every {
            personopplysningerService.hentPersoninfoMedRelasjoner(eq(barn3Fnr))
        } returns PersonInfo(fødselsdato = LocalDate.of(2017, 3, 1), kjønn = Kjønn.KVINNE, navn = "barn3")

        every {
            personopplysningerService.hentPersoninfoMedRelasjoner(eq(søker1Fnr))
        } returns PersonInfo(fødselsdato = LocalDate.of(1990, 2, 19), kjønn = Kjønn.KVINNE, navn = "søker1")

        every {
            personopplysningerService.hentPersoninfoMedRelasjoner(eq(søker2Fnr))
        } returns PersonInfo(fødselsdato = LocalDate.of(1991, 2, 20), kjønn = Kjønn.MANN, navn = "søker2")

        every {
            personopplysningerService.hentPersoninfoMedRelasjoner(eq(søker3Fnr))
        } returns PersonInfo(fødselsdato = LocalDate.of(1990, 1, 10), kjønn = Kjønn.KVINNE, navn = "søker3")

        val fagsak0 = fagsakService.hentEllerOpprettFagsak(FagsakRequest(
                søker1Fnr
        ))

        val fagsak1 = fagsakService.hentEllerOpprettFagsak(FagsakRequest(
                søker2Fnr
        ))

        val førsteBehandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                søker1Fnr,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))
        stegService.håndterPersongrunnlag(førsteBehandling,
                                          RegistrerPersongrunnlagDTO(ident = søker1Fnr, barnasIdenter = listOf(barn1Fnr)))

        behandlingService.oppdaterStatusPåBehandling(førsteBehandling.id, BehandlingStatus.AVSLUTTET)

        val andreBehandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                søker1Fnr,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))
        stegService.håndterPersongrunnlag(andreBehandling,
                                          RegistrerPersongrunnlagDTO(ident = søker1Fnr,
                                                                     barnasIdenter = listOf(barn1Fnr, barn2Fnr)))


        val tredjeBehandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                søker2Fnr,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))
        stegService.håndterPersongrunnlag(tredjeBehandling,
                                          RegistrerPersongrunnlagDTO(ident = søker2Fnr, barnasIdenter = listOf(barn1Fnr)))

        val søkeresultat1 = fagsakService.hentFagsakDeltager(søker1Fnr)
        assertEquals(1, søkeresultat1.size)
        assertEquals(Kjønn.KVINNE, søkeresultat1[0].kjønn)
        assertEquals("søker1", søkeresultat1[0].navn)
        assertEquals(fagsak0.data!!.id, søkeresultat1[0].fagsakId)

        val søkeresultat2 = fagsakService.hentFagsakDeltager(barn1Fnr)
        assertEquals(3, søkeresultat2.size)
        var matching = 0
        søkeresultat2.forEach {
            matching += if (it.fagsakId == fagsak0.data!!.id) 1 else if (it.fagsakId == fagsak1.data!!.id) 10 else 0
        }
        assertEquals(11, matching)
        assertEquals(1, søkeresultat2.filter { it.ident == barn1Fnr }.size)

        val søkeresultat3 = fagsakService.hentFagsakDeltager(barn2Fnr)
        assertEquals(3, søkeresultat3.size)
        assertEquals(1, søkeresultat3.filter { it.ident == barn2Fnr }.size)
        assertNull(søkeresultat3.find { it.ident == barn2Fnr }!!.fagsakId)
        assertEquals(fagsak0.data!!.id, søkeresultat3.find { it.ident == søker1Fnr }!!.fagsakId)
        assertEquals(1, søkeresultat3.filter { it.ident == søker3Fnr }.size)
        assertEquals("søker3", søkeresultat3.filter { it.ident == søker3Fnr }[0].navn)
        assertNull(søkeresultat3.find { it.ident == søker3Fnr }!!.fagsakId)

        val fagsak = fagsakService.hent(PersonIdent(søker1Fnr))!!
        Assertions.assertEquals(FagsakStatus.OPPRETTET.name,
                                (MockKafkaProducer.meldingSendtFor(fagsak) as SakDVH).sakStatus)

    }

    @Test
    fun `Skal teste at man henter alle fagsakene til barnet`() {
        val mor = randomFnr()
        val barnFnr = randomFnr()

        val fagsakMor = fagsakService.hentEllerOpprettFagsakForPersonIdent(mor)
        val behandlingMor = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsakMor))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandlingMor.id, mor, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)


        val far = randomFnr()

        val fagsakFar = fagsakService.hentEllerOpprettFagsakForPersonIdent(far)
        val behandlingFar = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsakFar))

        val personopplysningGrunnlagFar =
                lagTestPersonopplysningGrunnlag(behandlingFar.id, far, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlagFar)

        val fagsaker = fagsakService.hentFagsakerPåPerson(PersonIdent(barnFnr))
        assertEquals(2, fagsaker.size)
    }
}