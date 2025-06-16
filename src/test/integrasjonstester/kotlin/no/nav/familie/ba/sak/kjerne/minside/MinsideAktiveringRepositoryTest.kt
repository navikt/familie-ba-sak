package no.nav.familie.ba.sak.kjerne.minside

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException

class MinsideAktiveringRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val minsideAktiveringRepository: MinsideAktiveringRepository,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
) : AbstractSpringIntegrationTest() {
    @BeforeEach
    fun setup() {
        databaseCleanupService.truncate()
    }

    @Nested
    inner class FindByAktør {
        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `skal finne MinsideAktivering for aktør`(aktivert: Boolean) {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val minsideAktivering =
                MinsideAktivering(
                    aktør = aktør,
                    aktivert = aktivert,
                )

            minsideAktiveringRepository.save(minsideAktivering)

            // Act
            val lagretMinsideAktiveringForAktør = minsideAktiveringRepository.findByAktør(aktør)

            // Assert
            assertNotNull(lagretMinsideAktiveringForAktør)
            assertThat(lagretMinsideAktiveringForAktør.aktør).isEqualTo(aktør)
            assertThat(lagretMinsideAktiveringForAktør.aktivert).isEqualTo(aktivert)
        }
    }

    @Nested
    inner class ExistsByAktørAndAktivertIsTrue {
        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `skal sjekke om aktivert MinsideAktivering eksisterer for aktør`(aktivert: Boolean) {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val aktivertMinsideAktivering =
                MinsideAktivering(
                    aktør = aktør,
                    aktivert = aktivert,
                )

            minsideAktiveringRepository.save(aktivertMinsideAktivering)

            // Act
            val aktørHarAktivertMinsideAktivering = minsideAktiveringRepository.existsByAktørAndAktivertIsTrue(aktør)

            // Assert
            assertThat(aktørHarAktivertMinsideAktivering).isEqualTo(aktivert)
        }
    }

    @Nested
    inner class FindAllByAktørInAndAktivertIsTrue {
        @Test
        fun `skal finne alle aktiverte MinsideAktiveringer for en liste av aktører`() {
            // Arrange
            val aktør1 = aktørIdRepository.save(randomAktør())
            val aktør2 = aktørIdRepository.save(randomAktør())
            val aktører = listOf(aktør1, aktør2)

            val minsideAktivering1 =
                MinsideAktivering(
                    aktør = aktør1,
                    aktivert = true,
                )
            val minsideAktivering2 =
                MinsideAktivering(
                    aktør = aktør2,
                    aktivert = false,
                )

            minsideAktiveringRepository.saveAll(listOf(minsideAktivering1, minsideAktivering2))

            // Act
            val lagredeMinsideAktiveringer = minsideAktiveringRepository.findAllByAktørInAndAktivertIsTrue(aktører)

            // Assert
            assertThat(lagredeMinsideAktiveringer).hasSize(1)
            assertThat(lagredeMinsideAktiveringer.map { it.aktør }).containsExactlyInAnyOrder(aktør1)
            assertThat(lagredeMinsideAktiveringer.map { it.aktivert }).containsExactlyInAnyOrder(true)
        }
    }

    @Nested
    inner class Save {
        @Test
        fun `skal feile dersom man forsøker å opprette MinsideAktivering på aktør som allerede har MinsideAktivering`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())

            minsideAktiveringRepository.save(
                MinsideAktivering(
                    aktør = aktør,
                    aktivert = true,
                ),
            )

            // Act & Assert
            assertThrows<DataIntegrityViolationException> {
                minsideAktiveringRepository.save(MinsideAktivering(aktør = aktør, aktivert = true))
            }
        }
    }
}
