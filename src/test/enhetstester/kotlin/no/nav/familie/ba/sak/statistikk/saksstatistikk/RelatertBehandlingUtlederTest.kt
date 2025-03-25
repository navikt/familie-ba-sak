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
        @Test
        fun `skal finne siste vedtatte barnetrygdbehandling selv for revurdering klage når toggle er skrudd av`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurderingKlage =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.KLAGE,
                    aktivertTid = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val sisteVedtatteBarnetrygdbehandling =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    aktivertTid = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { unleashService.isEnabled(FeatureToggle.BEHANDLE_KLAGE, false) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(revurderingKlage.fagsak.id) } returns sisteVedtatteBarnetrygdbehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurderingKlage)

            // Assert
            verify { klageService wasNot called }
            assertThat(relatertBehandling?.id).isEqualTo(sisteVedtatteBarnetrygdbehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.BA)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(sisteVedtatteBarnetrygdbehandling.aktivertTidspunkt)
        }

        @Test
        fun `skal kaste feil om ingen vedtatt klagebehandling finnes når den innsendte behandling er en revurdering med årsak klage`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurderingKlage =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.KLAGE,
                    aktivertTid = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            every { klageService.hentSisteVedtatteKlagebehandling(revurderingKlage.fagsak.id) } returns null

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    relatertBehandlingUtleder.utledRelatertBehandling(revurderingKlage)
                }
            assertThat(exception.message).isEqualTo("Forventer en vedtatt klagebehandling for behandling ${revurderingKlage.id}")
            verify { behandlingHentOgPersisterService wasNot called }
        }

        @Test
        fun `skal kaste feil om ingen vedtatt klagebehandling finnes når den innsendte behandling er en revurdering med årsak iverksette ka vedtak`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurderingKlage =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.IVERKSETTE_KA_VEDTAK,
                    aktivertTid = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            every { klageService.hentSisteVedtatteKlagebehandling(revurderingKlage.fagsak.id) } returns null

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    relatertBehandlingUtleder.utledRelatertBehandling(revurderingKlage)
                }
            assertThat(exception.message).isEqualTo("Forventer en vedtatt klagebehandling for behandling ${revurderingKlage.id}")
            verify { behandlingHentOgPersisterService wasNot called }
        }

        @Test
        fun `skal utlede relatert behandling med klagebehandlingen som siste vedtatte behandling når den innsendte behandling er en revurdering med årsak klage`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurderingKlage =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.KLAGE,
                    aktivertTid = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val sisteVedtatteKlagebehandling =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt,
                )

            every { klageService.hentSisteVedtatteKlagebehandling(revurderingKlage.fagsak.id) } returns sisteVedtatteKlagebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurderingKlage)

            // Assert
            verify { behandlingHentOgPersisterService wasNot called }
            assertThat(relatertBehandling?.id).isEqualTo(sisteVedtatteKlagebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KLAGE)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(sisteVedtatteKlagebehandling.vedtaksdato)
        }

        @Test
        fun `skal utlede relatert behandling med klagebehandlingen som siste vedtatte behandling når den innsendte behandling er en revurdering med årsak iverksetter ka vedtak`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurderingIverksetteKaVedtak =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.IVERKSETTE_KA_VEDTAK,
                    aktivertTid = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val sisteVedtatteKlagebehandling =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt,
                )

            every { klageService.hentSisteVedtatteKlagebehandling(revurderingIverksetteKaVedtak.fagsak.id) } returns sisteVedtatteKlagebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurderingIverksetteKaVedtak)

            // Assert
            verify { behandlingHentOgPersisterService wasNot called }
            assertThat(relatertBehandling?.id).isEqualTo(sisteVedtatteKlagebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KLAGE)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(sisteVedtatteKlagebehandling.vedtaksdato)
        }

        @Test
        fun `skal utlede relatert behandling med barnetrygdbehandlingen som siste vedtatte behandling når den innsendte behandling ikke er en revurdering med årsak klage eller iverksetter ka vedtak`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurdering =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    aktivertTid = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val sisteVedtatteBarnetrygdbehandling =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
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
        fun `skal utlede relatert behandling med barnetrygdbehandlingen som siste vedtatte behandling når den innsendte behandling ikke er en revurdering`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val behandling =
                lagBehandling(
                    behandlingType = BehandlingType.TEKNISK_ENDRING,
                    årsak = BehandlingÅrsak.TEKNISK_ENDRING,
                    aktivertTid = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val sisteVedtatteBarnetrygdbehandling =
                lagBehandling(
                    behandlingType = BehandlingType.TEKNISK_ENDRING,
                    aktivertTid = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns sisteVedtatteBarnetrygdbehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(behandling)

            // Assert
            verify { klageService wasNot called }
            assertThat(relatertBehandling?.id).isEqualTo(sisteVedtatteBarnetrygdbehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.BA)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(sisteVedtatteBarnetrygdbehandling.aktivertTidspunkt)
        }

        @Test
        fun `skal ikke utlede relatert behandling når ingen barnetrygdbehandling er vedtatt og den innsendte behandling ikke er en revurdering med årsak klage eller iverksette ka vedtak`() {
            // Arrange
            val revurdering =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    aktivertTid = LocalDateTime.now(),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(revurdering.fagsak.id) } returns null

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

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
        fun `skal ikke utlede relatert behandling når kun behandling som ikke er revurdering eller teknisk endring for barnetrygd er vedtatt`(
            behandlingType: BehandlingType,
        ) {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurdering =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    aktivertTid = nåtidspunkt,
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val sisteVedtatteBarnetrygdbehandling =
                lagBehandling(
                    behandlingType = behandlingType,
                    aktivertTid = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(revurdering.fagsak.id) } returns sisteVedtatteBarnetrygdbehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            verify { klageService wasNot called }
            assertThat(relatertBehandling).isNull()
        }
    }
}
