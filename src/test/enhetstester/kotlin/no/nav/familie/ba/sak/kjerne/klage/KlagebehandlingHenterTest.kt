package no.nav.familie.ba.sak.kjerne.klage

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagKlagebehandlingDto
import no.nav.familie.ba.sak.datagenerator.lagKlageinstansResultatDto
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import java.util.UUID

class KlagebehandlingHenterTest {
    private val klageKlient = mockk<KlageKlient>()
    private val klagebehandlingHenter =
        KlagebehandlingHenter(
            klageKlient = klageKlient,
        )

    @Nested
    inner class HentKlagebehandlingerPåFagsak {
        @Test
        fun `skal hente klagebehandlinger på fagsak`() {
            // Arrange
            val fagsakId = 1L

            val klagebehandlingerForFagsak1 = listOf(lagKlagebehandlingDto(), lagKlagebehandlingDto())

            every { klageKlient.hentKlagebehandlinger(fagsakId) } returns klagebehandlingerForFagsak1

            // Act
            val resultat = klagebehandlingHenter.hentKlagebehandlingerPåFagsak(fagsakId)

            // Assert
            assertThat(resultat).isEqualTo(klagebehandlingerForFagsak1)
        }
    }

    @Nested
    inner class HentForrigeVedtatteKlagebehandling {
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
            val nåtidspunkt = LocalDateTime.now()

            val behandling =
                lagBehandling(
                    aktivertTid = nåtidspunkt,
                )

            val klagebehandlingDto =
                lagKlagebehandlingDto(
                    fagsakId = UUID.randomUUID(),
                    vedtaksdato = nåtidspunkt.minusSeconds(1),
                    status = behandlingStatus,
                )

            every { klageKlient.hentKlagebehandlinger(behandling.fagsak.id) } returns listOf(klagebehandlingDto)

            // Act
            val forrigeVedtatteKlagebehandling = klagebehandlingHenter.hentForrigeVedtatteKlagebehandling(behandling)

            // Assert
            assertThat(forrigeVedtatteKlagebehandling).isNull()
        }

        @ParameterizedTest
        @EnumSource(value = HenlagtÅrsak::class)
        fun `skal filtrer bort klagebehandlinger med henlagt årsak`(
            henlagtÅrsak: HenlagtÅrsak,
        ) {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val behandling =
                lagBehandling(
                    aktivertTid = nåtidspunkt,
                )

            val klagebehandlingDto =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = henlagtÅrsak,
                )

            every { klageKlient.hentKlagebehandlinger(behandling.fagsak.id) } returns listOf(klagebehandlingDto)

            // Act
            val forrigeVedtatteKlagebehandling = klagebehandlingHenter.hentForrigeVedtatteKlagebehandling(behandling)

            // Assert
            assertThat(forrigeVedtatteKlagebehandling).isNull()
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
            val nåtidspunkt = LocalDateTime.now()

            val behandling =
                lagBehandling(
                    aktivertTid = nåtidspunkt,
                )

            val klagebehandlingDto =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = behandlingResultat,
                )

            every { klageKlient.hentKlagebehandlinger(behandling.fagsak.id) } returns listOf(klagebehandlingDto)

            // Act
            val forrigeVedtatteKlagebehandling = klagebehandlingHenter.hentForrigeVedtatteKlagebehandling(behandling)

            // Assert
            assertThat(forrigeVedtatteKlagebehandling).isNull()
        }

        @Test
        fun `skal filtrer bort klagebehandlinger med behandlingsresultat som er null`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val behandling =
                lagBehandling(
                    aktivertTid = nåtidspunkt,
                )

            val klagebehandlingDto =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = null,
                )

            every { klageKlient.hentKlagebehandlinger(behandling.fagsak.id) } returns listOf(klagebehandlingDto)

            // Act
            val forrigeVedtatteKlagebehandling = klagebehandlingHenter.hentForrigeVedtatteKlagebehandling(behandling)

            // Assert
            assertThat(forrigeVedtatteKlagebehandling).isNull()
        }

        @Test
        fun `skal filtrer bort klagebehandlinger med vedtaksdato som er null`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val behandling =
                lagBehandling(
                    aktivertTid = nåtidspunkt,
                )

            val klagebehandlingDto =
                lagKlagebehandlingDto(
                    vedtaksdato = null,
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = BehandlingResultat.MEDHOLD,
                )

            every { klageKlient.hentKlagebehandlinger(behandling.fagsak.id) } returns listOf(klagebehandlingDto)

            // Act
            val forrigeVedtatteKlagebehandling = klagebehandlingHenter.hentForrigeVedtatteKlagebehandling(behandling)

            // Assert
            assertThat(forrigeVedtatteKlagebehandling).isNull()
        }

        @Test
        fun `skal hente forrige vedtatte klagebehandling med korrekt behandlingsresultat`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.of(2025, 1, 1, 12, 0)

            val klageFagsakId = UUID.randomUUID()

            val behandling =
                lagBehandling(
                    aktivertTid = nåtidspunkt,
                )

            val klagebehandlingDto1 =
                lagKlagebehandlingDto(
                    fagsakId = klageFagsakId,
                    vedtaksdato = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = BehandlingResultat.MEDHOLD,
                )

            val klagebehandlingDto2 =
                lagKlagebehandlingDto(
                    fagsakId = klageFagsakId,
                    vedtaksdato = nåtidspunkt.minusSeconds(3),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = BehandlingResultat.IKKE_MEDHOLD,
                    klageinstansResultat =
                        listOf(
                            lagKlageinstansResultatDto(
                                type = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
                                mottattEllerAvsluttetTidspunkt = nåtidspunkt.minusSeconds(1),
                            ),
                        ),
                )

            val klagebehandlingDto3 =
                lagKlagebehandlingDto(
                    fagsakId = klageFagsakId,
                    vedtaksdato = nåtidspunkt.minusSeconds(4),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST,
                )

            every { klageKlient.hentKlagebehandlinger(behandling.fagsak.id) } returns listOf(klagebehandlingDto1, klagebehandlingDto2, klagebehandlingDto3)

            val forventetForrigeVedtatteKlagebehandling = klagebehandlingDto2.copy(vedtaksdato = nåtidspunkt.minusSeconds(1))

            // Act
            val forrigeVedtatteKlagebehandling = klagebehandlingHenter.hentForrigeVedtatteKlagebehandling(behandling)

            // Assert
            assertThat(forrigeVedtatteKlagebehandling).isEqualTo(forventetForrigeVedtatteKlagebehandling)
        }

        @Test
        fun `skal filtrer bort klagebehandlinger med vedtaksdato etter den innsendte behandlingen`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.of(2025, 1, 1, 12, 0)

            val klageFagsakId = UUID.randomUUID()

            val behandling =
                lagBehandling(
                    aktivertTid = nåtidspunkt,
                )

            val klagebehandlingDto1 =
                lagKlagebehandlingDto(
                    fagsakId = klageFagsakId,
                    vedtaksdato = nåtidspunkt.plusSeconds(1),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = BehandlingResultat.MEDHOLD,
                )

            val klagebehandlingDto2 =
                lagKlagebehandlingDto(
                    fagsakId = klageFagsakId,
                    vedtaksdato = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = BehandlingResultat.MEDHOLD,
                )

            every { klageKlient.hentKlagebehandlinger(behandling.fagsak.id) } returns listOf(klagebehandlingDto1, klagebehandlingDto2)

            // Act
            val forrigeVedtatteKlagebehandling = klagebehandlingHenter.hentForrigeVedtatteKlagebehandling(behandling)

            // Assert
            assertThat(forrigeVedtatteKlagebehandling).isEqualTo(klagebehandlingDto2)
        }
    }
}
