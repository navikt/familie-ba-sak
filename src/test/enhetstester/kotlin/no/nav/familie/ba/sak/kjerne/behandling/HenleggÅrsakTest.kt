import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class HenleggÅrsakTest {
    @ParameterizedTest
    @EnumSource(BehandlingÅrsak::class, names = ["SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID", "SMÅBARNSTILLEGG"])
    fun `Skal returnere HENLAGT_AUTOMATISK_SMÅBARNSTILLEGG for AUTOMATISK_HENLAGT hvis årsaken er småbarnstillegg relatert`(behandlingÅrsak: BehandlingÅrsak) {
        // Act
        val resultat = HenleggÅrsak.AUTOMATISK_HENLAGT.tilBehandlingsresultat(behandlingÅrsak)

        // Assert
        assertEquals(Behandlingsresultat.HENLAGT_AUTOMATISK_SMÅBARNSTILLEGG, resultat)
    }

    @Test
    fun `Skal returnere riktig behandlingsresultat for FEILAKTIG_OPPRETTET`() {
        // Act
        val resultat = HenleggÅrsak.FEILAKTIG_OPPRETTET.tilBehandlingsresultat(BehandlingÅrsak.FØDSELSHENDELSE)

        // Assert
        assertEquals(Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET, resultat)
    }

    @Test
    fun `Skal returnere riktig behandlingsresultat for SØKNAD_TRUKKET`() {
        // Act
        val resultat = HenleggÅrsak.SØKNAD_TRUKKET.tilBehandlingsresultat(BehandlingÅrsak.FØDSELSHENDELSE)

        // Assert
        assertEquals(Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET, resultat)
    }

    @Test
    fun `Skal returnere riktig behandlingsresultat for TEKNISK_VEDLIKEHOLD`() {
        // Act
        val resultat = HenleggÅrsak.TEKNISK_VEDLIKEHOLD.tilBehandlingsresultat(BehandlingÅrsak.FØDSELSHENDELSE)

        // Assert
        assertEquals(Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD, resultat)
    }
}
