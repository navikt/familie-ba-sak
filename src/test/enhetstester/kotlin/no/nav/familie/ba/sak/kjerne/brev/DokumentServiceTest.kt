package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpost
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.domene.ManuellBrevmottaker
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerDb
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerRepository
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DokumentServiceTest {
    val integrasjonClient = mockk<IntegrasjonClient>(relaxed = true)
    val vilkårsvurderingService = mockk<VilkårsvurderingService>(relaxed = true)
    val vilkårsvurderingForNyBehandlingService = mockk<VilkårsvurderingForNyBehandlingService>(relaxed = true)
    val utgåendeJournalføringService = mockk<UtgåendeJournalføringService>(relaxed = true)
    val journalføringRepository = mockk<JournalføringRepository>(relaxed = true)
    val taskRepository = mockk<TaskRepositoryWrapper>(relaxed = true)
    val fagsakRepository = mockk<FagsakRepository>(relaxed = true)
    val organisasjonService = mockk<OrganisasjonService>(relaxed = true)
    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>(relaxed = true)
    val personidentService = mockk<PersonidentService>()
    val brevmottakerRepository = mockk<BrevmottakerRepository>()
    val brevmottakerService =
        spyk<BrevmottakerService>(
            BrevmottakerService(
                brevmottakerRepository = brevmottakerRepository,
                loggService = mockk(),
                validerBrevmottakerService = mockk(),
            ),
        )

    private val dokumentService: DokumentService =
        spyk(
            DokumentService(
                journalføringRepository = journalføringRepository,
                taskRepository = taskRepository,
                vilkårsvurderingService = vilkårsvurderingService,
                vilkårsvurderingForNyBehandlingService = vilkårsvurderingForNyBehandlingService,
                rolleConfig = mockk(relaxed = true),
                settPåVentService = mockk(relaxed = true),
                utgåendeJournalføringService = utgåendeJournalføringService,
                fagsakRepository = fagsakRepository,
                organisasjonService = organisasjonService,
                behandlingHentOgPersisterService = behandlingHentOgPersisterService,
                dokumentGenereringService = mockk(relaxed = true),
                brevmottakerService = brevmottakerService,
                validerBrevmottakerService = mockk(relaxed = true),
            ),
        )

    @Test
    fun `sendManueltBrev skal journalføre med brukerIdType ORGNR når fagsakType er INSTITUSJON`() {
        val avsenderMottaker = slot<AvsenderMottaker>()
        val behandling =
            lagBehandling(
                Fagsak(
                    type = FagsakType.INSTITUSJON,
                    aktør = randomAktør(),
                    institusjon = Institusjon(orgNummer = "123456789", tssEksternId = "xxx"),
                ),
            )

        every { fagsakRepository.finnFagsak(any()) } returns behandling.fagsak
        val orgNummer = behandling.fagsak.institusjon!!.orgNummer

        every {
            utgåendeJournalføringService.journalførManueltBrev(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                dokumenttype = any(),
                førsteside = any(),
                eksternReferanseId = any(),
                avsenderMottaker = capture(avsenderMottaker),
            )
        } returns "mockJournalpostId"
        every { journalføringRepository.save(any()) } returns
            DbJournalpost(
                behandling = behandling,
                journalpostId = "id",
            )
        every { organisasjonService.hentOrganisasjon(any()) } returns
            Organisasjon(
                organisasjonsnummer = orgNummer,
                navn = "Testinstitusjon",
            )
        every { brevmottakerService.hentBrevmottakere(behandling.id) } returns emptyList()

        runCatching {
            dokumentService.sendManueltBrev(
                ManueltBrevRequest(
                    brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
                    enhet = Enhet("enhet", "enhetNavn"),
                ),
                behandling = behandling,
                fagsakId = behandling.fagsak.id,
            )
        }
        assert(avsenderMottaker.isCaptured) { "AvsenderMottaker skal være satt for ikke å defaulte til Bruker" }
        assertThat(avsenderMottaker.captured.idType).isEqualTo(BrukerIdType.ORGNR)
        assertThat(avsenderMottaker.captured.id).isEqualTo(orgNummer)
        assertThat(avsenderMottaker.captured.navn).isEqualTo("Testinstitusjon")
    }

    @Test
    fun `sendManueltBrev skal journalføre uten eksplisitt AvsenderMottaker når mottaker er bruker`() {
        val avsenderMottaker = slot<AvsenderMottaker>()
        val behandling = lagBehandling()

        every { fagsakRepository.finnFagsak(any()) } returns behandling.fagsak

        every {
            utgåendeJournalføringService.journalførManueltBrev(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                dokumenttype = any(),
                førsteside = any(),
                eksternReferanseId = any(),
                avsenderMottaker = capture(avsenderMottaker),
            )
        } returns "mockJournalpostId"
        every { journalføringRepository.save(any()) } returns
            DbJournalpost(
                behandling = behandling,
                journalpostId = "id",
            )

        every { brevmottakerService.hentBrevmottakere(behandling.id) } returns emptyList()

        runCatching {
            dokumentService.sendManueltBrev(
                ManueltBrevRequest(
                    brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
                    enhet = Enhet("enhet", "enhetNavn"),
                ),
                behandling = behandling,
                fagsakId = behandling.fagsak.id,
            )
        }

        assert(!avsenderMottaker.isCaptured) { "AvsenderMottaker trenger ikke være satt når mottaker er bruker" }
    }

    @Test
    fun `sendManueltBrev skal legge til opplysningspliktvilkåret når gjeldende og forrige vilkårsvurdering mangler`() {
        val brevSomFørerTilOpplysningsplikt = Brevmal.values().filter { it.førerTilOpplysningsplikt() }

        brevSomFørerTilOpplysningsplikt.forEach { brevmal ->
            val behandling = lagBehandling()
            val vilkårsvurdering = lagVilkårsvurdering(lagPerson().aktør, behandling, Resultat.IKKE_VURDERT)
            val personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!

            // Scenario uten eksisterende vilkårsvurdering
            every { vilkårsvurderingService.hentAktivForBehandling(any()) } returns null
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null
            every {
                vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
                    any(),
                    any(),
                    null,
                )
            } returns
                vilkårsvurdering

            every { journalføringRepository.save(any()) } returns
                DbJournalpost(behandling = behandling, journalpostId = "id")
            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns emptyList()

            sendBrev(brevmal, behandling)

            assertThat(personResultat.andreVurderinger)
                .extracting("type")
                .containsExactly(AnnenVurderingType.OPPLYSNINGSPLIKT)
            verify(exactly = 1) {
                behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
            }
            verify(exactly = 1) {
                vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(behandling, any(), null)
            }
        }
    }

    @Test
    fun `sendManueltBrev skal legge til opplysningspliktvilkåret når gjeldende vilkårsvurdering mangler, men forrige finnes`() {
        val brevSomFørerTilOpplysningsplikt = Brevmal.values().filter { it.førerTilOpplysningsplikt() }

        brevSomFørerTilOpplysningsplikt.forEach { brevmal ->
            val behandling = lagBehandling()
            val forrigeVedtatteBehandling = lagBehandling()
            val vilkårsvurdering = lagVilkårsvurdering(lagPerson().aktør, behandling, Resultat.IKKE_VURDERT)
            val personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!

            // Scenario uten eksisterende vilkårsvurdering
            every { vilkårsvurderingService.hentAktivForBehandling(any()) } returns null
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeVedtatteBehandling
            every {
                vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
                    any(),
                    any(),
                    forrigeVedtatteBehandling,
                )
            } returns
                vilkårsvurdering

            every { journalføringRepository.save(any()) } returns
                DbJournalpost(behandling = behandling, journalpostId = "id")
            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns emptyList()

            sendBrev(brevmal, behandling)

            assertThat(personResultat.andreVurderinger)
                .extracting("type")
                .containsExactly(AnnenVurderingType.OPPLYSNINGSPLIKT)
            verify(exactly = 1) {
                behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
            }
            verify(exactly = 1) {
                vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
                    behandling,
                    any(),
                    forrigeVedtatteBehandling,
                )
            }
        }
    }

    @Test
    fun `sendManueltBrev skal legge til opplysningspliktvilkåret når vilkårsvurderingen finnes`() {
        val brevSomFørerTilOpplysningsplikt = Brevmal.values().filter { it.førerTilOpplysningsplikt() }

        brevSomFørerTilOpplysningsplikt.forEach { brevmal ->
            val behandling = lagBehandling()
            val vilkårsvurdering = lagVilkårsvurdering(lagPerson().aktør, behandling, Resultat.IKKE_VURDERT)
            val personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!

            // Scenario med eksisterende vilkårsvurdering
            every { vilkårsvurderingService.hentAktivForBehandling(any()) } returns vilkårsvurdering
            every { journalføringRepository.save(any()) } returns
                DbJournalpost(behandling = behandling, journalpostId = "id")
            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns emptyList()

            sendBrev(brevmal, behandling)

            assertThat(personResultat.andreVurderinger)
                .extracting("type")
                .containsExactly(AnnenVurderingType.OPPLYSNINGSPLIKT)
            verify(exactly = 0) {
                vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(behandling, any(), null)
            }
        }
    }

    @Test
    fun `sendManueltBrev skal sende manuelt brev til FULLMEKTIG og bruker som har FULLMEKTIG manuelt brev mottaker`() {
        val behandling = lagBehandling()
        val manueltBrevRequest = ManueltBrevRequest(brevmal = Brevmal.SVARTIDSBREV)
        val avsenderMottakere = mutableListOf<AvsenderMottaker>()

        every { brevmottakerService.hentBrevmottakere(behandling.id) } returns
            listOf(
                BrevmottakerDb(
                    behandlingId = behandling.id,
                    type = MottakerType.FULLMEKTIG,
                    navn = "Fullmektig navn",
                    adresselinje1 = "Test adresse",
                    postnummer = "0000",
                    poststed = "Oslo",
                    landkode = "NO",
                ),
            )
        every {
            utgåendeJournalføringService.journalførManueltBrev(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                dokumenttype = any(),
                førsteside = any(),
                eksternReferanseId = any(),
                avsenderMottaker = capture(avsenderMottakere),
            )
        } returns "mockJournalPostId" andThen "mockJournalPostId1"

        every { journalføringRepository.save(any()) } returns mockk()
        every { taskRepository.save(any()) } returns mockk()
        every { fagsakRepository.finnFagsak(behandling.fagsak.id) } returns behandling.fagsak

        dokumentService.sendManueltBrev(manueltBrevRequest, behandling, behandling.fagsak.id)

        verify(exactly = 2) {
            utgåendeJournalføringService.journalførManueltBrev(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                dokumenttype = any(),
                førsteside = any(),
                eksternReferanseId = any(),
                avsenderMottaker = any(),
            )
        }
        verify(exactly = 2) { journalføringRepository.save(any()) }
        verify(exactly = 2) { taskRepository.save(any()) }

        assertEquals(1, avsenderMottakere.size)
        assertEquals("Fullmektig navn", avsenderMottakere.single { it.idType == null }.navn)
    }

    @Test
    fun `sendManueltBrev skal sende manuelt brev til FULLMEKTIG og bruker ved manuell brevmottaker på fagsak`() {
        val aktør = randomAktør()
        val fagsak = Fagsak(aktør = aktør)
        val brevmottakere =
            listOf(
                ManuellBrevmottaker(
                    type = MottakerType.FULLMEKTIG,
                    navn = "Fullmektig navn",
                    adresselinje1 = "Test adresse",
                    postnummer = "0000",
                    poststed = "Oslo",
                    landkode = "NO",
                ),
            )
        val manueltBrevRequest =
            ManueltBrevRequest(
                brevmal = Brevmal.SVARTIDSBREV,
                manuelleBrevmottakere = brevmottakere,
            )
        val avsenderMottakere = mutableListOf<AvsenderMottaker>()

        every {
            utgåendeJournalføringService.journalførManueltBrev(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                dokumenttype = any(),
                førsteside = any(),
                eksternReferanseId = any(),
                avsenderMottaker = capture(avsenderMottakere),
            )
        } returns "mockJournalPostId" andThen "mockJournalPostId1"

        every { journalføringRepository.save(any()) } returns mockk()
        every { taskRepository.save(any()) } returns mockk()
        every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

        dokumentService.sendManueltBrev(manueltBrevRequest, null, fagsak.id)

        verify(exactly = 2) {
            utgåendeJournalføringService.journalførManueltBrev(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                dokumenttype = any(),
                førsteside = any(),
                eksternReferanseId = any(),
                avsenderMottaker = any(),
            )
        }
        verify(exactly = 2) { taskRepository.save(any()) }

        assertEquals(1, avsenderMottakere.size)
        assertEquals("Fullmektig navn", avsenderMottakere.single { it.idType == null }.navn)
    }

    @Test
    fun `sendManueltBrev skal sende informasjonsbrev manuelt på fagsak`() {
        val fagsak = defaultFagsak()
        val manueltBrevRequest =
            ManueltBrevRequest(brevmal = Brevmal.INFORMASJONSBREV_KAN_SØKE)

        every {
            utgåendeJournalføringService.journalførManueltBrev(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                dokumenttype = any(),
                førsteside = any(),
                eksternReferanseId = any(),
                avsenderMottaker = any(),
            )
        } returns "mockJournalPostId"

        every { taskRepository.save(any()) } returns mockk()

        dokumentService.sendManueltBrev(manueltBrevRequest, null, fagsak.id)

        verify(exactly = 1) {
            utgåendeJournalføringService.journalførManueltBrev(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                dokumenttype = any(),
                førsteside = any(),
                eksternReferanseId = any(),
                avsenderMottaker = any(),
            )
        }
        verify(exactly = 0) { journalføringRepository.save(any()) }
        verify(exactly = 1) { taskRepository.save(any()) }
    }

    private fun sendBrev(
        brevmal: Brevmal,
        behandling: Behandling,
    ) {
        dokumentService.sendManueltBrev(
            ManueltBrevRequest(
                brevmal = brevmal,
                enhet = Enhet("enhet", "enhetNavn"),
            ),
            behandling = behandling,
            fagsakId = behandling.fagsak.id,
        )
    }
}
