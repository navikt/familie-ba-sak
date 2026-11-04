package no.nav.familie.ba.sak.ekstern.pensjon

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagFerdigstiltBehandlingForPensjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class FerdigstiltBehandlingForPensjonRepositoryTest(
    @Autowired private val ferdigstiltBehandlingForPensjonRepository: FerdigstiltBehandlingForPensjonRepository,
    @Autowired private val jdbcTemplate: JdbcTemplate,
    @Autowired transactionManager: PlatformTransactionManager,
) : AbstractSpringIntegrationTest() {
    private val transactionTemplate = TransactionTemplate(transactionManager)
    private val behandlingIdSomBlirLagret = -903001L
    private val annenBehandlingIdSomBlirLagret = -903002L

    @AfterEach
    fun slettLagredeFerdigstilteBehandlinger() {
        val entities =
            listOf(
                behandlingIdSomBlirLagret,
                annenBehandlingIdSomBlirLagret,
            ).mapNotNull { ferdigstiltBehandlingForPensjonRepository.findByBehandlingId(it) }
        ferdigstiltBehandlingForPensjonRepository.deleteAll(entities)
    }

    @Nested
    inner class FinnHøyesteSekvensnummer {
        @Test
        fun `skal returnere 0 når tabellen er tom`() {
            transactionTemplate.executeWithoutResult { transaksjon ->
                // Arrange
                ferdigstiltBehandlingForPensjonRepository.deleteAllInBatch()

                // Act
                val høyesteSekvensnummer = ferdigstiltBehandlingForPensjonRepository.finnHøyesteSekvensnummer()

                // Assert
                assertThat(høyesteSekvensnummer).isEqualTo(0L)
                transaksjon.setRollbackOnly()
            }
        }

        @Test
        fun `skal returnere høyeste lagrede sekvensnummer`() {
            // Arrange
            val høyesteSekvensnummerFørTest = ferdigstiltBehandlingForPensjonRepository.finnHøyesteSekvensnummer()
            ferdigstiltBehandlingForPensjonRepository.save(
                lagFerdigstiltBehandlingForPensjon(sekvensnummer = høyesteSekvensnummerFørTest + 5, behandlingId = behandlingIdSomBlirLagret),
            )
            ferdigstiltBehandlingForPensjonRepository.save(
                lagFerdigstiltBehandlingForPensjon(sekvensnummer = høyesteSekvensnummerFørTest + 1, behandlingId = annenBehandlingIdSomBlirLagret),
            )

            // Act
            val høyesteSekvensnummer = ferdigstiltBehandlingForPensjonRepository.finnHøyesteSekvensnummer()

            // Assert
            assertThat(høyesteSekvensnummer).isEqualTo(høyesteSekvensnummerFørTest + 5)
        }
    }

    @Nested
    inner class FindByBehandlingId {
        @Test
        fun `skal finne lagret ferdigstilt behandling for behandlingId`() {
            // Arrange
            val ferdigstiltBehandling =
                lagFerdigstiltBehandlingForPensjon(
                    sekvensnummer = ferdigstiltBehandlingForPensjonRepository.finnHøyesteSekvensnummer() + 1,
                    behandlingId = behandlingIdSomBlirLagret,
                )
            ferdigstiltBehandlingForPensjonRepository.save(ferdigstiltBehandling)

            // Act
            val lagretFerdigstiltBehandling = ferdigstiltBehandlingForPensjonRepository.findByBehandlingId(behandlingIdSomBlirLagret)

            // Assert
            assertThat(lagretFerdigstiltBehandling).isNotNull
            assertThat(lagretFerdigstiltBehandling!!.sekvensnummer).isEqualTo(ferdigstiltBehandling.sekvensnummer)
            assertThat(lagretFerdigstiltBehandling.behandlingId).isEqualTo(ferdigstiltBehandling.behandlingId)
            assertThat(lagretFerdigstiltBehandling.ident).isEqualTo(ferdigstiltBehandling.ident)
            assertThat(lagretFerdigstiltBehandling.vedtakstidspunkt).isEqualTo(ferdigstiltBehandling.vedtakstidspunkt)
            assertThat(lagretFerdigstiltBehandling.endringstidspunkt).isEqualTo(ferdigstiltBehandling.endringstidspunkt)
        }

        @Test
        fun `skal returnere null når behandlingId ikke er lagret`() {
            // Act
            val lagretFerdigstiltBehandling = ferdigstiltBehandlingForPensjonRepository.findByBehandlingId(behandlingIdSomBlirLagret)

            // Assert
            assertThat(lagretFerdigstiltBehandling).isNull()
        }
    }

    @Nested
    inner class SaveAndFlush {
        @Test
        fun `skal ikke kunne lagre to ferdigstilte behandlinger med samme behandlingId`() {
            // Arrange
            val høyesteSekvensnummerFørTest = ferdigstiltBehandlingForPensjonRepository.finnHøyesteSekvensnummer()
            ferdigstiltBehandlingForPensjonRepository.save(
                lagFerdigstiltBehandlingForPensjon(sekvensnummer = høyesteSekvensnummerFørTest + 1, behandlingId = behandlingIdSomBlirLagret),
            )

            // Act & Assert
            assertThrows<DataIntegrityViolationException> {
                ferdigstiltBehandlingForPensjonRepository.saveAndFlush(
                    lagFerdigstiltBehandlingForPensjon(sekvensnummer = høyesteSekvensnummerFørTest + 2, behandlingId = behandlingIdSomBlirLagret),
                )
            }
        }
    }

    @Nested
    inner class LåsTabell {
        @Test
        fun `skal holde eksklusiv lås på tabellen til transaksjonen er ferdig`() {
            // Act & Assert
            transactionTemplate.executeWithoutResult {
                ferdigstiltBehandlingForPensjonRepository.låsTabell()

                assertThat(hentLåserPåTabellen(kunDenneTilkoblingen = true)).contains("ExclusiveLock")
            }

            assertThat(hentLåserPåTabellen(kunDenneTilkoblingen = false)).doesNotContain("ExclusiveLock")
        }
    }

    private fun hentLåserPåTabellen(kunDenneTilkoblingen: Boolean): List<String> =
        jdbcTemplate
            .queryForList(
                """
                SELECT mode FROM pg_locks
                WHERE locktype = 'relation'
                  AND relation = 'ferdigstilt_behandling_for_pensjon'::regclass
                  ${if (kunDenneTilkoblingen) "AND pid = pg_backend_pid()" else ""}
                """.trimIndent(),
                String::class.java,
            ).filterNotNull()
}
