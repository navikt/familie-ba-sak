package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpost
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.DokumentService.Companion.alleredeDistribuertMelding
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException

internal class DokumentServiceEnhetstest {
    val integrasjonClient = mockk<IntegrasjonClient>(relaxed = true)
    val vilkårsvurderingService = mockk<VilkårsvurderingService>(relaxed = true)
    val vilkårsvurderingForNyBehandlingService = mockk<VilkårsvurderingForNyBehandlingService>(relaxed = true)
    val utgåendeJournalføringService = mockk<UtgåendeJournalføringService>(relaxed = true)
    val journalføringRepository = mockk<JournalføringRepository>(relaxed = true)
    val fagsakRepository = mockk<FagsakRepository>(relaxed = true)
    val organisasjonService = mockk<OrganisasjonService>(relaxed = true)
    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>(relaxed = true)

    private val dokumentService: DokumentService = spyk(
        DokumentService(
            integrasjonClient = integrasjonClient,
            loggService = mockk(relaxed = true),
            persongrunnlagService = mockk(relaxed = true),
            journalføringRepository = journalføringRepository,
            taskRepository = mockk(relaxed = true),
            brevKlient = mockk(relaxed = true),
            brevService = mockk(relaxed = true),
            vilkårsvurderingService = vilkårsvurderingService,
            vilkårsvurderingForNyBehandlingService = vilkårsvurderingForNyBehandlingService,
            rolleConfig = mockk(relaxed = true),
            settPåVentService = mockk(relaxed = true),
            utgåendeJournalføringService = utgåendeJournalføringService,
            fagsakRepository = fagsakRepository,
            organisasjonService = organisasjonService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService
        )
    )

    @Test
    fun `Skal kalle 'loggBrevIkkeDistribuertUkjentAdresse' ved 400 kode og 'Mottaker har ukjent adresse' melding`() {
        every { dokumentService.håndterMottakerDødIngenAdressePåBehandling(any(), any(), any()) } returns Unit
        every {
            integrasjonClient.distribuerBrev(any(), any())
        } throws RessursException(
            httpStatus = HttpStatus.BAD_REQUEST,
            ressurs = Ressurs.failure(),
            cause = RestClientResponseException("Mottaker har ukjent adresse", 400, "", null, null, null)
        )

        val journalpostId = "testId"
        dokumentService.prøvDistribuerBrevOgLoggHendelse(
            journalpostId,
            1L,
            BehandlerRolle.BESLUTTER,
            Brevmal.SVARTIDSBREV
        )

        verify(exactly = 1) { dokumentService.loggBrevIkkeDistribuertUkjentAdresse(any(), any(), any()) }
    }

    @Test
    fun `Skal kalle 'håndterMottakerDødIngenAdressePåBehandling' ved 410 Gone svar under distribuering`() {
        every { dokumentService.håndterMottakerDødIngenAdressePåBehandling(any(), any(), any()) } returns Unit
        every {
            integrasjonClient.distribuerBrev(any(), any())
        } throws RessursException(
            httpStatus = HttpStatus.GONE,
            ressurs = Ressurs.failure(),
            cause = RestClientResponseException("", 410, "", null, null, null)
        )

        val journalpostId = "testId"
        dokumentService.prøvDistribuerBrevOgLoggHendelse(
            journalpostId,
            1L,
            BehandlerRolle.BESLUTTER,
            Brevmal.SVARTIDSBREV
        )

        verify(exactly = 1) { dokumentService.håndterMottakerDødIngenAdressePåBehandling(any(), any(), any()) }
    }

    @Test
    fun `Skal hoppe over distribuering ved 409 Conflict mot dokdist`() {
        every { dokumentService.logger.info(any()) } returns Unit
        every { dokumentService.logger.warn(any()) } returns Unit

        every {
            integrasjonClient.distribuerBrev(any(), any())
        } throws RessursException(
            httpStatus = HttpStatus.CONFLICT,
            ressurs = Ressurs.failure(),
            cause = RestClientResponseException("", 409, "", null, null, null)
        )

        val journalpostId = "testId"
        val behandlingId = 1L
        dokumentService.prøvDistribuerBrevOgLoggHendelse(
            journalpostId = journalpostId,
            behandlingId = behandlingId,
            loggBehandlerRolle = BehandlerRolle.BESLUTTER,
            brevmal = Brevmal.SVARTIDSBREV
        )

        verify { dokumentService.logger.warn(alleredeDistribuertMelding(journalpostId, behandlingId)) }
    }

    @Test
    fun `sendManueltBrev skal journalføre med brukerIdType ORGNR hvis brukers id er 9 siffer, og FNR ellers`() {
        listOf("123456789", "12345678911").forEach { brukerId ->
            val avsenderMottaker = slot<AvsenderMottaker>()
            val behandling = lagBehandling()

            val aktør = mockk<Aktør>()
            every { aktør.aktivFødselsnummer() } returns "12345678911"
            val fagsak = mockk<Fagsak>()
            every { fagsak.aktør } returns aktør
            every { fagsakRepository.finnFagsak(any()) } returns fagsak

            every {
                utgåendeJournalføringService.journalførManueltBrev(
                    fnr = any(),
                    fagsakId = any(),
                    journalførendeEnhet = any(),
                    brev = any(),
                    førsteside = any(),
                    dokumenttype = any(),
                    avsenderMottaker = capture(avsenderMottaker)
                )
            } returns "mockJournalpostId"
            every { journalføringRepository.save(any()) } returns DbJournalpost(
                behandling = behandling,
                journalpostId = "id"
            )
            every { organisasjonService.hentOrganisasjon(any()) } returns Organisasjon(
                organisasjonsnummer = brukerId,
                navn = "Testinstitusjon"
            )

            runCatching {
                dokumentService.sendManueltBrev(
                    ManueltBrevRequest(
                        brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
                        mottakerIdent = brukerId,
                        enhet = Enhet("enhet", "enhetNavn")
                    ),
                    behandling = behandling,
                    fagsakId = behandling.fagsak.id
                )
            }
            when (brukerId.length) {
                9 -> {
                    assert(avsenderMottaker.isCaptured) { "AvsenderMottaker skal være fanget" }
                    assertThat(avsenderMottaker.captured.idType).isEqualTo(BrukerIdType.ORGNR)
                    assertThat(avsenderMottaker.captured.id).isEqualTo(brukerId)
                    assertThat(avsenderMottaker.captured.navn).isEqualTo("Testinstitusjon")
                }

                else -> assert(!avsenderMottaker.isCaptured) { "AvsenderMottaker skal ikke være fanget" }
            }
        }
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
                    null
                )
            } returns
                vilkårsvurdering

            every { journalføringRepository.save(any()) } returns
                DbJournalpost(behandling = behandling, journalpostId = "id")

            sendBrev(brevmal, behandling)

            assertThat(personResultat.andreVurderinger).extracting("type")
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
                    forrigeVedtatteBehandling
                )
            } returns
                vilkårsvurdering

            every { journalføringRepository.save(any()) } returns
                DbJournalpost(behandling = behandling, journalpostId = "id")

            sendBrev(brevmal, behandling)

            assertThat(personResultat.andreVurderinger).extracting("type")
                .containsExactly(AnnenVurderingType.OPPLYSNINGSPLIKT)
            verify(exactly = 1) {
                behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
            }
            verify(exactly = 1) {
                vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
                    behandling,
                    any(),
                    forrigeVedtatteBehandling
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

            sendBrev(brevmal, behandling)

            assertThat(personResultat.andreVurderinger).extracting("type")
                .containsExactly(AnnenVurderingType.OPPLYSNINGSPLIKT)
            verify(exactly = 0) {
                vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(behandling, any(), null)
            }
        }
    }

    private fun sendBrev(brevmal: Brevmal, behandling: Behandling) {
        dokumentService.sendManueltBrev(
            ManueltBrevRequest(
                brevmal = brevmal,
                mottakerIdent = "123456789",
                enhet = Enhet("enhet", "enhetNavn")
            ),
            behandling = behandling,
            fagsakId = behandling.fagsak.id
        )
    }
}
