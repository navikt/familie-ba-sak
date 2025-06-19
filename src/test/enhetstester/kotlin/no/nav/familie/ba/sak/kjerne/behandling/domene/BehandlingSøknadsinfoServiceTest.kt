package no.nav.familie.ba.sak.kjerne.behandling.domene

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.Søknadsinfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingSøknadsinfoServiceTest {
    private val behandlingSøknadsinfoRepository = mockk<BehandlingSøknadsinfoRepository>(relaxed = true)
    private val behandlingSøknadsinfoService = BehandlingSøknadsinfoService(behandlingSøknadsinfoRepository)
    private val behandling = lagBehandling()
    private val journalpostId = "123456789"
    private val søknadMottattDato = LocalDate.of(2025, 1, 1)
    private val søknadsinfo =
        Søknadsinfo(
            journalpostId = journalpostId,
            brevkode = "BREV123",
            erDigital = true,
        )

    @BeforeEach
    fun setUp() {
        every { behandlingSøknadsinfoRepository.save(any()) } returnsArgument 0
        every { behandlingSøknadsinfoRepository.findByBehandlingId(any()) } returns
            setOf(
                BehandlingSøknadsinfo(
                    behandling = behandling,
                    journalpostId = journalpostId,
                    mottattDato = søknadMottattDato.atStartOfDay(),
                    brevkode = "BREV123",
                    erDigital = true,
                ),
            )
    }

    @Test
    fun lagreSøknadsinfo() {
        // Act
        behandlingSøknadsinfoService.lagreSøknadsinfo(
            behandling = behandling,
            søknadsinfo = søknadsinfo,
            mottattDato = søknadMottattDato,
        )

        // Assert
        verify(exactly = 1) {
            behandlingSøknadsinfoRepository.save(
                BehandlingSøknadsinfo(
                    behandling = behandling,
                    journalpostId = journalpostId,
                    mottattDato = søknadMottattDato.atStartOfDay(),
                    brevkode = "BREV123",
                    erDigital = true,
                ),
            )
        }
    }

    @Test
    fun hentSøkønadMottattDato() {
        val søknadMottattDato = behandlingSøknadsinfoService.hentSøknadMottattDato(behandlingId = behandling.id)
        verify(exactly = 1) { behandlingSøknadsinfoRepository.findByBehandlingId(behandling.id) }
        assertThat(søknadMottattDato).isEqualTo(this.søknadMottattDato.atStartOfDay())
    }

    @Test
    fun hentJournalpostId() {
        val journalpostId = behandlingSøknadsinfoService.hentJournalpostId(behandlingId = behandling.id)
        verify(exactly = 1) { behandlingSøknadsinfoRepository.findByBehandlingId(behandling.id) }
        assertThat(journalpostId).isEqualTo(this.journalpostId)
    }
}
