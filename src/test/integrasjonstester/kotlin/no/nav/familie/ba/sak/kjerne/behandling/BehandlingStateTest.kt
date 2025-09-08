package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired

class BehandlingStateTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class AktivBehandling {
        @Test
        fun `kan ikke ha flere behandlinger med aktiv true`() {
            // Arrange
            val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
            opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true, fagsak = fagsak)

            // Act & Assert
            assertThatThrownBy {
                opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true, fagsak = fagsak)
            }.hasMessageContaining("uidx_behandling_01")
        }

        @Test
        fun `kan maks ha en parallell behandling i arbeid`() {
            BehandlingStatus
                .entries
                .filter { it != BehandlingStatus.AVSLUTTET && it != BehandlingStatus.SATT_PÅ_MASKINELL_VENT }
                .forEach {
                    // Arrange
                    val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
                    opprettBehandling(status = it, aktiv = false, fagsak = fagsak)

                    // Act & Assert
                    assertThatThrownBy {
                        opprettBehandling(status = it, aktiv = true, fagsak = fagsak)
                    }.hasMessageContaining("uidx_behandling_02")
                }
        }

        @Test
        fun `kan ikke ha 2 behandlinger med status SATT_PÅ_VENTSATT_PÅ_MASKINELL_VENT`() {
            // Arrange
            val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
            opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false, fagsak = fagsak)

            // Act & Assert
            assertThatThrownBy {
                opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = true, fagsak = fagsak)
            }.hasMessageContaining("uidx_behandling_03")
        }

        @Test
        fun `skal kunne ha aktiv tvers ulike fagsaker`() {
            // Arrange
            val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
            opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true, fagsak = fagsak)
            val annenFagsak =
                fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())

            // Act & Assert
            assertDoesNotThrow {
                opprettBehandling(annenFagsak, status = BehandlingStatus.AVSLUTTET, aktiv = true)
            }
        }
    }

    @Nested
    inner class BehandlingStatuser {
        @Test
        fun `kan ha flere behandlinger som er avsluttet`() {
            // Arrange
            val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())

            // Act & Assert
            assertDoesNotThrow {
                opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = false, fagsak = fagsak)
                opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true, fagsak = fagsak)
            }
        }

        @Test
        fun `kan ha en behandling på maskinell vent og en med status utredes`() {
            // Arrange
            val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())

            // Act & Assert
            assertDoesNotThrow {
                opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = false, fagsak = fagsak)
                opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false, fagsak = fagsak)
                opprettBehandling(status = BehandlingStatus.UTREDES, aktiv = true, fagsak = fagsak)
            }
        }
    }

    private fun opprettBehandling(
        fagsak: Fagsak,
        status: BehandlingStatus,
        aktiv: Boolean,
    ): Behandling {
        val behandling = lagBehandlingUtenId(fagsak = fagsak, status = status, aktiv = aktiv)
        return behandlingRepository.saveAndFlush(behandling)
    }
}
