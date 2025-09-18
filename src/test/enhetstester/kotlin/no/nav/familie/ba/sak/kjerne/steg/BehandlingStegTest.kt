package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class BehandlingStegTest {
    @Nested
    inner class HentNesteSteg {
        @Test
        fun `skal returnere FERDIGSTILLE_BEHANDLING for HENLEGG_BEHANDLING`() {
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)

            // Act
            val nesteSteg =
                hentNesteSteg(
                    behandling = behandling,
                    utførendeStegType = StegType.HENLEGG_BEHANDLING,
                )

            // Assert
            assertThat(nesteSteg).isEqualTo(StegType.FERDIGSTILLE_BEHANDLING)
        }

        @Nested
        inner class Omregning18År {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg uten endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.OMREGNING_18ÅR)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med endringer i utbetaling, foruten om steg type behandlingsresultat`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.OMREGNING_18ÅR)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal kaste exception for neste steg for behandlingsresultat dersom det er endringer i utbetaling for omregningsårsak`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.OMREGNING_18ÅR)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                        )
                    }
                assertThat(exception.message).isEqualTo("Det finnes endringer i utbetaling for behandling med omregningsårsak.")
            }

            @Test
            fun `skal kaste exception for neste steg for behandlingsresultat dersom endringer i utbetaling ikke er relevant`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.OMREGNING_18ÅR)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("Endringer i utbetaling må utledes før man kan gå videre til neste steg.")
            }

            @Test
            fun `skal kaste exception om stegtype ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.OMREGNING_18ÅR)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.REGISTRERE_INSTITUSJON,
                        )
                    }
                assertThat(exception.message).isEqualTo("Stegtype Registrere institusjon er ikke implementert for behandling med årsak OMREGNING_18ÅR og type FØRSTEGANGSBEHANDLING.")
            }
        }

        @Nested
        inner class OmregningSmåbarnstillegg {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg uten endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med endringer i utbetaling, foruten om steg type behandlingsresultat`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal kaste exception for neste steg for behandlingsresultat dersom det er endringer i utbetaling for omregningsårsak`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                        )
                    }
                assertThat(exception.message).isEqualTo("Det finnes endringer i utbetaling for behandling med omregningsårsak.")
            }

            @Test
            fun `skal kaste exception for neste steg for behandlingsresultat dersom endringer i utbetaling ikke er relevant`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("Endringer i utbetaling må utledes før man kan gå videre til neste steg.")
            }

            @Test
            fun `skal kaste exception om stegtype ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.REGISTRERE_INSTITUSJON,
                        )
                    }
                assertThat(exception.message).isEqualTo("Stegtype Registrere institusjon er ikke implementert for behandling med årsak OMREGNING_SMÅBARNSTILLEGG og type FØRSTEGANGSBEHANDLING.")
            }
        }

        @Nested
        inner class Migrering {
            @Test
            fun `skal kaste feil om årsak er migrering`() {
                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling =
                                lagBehandling(
                                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                                    årsak = BehandlingÅrsak.MIGRERING,
                                ),
                            utførendeStegType = FØRSTE_STEG,
                        )
                    }

                // Assert
                assertThat(exception.message).isEqualTo("Maskinell migrering er ikke mulig å behandle lenger")
            }
        }

        @Nested
        inner class HelmanuellMigrering {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, IVERKSETT_MOT_OPPDRAG",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.HELMANUELL_MIGRERING)

                // Act
                val nesteSteg = hentNesteSteg(behandling = behandling, utførendeStegType = nåværendeSteg)

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal kaste feil om steg type ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.HELMANUELL_MIGRERING)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(behandling = behandling, utførendeStegType = StegType.REGISTRERE_INSTITUSJON)
                    }
                assertThat(exception.message).isEqualTo("StegType Registrere institusjon er ugyldig ved manuell migreringsbehandling")
            }
        }

        @Nested
        inner class EndreMigreringsdato {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal returnere neste steg`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO)

                // Act
                val nesteSteg = hentNesteSteg(behandling = behandling, utførendeStegType = nåværendeSteg)

                // Asert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal kaste feil om steg ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(behandling = behandling, utførendeStegType = StegType.REGISTRERE_INSTITUSJON)
                    }
                assertThat(exception.message).isEqualTo("StegType ${StegType.REGISTRERE_INSTITUSJON.displayName()} er ugyldig ved migreringsbehandling med endre migreringsdato")
            }
        }

        @Nested
        inner class IverksetteKaVedtak {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, FERDIGSTILLE_BEHANDLING",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal returnere neste steg med ingen endring i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.IVERKSETTE_KA_VEDTAK)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Asert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, IVERKSETT_MOT_OPPDRAG",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal returnere neste steg med endring i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.IVERKSETTE_KA_VEDTAK)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Asert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal returnere neste steg når endring i utbetaling ikke er relevant for alle stegtyper bortsett fra BESLUTTE_VEDTAK`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.IVERKSETTE_KA_VEDTAK)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                    )

                // Asert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal kaste exception om endring i utbetaling ikke er relevant for stegtype BESLUTTE_VEDTAK`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.IVERKSETTE_KA_VEDTAK)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BESLUTTE_VEDTAK,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("Endringer i utbetaling må utledes før man kan gå videre til neste steg.")
            }

            @Test
            fun `skal kaste exception om stegtype ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.IVERKSETTE_KA_VEDTAK)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.REGISTRERE_INSTITUSJON,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("StegType ${StegType.REGISTRERE_INSTITUSJON.displayName()} ugyldig ved teknisk endring")
            }
        }

        @Nested
        inner class TekniskEndring {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, FERDIGSTILLE_BEHANDLING",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal returnere neste steg med ingen endring i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.TEKNISK_ENDRING)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Asert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, IVERKSETT_MOT_OPPDRAG",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal returnere neste steg med endring i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.TEKNISK_ENDRING)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Asert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal returnere neste steg når endring i utbetaling ikke er relevant for alle stegtyper bortsett fra BESLUTTE_VEDTAK`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.TEKNISK_ENDRING)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                    )

                // Asert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal kaste exception om endring i utbetaling ikke er relevant for stegtype BESLUTTE_VEDTAK`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.TEKNISK_ENDRING)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BESLUTTE_VEDTAK,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("Endringer i utbetaling må utledes før man kan gå videre til neste steg.")
            }

            @Test
            fun `skal kaste exception om stegtype ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.TEKNISK_ENDRING)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.REGISTRERE_INSTITUSJON,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("StegType ${StegType.REGISTRERE_INSTITUSJON.displayName()} ugyldig ved teknisk endring")
            }
        }

        @Nested
        inner class Fødselshendelser {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, FILTRERING_FØDSELSHENDELSER",
                "FILTRERING_FØDSELSHENDELSER, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, IVERKSETT_MOT_OPPDRAG",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal returnere neste steg med endring i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)

                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, FILTRERING_FØDSELSHENDELSER",
                "FILTRERING_FØDSELSHENDELSER, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, HENLEGG_BEHANDLING",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal returnere neste steg med ingen endring i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)

                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, FILTRERING_FØDSELSHENDELSER",
                "FILTRERING_FØDSELSHENDELSER, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, HENLEGG_BEHANDLING",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal returnere neste steg hvor endring i utbetaling ikke er relevant`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)

                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal kaste feil om stegtype ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.REGISTRERE_INSTITUSJON,
                        )
                    }
                assertThat(exception.message).isEqualTo("Stegtype ${StegType.REGISTRERE_INSTITUSJON.displayName()} er ikke implementert for fødselshendelser")
            }
        }

        @Nested
        inner class Søknad {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, REGISTRERE_SØKNAD",
                "REGISTRERE_INSTITUSJON, REGISTRERE_SØKNAD",
                "REGISTRERE_SØKNAD, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, IVERKSETT_MOT_OPPDRAG",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, IVERKSETT_MOT_FAMILIE_TILBAKE",
                "IVERKSETT_MOT_FAMILIE_TILBAKE, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, REGISTRERE_SØKNAD",
                "REGISTRERE_INSTITUSJON, REGISTRERE_SØKNAD",
                "REGISTRERE_SØKNAD, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, JOURNALFØR_VEDTAKSBREV",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, IVERKSETT_MOT_FAMILIE_TILBAKE",
                "IVERKSETT_MOT_FAMILIE_TILBAKE, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg uten endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal kaste feil om stegtype ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.FILTRERING_FØDSELSHENDELSER,
                        )
                    }
                assertThat(exception.message).isEqualTo("Stegtype ${StegType.FILTRERING_FØDSELSHENDELSER.displayName()} er ikke implementert for behandling med årsak SØKNAD og type FØRSTEGANGSBEHANDLING.")
            }

            @Test
            fun `Skal kaste feil dersom det er en søknad og det forsøkes å gå videre fra beslutt vedtak uten at det har vært sjekk om det finnes endringer i utbetaling`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling,
                            utførendeStegType = StegType.BESLUTTE_VEDTAK,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("Endringer i utbetaling må utledes før man kan gå videre til neste steg.")
            }

            @Test
            fun `skal hente neste steg for REGISTRERE_PERSONGRUNNLAG når fagsak er av typen institusjon`() {
                // Arrange
                val fagsak = lagFagsak(type = FagsakType.INSTITUSJON)
                val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SØKNAD)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = StegType.REGISTRERE_PERSONGRUNNLAG,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(StegType.REGISTRERE_INSTITUSJON)
            }
        }

        @Nested
        inner class SmåbarnstilleggEndringFramITid {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, IVERKSETT_MOT_OPPDRAG",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, JOURNALFØR_VEDTAKSBREV",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med ingen endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg når endringer i utbetaling ikke er relevant for alle stegtyper utenom BESLUTTE_VEDTAK`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal kaste exception om endring i utbetaling ikke er relevant når stegtype er BESLUTTE_VEDTAK`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BESLUTTE_VEDTAK,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("Endringer i utbetaling må utledes før man kan gå videre til neste steg.")
            }

            @Test
            fun `skal hente IVERKSETT_MOT_OPPDRAG som neste steg for steget BEHANDLINGSRESULTAT`() {
                // Arrange
                val behandling =
                    lagBehandling(
                        årsak = BehandlingÅrsak.SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID,
                        skalBehandlesAutomatisk = true,
                        resultat = Behandlingsresultat.FORTSATT_INNVILGET,
                        status = BehandlingStatus.IVERKSETTER_VEDTAK,
                    )

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(StegType.IVERKSETT_MOT_OPPDRAG)
            }

            @Test
            fun `skal hente VURDER_TILBAKEKREVING som neste steg for steget BEHANDLINGSRESULTAT hvis behandling ikke skal behandles automatisk`() {
                // Arrange
                val behandling =
                    lagBehandling(
                        årsak = BehandlingÅrsak.SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID,
                        skalBehandlesAutomatisk = false,
                        resultat = Behandlingsresultat.FORTSATT_INNVILGET,
                        status = BehandlingStatus.IVERKSETTER_VEDTAK,
                    )

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(StegType.VURDER_TILBAKEKREVING)
            }

            @Test
            fun `skal hente VURDER_TILBAKEKREVING som neste steg for steget BEHANDLINGSRESULTAT hvis resultatet på behandlingen ikke er FORTSATT_INNVILGET`() {
                // Arrange
                val behandling =
                    lagBehandling(
                        årsak = BehandlingÅrsak.SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID,
                        skalBehandlesAutomatisk = true,
                        resultat = Behandlingsresultat.IKKE_VURDERT,
                        status = BehandlingStatus.IVERKSETTER_VEDTAK,
                    )

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(StegType.VURDER_TILBAKEKREVING)
            }

            @Test
            fun `skal hente VURDER_TILBAKEKREVING som neste steg for steget BEHANDLINGSRESULTAT hvis statusen på behandlingen ikke er IVERKSETTER_VEDTAK`() {
                // Arrange
                val behandling =
                    lagBehandling(
                        årsak = BehandlingÅrsak.SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID,
                        skalBehandlesAutomatisk = true,
                        resultat = Behandlingsresultat.FORTSATT_INNVILGET,
                        status = BehandlingStatus.UTREDES,
                    )

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(StegType.VURDER_TILBAKEKREVING)
            }

            @Test
            fun `skal kaste exception om stegtype ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.REGISTRERE_INSTITUSJON,
                        )
                    }
                assertThat(exception.message).isEqualTo("Stegtype ${StegType.REGISTRERE_INSTITUSJON.displayName()} er ikke implementert for småbarnstillegg")
            }
        }

        @Nested
        inner class Småbarnstillegg {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT,  VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, IVERKSETT_MOT_OPPDRAG",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, IVERKSETT_MOT_FAMILIE_TILBAKE",
                "IVERKSETT_MOT_FAMILIE_TILBAKE, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SMÅBARNSTILLEGG)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT,  VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, JOURNALFØR_VEDTAKSBREV",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, IVERKSETT_MOT_FAMILIE_TILBAKE",
                "IVERKSETT_MOT_FAMILIE_TILBAKE, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med ingen endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SMÅBARNSTILLEGG)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT,  VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, IVERKSETT_MOT_FAMILIE_TILBAKE",
                "IVERKSETT_MOT_FAMILIE_TILBAKE, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med hvor endringer i utbetaling ikke er relevant, men ikke for stegtype BESLUTTE_VEDTAK`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SMÅBARNSTILLEGG)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal kaste exception om man henter neste steg for BESLUTTER_VEDTAK og endring i utbetaling ikke er relevant`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SMÅBARNSTILLEGG)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BESLUTTE_VEDTAK,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("Endringer i utbetaling må utledes før man kan gå videre til neste steg.")
            }

            @Test
            fun `skal hente neste steg IVERKSETT_MOT_OPPDRAG for steg BEHANDLINGSRESULTAT gitt riktig tilstand for behandling`() {
                // Arrange
                val behandling =
                    lagBehandling(
                        årsak = BehandlingÅrsak.SMÅBARNSTILLEGG,
                        skalBehandlesAutomatisk = true,
                        status = BehandlingStatus.IVERKSETTER_VEDTAK,
                    )

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(StegType.IVERKSETT_MOT_OPPDRAG)
            }

            @Test
            fun `skal hente neste steg VURDER_TILBAKEKREVING for steg BEHANDLINGSRESULTAT om behandlingen ikke skal automatisk behandles`() {
                // Arrange
                val behandling =
                    lagBehandling(
                        årsak = BehandlingÅrsak.SMÅBARNSTILLEGG,
                        skalBehandlesAutomatisk = false,
                        status = BehandlingStatus.IVERKSETTER_VEDTAK,
                    )

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(StegType.VURDER_TILBAKEKREVING)
            }

            @Test
            fun `skal hente neste steg VURDER_TILBAKEKREVING for steg BEHANDLINGSRESULTAT om statusen på behandlingen ikke er IVERKSETTER_VEDTAK`() {
                // Arrange
                val behandling =
                    lagBehandling(
                        årsak = BehandlingÅrsak.SMÅBARNSTILLEGG,
                        skalBehandlesAutomatisk = true,
                        status = BehandlingStatus.UTREDES,
                    )

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(StegType.VURDER_TILBAKEKREVING)
            }

            @Test
            fun `skal kaste exception om stegtype ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SMÅBARNSTILLEGG)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.REGISTRERE_INSTITUSJON,
                        )
                    }
                assertThat(exception.message).isEqualTo("Stegtype ${StegType.REGISTRERE_INSTITUSJON.displayName()} er ikke implementert for småbarnstillegg")
            }
        }

        @Nested
        inner class Finnmarkstillegg {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, IVERKSETT_MOT_OPPDRAG",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, FERDIGSTILLE_BEHANDLING",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med ingen endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, FERDIGSTILLE_BEHANDLING",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg hvor endringer i utbetaling ikke er relevant`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal kaste exception om stegtype ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.REGISTRERE_INSTITUSJON,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo(
                    "Stegtype ${StegType.REGISTRERE_INSTITUSJON.displayName()} er ikke implementert for behandling med årsak ${behandling.opprettetÅrsak} og type ${behandling.type}.",
                )
            }
        }

        @Nested
        inner class Svalbardtillegg {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, IVERKSETT_MOT_OPPDRAG",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, FERDIGSTILLE_BEHANDLING",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med ingen endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, FERDIGSTILLE_BEHANDLING",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg hvor endringer i utbetaling ikke er relevant`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal kaste exception om stegtype ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.REGISTRERE_INSTITUSJON,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo(
                    "Stegtype ${StegType.REGISTRERE_INSTITUSJON.displayName()} er ikke implementert for behandling med årsak ${behandling.opprettetÅrsak} og type ${behandling.type}.",
                )
            }
        }

        @Nested
        inner class Satsendring {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, IVERKSETT_MOT_OPPDRAG",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SATSENDRING)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, FERDIGSTILLE_BEHANDLING",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med ingen endringer i utbetaling og hvor kategorien på behandlingen er EØS`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SATSENDRING, behandlingKategori = BehandlingKategori.EØS)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal skal kaste exception når man henter neste steg for BEHANDLINGSRESULTAT når endring i utbetaling ikke er relevant`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SATSENDRING)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("Behandlingen har ingen endringer i utbetaling.")
            }

            @Test
            fun `skal skal kaste exception når man henter neste steg for BEHANDLINGSRESULTAT når ingen endring i utbetaling men behandlingskategorien ikke er EØS`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SATSENDRING, behandlingKategori = BehandlingKategori.NASJONAL)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                        )
                    }
                assertThat(exception.message).isEqualTo("Behandlingen har ingen endringer i utbetaling.")
            }

            @Test
            fun `skal skal kaste exception når man henter neste steg for BEHANDLINGSRESULTAT når endring i utbetaling ikke er relevant og behandlingskategorien er EØS`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SATSENDRING, behandlingKategori = BehandlingKategori.EØS)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("Behandlingen har ingen endringer i utbetaling.")
            }

            @Test
            fun `skal kaste exception om stegtype ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SATSENDRING)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.REGISTRERE_INSTITUSJON,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                        )
                    }
                assertThat(exception.message).isEqualTo(
                    "Stegtype ${StegType.REGISTRERE_INSTITUSJON.displayName()} er ikke implementert for behandling med årsak ${behandling.opprettetÅrsak} og type ${behandling.type}.",
                )
            }
        }

        @Nested
        inner class MånedligValutajustering {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, IVERKSETT_MOT_OPPDRAG",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med endringer i utbetaling`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.MÅNEDLIG_VALUTAJUSTERING)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, FERDIGSTILLE_BEHANDLING",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med ingen endringer i utbetaling og hvor kategorien på behandlingen er EØS`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.MÅNEDLIG_VALUTAJUSTERING, behandlingKategori = BehandlingKategori.EØS)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal skal kaste exception når man henter neste steg for BEHANDLINGSRESULTAT når endring i utbetaling ikke er relevant`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.MÅNEDLIG_VALUTAJUSTERING)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("Behandlingen har ingen endringer i utbetaling.")
            }

            @Test
            fun `skal skal kaste exception når man henter neste steg for BEHANDLINGSRESULTAT når ingen endring i utbetaling men behandlingskategorien ikke er EØS`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.MÅNEDLIG_VALUTAJUSTERING, behandlingKategori = BehandlingKategori.NASJONAL)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                        )
                    }
                assertThat(exception.message).isEqualTo("Behandlingen har ingen endringer i utbetaling.")
            }

            @Test
            fun `skal skal kaste exception når man henter neste steg for BEHANDLINGSRESULTAT når endring i utbetaling ikke er relevant og behandlingskategorien er EØS`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.MÅNEDLIG_VALUTAJUSTERING, behandlingKategori = BehandlingKategori.EØS)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BEHANDLINGSRESULTAT,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("Behandlingen har ingen endringer i utbetaling.")
            }

            @Test
            fun `skal kaste exception om stegtype ikke er støttet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.MÅNEDLIG_VALUTAJUSTERING)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.REGISTRERE_INSTITUSJON,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                        )
                    }
                assertThat(exception.message).isEqualTo(
                    "Stegtype ${StegType.REGISTRERE_INSTITUSJON.displayName()} er ikke implementert for behandling med årsak ${behandling.opprettetÅrsak} og type ${behandling.type}.",
                )
            }
        }

        @Nested
        inner class ÅrligKontroll {
            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "REGISTRERE_INSTITUSJON, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, IVERKSETT_MOT_OPPDRAG",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, IVERKSETT_MOT_FAMILIE_TILBAKE",
                "IVERKSETT_MOT_FAMILIE_TILBAKE, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med endringer i utbetaling, normal fagsaktype`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, VILKÅRSVURDERING",
                "REGISTRERE_INSTITUSJON, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, JOURNALFØR_VEDTAKSBREV",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, IVERKSETT_MOT_FAMILIE_TILBAKE",
                "IVERKSETT_MOT_FAMILIE_TILBAKE, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med ingen endringer i utbetaling, normal fagsaktype`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal skal kaste exception når man henter neste steg for BESLUTTE_VEDTAK når endring i utbetaling ikke er relevant, normal fagsaktype`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BESLUTTE_VEDTAK,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("Endringer i utbetaling må utledes før man kan gå videre til neste steg.")
            }

            @Test
            fun `skal skal kaste exception hvis stegtype ikke er støttet, normal fagsaktype`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.REGISTRERE_SØKNAD,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo(
                    "Stegtype ${StegType.REGISTRERE_SØKNAD.displayName()} er ikke implementert for behandling med årsak ${behandling.opprettetÅrsak} og type ${behandling.type}.",
                )
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, REGISTRERE_INSTITUSJON",
                "REGISTRERE_INSTITUSJON, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, IVERKSETT_MOT_OPPDRAG",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, IVERKSETT_MOT_FAMILIE_TILBAKE",
                "IVERKSETT_MOT_FAMILIE_TILBAKE, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med endringer i utbetaling, institusjon fagsaktype`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val fagsak = lagFagsak(type = FagsakType.INSTITUSJON)
                val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @ParameterizedTest(name = "Henter neste steg for {0}")
            @CsvSource(
                "REGISTRERE_PERSONGRUNNLAG, REGISTRERE_INSTITUSJON",
                "REGISTRERE_INSTITUSJON, VILKÅRSVURDERING",
                "VILKÅRSVURDERING, BEHANDLINGSRESULTAT",
                "BEHANDLINGSRESULTAT, VURDER_TILBAKEKREVING",
                "VURDER_TILBAKEKREVING, SEND_TIL_BESLUTTER",
                "SEND_TIL_BESLUTTER, BESLUTTE_VEDTAK",
                "BESLUTTE_VEDTAK, JOURNALFØR_VEDTAKSBREV",
                "IVERKSETT_MOT_OPPDRAG, VENTE_PÅ_STATUS_FRA_ØKONOMI",
                "VENTE_PÅ_STATUS_FRA_ØKONOMI, IVERKSETT_MOT_FAMILIE_TILBAKE",
                "IVERKSETT_MOT_FAMILIE_TILBAKE, JOURNALFØR_VEDTAKSBREV",
                "JOURNALFØR_VEDTAKSBREV, DISTRIBUER_VEDTAKSBREV",
                "DISTRIBUER_VEDTAKSBREV, FERDIGSTILLE_BEHANDLING",
                "FERDIGSTILLE_BEHANDLING, BEHANDLING_AVSLUTTET",
                "BEHANDLING_AVSLUTTET, BEHANDLING_AVSLUTTET",
            )
            fun `skal hente neste steg med ingen endringer i utbetaling, institusjon fagsaktype`(
                nåværendeSteg: StegType,
                forventetResultat: StegType,
            ) {
                // Arrange
                val fagsak = lagFagsak(type = FagsakType.INSTITUSJON)
                val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

                // Act
                val nesteSteg =
                    hentNesteSteg(
                        behandling = behandling,
                        utførendeStegType = nåværendeSteg,
                        endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING,
                    )

                // Assert
                assertThat(nesteSteg).isEqualTo(forventetResultat)
            }

            @Test
            fun `skal skal kaste exception når man henter neste steg for BESLUTTE_VEDTAK når endring i utbetaling ikke er relevant, institusjon fagsaktype`() {
                // Arrange
                val fagsak = lagFagsak(type = FagsakType.INSTITUSJON)
                val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.BESLUTTE_VEDTAK,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo("Endringer i utbetaling må utledes før man kan gå videre til neste steg.")
            }

            @Test
            fun `skal skal kaste exception hvis stegtype ikke er støttet, institusjon fagsaktype`() {
                // Arrange
                val fagsak = lagFagsak(type = FagsakType.INSTITUSJON)
                val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        hentNesteSteg(
                            behandling = behandling,
                            utførendeStegType = StegType.REGISTRERE_SØKNAD,
                            endringerIUtbetaling = EndringerIUtbetalingForBehandlingSteg.IKKE_RELEVANT,
                        )
                    }
                assertThat(exception.message).isEqualTo(
                    "Stegtype ${StegType.REGISTRERE_SØKNAD.displayName()} er ikke implementert for behandling med årsak ${behandling.opprettetÅrsak} og type ${behandling.type}.",
                )
            }
        }
    }

    @Nested
    inner class DisplayName {
        @ParameterizedTest(name = "Henter display name for steg {0}")
        @CsvSource(
            "HENLEGG_BEHANDLING, Henlegg behandling",
            "REGISTRERE_INSTITUSJON, Registrere institusjon",
            "REGISTRERE_PERSONGRUNNLAG, Registrere persongrunnlag",
            "REGISTRERE_SØKNAD, Registrere søknad",
            "FILTRERING_FØDSELSHENDELSER, Filtrering fødselshendelser",
            "VILKÅRSVURDERING, Vilkårsvurdering",
            "BEHANDLINGSRESULTAT, Behandlingsresultat",
            "VURDER_TILBAKEKREVING, Vurder tilbakekreving",
            "SEND_TIL_BESLUTTER, Send til beslutter",
            "BESLUTTE_VEDTAK, Beslutte vedtak",
            "IVERKSETT_MOT_OPPDRAG, Iverksett mot oppdrag",
            "VENTE_PÅ_STATUS_FRA_ØKONOMI, Vente på status fra økonomi",
            "IVERKSETT_MOT_FAMILIE_TILBAKE, Iverksett mot familie tilbake",
            "JOURNALFØR_VEDTAKSBREV, Journalfør vedtaksbrev",
            "DISTRIBUER_VEDTAKSBREV, Distribuer vedtaksbrev",
            "FERDIGSTILLE_BEHANDLING, Ferdigstille behandling",
            "BEHANDLING_AVSLUTTET, Behandling avsluttet",
        )
        fun `skal ha riktig display name`(
            stegType: StegType,
            navn: String,
        ) {
            // Act
            val displayName = stegType.displayName()

            // Assert
            assertThat(displayName).isEqualTo(navn)
        }
    }

    @Nested
    inner class ErGyldigIKombinasjonMedStatus {
        @Test
        fun `skal returnere true for StegType REGISTRERE_SØKNAD og BehandlingStatus UTREDES`() {
            // Act
            val erGyldig = StegType.REGISTRERE_SØKNAD.erGyldigIKombinasjonMedStatus(BehandlingStatus.UTREDES)

            // Assert
            assertThat(erGyldig).isTrue()
        }

        @Test
        fun `skal returnere false for StegType REGISTRERE_SØKNAD og BehandlingStatus IVERKSETTER_VEDTAK`() {
            // Act
            val erGyldig = StegType.REGISTRERE_SØKNAD.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK)

            // Assert
            assertThat(erGyldig).isFalse()
        }

        @Test
        fun `skal returnere false for StegType BEHANDLING_AVSLUTTET og BehandlingStatus IVERKSETTER_VEDTAK`() {
            // Act
            val erGyldig = StegType.BEHANDLING_AVSLUTTET.erGyldigIKombinasjonMedStatus(BehandlingStatus.FATTER_VEDTAK)

            // Assert
            assertThat(erGyldig).isFalse()
        }
    }
}
