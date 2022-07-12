package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.brev.DokumentService.Companion.alleredeDistribuertMelding
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException

internal class DokumentServiceEnhetstest {
    val integrasjonClient = mockk<IntegrasjonClient>(relaxed = true)

    private val dokumentService: DokumentService = spyk(
        DokumentService(
            integrasjonClient = integrasjonClient,

            loggService = mockk(relaxed = true),
            persongrunnlagService = mockk(relaxed = true),
            journalføringRepository = mockk(relaxed = true),
            taskRepository = mockk(relaxed = true),
            brevKlient = mockk(relaxed = true),
            brevService = mockk(relaxed = true),
            vilkårsvurderingService = mockk(relaxed = true),
            rolleConfig = mockk(relaxed = true),
            settPåVentService = mockk(relaxed = true),
            utgåendeJournalføringService = mockk(relaxed = true)
        )
    )

    @Test
    fun `Skal kalle "loggBrevIkkeDistribuertUkjentAdresse" ved 400 kode og "Mottaker har ukjent adresse" melding`() {
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
    fun `Skal kalle "håndterMottakerDødIngenAdressePåBehandling" ved 410 Gone svar under distribuering"`() {
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
}
