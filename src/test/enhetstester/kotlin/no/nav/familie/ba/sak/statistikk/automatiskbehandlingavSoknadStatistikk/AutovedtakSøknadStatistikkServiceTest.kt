package no.nav.familie.ba.sak.statistikk.automatiskbehandlingavSoknadStatistikk

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.kjerne.autovedtak.søknad.AutovedtakSøknadMåBehandlesManueltÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AutovedtakSøknadStatistikkServiceTest {
    private val repository = mockk<AutomatiskBehandlingAvSøknadStatistikkRepository>()
    private val service = AutovedtakSøknadStatistikkService(repository)

    @Test
    fun `skal lagre alle årsaker i rekkefølge med enum navn og uten journalpost`() {
        // Arrange
        val lagrede = slot<Iterable<AutomatiskBehandlingAvSøknadStatistikk>>()
        every { repository.saveAll(capture(lagrede)) } answers { lagrede.captured.toList() }

        // Act
        val årsaker =
            listOf(
                AutovedtakSøknadMåBehandlesManueltÅrsak.SØKER_VILKÅR_BOSATT_I_RIKET,
                AutovedtakSøknadMåBehandlesManueltÅrsak.BARN_VILKÅR_BOSATT_I_RIKET,
                AutovedtakSøknadMåBehandlesManueltÅrsak.BARN_VILKÅR_BOSATT_I_RIKET,
            )
        service.lagreÅrsaker(42, årsaker)

        // Assert
        verify(exactly = 1) { repository.saveAll(any<Iterable<AutomatiskBehandlingAvSøknadStatistikk>>()) }
        assertThat(lagrede.captured.map { it.behandlingId }).containsExactly(42, 42, 42)
        assertThat(lagrede.captured.map { it.årsakTilManuellBehandling }).containsExactlyElementsOf(årsaker.map { it.name })
        assertThat(lagrede.captured.map { it.journalpostId }).containsOnlyNulls()
    }
}
