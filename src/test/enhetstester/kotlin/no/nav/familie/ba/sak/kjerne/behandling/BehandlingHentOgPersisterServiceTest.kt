package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BehandlingHentOgPersisterServiceTest {
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val saksstatistikkEventPublisher = mockk<SaksstatistikkEventPublisher>()
    private val vedtakRepository = mockk<VedtakRepository>()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService =
        BehandlingHentOgPersisterService(
            behandlingRepository = behandlingRepository,
            saksstatistikkEventPublisher = saksstatistikkEventPublisher,
            vedtakRepository = vedtakRepository,
        )

    @Nested
    inner class HentVisningsbehandlinger {
        @Test
        fun `skal hente visningsbehandlinger`() {
            // Arrange
            val fagsak = lagFagsak()
            val vedtaksdato = LocalDateTime.now()

            val behandling1 =
                lagBehandling(
                    fagsak = fagsak,
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.SØKNAD,
                )

            val behandling2 =
                lagBehandling(
                    fagsak = fagsak,
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                )

            every { vedtakRepository.finnVedtaksdatoForBehandling(any()) } returns vedtaksdato

            every { behandlingRepository.finnBehandlinger(fagsak.id) } returns listOf(behandling1, behandling2)

            // Act
            val visningsbehandlinger = behandlingHentOgPersisterService.hentVisningsbehandlinger(fagsak.id)

            // Assert
            assertThat(visningsbehandlinger).hasSize(2)
            assertThat(visningsbehandlinger).anySatisfy {
                assertThat(it.behandlingId).isEqualTo(behandling1.id)
                assertThat(it.opprettetTidspunkt).isEqualTo(behandling1.opprettetTidspunkt)
                assertThat(it.aktivertTidspunkt).isEqualTo(behandling1.aktivertTidspunkt)
                assertThat(it.kategori).isEqualTo(behandling1.kategori)
                assertThat(it.underkategori).isEqualTo(behandling1.underkategori)
                assertThat(it.aktiv).isEqualTo(behandling1.aktiv)
                assertThat(it.opprettetÅrsak).isEqualTo(behandling1.opprettetÅrsak)
                assertThat(it.type).isEqualTo(behandling1.type)
                assertThat(it.status).isEqualTo(behandling1.status)
                assertThat(it.resultat).isEqualTo(behandling1.resultat)
                assertThat(it.vedtaksdato).isEqualTo(vedtaksdato)
            }
            assertThat(visningsbehandlinger).anySatisfy {
                assertThat(it.behandlingId).isEqualTo(behandling2.id)
                assertThat(it.opprettetTidspunkt).isEqualTo(behandling2.opprettetTidspunkt)
                assertThat(it.aktivertTidspunkt).isEqualTo(behandling2.aktivertTidspunkt)
                assertThat(it.kategori).isEqualTo(behandling2.kategori)
                assertThat(it.underkategori).isEqualTo(behandling2.underkategori)
                assertThat(it.aktiv).isEqualTo(behandling2.aktiv)
                assertThat(it.opprettetÅrsak).isEqualTo(behandling2.opprettetÅrsak)
                assertThat(it.type).isEqualTo(behandling2.type)
                assertThat(it.status).isEqualTo(behandling2.status)
                assertThat(it.resultat).isEqualTo(behandling2.resultat)
                assertThat(it.vedtaksdato).isEqualTo(vedtaksdato)
            }
        }

        @Test
        fun `skal filterer bort behandlinger med oppdatert utvidet klassekode`() {
            // Arrange
            val fagsak = lagFagsak()
            val vedtaksdato = LocalDateTime.now()

            val behandling1 =
                lagBehandling(
                    fagsak = fagsak,
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.SØKNAD,
                )

            val behandling2 =
                lagBehandling(
                    fagsak = fagsak,
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.OPPDATER_UTVIDET_KLASSEKODE,
                )

            every { vedtakRepository.finnVedtaksdatoForBehandling(any()) } returns vedtaksdato

            every { behandlingRepository.finnBehandlinger(fagsak.id) } returns listOf(behandling1, behandling2)

            // Act
            val visningsbehandlinger = behandlingHentOgPersisterService.hentVisningsbehandlinger(fagsak.id)

            // Assert
            assertThat(visningsbehandlinger).hasSize(1)
            assertThat(visningsbehandlinger).anySatisfy {
                assertThat(it.behandlingId).isEqualTo(behandling1.id)
                assertThat(it.opprettetTidspunkt).isEqualTo(behandling1.opprettetTidspunkt)
                assertThat(it.aktivertTidspunkt).isEqualTo(behandling1.aktivertTidspunkt)
                assertThat(it.kategori).isEqualTo(behandling1.kategori)
                assertThat(it.underkategori).isEqualTo(behandling1.underkategori)
                assertThat(it.aktiv).isEqualTo(behandling1.aktiv)
                assertThat(it.opprettetÅrsak).isEqualTo(behandling1.opprettetÅrsak)
                assertThat(it.type).isEqualTo(behandling1.type)
                assertThat(it.status).isEqualTo(behandling1.status)
                assertThat(it.resultat).isEqualTo(behandling1.resultat)
                assertThat(it.vedtaksdato).isEqualTo(vedtaksdato)
            }
        }
    }
}
