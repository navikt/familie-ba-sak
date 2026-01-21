package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.felles.utbetalingsgenerator.domain.Opphør
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class EndretMigreringsdatoUtlederTest {
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val tilkjentYtelseRepository: TilkjentYtelseRepository = mockk()
    private val behandlingMigreringsinfoRepository: BehandlingMigreringsinfoRepository = mockk()
    private val endretMigreringsdatoUtleder: EndretMigreringsdatoUtleder =
        EndretMigreringsdatoUtleder(
            behandlingHentOgPersisterService,
            behandlingMigreringsinfoRepository,
            tilkjentYtelseRepository,
        )

    @Test
    fun `skal returnere null hvis forrige tilkjente ytelse er null`() {
        // Arrange
        val fagsak = Fagsak(0L, randomAktør())

        // Act
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
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

        val forrigeTilkjentYtelse =
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
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
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
            behandlingMigreringsinfoRepository.finnSisteBehandlingMigreringsInfoPåFagsak(fagsak.id)
        } returns null

        // Act
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
                forrigeTilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(endretMigreringsdato).isNull()
    }

    @Test
    fun `skal skal kaste exception om man prøver å sette ny migreringsdato etter forrige migreringsdato`() {
        // Arrange
        val migreringsdato = LocalDate.of(2024, 11, 1)
        val migreringsdatoEndretDato = LocalDate.of(2024, 5, 1)

        val fagsak = Fagsak(0L, randomAktør())

        val migreringsBehandling1 =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            )

        val forrigeTilkjentYtelse =
            lagTilkjentYtelse(
                behandling = migreringsBehandling1,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = migreringsdato.toYearMonth(),
                            tom = migreringsdato.plusMonths(1).toYearMonth(),
                        ),
                    )
                },
            )

        val migreringsBehandling2 =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            )

        every {
            behandlingHentOgPersisterService.hentBehandlinger(fagsak.id)
        } returns listOf(migreringsBehandling1, migreringsBehandling2)

        every {
            behandlingMigreringsinfoRepository.finnSisteBehandlingMigreringsInfoPåFagsak(fagsak.id)
        } returns
            BehandlingMigreringsinfo(
                behandling = migreringsBehandling1,
                migreringsdato = migreringsdato,
            ).also { it.endretTidspunkt = migreringsdatoEndretDato.atStartOfDay() }

        // Act & assert
        val exception =
            assertThrows<FunksjonellFeil> {
                endretMigreringsdatoUtleder.utled(
                    fagsak = fagsak,
                    forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                )
            }
        assertThat(exception.message).isEqualTo("Ny migreringsdato pluss 1 mnd kan ikke være etter første fom i forrige behandling")
    }

    @Test
    fun `skal ikke kaste exception om man prøver å sette ny migreringsdato etter forrige migreringsdato når det er satsendring`() {
        // Arrange
        val migreringsdato = LocalDate.of(2024, 11, 1)
        val migreringsdatoEndretDato = LocalDate.of(2024, 5, 1)

        val fagsak = Fagsak(0L, randomAktør())

        val migreringsBehandling1 =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            )

        val forrigeTilkjentYtelse =
            lagTilkjentYtelse(
                behandling = migreringsBehandling1,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = migreringsdato.toYearMonth(),
                            tom = migreringsdato.plusMonths(1).toYearMonth(),
                        ),
                    )
                },
            )

        val migreringsBehandling2 =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            )

        every {
            behandlingHentOgPersisterService.hentBehandlinger(fagsak.id)
        } returns listOf(migreringsBehandling1, migreringsBehandling2)

        every {
            behandlingMigreringsinfoRepository.finnSisteBehandlingMigreringsInfoPåFagsak(fagsak.id)
        } returns
            BehandlingMigreringsinfo(
                behandling = migreringsBehandling1,
                migreringsdato = migreringsdato,
            ).also { it.endretTidspunkt = migreringsdatoEndretDato.atStartOfDay() }

        every { tilkjentYtelseRepository.findByFagsak(fagsak.id) } returns listOf(forrigeTilkjentYtelse)

        // Act & assert
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                erSatsendring = true,
            )

        assertThat(endretMigreringsdato).isNull()
    }

    @Test
    fun `skal returnere null dersom det kun finnes 1 behandling av typen MIGRERING_FRA_INFOTRYGD og dette er den første behandlingen i fagsaken`() {
        // Arrange
        val migreringsdato = LocalDate.of(2021, 11, 1)

        val fagsak = Fagsak(0L, randomAktør())

        val migreringsBehandling1 =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            )

        val forrigeTilkjentYtelse =
            lagTilkjentYtelse(
                behandling = migreringsBehandling1,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = migreringsdato.plusMonths(2).toYearMonth(),
                            tom = migreringsdato.plusMonths(3).toYearMonth(),
                        ),
                    )
                },
            )

        val behandling2 =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.REVURDERING,
                årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            )

        every {
            behandlingHentOgPersisterService.hentBehandlinger(fagsak.id)
        } returns listOf(migreringsBehandling1, behandling2)

        // Act
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
            )

        // Assert
        assertThat(endretMigreringsdato).isNull()
    }

    @Test
    fun `skal returnere ny migreringsdato pluss 1 mnd om man ikke har opphørt fra ny migreringsdato pluss 1 mnd i en tidligere behandling`() {
        // Arrange
        val migreringsdato = LocalDate.of(2021, 11, 1)
        val migreringsdatoEndretDato = LocalDate.of(2024, 5, 1)

        val fagsak = Fagsak(0L, randomAktør())

        val migreringsBehandling1 =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            )

        val forrigeTilkjentYtelse =
            lagTilkjentYtelse(
                behandling = migreringsBehandling1,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = migreringsdato.plusMonths(2).toYearMonth(),
                            tom = migreringsdato.plusMonths(3).toYearMonth(),
                        ),
                    )
                },
            )

        val migreringsBehandling2 =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
            )

        every {
            behandlingHentOgPersisterService.hentBehandlinger(fagsak.id)
        } returns listOf(migreringsBehandling1, migreringsBehandling2)

        every {
            behandlingMigreringsinfoRepository.finnSisteBehandlingMigreringsInfoPåFagsak(fagsak.id)
        } returns
            BehandlingMigreringsinfo(
                behandling = migreringsBehandling1,
                migreringsdato = migreringsdato,
            ).also { it.endretTidspunkt = migreringsdatoEndretDato.atStartOfDay() }

        every { tilkjentYtelseRepository.findByFagsak(fagsak.id) } returns listOf(forrigeTilkjentYtelse)

        // Act
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
            )

        // Assert
        assertThat(endretMigreringsdato).isEqualTo(YearMonth.of(2021, 12))
    }

    @Test
    fun `skal returnere null dersom man allerede har opphørt fra migreringsdato pluss 1 mnd`() {
        // Arrange
        val migreringsdato = LocalDate.of(2024, 11, 1)
        val migreringsdatoEndretDato = LocalDate.of(2024, 11, 1)

        val fagsak = Fagsak(0L, randomAktør())

        val migreringsBehandling1 =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            )

        val forrigeTilkjentYtelse =
            lagTilkjentYtelse(
                behandling = migreringsBehandling1,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = migreringsdato.plusMonths(2).toYearMonth(),
                            tom = migreringsdato.plusMonths(3).toYearMonth(),
                        ),
                    )
                },
                utbetalingsoppdrag =
                    jsonMapper.writeValueAsString(
                        lagUtbetalingsoppdrag(
                            listOf(
                                lagUtbetalingsperiode(
                                    behandlingId = migreringsBehandling1.id,
                                    periodeId = 0,
                                    forrigePeriodeId = null,
                                    ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                                    opphør = Opphør(migreringsdato.plusMonths(1).toYearMonth().toLocalDate()),
                                ),
                            ),
                        ),
                    ),
            )

        val migreringsBehandling2 =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
            )

        every {
            behandlingHentOgPersisterService.hentBehandlinger(fagsak.id)
        } returns listOf(migreringsBehandling1, migreringsBehandling2)

        every {
            behandlingMigreringsinfoRepository.finnSisteBehandlingMigreringsInfoPåFagsak(fagsak.id)
        } returns
            BehandlingMigreringsinfo(
                behandling = migreringsBehandling1,
                migreringsdato = migreringsdato,
            ).also { it.endretTidspunkt = migreringsdatoEndretDato.atStartOfDay() }

        every { tilkjentYtelseRepository.findByFagsak(fagsak.id) } returns listOf(forrigeTilkjentYtelse)

        // Act
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
            )

        // Assert
        assertThat(endretMigreringsdato).isNull()
    }

    @Test
    fun `skal returnere null dersom man allerede har opphørt fra migreringsdato pluss 1 mnd og migreringsdato ikke er den første i mnd`() {
        // Arrange
        val migreringsdato = LocalDate.of(2024, 11, 11)
        val migreringsdatoEndretDato = LocalDate.of(2024, 11, 11)

        val fagsak = Fagsak(0L, randomAktør())

        val migreringsBehandling1 =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            )

        val forrigeTilkjentYtelse =
            lagTilkjentYtelse(
                behandling = migreringsBehandling1,
                lagAndelerTilkjentYtelse = {
                    setOf(
                        lagAndelTilkjentYtelse(
                            tilkjentYtelse = it,
                            fom = migreringsdato.plusMonths(2).toYearMonth(),
                            tom = migreringsdato.plusMonths(3).toYearMonth(),
                        ),
                    )
                },
                utbetalingsoppdrag =
                    jsonMapper.writeValueAsString(
                        lagUtbetalingsoppdrag(
                            listOf(
                                lagUtbetalingsperiode(
                                    behandlingId = migreringsBehandling1.id,
                                    periodeId = 0,
                                    forrigePeriodeId = null,
                                    ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                                    opphør = Opphør(migreringsdato.plusMonths(1).toYearMonth().toLocalDate()),
                                ),
                            ),
                        ),
                    ),
            )

        val migreringsBehandling2 =
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
            )

        every {
            behandlingHentOgPersisterService.hentBehandlinger(fagsak.id)
        } returns listOf(migreringsBehandling1, migreringsBehandling2)

        every {
            behandlingMigreringsinfoRepository.finnSisteBehandlingMigreringsInfoPåFagsak(fagsak.id)
        } returns
            BehandlingMigreringsinfo(
                behandling = migreringsBehandling1,
                migreringsdato = migreringsdato,
            ).also { it.endretTidspunkt = migreringsdatoEndretDato.atStartOfDay() }

        every { tilkjentYtelseRepository.findByFagsak(fagsak.id) } returns listOf(forrigeTilkjentYtelse)

        // Act
        val endretMigreringsdato =
            endretMigreringsdatoUtleder.utled(
                fagsak = fagsak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
            )

        // Assert
        assertThat(endretMigreringsdato).isNull()
    }
}
