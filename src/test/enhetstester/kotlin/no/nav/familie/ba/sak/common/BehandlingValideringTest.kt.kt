package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE

class BehandlingValideringTest {
    @ParameterizedTest
    @EnumSource(BehandlingStatus::class, names = ["UTREDES"], mode = EXCLUDE)
    fun `Skal kaste feil for alle behandlingstatus utenom UTREDES dersom behandling forsøkes å redigeres på`(
        behandlingStatus: BehandlingStatus,
    ) {
        val melding = assertThrows<FunksjonellFeil> { validerBehandlingKanRedigeres(behandlingStatus) }.message

        assertThat(melding).isEqualTo("Behandlingen er låst for videre redigering da den har statusen ${behandlingStatus.name}")
    }
}
