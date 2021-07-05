package no.nav.familie.ba.sak.kjerne.fagsak

import io.mockk.every
import io.mockk.verify
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.ekstern.restDomene.RestSøkParam
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.*
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
@ActiveProfiles("dev", "mock-brev-klient", "mock-pdl", "mock-infotrygd-barnetrygd")
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
    fun `Skal oppgi person med løpende fagsak som fagsakdeltaker til ba-mottak`() {
        val personIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsak(PersonIdent(personIdent))
                .also { fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE) }

        fagsakController.HentFagsakdeltagere(RestSøkParam(personIdent, emptyList())).apply {
            assertEquals(personIdent, body!!.data!!.first().ident)
        }
    }

    @Test
    fun `Skal oppgi person med avluttet fagsak som fagsakdeltaker til ba-mottak, så lenge siste behandling ikke har status henlagt eller teknisk opphørt`() {
        val personIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsak(PersonIdent(personIdent)).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.AVSLUTTET)
        }

        behandlingService.opprettBehandling(nyOrdinærBehandling(personIdent)).also {
            behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(it.id, StegType.BEHANDLING_AVSLUTTET)
            behandlingService.oppdaterStatusPåBehandling(it.id, BehandlingStatus.AVSLUTTET)
        }

        fagsakController.HentFagsakdeltagere(RestSøkParam(personIdent, emptyList())).apply {
            assertEquals(personIdent, body!!.data!!.first().ident)
        }
    }

    @Test
    fun `Skal ikke oppgi person med avluttet fagsak som følge av teknisk opphør som fagsakdeltaker til ba-mottak`() {
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

        fagsakController.HentFagsakdeltagere(RestSøkParam(personIdent, emptyList())).apply {
            assertEquals(emptyList<RestFagsakDeltager>(), body!!.data!!)
        }
    }

    @Test
    fun `Skal ikke oppgi person som fagsakdeltaker til ba-mottak når personen mangler fagsak i ba-sak`() {
        val personIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsak(PersonIdent(ClientMocks.søkerFnr[0]))
                .also { fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE) }

        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(ClientMocks.søkerFnr[0]))
        persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(personIdent,
                                                                  ClientMocks.barnFnr.toList(),
                                                                  behandling,
                                                                  Målform.NB)

        fagsakController.HentFagsakdeltagere(RestSøkParam(personIdent, emptyList())).apply {
            assertEquals(emptyList<RestFagsakDeltager>(), body!!.data!!)
        }
    }

    @Test
    fun `Skal kun oppgi barna som fagsakdeltakere til ba-mottak`() {
        val personIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsak(PersonIdent(ClientMocks.søkerFnr[0]))

        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(ClientMocks.søkerFnr[0]))
        persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(personIdent,
                                                                  ClientMocks.barnFnr.toList(),
                                                                  behandling,
                                                                  Målform.NB)

        fagsakController.HentFagsakdeltagere(RestSøkParam(personIdent, ClientMocks.barnFnr.toList())).apply {
            assertEquals(ClientMocks.barnFnr.toList().sorted(), body!!.data!!.map { it.ident }.sorted())
            assertEquals(2, body!!.data!!.size)
        }
    }


    @Test
    fun `Skal oppgi person med opprettet fagsak som fagsakdeltaker til ba-mottak`() {
        val personIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsak(PersonIdent(personIdent))

        fagsakController.HentFagsakdeltagere(RestSøkParam(personIdent, emptyList())).apply {
            assertEquals(personIdent, body!!.data!!.first().ident)
        }
    }

}
