package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagEksternBehandlingRelasjon
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class EksternBehandlingRelasjonRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val eksternBehandlingRelasjonRepository: EksternBehandlingRelasjonRepository,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class FindAllByInternBehandlingId {
        @Test
        fun `skal finne alle eksterne behandling relasjoner for intern behandling id`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak, status = BehandlingStatus.AVSLUTTET))

            val eksternBehandlingRelasjon1 =
                lagEksternBehandlingRelasjon(
                    internBehandlingId = behandling.id,
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )

            val eksternBehandlingRelasjon2 =
                lagEksternBehandlingRelasjon(
                    internBehandlingId = behandling.id,
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.TILBAKEKREVING,
                )

            eksternBehandlingRelasjonRepository.saveAll(
                listOf(
                    eksternBehandlingRelasjon1,
                    eksternBehandlingRelasjon2,
                ),
            )

            // Act
            val eksternBehandlingRelasjoner =
                eksternBehandlingRelasjonRepository.findAllByInternBehandlingId(
                    internBehandlingId = behandling.id,
                )

            // Assert
            assertThat(eksternBehandlingRelasjoner).hasSize(2)
            assertThat(eksternBehandlingRelasjoner).anySatisfy {
                assertThat(it.id).isNotNull()
                assertThat(it.internBehandlingId).isEqualTo(behandling.id)
                assertThat(it.eksternBehandlingId).isEqualTo(eksternBehandlingRelasjon1.eksternBehandlingId)
                assertThat(it.eksternBehandlingFagsystem).isEqualTo(eksternBehandlingRelasjon1.eksternBehandlingFagsystem)
                assertThat(it.opprettetTidspunkt).isNotNull()
            }
            assertThat(eksternBehandlingRelasjoner).anySatisfy {
                assertThat(it.id).isNotNull()
                assertThat(it.internBehandlingId).isEqualTo(behandling.id)
                assertThat(it.eksternBehandlingId).isEqualTo(eksternBehandlingRelasjon2.eksternBehandlingId)
                assertThat(it.eksternBehandlingFagsystem).isEqualTo(eksternBehandlingRelasjon2.eksternBehandlingFagsystem)
                assertThat(it.opprettetTidspunkt).isNotNull()
            }
        }

        @Test
        fun `skal returnere en tom liste om ingen ekstern behandling relasjon finnes for den etterspurte interne behandling iden`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling1 =
                behandlingRepository.save(
                    lagBehandlingUtenId(
                        fagsak = fagsak,
                        aktiv = false,
                        resultat = Behandlingsresultat.INNVILGET,
                        status = BehandlingStatus.AVSLUTTET,
                    ),
                )
            val behandling2 =
                behandlingRepository.save(
                    lagBehandlingUtenId(
                        fagsak = fagsak,
                        aktiv = false,
                        resultat = Behandlingsresultat.INNVILGET,
                        status = BehandlingStatus.AVSLUTTET,
                    ),
                )

            val eksternBehandlingRelasjon1 =
                lagEksternBehandlingRelasjon(
                    internBehandlingId = behandling2.id,
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )

            val eksternBehandlingRelasjon2 =
                lagEksternBehandlingRelasjon(
                    internBehandlingId = behandling2.id,
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.TILBAKEKREVING,
                )

            eksternBehandlingRelasjonRepository.saveAll(
                listOf(
                    eksternBehandlingRelasjon1,
                    eksternBehandlingRelasjon2,
                ),
            )

            // Act
            val eksternBehandlingRelasjoner =
                eksternBehandlingRelasjonRepository.findAllByInternBehandlingId(
                    internBehandlingId = behandling1.id,
                )

            // Assert
            assertThat(eksternBehandlingRelasjoner).isEmpty()
        }
    }
}
