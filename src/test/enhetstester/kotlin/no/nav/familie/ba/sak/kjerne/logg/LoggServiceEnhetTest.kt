package no.nav.familie.ba.sak.kjerne.logg

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.datagenerator.lagBehandlingMedId
import no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak.SammensattKontrollsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class LoggServiceEnhetTest {
    @MockK
    private lateinit var loggRepository: LoggRepository

    @MockK
    private lateinit var rolleConfig: RolleConfig

    @InjectMockKs
    private lateinit var loggService: LoggService

    @Test
    fun `loggSammensattKontrollsakLagtTil skal lagre ned logg på at sammensatt kontrollsak er opprettet`() {
        val behandling = lagBehandlingMedId(id = 1)
        val sammensattKontrollsak = SammensattKontrollsak(behandlingId = behandling.id, fritekst = "test")

        every { loggRepository.save(any()) } returnsArgument 0

        val opprettetLogg = loggService.loggSammensattKontrollsakLagtTil(sammensattKontrollsak)

        assertThat(opprettetLogg.type).isEqualTo(LoggType.SAMMENSATT_KONTROLLSAK_LAGT_TIL)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
    }

    @Test
    fun `loggSammensattKontrollsakEndret skal lagre ned logg på at sammensatt kontrollsak er endret`() {
        val behandling = lagBehandlingMedId(id = 1)
        val oppdatertSammensattKontrollsak = SammensattKontrollsak(behandlingId = behandling.id, fritekst = "test2")

        every { loggRepository.save(any()) } returnsArgument 0

        val opprettetLogg = loggService.loggSammensattKontrollsakEndret(oppdatertSammensattKontrollsak)

        assertThat(opprettetLogg.type).isEqualTo(LoggType.SAMMENSATT_KONTROLLSAK_ENDRET)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
    }

    @Test
    fun `loggSammensattKontrollsakFjernet skal lagre ned logg på at sammensatt kontrollsak er fjernet`() {
        val behandling = lagBehandlingMedId(id = 1)

        every { loggRepository.save(any()) } returnsArgument 0

        val opprettetLogg = loggService.loggSammensattKontrollsakFjernet(behandlingId = behandling.id)

        assertThat(opprettetLogg.type).isEqualTo(LoggType.SAMMENSATT_KONTROLLSAK_FJERNET)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
    }
}
