package no.nav.familie.ba.sak.statistikk.saksstatistikk

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime

class RelatertBehandlingUtlederTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val unleashService = mockk<UnleashNextMedContextService>()
    private val relatertBehandlingUtleder =
        RelatertBehandlingUtleder(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            unleashService = unleashService,
        )

    @BeforeEach
    fun oppsett() {
        every { unleashService.isEnabled(FeatureToggle.BEHANDLE_KLAGE, false) } returns true
    }

    @Nested
    inner class UtledRelatertBehandling {
        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["KLAGE", "IVERKSETTE_KA_VEDTAK"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal utlede relatert behandling med barnetrygdbehandling som forrige vedtatte behandling selv for revurdering med årsak klage eller årsak iverksette ka vedtak når toggle er skrudd av`(
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

            every { unleashService.isEnabled(FeatureToggle.BEHANDLE_KLAGE, false) } returns false
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(revurdering) } returns forrigeVedtatteBarnetrygdbehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            assertThat(relatertBehandling?.id).isEqualTo(forrigeVedtatteBarnetrygdbehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.BA)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(forrigeVedtatteBarnetrygdbehandling.aktivertTidspunkt)
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["KLAGE", "IVERKSETTE_KA_VEDTAK"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal ikke utlede relatert behandling når den innsendte behandling er en revurdering med årsak klage eller årsak iverksette ka vedtak`(
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

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            verify { behandlingHentOgPersisterService wasNot called }
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
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(forrigeVedtatteBarnetrygdbehandling.aktivertTidspunkt)
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
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(forrigeVedtatteBarnetrygdbehandling.aktivertTidspunkt)
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
