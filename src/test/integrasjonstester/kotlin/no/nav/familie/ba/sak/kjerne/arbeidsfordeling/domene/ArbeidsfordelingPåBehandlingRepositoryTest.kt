package no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagFagsak
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.oppgave.lagArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.domain.AbstractPersistable_.id
import java.time.LocalDateTime

class ArbeidsfordelingPåBehandlingRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class HentArbeidsfordelingPåBehandlingTest {
        @Test
        fun `skal hente arbeidsfordeling på behandling`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                )

            arbeidsfordelingPåBehandlingRepository.save(arbeidsfordelingPåBehandling)

            // Act
            val lagretArbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(behandlingId)

            // Assert
            assertThat(lagretArbeidsfordelingPåBehandling).isEqualTo(arbeidsfordelingPåBehandling)
        }

        @Test
        fun `skal kaste feil hvis arbeidsfordeling på behandling returnerer null`() {
            // Act & assert
            val exception =
                assertThrows<Feil> {
                    arbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(1L)
                }
            assertThat(exception.message).isEqualTo("Finner ikke tilknyttet arbeidsfordelingsenhet på behandling 1")
        }
    }

    @Nested
    inner class FinnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak {
        @Test
        fun `skal ikke hente arbeidsfordelingPåBehandling med behandlende enhet er midlertidig enhet`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsak(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandling(fagsak = fagsak, status = BehandlingStatus.AVSLUTTET))

            arbeidsfordelingPåBehandlingRepository.save(
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandling.id,
                    behandlendeEnhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                ),
            )

            // Act
            val lagretArbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(fagsak.id)

            // Assert
            assertThat(lagretArbeidsfordelingPåBehandling).isNull()
        }

        @Test
        fun `skal ikke hente arbeidsfordeling på behandling som ikke har status AVSLUTTET`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsak(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandling(fagsak = fagsak, status = BehandlingStatus.FATTER_VEDTAK, aktivertTid = LocalDateTime.now()))

            arbeidsfordelingPåBehandlingRepository.save(lagArbeidsfordelingPåBehandling(behandlingId = behandling.id))

            // Act
            val lagretArbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(fagsak.id)

            // Assert
            assertThat(lagretArbeidsfordelingPåBehandling).isNull()
        }

        @ParameterizedTest
        @EnumSource(Behandlingsresultat::class, names = ["HENLAGT_FEILAKTIG_OPPRETTET", "HENLAGT_SØKNAD_TRUKKET", "HENLAGT_AUTOMATISK_FØDSELSHENDELSE", "HENLAGT_TEKNISK_VEDLIKEHOLD"], mode = EnumSource.Mode.INCLUDE)
        fun `skal ikke hente arbeidsfordeling på behandling som har resultat HENLAGT`(behandlingsResultat: Behandlingsresultat) {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsak(aktør = aktør))
            val behandling =
                behandlingRepository.save(
                    lagBehandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.AVSLUTTET,
                        resultat = behandlingsResultat,
                        aktivertTid = LocalDateTime.now(),
                    ),
                )

            arbeidsfordelingPåBehandlingRepository.save(lagArbeidsfordelingPåBehandling(behandlingId = behandling.id))

            // Act
            val lagretArbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(fagsak.id)

            // Assert
            assertThat(lagretArbeidsfordelingPåBehandling).isNull()
        }

        @TestFactory
        fun `skal hente arbeidsfordeling på behandling som har resultat som ikke er henlagt`(): Collection<DynamicTest> {
            // Skal samsvare med SQL query WHERE clause
            val henlagteBehandlingresultater =
                listOf(
                    Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
                    Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET,
                    Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE,
                    Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD,
                )
            val resultaterSomIkkeErHenlagt = Behandlingsresultat.values().filterNot { it in henlagteBehandlingresultater }

            return resultaterSomIkkeErHenlagt.map { behandlingsResultat ->
                dynamicTest("resultat: $behandlingsResultat skal ikke bli filtrert") {
                    // Arrange
                    val aktør = aktørIdRepository.save(randomAktør())
                    val fagsak = fagsakRepository.save(lagFagsak(aktør = aktør))
                    val behandling =
                        behandlingRepository.save(
                            lagBehandling(
                                fagsak = fagsak,
                                status = BehandlingStatus.AVSLUTTET,
                                resultat = behandlingsResultat,
                                aktivertTid = LocalDateTime.now(),
                            ),
                        )

                    val arbeidsfordeling = arbeidsfordelingPåBehandlingRepository.save(lagArbeidsfordelingPåBehandling(behandlingId = behandling.id))

                    // Act
                    val lagretArbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(fagsak.id)

                    // Assert
                    assertThat(lagretArbeidsfordelingPåBehandling).isEqualTo(arbeidsfordeling)
                }
            }
        }

        @Test
        fun `skal hente nyeste arbeidsfordeling på behandling`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsak(aktør = aktør))

            val nyBehandling =
                behandlingRepository.save(
                    lagBehandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.AVSLUTTET,
                        aktivertTid = LocalDateTime.now(),
                    ),
                )
            val gammelBehandling =
                behandlingRepository.save(
                    lagBehandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.AVSLUTTET,
                        aktivertTid = LocalDateTime.now().minusDays(10),
                        aktiv = false,
                    ),
                )

            val arbeidsfordelingNy = arbeidsfordelingPåBehandlingRepository.save(lagArbeidsfordelingPåBehandling(behandlingId = nyBehandling.id))
            arbeidsfordelingPåBehandlingRepository.save(lagArbeidsfordelingPåBehandling(behandlingId = gammelBehandling.id))

            // Act
            val lagretArbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(fagsak.id)

            // Assert
            assertThat(lagretArbeidsfordelingPåBehandling).isEqualTo(arbeidsfordelingNy)
        }
    }
}
