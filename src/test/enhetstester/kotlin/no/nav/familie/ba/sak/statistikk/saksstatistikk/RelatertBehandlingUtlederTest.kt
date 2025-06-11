package no.nav.familie.ba.sak.statistikk.saksstatistikk

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEksternBehandlingRelasjon
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.EksternBehandlingRelasjonService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime

class RelatertBehandlingUtlederTest {
    private val eksternBehandlingRelasjonService = mockk<EksternBehandlingRelasjonService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val relatertBehandlingUtleder =
        RelatertBehandlingUtleder(
            eksternBehandlingRelasjonService = eksternBehandlingRelasjonService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        )

    @Nested
    inner class UtledRelatertBehandling {
        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["KLAGE", "IVERKSETTE_KA_VEDTAK"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal utlede relatert behandling når den innsendte behandling er en revurdering med årsak klage eller årsak iverksette ka vedtak`(
            behandlingÅrsak: BehandlingÅrsak,
        ) {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurdering =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = behandlingÅrsak,
                    aktivertTid = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val eksternBehandlingRelasjon = lagEksternBehandlingRelasjon()

            every {
                eksternBehandlingRelasjonService.finnEksternBehandlingRelasjon(
                    behandlingId = revurdering.id,
                    fagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )
            } returns eksternBehandlingRelasjon

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            verify { behandlingHentOgPersisterService wasNot called }
            assertThat(relatertBehandling?.id).isEqualTo(eksternBehandlingRelasjon.eksternBehandlingId)
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KLAGE)
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["KLAGE", "IVERKSETTE_KA_VEDTAK"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal ikke utlede relatert behandling om ingen ekstern behandling relasjon finnes  når den innsendte behandling er en revurdering med årsak klage eller årsak iverksette ka vedtak`(
            behandlingÅrsak: BehandlingÅrsak,
        ) {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurdering =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = behandlingÅrsak,
                    aktivertTid = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            every {
                eksternBehandlingRelasjonService.finnEksternBehandlingRelasjon(
                    behandlingId = revurdering.id,
                    fagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )
            } returns null

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            assertThat(relatertBehandling).isNull()
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["KLAGE", "IVERKSETTE_KA_VEDTAK"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal utlede relatert behandling med barnetrygdbehandlingen som forrige vedtatte behandling når den innsendte behandling er en revurdering som ikke har årsak klage eller iverksetter ka vedtak`(
            behandlingÅrsak: BehandlingÅrsak,
        ) {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurdering =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = behandlingÅrsak,
                    aktivertTid = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val forrigeVedtatteBarnetrygdbehandling =
                lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    aktivertTid = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(revurdering) } returns forrigeVedtatteBarnetrygdbehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            assertThat(relatertBehandling?.id).isEqualTo(forrigeVedtatteBarnetrygdbehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.BA)
        }

        @Test
        fun `skal utlede relatert behandling med barnetrygdbehandlingen som forrige vedtatte behandling når den innsendte behandling er en teknisk endring`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val tekniskEndring =
                lagBehandling(
                    behandlingType = BehandlingType.TEKNISK_ENDRING,
                    årsak = BehandlingÅrsak.TEKNISK_ENDRING,
                    aktivertTid = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val forrigeVedtatteBarnetrygdbehandling =
                lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    aktivertTid = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(tekniskEndring) } returns forrigeVedtatteBarnetrygdbehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(tekniskEndring)

            // Assert
            assertThat(relatertBehandling?.id).isEqualTo(forrigeVedtatteBarnetrygdbehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.BA)
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingType::class,
            names = ["REVURDERING", "TEKNISK_ENDRING"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal ikke utlede relatert behandling når ingen barnetrygdbehandlingen er vedtatte for den innsendte revurderingen eller teknisk endringen`(
            behandlingType: BehandlingType,
        ) {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val behandling =
                lagBehandling(
                    behandlingType = behandlingType,
                    årsak = if (behandlingType == BehandlingType.TEKNISK_ENDRING) BehandlingÅrsak.TEKNISK_ENDRING else BehandlingÅrsak.SØKNAD,
                    aktivertTid = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(behandling)

            // Assert
            assertThat(relatertBehandling).isNull()
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingType::class,
            names = ["REVURDERING", "TEKNISK_ENDRING"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal ikke utlede relatert behandling når den innsendte behandling ikke er en revurdering eller teknisk endring`(
            behandlingType: BehandlingType,
        ) {
            // Arrange
            val revurdering =
                lagBehandling(
                    behandlingType = behandlingType,
                    årsak = BehandlingÅrsak.SØKNAD,
                    aktivertTid = LocalDateTime.now(),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            verify { behandlingHentOgPersisterService wasNot called }
            assertThat(relatertBehandling).isNull()
        }
    }
}
