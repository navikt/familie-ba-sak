package no.nav.familie.ba.sak.statistikk.saksstatistikk

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagKlagebehandlingDto
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.klage.KlageService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime

class RelatertBehandlingUtlederTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val klageService = mockk<KlageService>()
    private val unleashService = mockk<UnleashNextMedContextService>()
    private val relatertBehandlingUtleder =
        RelatertBehandlingUtleder(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            klageService = klageService,
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
        fun `skal utlede relatert behandling med barnetrygdbehandling som siste vedtatte behandling selv for revurdering med årsak klage eller årsak iverksette ka vedtak når toggle er skrudd av`(
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

            val sisteVedtatteBarnetrygdbehandling =
                lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    aktivertTid = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { unleashService.isEnabled(FeatureToggle.BEHANDLE_KLAGE, false) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(revurdering.fagsak.id) } returns sisteVedtatteBarnetrygdbehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            verify { klageService wasNot called }
            assertThat(relatertBehandling?.id).isEqualTo(sisteVedtatteBarnetrygdbehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.BA)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(sisteVedtatteBarnetrygdbehandling.aktivertTidspunkt)
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["KLAGE", "IVERKSETTE_KA_VEDTAK"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal kaste feil om ingen vedtatt klagebehandling finnes når den innsendte behandling er en revurdering med årsak klage eller årsak iverksette ka vedtak`(
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

            every { klageService.hentSisteVedtatteKlagebehandling(revurdering.fagsak.id) } returns null

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    relatertBehandlingUtleder.utledRelatertBehandling(revurdering)
                }
            assertThat(exception.message).isEqualTo("Forventer en vedtatt klagebehandling for fagsak ${revurdering.fagsak.id} og behandling ${revurdering.id}")
            verify { behandlingHentOgPersisterService wasNot called }
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["KLAGE", "IVERKSETTE_KA_VEDTAK"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal utlede relatert behandling med klagebehandlingen som siste vedtatte behandling når den innsendte behandling er en revurdering med årsak klage eller årsak iverksette ka vedtak`(
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

            val sisteVedtatteKlagebehandling =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt,
                )

            every { klageService.hentSisteVedtatteKlagebehandling(revurdering.fagsak.id) } returns sisteVedtatteKlagebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            verify { behandlingHentOgPersisterService wasNot called }
            assertThat(relatertBehandling?.id).isEqualTo(sisteVedtatteKlagebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KLAGE)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(sisteVedtatteKlagebehandling.vedtaksdato)
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["KLAGE", "IVERKSETTE_KA_VEDTAK"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal utlede relatert behandling med barnetrygdbehandlingen som siste vedtatte behandling når den innsendte behandling er en revurdering som ikke har årsak klage eller iverksetter ka vedtak`(
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

            val sisteVedtatteBarnetrygdbehandling =
                lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    aktivertTid = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(revurdering.fagsak.id) } returns sisteVedtatteBarnetrygdbehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            verify { klageService wasNot called }
            assertThat(relatertBehandling?.id).isEqualTo(sisteVedtatteBarnetrygdbehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.BA)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(sisteVedtatteBarnetrygdbehandling.aktivertTidspunkt)
        }

        @Test
        fun `skal utlede relatert behandling med barnetrygdbehandlingen som siste vedtatte behandling når den innsendte behandling er en teknisk endring`() {
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

            val sisteVedtatteBarnetrygdbehandling =
                lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    aktivertTid = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(tekniskEndring.fagsak.id) } returns sisteVedtatteBarnetrygdbehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(tekniskEndring)

            // Assert
            verify { klageService wasNot called }
            assertThat(relatertBehandling?.id).isEqualTo(sisteVedtatteBarnetrygdbehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.BA)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(sisteVedtatteBarnetrygdbehandling.aktivertTidspunkt)
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

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(behandling)

            // Assert
            verify { klageService wasNot called }
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
            verify { klageService wasNot called }
            verify { behandlingHentOgPersisterService wasNot called }
            assertThat(relatertBehandling).isNull()
        }
    }
}
