package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.internal.AndelTilkjentYtelseKorreksjon
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.PatchetAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.PatchetAndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.felles.utbetalingsgenerator.domain.Opphør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class OppdaterTilkjentYtelseServiceTest {
    private val endretUtbetalingAndelHentOgPersisterService = mockk<EndretUtbetalingAndelHentOgPersisterService>()
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val patchetAndelTilkjentYtelseRepository = mockk<PatchetAndelTilkjentYtelseRepository>()
    private val zoneId = ZoneId.of("Europe/Oslo")
    private val dagensDato = LocalDate.of(2024, 11, 6)
    private val clock: Clock =
        Clock.fixed(
            dagensDato.atStartOfDay(zoneId).toInstant(),
            zoneId,
        )
    private val oppdaterTilkjentYtelseService =
        OppdaterTilkjentYtelseService(
            endretUtbetalingAndelHentOgPersisterService = endretUtbetalingAndelHentOgPersisterService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            patchetAndelTilkjentYtelseRepository = patchetAndelTilkjentYtelseRepository,
            clockProvider = TestClockProvider(clock),
        )

    @Nested
    inner class OppdaterTilkjentYtelseMedUtbetalingsoppdrag {
        @Test
        fun `skal oppdatere tilkjent ytelse med beregnet utbetalingsoppdrag`() {
            // Arrange
            val behandling = lagBehandling()
            val fom = dagensDato.plusMonths(1).toYearMonth()
            val tom = dagensDato.plusMonths(2).toYearMonth()
            val tilkjentYtelse =
                lagTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = null,
                    stønadTom = null,
                    opphørFom = null,
                    lagAndelerTilkjentYtelse = {
                        setOf(
                            lagAndelTilkjentYtelse(
                                tilkjentYtelse = it,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                id = 1,
                                fom = fom,
                                tom = tom,
                            ),
                        )
                    },
                )
            val beregnetUtbetalingsoppdragLongId =
                lagBeregnetUtbetalingsoppdragLongId(
                    listOf(
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                            fom = fom.førsteDagIInneværendeMåned(),
                            tom = tom.sisteDagIInneværendeMåned(),
                        ),
                    ),
                    listOf(
                        lagAndelMedPeriodeIdLong(
                            id = 1,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            kildeBehandlingId = behandling.id,
                        ),
                    ),
                )

            every { endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandling.id) } returns emptyList()
            every { tilkjentYtelseRepository.save(any()) } returns tilkjentYtelse

            // Act
            oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(tilkjentYtelse, beregnetUtbetalingsoppdragLongId)

            // Assert
            assertThat(tilkjentYtelse.utbetalingsoppdrag).isNotNull()
            assertThat(tilkjentYtelse.stønadFom).isEqualTo(fom)
            assertThat(tilkjentYtelse.stønadTom).isEqualTo(tom)
            assertThat(tilkjentYtelse.endretDato).isEqualTo(dagensDato)
            assertThat(tilkjentYtelse.opphørFom).isNull()

            val andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag = tilkjentYtelse.andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }
            assertThat(andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag).hasSize(1)
            val oppdatertAndel = andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag.single()
            assertThat(oppdatertAndel.periodeOffset).isEqualTo(0)
            assertThat(oppdatertAndel.forrigePeriodeOffset).isNull()
            assertThat(oppdatertAndel.kildeBehandlingId).isEqualTo(behandling.id)
        }

        @Test
        fun `skal oppdatere tilkjent ytelse med beregnet utbetalingsoppdrag når det finnes endrede utbetalingsandeler`() {
            // Arrange
            val behandling = lagBehandling()
            val fom = dagensDato.plusMonths(1).toYearMonth()
            val tom = dagensDato.plusMonths(2).toYearMonth()
            val søker = lagPerson()
            val tilkjentYtelse =
                lagTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = null,
                    stønadTom = null,
                    opphørFom = null,
                    lagAndelerTilkjentYtelse = {
                        setOf(
                            lagAndelTilkjentYtelse(
                                tilkjentYtelse = it,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                person = søker,
                                id = 1,
                                fom = fom,
                                tom = tom,
                            ),
                        )
                    },
                )
            val beregnetUtbetalingsoppdragLongId =
                lagBeregnetUtbetalingsoppdragLongId(
                    listOf(
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                            fom = fom.førsteDagIInneværendeMåned(),
                            tom = tom.sisteDagIInneværendeMåned(),
                        ),
                    ),
                    listOf(
                        lagAndelMedPeriodeIdLong(
                            id = 1,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            kildeBehandlingId = behandling.id,
                        ),
                    ),
                )

            every { endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandling.id) } returns
                listOf(
                    lagEndretUtbetalingAndel(
                        behandlingId = behandling.id,
                        personer = setOf(søker),
                        fom = fom,
                        tom = tom,
                        prosent = 0,
                    ),
                )
            every { tilkjentYtelseRepository.save(any()) } returns tilkjentYtelse

            // Act
            oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(tilkjentYtelse, beregnetUtbetalingsoppdragLongId)

            // Assert
            assertThat(tilkjentYtelse.utbetalingsoppdrag).isNotNull()
            assertThat(tilkjentYtelse.stønadFom).isEqualTo(fom)
            assertThat(tilkjentYtelse.stønadTom).isEqualTo(tom)
            assertThat(tilkjentYtelse.endretDato).isEqualTo(dagensDato)
            assertThat(tilkjentYtelse.opphørFom).isNull()

            val andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag = tilkjentYtelse.andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }
            assertThat(andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag).hasSize(1)
            val oppdatertAndel = andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag.single()
            assertThat(oppdatertAndel.periodeOffset).isEqualTo(0)
            assertThat(oppdatertAndel.forrigePeriodeOffset).isNull()
            assertThat(oppdatertAndel.kildeBehandlingId).isEqualTo(behandling.id)
        }

        @Test
        fun `skal oppdatere tilkjent ytelse med beregnet utbetalingsoppdrag ved rent opphør`() {
            // Arrange
            val behandling = lagBehandling()
            val fom = dagensDato.plusMonths(1).toYearMonth()
            val tom = dagensDato.plusMonths(2).toYearMonth()
            val tilkjentYtelse =
                lagTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = null,
                    stønadTom = null,
                    opphørFom = null,
                    lagAndelerTilkjentYtelse = {
                        setOf(
                            lagAndelTilkjentYtelse(
                                tilkjentYtelse = it,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                id = 1,
                                fom = fom,
                                tom = tom,
                            ),
                        )
                    },
                )
            val beregnetUtbetalingsoppdragLongId =
                lagBeregnetUtbetalingsoppdragLongId(
                    listOf(
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                            fom = fom.førsteDagIInneværendeMåned(),
                            tom = tom.plusMonths(1).sisteDagIInneværendeMåned(),
                            opphør = Opphør(opphørDatoFom = tom.plusMonths(1).førsteDagIInneværendeMåned()),
                        ),
                    ),
                    listOf(
                        lagAndelMedPeriodeIdLong(
                            id = 1,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            kildeBehandlingId = behandling.id,
                        ),
                    ),
                )

            every { endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandling.id) } returns emptyList()
            every { tilkjentYtelseRepository.save(any()) } returns tilkjentYtelse

            // Act
            oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(tilkjentYtelse, beregnetUtbetalingsoppdragLongId)

            // Assert
            assertThat(tilkjentYtelse.utbetalingsoppdrag).isNotNull()
            assertThat(tilkjentYtelse.stønadFom).isNull()
            assertThat(tilkjentYtelse.stønadTom).isEqualTo(tom)
            assertThat(tilkjentYtelse.endretDato).isEqualTo(dagensDato)
            assertThat(tilkjentYtelse.opphørFom).isEqualTo(tom.plusMonths(1))

            val andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag = tilkjentYtelse.andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }
            assertThat(andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag).hasSize(1)
            val oppdatertAndel = andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag.single()
            assertThat(oppdatertAndel.periodeOffset).isEqualTo(0)
            assertThat(oppdatertAndel.forrigePeriodeOffset).isNull()
            assertThat(oppdatertAndel.kildeBehandlingId).isEqualTo(behandling.id)
        }

        @Test
        fun `skal oppdatere tilkjent ytelse med beregnet utbetalingsoppdrag ved opphør`() {
            // Arrange
            val behandling = lagBehandling()
            val fom = dagensDato.plusMonths(1).toYearMonth()
            val tom = dagensDato.plusMonths(2).toYearMonth()
            val tilkjentYtelse =
                lagTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = null,
                    stønadTom = null,
                    opphørFom = null,
                    lagAndelerTilkjentYtelse = {
                        setOf(
                            lagAndelTilkjentYtelse(
                                tilkjentYtelse = it,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                id = 1,
                                fom = fom,
                                tom = tom,
                            ),
                            lagAndelTilkjentYtelse(
                                tilkjentYtelse = it,
                                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                id = 2,
                                fom = fom,
                                tom = tom,
                            ),
                        )
                    },
                )
            val beregnetUtbetalingsoppdragLongId =
                lagBeregnetUtbetalingsoppdragLongId(
                    listOf(
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                            fom = fom.førsteDagIInneværendeMåned(),
                            tom = tom.plusMonths(1).sisteDagIInneværendeMåned(),
                            opphør = Opphør(opphørDatoFom = tom.plusMonths(1).førsteDagIInneværendeMåned()),
                        ),
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 1,
                            forrigePeriodeId = null,
                            ytelseTypeBa = YtelsetypeBA.UTVIDET_BARNETRYGD,
                            fom = fom.førsteDagIInneværendeMåned(),
                            tom = tom.sisteDagIInneværendeMåned(),
                        ),
                    ),
                    listOf(
                        lagAndelMedPeriodeIdLong(
                            id = 1,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            kildeBehandlingId = behandling.id,
                        ),
                        lagAndelMedPeriodeIdLong(
                            id = 2,
                            periodeId = 1,
                            forrigePeriodeId = null,
                            kildeBehandlingId = behandling.id,
                        ),
                    ),
                )

            every { endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandling.id) } returns emptyList()
            every { tilkjentYtelseRepository.save(any()) } returns tilkjentYtelse

            // Act
            oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(tilkjentYtelse, beregnetUtbetalingsoppdragLongId)

            // Assert
            assertThat(tilkjentYtelse.utbetalingsoppdrag).isNotNull()
            assertThat(tilkjentYtelse.stønadFom).isEqualTo(fom)
            assertThat(tilkjentYtelse.stønadTom).isEqualTo(tom)
            assertThat(tilkjentYtelse.endretDato).isEqualTo(dagensDato)
            assertThat(tilkjentYtelse.opphørFom).isNull()

            val andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag = tilkjentYtelse.andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }
            assertThat(andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag).hasSize(2)
            val førsteOppdaterteAndel = andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag.first()
            val andreOppdaterteAndel = andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag.last()
            assertThat(førsteOppdaterteAndel.periodeOffset).isEqualTo(0)
            assertThat(andreOppdaterteAndel.periodeOffset).isEqualTo(1)
            andelerSomSkalOppdateresMedDataFraBeregnetUtbetalingsoppdrag.forEach {
                assertThat(it.forrigePeriodeOffset).isNull()
                assertThat(it.kildeBehandlingId).isEqualTo(behandling.id)
            }
        }

        @Test
        fun `skal kaste feil dersom antall andeler med utbetaling ikke stemmer overens med antall andeler med periode id i beregnet utbetalingsoppdrag`() {
            // Arrange
            val behandling = lagBehandling()
            val fom = dagensDato.plusMonths(1).toYearMonth()
            val tom = dagensDato.plusMonths(2).toYearMonth()
            val tilkjentYtelse =
                lagTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = null,
                    stønadTom = null,
                    opphørFom = null,
                    lagAndelerTilkjentYtelse = {
                        setOf(
                            lagAndelTilkjentYtelse(
                                tilkjentYtelse = it,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                id = 1,
                                fom = fom,
                                tom = tom,
                            ),
                            lagAndelTilkjentYtelse(
                                tilkjentYtelse = it,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                id = 2,
                                forrigeperiodeIdOffset = 1,
                                fom = fom.plusMonths(1),
                                tom = tom.plusMonths(4),
                            ),
                        )
                    },
                )
            val beregnetUtbetalingsoppdragLongId =
                lagBeregnetUtbetalingsoppdragLongId(
                    listOf(
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                            fom = fom.førsteDagIInneværendeMåned(),
                            tom = tom.sisteDagIInneværendeMåned(),
                        ),
                    ),
                    listOf(
                        lagAndelMedPeriodeIdLong(
                            id = 1,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            kildeBehandlingId = behandling.id,
                        ),
                    ),
                )

            every { endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandling.id) } returns emptyList()
            every { tilkjentYtelseRepository.save(any()) } returns tilkjentYtelse

            // Act & Assert
            val exception = assertThrows<Feil> { oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(tilkjentYtelse, beregnetUtbetalingsoppdragLongId) }

            assertThat(exception.message).isEqualTo("Antallet andeler med oppdatert periodeOffset, forrigePeriodeOffset og kildeBehandlingId fra ny generator skal være likt antallet andeler med kalkulertUtbetalingsbeløp != 0. Generator gir 1 andeler men det er 2 andeler med kalkulertUtbetalingsbeløp != 0")
        }

        @Test
        fun `skal kaste feil dersom vi ikke finner andel med periode id i beregnet utbetalingsoppdrag som matcher andel med utbetaling`() {
            // Arrange
            val behandling = lagBehandling()
            val fom = dagensDato.plusMonths(1).toYearMonth()
            val tom = dagensDato.plusMonths(2).toYearMonth()
            val tilkjentYtelse =
                lagTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = null,
                    stønadTom = null,
                    opphørFom = null,
                    lagAndelerTilkjentYtelse = {
                        setOf(
                            lagAndelTilkjentYtelse(
                                tilkjentYtelse = it,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                id = 1,
                                fom = fom,
                                tom = tom,
                            ),
                            lagAndelTilkjentYtelse(
                                tilkjentYtelse = it,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                                id = 2,
                                forrigeperiodeIdOffset = 1,
                                fom = fom.plusMonths(1),
                                tom = tom.plusMonths(4),
                            ),
                        )
                    },
                )
            val beregnetUtbetalingsoppdragLongId =
                lagBeregnetUtbetalingsoppdragLongId(
                    listOf(
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                            fom = fom.førsteDagIInneværendeMåned(),
                            tom = tom.sisteDagIInneværendeMåned(),
                        ),
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 1,
                            forrigePeriodeId = 0,
                            ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                            fom = fom.førsteDagIInneværendeMåned(),
                            tom = tom.sisteDagIInneværendeMåned(),
                        ),
                    ),
                    listOf(
                        lagAndelMedPeriodeIdLong(
                            id = 1,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            kildeBehandlingId = behandling.id,
                        ),
                        // Følgende andel med periode id, har ulik id enn andelen den skal representere
                        lagAndelMedPeriodeIdLong(
                            id = 3,
                            periodeId = 1,
                            forrigePeriodeId = 0,
                            kildeBehandlingId = behandling.id,
                        ),
                    ),
                )

            every { endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandling.id) } returns emptyList()
            every { tilkjentYtelseRepository.save(any()) } returns tilkjentYtelse

            // Act & Assert
            val exception = assertThrows<Feil> { oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(tilkjentYtelse, beregnetUtbetalingsoppdragLongId) }

            assertThat(exception.message).isEqualTo("Feil ved oppdaterig av offset på andeler. Finner ikke andel med id 2 blandt andelene med oppdatert offset fra ny generator. Ny generator returnerer andeler med ider [1, 3]")
        }
    }

    @Nested
    inner class OppdaterTilkjentYtelseMedKorrigerteAndeler {
        @Test
        fun `skal fjerne andeler med feil og legge til korrigerte andeler`() {
            // Arrange
            val andelTilkjentYtelse1 = lagAndelTilkjentYtelse(id = 1, fom = YearMonth.of(2023, 2), tom = YearMonth.of(2024, 7), periodeIdOffset = 0, forrigeperiodeIdOffset = null, kildeBehandlingId = 0)
            val andelTilkjentYtelse2 = lagAndelTilkjentYtelse(id = 2, fom = YearMonth.of(2024, 8), tom = YearMonth.of(2035, 3), periodeIdOffset = 1, forrigeperiodeIdOffset = 0, kildeBehandlingId = 0)

            val tilkjentYtelse = lagTilkjentYtelse(lagAndelerTilkjentYtelse = { setOf(andelTilkjentYtelse1, andelTilkjentYtelse2) })

            val andelTilkjentYtels1Korrigert = andelTilkjentYtelse1.copy(id = 0, periodeOffset = 2, forrigePeriodeOffset = 1, kildeBehandlingId = 1)
            val andelTilkjentYtels2Korrigert = andelTilkjentYtelse2.copy(id = 0, periodeOffset = 3, forrigePeriodeOffset = 2, kildeBehandlingId = 1)

            val andelTilkjentYtelseKorreksjoner =
                listOf(
                    AndelTilkjentYtelseKorreksjon(
                        andelMedFeil = andelTilkjentYtelse1,
                        korrigertAndel = andelTilkjentYtels1Korrigert,
                    ),
                    AndelTilkjentYtelseKorreksjon(
                        andelMedFeil = andelTilkjentYtelse2,
                        korrigertAndel = andelTilkjentYtels2Korrigert,
                    ),
                )

            every { patchetAndelTilkjentYtelseRepository.saveAll(any<List<PatchetAndelTilkjentYtelse>>()) } answers { firstArg() }

            // Act
            oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedKorrigerteAndeler(tilkjentYtelse = tilkjentYtelse, andelTilkjentYtelseKorreksjoner = andelTilkjentYtelseKorreksjoner)

            // Assert

            verify(exactly = 1) { patchetAndelTilkjentYtelseRepository.saveAll(any<List<PatchetAndelTilkjentYtelse>>()) }

            assertThat(tilkjentYtelse.andelerTilkjentYtelse).hasSize(2)
            assertThat(tilkjentYtelse.andelerTilkjentYtelse.map { it.periodeOffset }).containsExactlyInAnyOrder(3, 2)
            assertThat(tilkjentYtelse.andelerTilkjentYtelse.map { it.forrigePeriodeOffset }).containsExactlyInAnyOrder(2, 1)
            assertThat(tilkjentYtelse.andelerTilkjentYtelse.map { it.kildeBehandlingId }.toSet()).containsExactlyInAnyOrder(1)
        }

        @Test
        fun `skal kaste feil dersom den samme andelen er korrigert flere ganger`() {
            // Arrange
            val andelTilkjentYtelse1 = lagAndelTilkjentYtelse(id = 1, fom = YearMonth.of(2023, 2), tom = YearMonth.of(2024, 7), periodeIdOffset = 0, forrigeperiodeIdOffset = null, kildeBehandlingId = 0)

            val tilkjentYtelse = lagTilkjentYtelse(lagAndelerTilkjentYtelse = { setOf(andelTilkjentYtelse1) })

            val andelTilkjentYtels1Korrigert = andelTilkjentYtelse1.copy(id = 0, periodeOffset = 2, forrigePeriodeOffset = 1, kildeBehandlingId = 1)
            val andelTilkjentYtels1Korrigert2 = andelTilkjentYtelse1.copy(id = 0, periodeOffset = 3, forrigePeriodeOffset = 2, kildeBehandlingId = 1)

            val andelTilkjentYtelseKorreksjoner =
                listOf(
                    AndelTilkjentYtelseKorreksjon(
                        andelMedFeil = andelTilkjentYtelse1,
                        korrigertAndel = andelTilkjentYtels1Korrigert,
                    ),
                    AndelTilkjentYtelseKorreksjon(
                        andelMedFeil = andelTilkjentYtelse1,
                        korrigertAndel = andelTilkjentYtels1Korrigert2,
                    ),
                )

            // Act & Assert
            val feil = assertThrows<Feil> { oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedKorrigerteAndeler(tilkjentYtelse = tilkjentYtelse, andelTilkjentYtelseKorreksjoner = andelTilkjentYtelseKorreksjoner) }

            assertThat(feil.message).isEqualTo("Den samme andelen forekommer flere ganger blant andelene som er markert for sletting. Dette betyr at det finnes en splitt i utbetalingsoppdragene oversendt til Oppdrag som ikke eksisterer i andelene.")
        }
    }
}
