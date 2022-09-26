package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpost
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.DokumentService.Companion.alleredeDistribuertMelding
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
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
            utgåendeJournalføringService = utgåendeJournalføringService
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
    fun `sendManueltBrev skal legge til opplysningspliktvilkåret, om så ved å initiere vilkårsvurdering først`() {
        val vilkårsvurdering = lagVilkårsvurdering(lagPerson().aktør, lagBehandling(), Resultat.IKKE_VURDERT)
        val personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!

        // Scenario med eksisterende vilkårsvurdering
        every { vilkårsvurderingService.hentAktivForBehandling(any()) } returns vilkårsvurdering
        every { journalføringRepository.save(any()) } returns DbJournalpost(behandling = vilkårsvurdering.behandling, journalpostId = "id")

        sendBrevInnhenteOpplysninger(vilkårsvurdering.behandling)

        assertThat(personResultat.andreVurderinger).extracting("type").containsExactly(AnnenVurderingType.OPPLYSNINGSPLIKT)
        verify(exactly = 0) {
            vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(any(), any())
        }

        // Scenario uten eksisterende vilkårsvurdering
        personResultat.setAndreVurderinger(emptySet()) // nullstiller andreVurderinger
        every { vilkårsvurderingService.hentAktivForBehandling(any()) } returns null
        every { vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(any(), any()) } returns
                vilkårsvurdering

        sendBrevInnhenteOpplysninger(vilkårsvurdering.behandling)

        assertThat(personResultat.andreVurderinger).extracting("type").containsExactly(AnnenVurderingType.OPPLYSNINGSPLIKT)
        verify(exactly = 1) {
            vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(any(), any())
        }
    }

    private fun sendBrevInnhenteOpplysninger(behandling: Behandling) {
        dokumentService.sendManueltBrev(
            ManueltBrevRequest(
                brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
                mottakerIdent = "123456789",
                enhet = Enhet("enhet", "enhetNavn")
            ),
            behandling = behandling,
            fagsakId = behandling.fagsak.id
        )
    }


}
