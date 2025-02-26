package no.nav.familie.ba.sak.kjerne.logg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak.SammensattKontrollsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LoggServiceEnhetTest {
    private val loggRepository = mockk<LoggRepository>()
    private val rolleConfig = mockk<RolleConfig>()

    private val loggService = LoggService(loggRepository, rolleConfig)

    @Test
    fun `loggSammensattKontrollsakLagtTil skal lagre ned logg på at sammensatt kontrollsak er opprettet`() {
        val behandling = lagBehandling(id = 1)
        val sammensattKontrollsak = SammensattKontrollsak(behandlingId = behandling.id, fritekst = "test")

        every { loggRepository.save(any()) } returnsArgument 0

        val opprettetLogg = loggService.loggSammensattKontrollsakLagtTil(sammensattKontrollsak)

        assertThat(opprettetLogg.type).isEqualTo(LoggType.SAMMENSATT_KONTROLLSAK_LAGT_TIL)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
    }

    @Test
    fun `loggSammensattKontrollsakEndret skal lagre ned logg på at sammensatt kontrollsak er endret`() {
        val behandling = lagBehandling(id = 1)
        val oppdatertSammensattKontrollsak = SammensattKontrollsak(behandlingId = behandling.id, fritekst = "test2")

        every { loggRepository.save(any()) } returnsArgument 0

        val opprettetLogg = loggService.loggSammensattKontrollsakEndret(oppdatertSammensattKontrollsak)

        assertThat(opprettetLogg.type).isEqualTo(LoggType.SAMMENSATT_KONTROLLSAK_ENDRET)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
    }

    @Test
    fun `loggSammensattKontrollsakFjernet skal lagre ned logg på at sammensatt kontrollsak er fjernet`() {
        val behandling = lagBehandling(id = 1)

        every { loggRepository.save(any()) } returnsArgument 0

        val opprettetLogg = loggService.loggSammensattKontrollsakFjernet(behandlingId = behandling.id)

        assertThat(opprettetLogg.type).isEqualTo(LoggType.SAMMENSATT_KONTROLLSAK_FJERNET)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
    }
}
