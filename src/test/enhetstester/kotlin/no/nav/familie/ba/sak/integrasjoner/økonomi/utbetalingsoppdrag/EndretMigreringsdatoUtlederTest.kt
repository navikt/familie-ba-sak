package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTilkjentYtelse
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class EndretMigreringsdatoUtlederTest {
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val endretMigreringsdatoUtleder: EndretMigreringsdatoUtleder =
        EndretMigreringsdatoUtleder(
            behandlingHentOgPersisterService,
            behandlingService,
        )

    @Test
    fun `skal returnere null hvis forrige tilkjente ytelse er null`() {
        // Arrange
        val fagsak = Fagsak(0L, randomAktør())

        val tilkjentYtelse = lagTilkjentYtelse()

        // Act
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
                tilkjentYtelse = tilkjentYtelse,
                forrigeTilkjentYtelse = null,
            )

        // Assert
        assertThat(endretMigreringsdato).isNull()
    }

    @Test
    fun `skal returnere null om tilkjent ytelser andeler er tom`() {
        // Arrange
        val fagsak = Fagsak(0L, randomAktør())

        val behandling =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            )

        val tilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
                lagAndelerTilkjentYtelse = {
                    emptySet()
                },
            )

        // Act
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
                tilkjentYtelse = tilkjentYtelse,
                forrigeTilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(endretMigreringsdato).isNull()
    }

    @Test
    fun `skal returnere null om det ikke er en migrert sak`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 1)

        val fagsak = Fagsak(0L, randomAktør())

        val behandling =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            )

        val tilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = dagensDato.plusMonths(1).toYearMonth(),
                            tom = dagensDato.plusMonths(2).toYearMonth(),
                        ),
                    )
                },
            )

        every {
            behandlingHentOgPersisterService.hentBehandlinger(fagsak.id)
        } returns listOf(behandling)

        // Act
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
                tilkjentYtelse = tilkjentYtelse,
                forrigeTilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(endretMigreringsdato).isNull()
    }

    @Test
    fun `skal returnere null om det ikke finnes en migreringsdato på fagsaken`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 1)

        val fagsak = Fagsak(0L, randomAktør())

        val behandling =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            )

        val tilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = dagensDato.plusMonths(1).toYearMonth(),
                            tom = dagensDato.plusMonths(2).toYearMonth(),
                        ),
                    )
                },
            )

        every {
            behandlingHentOgPersisterService.hentBehandlinger(fagsak.id)
        } returns listOf(behandling)

        every {
            behandlingService.hentMigreringsdatoPåFagsak(fagsak.id)
        } returns null

        // Act
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
                tilkjentYtelse = tilkjentYtelse,
                forrigeTilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(endretMigreringsdato).isNull()
    }

    @Test
    fun `skal skal kaste exception om man prøver å sette ny migreringsdato etter forrige migreringsdato`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 1)

        val fagsak = Fagsak(0L, randomAktør())

        val behandling =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            )

        val tilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = dagensDato.toYearMonth(),
                            tom = dagensDato.plusMonths(1).toYearMonth(),
                        ),
                    )
                },
            )

        every {
            behandlingHentOgPersisterService.hentBehandlinger(fagsak.id)
        } returns listOf(behandling)

        every {
            behandlingService.hentMigreringsdatoPåFagsak(fagsak.id)
        } returns dagensDato

        // Act & assert
        val exception =
            assertThrows<IllegalStateException> {
                endretMigreringsdatoUtleder.utled(
                    fagsak = fagsak,
                    tilkjentYtelse = tilkjentYtelse,
                    forrigeTilkjentYtelse = tilkjentYtelse,
                )
            }
        assertThat(exception.message).isEqualTo("Ny migreringsdato pluss 1 mnd kan ikke være etter første fom i forrige behandling")
    }

    @Test
    fun `skal returnere ny migreringsdato pluss 1 om ny migreringsdato pluss 1 mnd er før første andel i forrige behandling og første andel i inneværende behandling kommer før første andel i forrige behandling`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 1)

        val fagsak = Fagsak(0L, randomAktør())

        val behandling =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            )

        val tilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = dagensDato.plusMonths(1).toYearMonth(),
                            tom = dagensDato.plusMonths(3).toYearMonth(),
                        ),
                    )
                },
            )

        val forrigeTilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = dagensDato.plusMonths(2).toYearMonth(),
                            tom = dagensDato.plusMonths(3).toYearMonth(),
                        ),
                    )
                },
            )

        every {
            behandlingHentOgPersisterService.hentBehandlinger(fagsak.id)
        } returns listOf(behandling)

        every {
            behandlingService.hentMigreringsdatoPåFagsak(fagsak.id)
        } returns dagensDato

        // Act
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
                tilkjentYtelse = tilkjentYtelse,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
            )

        // Assert
        assertThat(endretMigreringsdato).isEqualTo(YearMonth.of(2024, 12))
    }

    @Test
    fun `skal returnere ny migreringsdato om ny migreringsdato er før forrige migreringsdato med flere andeler`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 1)

        val fagsak = Fagsak(0L, randomAktør())

        val behandling =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            )

        val tilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = dagensDato.plusMonths(1).toYearMonth(),
                            tom = dagensDato.plusMonths(3).toYearMonth(),
                        ),
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = dagensDato.plusMonths(4).toYearMonth(),
                            tom = dagensDato.plusMonths(5).toYearMonth(),
                        ),
                    )
                },
            )

        val forrigeTilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = dagensDato.plusMonths(2).toYearMonth(),
                            tom = dagensDato.plusMonths(3).toYearMonth(),
                        ),
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = dagensDato.plusMonths(4).toYearMonth(),
                            tom = dagensDato.plusMonths(5).toYearMonth(),
                        ),
                    )
                },
            )

        every {
            behandlingHentOgPersisterService.hentBehandlinger(fagsak.id)
        } returns listOf(behandling)

        every {
            behandlingService.hentMigreringsdatoPåFagsak(fagsak.id)
        } returns dagensDato

        // Act
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
                tilkjentYtelse = tilkjentYtelse,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
            )

        // Assert
        assertThat(endretMigreringsdato).isEqualTo(YearMonth.of(2024, 12))
    }

    @Test
    fun `skal returnere null om ny migreringsdato er lik forrige migreringsdato`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 1)

        val fagsak = Fagsak(0L, randomAktør())

        val behandling =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            )

        val tilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = dagensDato.plusMonths(1).toYearMonth(),
                            tom = dagensDato.plusMonths(2).toYearMonth(),
                        ),
                    )
                },
            )

        every {
            behandlingHentOgPersisterService.hentBehandlinger(fagsak.id)
        } returns listOf(behandling)

        every {
            behandlingService.hentMigreringsdatoPåFagsak(fagsak.id)
        } returns dagensDato

        // Act
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
                tilkjentYtelse = tilkjentYtelse,
                forrigeTilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(endretMigreringsdato).isNull()
    }
}
