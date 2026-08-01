package no.nav.familie.ba.sak.kjerne.logg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak.SammensattKontrollsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LoggServiceEnhetTest {
    private val loggRepository = mockk<LoggRepository>()
    private val loggService = LoggService(loggRepository)

    @Test
    fun `loggSammensattKontrollsakLagtTil skal lagre ned logg på at sammensatt kontrollsak er opprettet`() {
        // Arrange
        val behandling = lagBehandling(id = 1)
        val sammensattKontrollsak = SammensattKontrollsak(behandlingId = behandling.id, fritekst = "test")

        every { loggRepository.save(any()) } returnsArgument 0

        // Act
        val opprettetLogg = loggService.loggSammensattKontrollsakLagtTil(sammensattKontrollsak)

        // Assert
        assertThat(opprettetLogg.type).isEqualTo(LoggType.SAMMENSATT_KONTROLLSAK_LAGT_TIL)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
    }

    @Test
    fun `loggSammensattKontrollsakEndret skal lagre ned logg på at sammensatt kontrollsak er endret`() {
        // Arrange
        val behandling = lagBehandling(id = 1)
        val oppdatertSammensattKontrollsak = SammensattKontrollsak(behandlingId = behandling.id, fritekst = "test2")

        every { loggRepository.save(any()) } returnsArgument 0

        // Act
        val opprettetLogg = loggService.loggSammensattKontrollsakEndret(oppdatertSammensattKontrollsak)

        // Assert
        assertThat(opprettetLogg.type).isEqualTo(LoggType.SAMMENSATT_KONTROLLSAK_ENDRET)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
    }

    @Test
    fun `loggSammensattKontrollsakFjernet skal lagre ned logg på at sammensatt kontrollsak er fjernet`() {
        // Arrange
        val behandling = lagBehandling(id = 1)

        every { loggRepository.save(any()) } returnsArgument 0

        // Act
        val opprettetLogg = loggService.loggSammensattKontrollsakFjernet(behandlingId = behandling.id)

        // Assert
        assertThat(opprettetLogg.type).isEqualTo(LoggType.SAMMENSATT_KONTROLLSAK_FJERNET)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
    }
}
