package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

internal class DokumentDistribueringServiceTest {
    private val taskService = mockk<TaskService>(relaxed = true)
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val loggService = mockk<LoggService>(relaxed = true)

    private val dokumentDistribueringService =
        DokumentDistribueringService(
            taskService = taskService,
            integrasjonKlient = integrasjonKlient,
            loggService = loggService,
        )

    @Test
    fun `Skal kalle 'loggBrevIkkeDistribuertUkjentAdresse' ved 400 kode og 'Mottaker har ukjent adresse' melding`() {
        every {
            integrasjonKlient.distribuerBrev(any())
        } throws HttpClientErrorException("Mottaker har ukjent adresse", HttpStatus.BAD_REQUEST, "", null, null, null)

        dokumentDistribueringService.prøvDistribuerBrevOgLoggHendelseFraBehandling(
            distribuerDokumentDTO = lagDistribuerDokumentDTO(),
            loggBehandlerRolle = BehandlerRolle.BESLUTTER,
        )

        verify(exactly = 1) { loggService.opprettBrevIkkeDistribuertUkjentAdresseLogg(any(), any()) }
    }

    @Test
    fun `Skal kalle 'håndterMottakerDødIngenAdressePåBehandling' ved 410 Gone svar under distribuering`() {
        every {
            integrasjonKlient.distribuerBrev(any())
        } throws HttpClientErrorException("", HttpStatus.GONE, "", null, null, null)

        dokumentDistribueringService.prøvDistribuerBrevOgLoggHendelseFraBehandling(
            distribuerDokumentDTO = lagDistribuerDokumentDTO(),
            loggBehandlerRolle = BehandlerRolle.BESLUTTER,
        )

        verify(exactly = 1) {
            loggService.opprettBrevIkkeDistribuertUkjentDødsboadresseLogg(any(), any())
        }
    }

    @Test
    fun `Skal hoppe over distribuering ved 409 Conflict mot dokdist`() {
        every {
            integrasjonKlient.distribuerBrev(any())
        } throws HttpClientErrorException("", HttpStatus.CONFLICT, "", null, null, null)

        assertDoesNotThrow {
            dokumentDistribueringService.prøvDistribuerBrevOgLoggHendelseFraBehandling(
                distribuerDokumentDTO = lagDistribuerDokumentDTO(),
                loggBehandlerRolle = BehandlerRolle.BESLUTTER,
            )
        }
    }

    private fun lagDistribuerDokumentDTO() =
        DistribuerDokumentDTO(
            journalpostId = "testId",
            behandlingId = 1L,
            brevmal = Brevmal.SVARTIDSBREV,
            fagsakId = 1L,
            erManueltSendt = true,
        )
}
