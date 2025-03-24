package no.nav.familie.ba.sak.kjerne.klage

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagKlagebehandlingDto
import no.nav.familie.ba.sak.datagenerator.lagKlageinstansResultatDto
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime

class KlagebehandlingHenterTest {
    private val klageClient = mockk<KlageClient>()
    private val klagebehandlingHenter =
        KlagebehandlingHenter(
            klageClient = klageClient,
        )

    @Nested
    inner class HentKlagebehandlingerPåFagsak {
        @Test
        fun `skal kaste exception om fagsaken ikke finnes i resultatet fra kallet til klageklienten`() {
            // Arrange
            val fagsakId = 4L

            every { klageClient.hentKlagebehandlinger(setOf(fagsakId)) } returns
                mapOf(
                    1L to listOf(lagKlagebehandlingDto(), lagKlagebehandlingDto()),
                    2L to listOf(lagKlagebehandlingDto()),
                    3L to listOf(lagKlagebehandlingDto(), lagKlagebehandlingDto()),
                )

            // Act & Assert
            val exception =
                assertThrows<Feil> {
                    klagebehandlingHenter.hentKlagebehandlingerPåFagsak(fagsakId)
                }
            assertThat(exception.message).isEqualTo("Fikk ikke fagsakId=$fagsakId tilbake fra kallet til klage.")
        }

        @Test
        fun `skal hente klagebehandlinger på fagsak`() {
            // Arrange
            val fagsakId = 1L

            val klagebehandlingerForFagsak1 = listOf(lagKlagebehandlingDto(), lagKlagebehandlingDto())
            val klagebehandlingerForFagsak2 = listOf(lagKlagebehandlingDto())
            val klagebehandlingerForFagsak3 = listOf(lagKlagebehandlingDto(), lagKlagebehandlingDto())

            every { klageClient.hentKlagebehandlinger(setOf(fagsakId)) } returns
                mapOf(
                    fagsakId to klagebehandlingerForFagsak1,
                    2L to klagebehandlingerForFagsak2,
                    3L to klagebehandlingerForFagsak3,
                )

            // Act
            val resultat = klagebehandlingHenter.hentKlagebehandlingerPåFagsak(fagsakId)

            // Assert
            assertThat(resultat).isEqualTo(klagebehandlingerForFagsak1)
        }
    }

    @Nested
    inner class HentSisteVedtatteKlagebehandling {
        @ParameterizedTest
        @EnumSource(
            value = BehandlingStatus::class,
            names = ["FERDIGSTILT"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal filtrer bort klagebehandlinger som ikke er ferdigstilt`(
            behandlingStatus: BehandlingStatus,
        ) {
            // Arrange
            val fagsakId = 1L

            val klagebehandlingDto =
                lagKlagebehandlingDto(
                    vedtaksdato = LocalDateTime.now(),
                    status = behandlingStatus,
                )

            every { klageClient.hentKlagebehandlinger(setOf(fagsakId)) } returns
                mapOf(
                    fagsakId to listOf(klagebehandlingDto),
                )

            // Act
            val sisteVedtatteKlagebehandling = klagebehandlingHenter.hentSisteVedtatteKlagebehandling(fagsakId)

            // Assert
            assertThat(sisteVedtatteKlagebehandling).isNull()
        }

        @ParameterizedTest
        @EnumSource(value = HenlagtÅrsak::class)
        fun `skal filtrer bort klagebehandlinger med henlagt årsak`(
            henlagtÅrsak: HenlagtÅrsak,
        ) {
            // Arrange
            val fagsakId = 1L

            val klagebehandlingDto =
                lagKlagebehandlingDto(
                    vedtaksdato = LocalDateTime.now(),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = henlagtÅrsak,
                )

            every { klageClient.hentKlagebehandlinger(setOf(fagsakId)) } returns
                mapOf(
                    fagsakId to listOf(klagebehandlingDto),
                )

            // Act
            val sisteVedtatteKlagebehandling = klagebehandlingHenter.hentSisteVedtatteKlagebehandling(fagsakId)

            // Assert
            assertThat(sisteVedtatteKlagebehandling).isNull()
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingResultat::class,
            names = ["MEDHOLD", "IKKE_MEDHOLD", "IKKE_MEDHOLD_FORMKRAV_AVVIST"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal filtrer bort klagebehandlinger med behandlingsresultat som ikke er korrekt`(
            behandlingResultat: BehandlingResultat,
        ) {
            // Arrange
            val fagsakId = 1L

            val klagebehandlingDto =
                lagKlagebehandlingDto(
                    vedtaksdato = LocalDateTime.now(),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = behandlingResultat,
                )

            every { klageClient.hentKlagebehandlinger(setOf(fagsakId)) } returns
                mapOf(
                    fagsakId to listOf(klagebehandlingDto),
                )

            // Act
            val sisteVedtatteKlagebehandling = klagebehandlingHenter.hentSisteVedtatteKlagebehandling(fagsakId)

            // Assert
            assertThat(sisteVedtatteKlagebehandling).isNull()
        }

        @Test
        fun `skal filtrer bort klagebehandlinger med behandlingsresultat som er null`() {
            // Arrange
            val fagsakId = 1L

            val klagebehandlingDto =
                lagKlagebehandlingDto(
                    vedtaksdato = LocalDateTime.now(),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = null,
                )

            every { klageClient.hentKlagebehandlinger(setOf(fagsakId)) } returns
                mapOf(
                    fagsakId to listOf(klagebehandlingDto),
                )

            // Act
            val sisteVedtatteKlagebehandling = klagebehandlingHenter.hentSisteVedtatteKlagebehandling(fagsakId)

            // Assert
            assertThat(sisteVedtatteKlagebehandling).isNull()
        }

        @Test
        fun `skal filtrer bort klagebehandlinger med vedtaksdato som er null`() {
            // Arrange
            val fagsakId = 1L

            val klagebehandlingDto =
                lagKlagebehandlingDto(
                    vedtaksdato = null,
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = BehandlingResultat.MEDHOLD,
                )

            every { klageClient.hentKlagebehandlinger(setOf(fagsakId)) } returns
                mapOf(
                    fagsakId to listOf(klagebehandlingDto),
                )

            // Act
            val sisteVedtatteKlagebehandling = klagebehandlingHenter.hentSisteVedtatteKlagebehandling(fagsakId)

            // Assert
            assertThat(sisteVedtatteKlagebehandling).isNull()
        }

        @Test
        fun `skal hente siste vedtatte klagebehandling med korrekt behandlingsresultat`() {
            // Arrange
            val fagsakId = 1L
            val nåtidspunkt = LocalDateTime.now()

            val klagebehandlingDto1 =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = BehandlingResultat.MEDHOLD,
                )

            val klagebehandlingDto2 =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt,
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = BehandlingResultat.IKKE_MEDHOLD,
                    klageinstansResultat =
                        listOf(
                            lagKlageinstansResultatDto(
                                mottattEllerAvsluttetTidspunkt = nåtidspunkt,
                            ),
                        ),
                )

            val klagebehandlingDto3 =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST,
                )

            every { klageClient.hentKlagebehandlinger(setOf(fagsakId)) } returns
                mapOf(
                    fagsakId to listOf(klagebehandlingDto1, klagebehandlingDto2, klagebehandlingDto3),
                )

            // Act
            val sisteVedtatteKlagebehandling = klagebehandlingHenter.hentSisteVedtatteKlagebehandling(fagsakId)

            // Assert
            assertThat(sisteVedtatteKlagebehandling).isEqualTo(klagebehandlingDto2)
        }
    }
}
