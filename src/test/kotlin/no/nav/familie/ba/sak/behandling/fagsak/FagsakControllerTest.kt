package no.nav.familie.ba.sak.behandling.fagsak

import io.mockk.every
import io.mockk.verify
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestPågåendeSakRequest
import no.nav.familie.ba.sak.behandling.restDomene.RestSøkParam
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev", "mock-dokgen", "mock-pdl", "mock-infotrygd-barnetrygd")
@Tag("integration")
class FagsakControllerTest(
        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val fagsakController: FagsakController,

        @Autowired
        private val mockPersonopplysningerService: PersonopplysningerService,

        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val mockInfotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,

        @Autowired
        private val mockIntegrasjonClient: IntegrasjonClient,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,
) {

    @Test
    @Tag("integration")
    fun `Skal opprette fagsak`() {
        val fnr = randomFnr()

        every {
            mockPersonopplysningerService.hentIdenter(Ident(fnr))
        } returns listOf(IdentInformasjon(ident = fnr, historisk = true, gruppe = "FOLKEREGISTERIDENT"))

        fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        assertEquals(fnr, fagsakService.hent(PersonIdent(fnr))?.hentAktivIdent()?.ident)
    }

    @Test
    @Tag("integration")
    fun `Skal opprette fagsak med aktørid`() {
        val aktørId = randomAktørId()

        val response = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = null, aktørId = aktørId.id))
        val restFagsak = response.body?.data
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(FagsakStatus.OPPRETTET, restFagsak?.status)
        assertNotNull(restFagsak?.søkerFødselsnummer)
    }

    @Test
    @Tag("integration")
    fun `Skal opprette skyggesak i Sak`() {
        val fnr = randomFnr()

        every {
            mockPersonopplysningerService.hentIdenter(Ident(fnr))
        } returns listOf(IdentInformasjon(ident = fnr, historisk = true, gruppe = "FOLKEREGISTERIDENT"))

        val fagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))

        verify(exactly = 1) { mockIntegrasjonClient.opprettSkyggesak(any(), fagsak.body?.data?.id!!) }
    }

    @Test
    @Tag("integration")
    fun `Skal returnere eksisterende fagsak på person som allerede finnes`() {
        val fnr = randomFnr()

        every {
            mockPersonopplysningerService.hentIdenter(Ident(fnr))
        } returns listOf(
                IdentInformasjon(ident = fnr, historisk = true, gruppe = "FOLKEREGISTERIDENT"))

        val nyRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        assertEquals(Ressurs.Status.SUKSESS, nyRestFagsak.body?.status)
        assertEquals(fnr, fagsakService.hent(PersonIdent(fnr))?.hentAktivIdent()?.ident)

        val eksisterendeRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(
                personIdent = fnr))
        assertEquals(Ressurs.Status.SUKSESS, eksisterendeRestFagsak.body?.status)
        assertEquals(eksisterendeRestFagsak.body!!.data!!.id, nyRestFagsak.body!!.data!!.id)
    }

    @Test
    @Tag("integration")
    fun `Skal returnere eksisterende fagsak på person som allerede finnes med gammel ident`() {
        val fnr = randomFnr()
        val nyttFnr = randomFnr()

        every {
            mockPersonopplysningerService.hentIdenter(Ident(fnr))
        } returns listOf(
                IdentInformasjon(ident = fnr, historisk = true, gruppe = "FOLKEREGISTERIDENT"))

        val nyRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        assertEquals(Ressurs.Status.SUKSESS, nyRestFagsak.body?.status)
        assertEquals(fnr, fagsakService.hent(PersonIdent(fnr))?.hentAktivIdent()?.ident)

        every {
            mockPersonopplysningerService.hentIdenter(Ident(nyttFnr))
        } returns listOf(
                IdentInformasjon(ident = fnr, historisk = true, gruppe = "FOLKEREGISTERIDENT"),
                IdentInformasjon(ident = nyttFnr, historisk = false, gruppe = "FOLKEREGISTERIDENT"))

        val eksisterendeRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(
                personIdent = nyttFnr))
        assertEquals(Ressurs.Status.SUKSESS, eksisterendeRestFagsak.body?.status)
        assertEquals(eksisterendeRestFagsak.body!!.data!!.id, nyRestFagsak.body!!.data!!.id)
        assertEquals(nyttFnr, eksisterendeRestFagsak.body!!.data?.søkerFødselsnummer)

    }

    @Test
    @Tag("integration")
    fun `Skal returnere eksisterende fagsak på person som allerede finnes basert på aktørid`() {
        val aktørId = randomAktørId()

        every {
            mockPersonopplysningerService.hentAktivPersonIdent(Ident(aktørId.id))
        } returns PersonIdent("123")

        val nyRestFagsak = fagsakController.hentEllerOpprettFagsak(
                FagsakRequest(personIdent = null, aktørId = aktørId.id)
        )
        assertEquals(Ressurs.Status.SUKSESS, nyRestFagsak.body?.status)

        val eksisterendeRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(
                personIdent = null, aktørId = aktørId.id))
        assertEquals(Ressurs.Status.SUKSESS, eksisterendeRestFagsak.body?.status)
        assertEquals(eksisterendeRestFagsak.body!!.data!!.id, nyRestFagsak.body!!.data!!.id)
    }

    @Test
    fun `Skal flagge pågående sak ved løpende fagsak på søker`() {
        val personIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsak(PersonIdent(personIdent))
                .also { fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE) }

        fagsakController.søkEtterPågåendeSak(RestPågåendeSakRequest(personIdent, emptyList())).apply {
            assertTrue(body!!.data!!.harPågåendeSakIBaSak)
            assertFalse(body!!.data!!.harPågåendeSakIInfotrygd)
        }
    }

    @Test
    fun `Skal flagge pågående sak ved avluttet fagsak når den siste behandlingen ikke har status henlagt|teknisk opphørt`() {
        val personIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsak(PersonIdent(personIdent)).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.AVSLUTTET)
        }

        behandlingService.opprettBehandling(nyOrdinærBehandling(personIdent)).also {
            behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(it.id, StegType.BEHANDLING_AVSLUTTET)
            behandlingService.oppdaterStatusPåBehandling(it.id, BehandlingStatus.AVSLUTTET)
        }

        fagsakController.søkEtterPågåendeSak(RestPågåendeSakRequest(personIdent, emptyList())).apply {
            assertTrue(body!!.data!!.harPågåendeSakIBaSak)
            assertFalse(body!!.data!!.harPågåendeSakIInfotrygd)
        }
    }

    @Test
    fun `Skal ikke flagge pågående sak ved avluttet fagsak som følge av teknisk opphør`() {
        val personIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsak(PersonIdent(personIdent)).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.AVSLUTTET)
        }

        behandlingService.opprettBehandling(NyBehandling(søkersIdent = personIdent,
                                                         behandlingType = BehandlingType.TEKNISK_OPPHØR,
                                                         behandlingÅrsak = BehandlingÅrsak.TEKNISK_OPPHØR,
                                                         kategori = BehandlingKategori.NASJONAL,
                                                         underkategori = BehandlingUnderkategori.ORDINÆR)
        ).also {
            behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(it.id, StegType.BEHANDLING_AVSLUTTET)
            behandlingService.oppdaterStatusPåBehandling(it.id, BehandlingStatus.AVSLUTTET)
        }

        fagsakController.søkEtterPågåendeSak(RestPågåendeSakRequest(personIdent, emptyList())).apply {
            assertFalse(body!!.data!!.harPågåendeSakIBaSak)
            assertFalse(body!!.data!!.harPågåendeSakIInfotrygd)
        }
    }

    @Test
    fun `Skal ikke ha pågående sak i ba-sak når søker mangler fagsak og det ikke er sak på annenpart`() {
        val personIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsak(PersonIdent(ClientMocks.søkerFnr[0]))
                .also { fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE) }

        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(ClientMocks.søkerFnr[0]))
        persongrunnlagService.lagreSøkerOgBarnIPersonopplysningsgrunnlaget(personIdent, ClientMocks.barnFnr.toList(), behandling, Målform.NB )

        fagsakController.søkEtterPågåendeSak(RestPågåendeSakRequest(personIdent, emptyList())).apply {
            assertFalse(body!!.data!!.harPågåendeSakIBaSak)
        }
    }

    @Test
    fun `Skal flagge pågående sak når søker mangler fagsak men det er sak på annenpart`() {
        val personIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsak(PersonIdent(ClientMocks.søkerFnr[0]))

        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(ClientMocks.søkerFnr[0]))
        persongrunnlagService.lagreSøkerOgBarnIPersonopplysningsgrunnlaget(personIdent, ClientMocks.barnFnr.toList(), behandling, Målform.NB )

        fagsakController.søkEtterPågåendeSak(RestPågåendeSakRequest(personIdent, ClientMocks.barnFnr.toList())).apply {
            assertTrue(body!!.data!!.harPågåendeSakIBaSak)
            assertFalse(body!!.data!!.harPågåendeSakIInfotrygd)
        }
    }


    @Test
    fun `Skal flagge pågående sak ved opprettet fagsak`() {
        val personIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsak(PersonIdent(personIdent))

        fagsakController.søkEtterPågåendeSak(RestPågåendeSakRequest(personIdent, emptyList())).apply {
            assertTrue(body!!.data!!.harPågåendeSakIBaSak)
            assertFalse(body!!.data!!.harPågåendeSakIInfotrygd)
        }
    }

    @Test
    fun `Skal flagge pågående sak i Infotrygd`() {
        val personIdent = randomFnr()

        every {
            mockInfotrygdBarnetrygdClient.harLøpendeSakIInfotrygd(any())
        } returns true andThen false

        fagsakController.søkEtterPågåendeSak(RestPågåendeSakRequest(personIdent, emptyList())).apply {
            assertFalse(body!!.data!!.harPågåendeSakIBaSak)
            assertTrue(body!!.data!!.harPågåendeSakIInfotrygd)
        }
        fagsakController.søkEtterPågåendeSak(RestPågåendeSakRequest(personIdent, emptyList())).apply {
            assertFalse(body!!.data!!.harPågåendeSakIInfotrygd)
        }
    }
}
