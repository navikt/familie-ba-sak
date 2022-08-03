package no.nav.familie.ba.sak.kjerne.verge

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VergeServiceTest {

    private val behandlingServcieMock: BehandlingService = mockk()

    @Test
    fun `RegistrerVerge() skal lagre verge og oppdater behandling`() {
        val behandling = lagBehandling()
        val behandlingSlot = slot<Behandling>()
        every { behandlingServcieMock.lagre(capture(behandlingSlot)) } returns behandling
        val vergeService = VergeService(behandlingServcieMock)
        val verge = Verge(BigInteger.ONE, "verge 1", "adresse 1", "12345678910", behandling)
        vergeService.RegistrerVergeForBehandling(behandling, verge)
        val behandlingCaptured = behandlingSlot.captured
        val vergeCaptured = behandlingCaptured.verge
        assertThat(vergeCaptured!!.id).isEqualTo(verge.id)
        assertThat(vergeCaptured!!.Navn).isEqualTo(verge.Navn)
        assertThat(vergeCaptured!!.Adresse).isEqualTo(verge.Adresse)
        assertThat(vergeCaptured!!.Ident).isEqualTo(verge.Ident)
        assertThat(vergeCaptured!!.behandling.id).isEqualTo(behandling.id)
    }
}
