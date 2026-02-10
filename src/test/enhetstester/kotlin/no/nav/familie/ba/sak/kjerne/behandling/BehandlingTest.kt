package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BehandlingTest {
    @Test
    fun `validerBehandling kaster feil hvis behandlingType og behandlingÅrsak ikke samsvarer ved teknisk endring`() {
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.TEKNISK_ENDRING,
                årsak = BehandlingÅrsak.SØKNAD,
            )
        assertThrows<RuntimeException> { behandling.validerBehandlingstype() }
    }

    @Test
    fun `validerBehandling kaster feil hvis man prøver å opprette revurdering uten andre vedtatte behandlinger`() {
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
                årsak = BehandlingÅrsak.SØKNAD,
            )
        assertThrows<RuntimeException> { behandling.validerBehandlingstype() }
    }

    @Test
    fun `validerBehandling kaster ikke feil hvis man prøver å opprette revurdering med andre vedtatte behandlinger`() {
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
                årsak = BehandlingÅrsak.SØKNAD,
            )
        assertDoesNotThrow {
            behandling.validerBehandlingstype(
                sisteBehandlingSomErVedtatt =
                    lagBehandling(
                        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                        årsak = BehandlingÅrsak.SØKNAD,
                    ),
            )
        }
    }

    @Test
    fun `Skal svare med overstyrt dokumenttittel på alle behandlinger som er definert som omgjøringsårsaker`() {
        BehandlingÅrsak.entries.forEach {
            if (it.erOmregningsårsak()) {
                assertNotNull(it.hentOverstyrtDokumenttittelForOmregningsbehandling())
            } else {
                assertNull(it.hentOverstyrtDokumenttittelForOmregningsbehandling())
            }
        }
    }

    @Nested
    inner class ErFinnmarkstillegg {
        @Test
        fun `skal returnere true om behandlingsårsak er finnmarkstillegg`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)

            // Act
            val erFinnmarkstillegg = behandling.erFinnmarkstillegg()

            // Assert
            assertThat(erFinnmarkstillegg).isTrue()
        }

        @Test
        fun `skal returnere false om behandlingsårsak ikke er finnmarkstillegg`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG)

            // Act
            val erFinnmarkstillegg = behandling.erFinnmarkstillegg()

            // Assert
            assertThat(erFinnmarkstillegg).isFalse()
        }
    }

    @Nested
    inner class ErSvalbardtillegg {
        @Test
        fun `skal returnere true om behandlingsårsak er svalbardtillegg`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG)

            // Act
            val erSvalbardtillegg = behandling.erSvalbardtillegg()

            // Assert
            assertThat(erSvalbardtillegg).isTrue()
        }

        @Test
        fun `skal returnere false om behandlingsårsak ikke er svalbardtillegg`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)

            // Act
            val erSvalbardtillegg = behandling.erSvalbardtillegg()

            // Assert
            assertThat(erSvalbardtillegg).isFalse()
        }
    }

    @Nested
    inner class ErFinnmarksTilleggEllerSvalbardtillegg {
        @Test
        fun `skal returnere true om behandlingsårsak er finnmarkstillegg`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)

            // Act
            val erFinnmarksEllerSvalbardtillegg = behandling.erFinnmarksEllerSvalbardtillegg()

            // Assert
            assertThat(erFinnmarksEllerSvalbardtillegg).isTrue()
        }

        @Test
        fun `skal returnere true om behandlingsårsak er svalbardtillegg`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG)

            // Act
            val erFinnmarksEllerSvalbardtillegg = behandling.erFinnmarksEllerSvalbardtillegg()

            // Assert
            assertThat(erFinnmarksEllerSvalbardtillegg).isTrue()
        }

        @Test
        fun `skal returnere false om behandlingsårsak ikke er finnmarkstillegg eller svalbardtillegg`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)

            // Act
            val erFinnmarksEllerSvalbardtillegg = behandling.erFinnmarksEllerSvalbardtillegg()

            // Assert
            assertThat(erFinnmarksEllerSvalbardtillegg).isFalse()
        }
    }

    @Nested
    inner class ErBehandlingMedVedtaksbrevutsending {
        @Test
        fun `kan sende vedtaksbrev for ordinær førstegangsbehandling`() {
            // ARrange
            val behandling = lagBehandling()

            // Act
            val erBehandlingMedVedtaksbrevutsending = behandling.erBehandlingMedVedtaksbrevutsending()

            // Assert
            assertThat(erBehandlingMedVedtaksbrevutsending).isTrue()
        }

        @Test
        fun `kan sende vedtaksbrev for ordinær revurdering`() {
            // Arrange
            val behandling = lagBehandling(behandlingType = BehandlingType.REVURDERING, årsak = BehandlingÅrsak.NYE_OPPLYSNINGER)

            // Act
            val erBehandlingMedVedtaksbrevutsending = behandling.erBehandlingMedVedtaksbrevutsending()

            // Assert
            assertThat(erBehandlingMedVedtaksbrevutsending).isTrue()
        }

        @ParameterizedTest
        @EnumSource(value = BehandlingÅrsak::class, names = ["ENDRE_MIGRERINGSDATO", "HELMANUELL_MIGRERING", "MIGRERING"])
        fun `kan ikke sende vedtaksbrev for behandlingstype MIGRERING_FRA_INFOTRYGD`(
            årsak: BehandlingÅrsak,
        ) {
            // Arrange
            val behandling = lagBehandling(behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD, årsak = årsak)

            // Act
            val erBehandlingMedVedtaksbrevutsending = behandling.erBehandlingMedVedtaksbrevutsending()

            // Assert
            assertThat(erBehandlingMedVedtaksbrevutsending).isFalse()
        }

        @Test
        fun `kan ikke sende vedtaksbrev for teknisk endring`() {
            // Arrange
            val behandling = lagBehandling(behandlingType = BehandlingType.TEKNISK_ENDRING)

            // Act
            val erBehandlingMedVedtaksbrevutsending = behandling.erBehandlingMedVedtaksbrevutsending()

            // Assert
            assertThat(erBehandlingMedVedtaksbrevutsending).isFalse()
        }

        @Test
        fun `kan ikke sende vedtaksbrev for når årsak er FALKS_IDENTITET`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.FALSK_IDENTITET)

            // Act
            val erBehandlingMedVedtaksbrevutsending = behandling.erBehandlingMedVedtaksbrevutsending()

            // Assert
            assertThat(erBehandlingMedVedtaksbrevutsending).isFalse()
        }

        @ParameterizedTest
        @EnumSource(value = BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING"])
        fun `kan ikke sende vedtaksbrev for revurdering med årsak satsendring eller månedlig valutajustering`(
            årsak: BehandlingÅrsak,
        ) {
            // Arrange
            val behandling = lagBehandling(behandlingType = BehandlingType.REVURDERING, årsak = årsak)

            // Act
            val erBehandlingMedVedtaksbrevutsending = behandling.erBehandlingMedVedtaksbrevutsending()

            // Assert
            assertThat(erBehandlingMedVedtaksbrevutsending).isFalse()
        }

        @ParameterizedTest
        @EnumSource(value = Behandlingsresultat::class, names = ["FORTSATT_INNVILGET", "FORTSATT_OPPHØRT"])
        fun `skal returnere false om behandlingsårsak er svalbardtillegg og behandlingsresultatet er FORTSATT_INNVILGET eller FORTSATT_OPPHØRT`(
            behandlingsresultat: Behandlingsresultat,
        ) {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG, resultat = behandlingsresultat)

            // Act
            val erBehandlingMedVedtaksbrevutsending = behandling.erBehandlingMedVedtaksbrevutsending()

            // Assert
            assertThat(erBehandlingMedVedtaksbrevutsending).isFalse()
        }

        @ParameterizedTest
        @EnumSource(value = Behandlingsresultat::class, names = ["FORTSATT_INNVILGET", "FORTSATT_OPPHØRT"])
        fun `skal returnere false om behandlingsårsak er finnmarkstillegg og behandlingsresultatet er FORTSATT_INNVILGET eller FORTSATT_OPPHØRT`(
            behandlingsresultat: Behandlingsresultat,
        ) {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG, resultat = behandlingsresultat)

            // Act
            val erBehandlingMedVedtaksbrevutsending = behandling.erBehandlingMedVedtaksbrevutsending()

            // Assert
            assertThat(erBehandlingMedVedtaksbrevutsending).isFalse()
        }
    }

    @Nested
    inner class SkalRettFraBehandlingsresultatTilIverksetting {
        @ParameterizedTest
        @EnumSource(value = BehandlingÅrsak::class, names = ["FINNMARKSTILLEGG", "SVALBARDTILLEGG"])
        fun `skal returnere true om behandlingen skal behandles automatisk og behandlingsårsak er finnmarkstillegg eller svalbardstillegg`(
            behandlingÅrsak: BehandlingÅrsak,
        ) {
            // Arrange
            val behandling = lagBehandling(skalBehandlesAutomatisk = true, årsak = behandlingÅrsak)

            // Act
            val skalRettFraBehandlingsresultatTilIverksetting = behandling.skalRettFraBehandlingsresultatTilIverksetting(false)

            // Assert
            assertThat(skalRettFraBehandlingsresultatTilIverksetting).isTrue()
        }
    }

    @Nested
    inner class ErAutomatiskOgSkalHaTidligereBehandling {
        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal returnere true for gitte behandlingsårsaker`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val behandling = lagBehandling(årsak = behandlingÅrsak)

            // Act
            val erAutomatiskOgSkalHaTidligereBehandling = behandling.erAutomatiskOgSkalHaTidligereBehandling()

            // Assert
            assertThat(erAutomatiskOgSkalHaTidligereBehandling).isTrue()
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal returnere false for gitte behandlingsårsaker`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val behandling = lagBehandling(årsak = behandlingÅrsak)

            // Act
            val erAutomatiskOgSkalHaTidligereBehandling = behandling.erAutomatiskOgSkalHaTidligereBehandling()

            // Assert
            assertThat(erAutomatiskOgSkalHaTidligereBehandling).isFalse()
        }
    }
}
