package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class SisteUtvidetAndelOverstyrerTest {
    @Test
    fun `skal oppdatere fom for siste utvidet andel til siste utvidet kjedelement sendt til Oppdrag og la andre siste andeler stå uendret`() {
        // Arrange
        val sisteAndelPerKjede =
            mapOf(
                IdentOgType("123", YtelsetypeBA.UTVIDET_BARNETRYGD) to
                    lagAndelTilkjentYtelse(
                        fom =
                            YearMonth.of(
                                2024,
                                5,
                            ),
                        tom = YearMonth.of(2033, 8),
                        periodeIdOffset = 2,
                    ),
                IdentOgType("456", YtelsetypeBA.ORDINÆR_BARNETRYGD) to
                    lagAndelTilkjentYtelse(
                        fom =
                            YearMonth.of(
                                2024,
                                1,
                            ),
                        tom = YearMonth.of(2033, 8),
                        periodeIdOffset = 1,
                    ),
            )

        val tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag =
            listOf(
                lagTilkjentYtelse().also {
                    it.utbetalingsoppdrag =
                        objectMapper.writeValueAsString(
                            lagUtbetalingsoppdrag(
                                utbetalingsperioder =
                                    listOf(
                                        lagUtbetalingsperiode(
                                            behandlingId = 1,
                                            periodeId = 0,
                                            forrigePeriodeId = null,
                                            ytelseTypeBa = YtelsetypeBA.UTVIDET_BARNETRYGD,
                                            fom = LocalDate.of(2024, 5, 1),
                                            tom = LocalDate.of(2033, 8, 31),
                                            opphør = null,
                                        ),
                                        lagUtbetalingsperiode(
                                            behandlingId = 1,
                                            periodeId = 1,
                                            forrigePeriodeId = null,
                                            ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                                            fom = LocalDate.of(2024, 1, 1),
                                            tom = LocalDate.of(2033, 8, 31),
                                            opphør = null,
                                        ),
                                    ),
                            ),
                        )
                },
                lagTilkjentYtelse().also {
                    it.utbetalingsoppdrag =
                        objectMapper.writeValueAsString(
                            lagUtbetalingsoppdrag(
                                utbetalingsperioder =
                                    listOf(
                                        lagUtbetalingsperiode(
                                            behandlingId = 1,
                                            periodeId = 2,
                                            forrigePeriodeId = 0,
                                            ytelseTypeBa = YtelsetypeBA.UTVIDET_BARNETRYGD,
                                            fom = LocalDate.of(2025, 1, 1),
                                            tom = LocalDate.of(2033, 8, 31),
                                            opphør = null,
                                        ),
                                    ),
                            ),
                        )
                },
            )

        // Act
        val sisteAndelPerKjedeMedOverstyrtUtvidetAndel =
            SisteUtvidetAndelOverstyrer.overstyrSisteUtvidetBarnetrygdAndel(
                sisteAndelPerKjede = sisteAndelPerKjede,
                tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag = tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag,
                skalBrukeNyKlassekodeForUtvidetBarnetrygd = true,
            )

        // Assert
        val sisteUtvidetAndelDataLongId = sisteAndelPerKjedeMedOverstyrtUtvidetAndel.entries.single { it.key.type == YtelsetypeBA.UTVIDET_BARNETRYGD }.value
        assertThat(sisteUtvidetAndelDataLongId.fom).isEqualTo(YearMonth.of(2025, 1))

        val sisteOrdinærAndelDataLongId = sisteAndelPerKjedeMedOverstyrtUtvidetAndel.entries.single { it.key.type == YtelsetypeBA.ORDINÆR_BARNETRYGD }.value
        assertThat(sisteOrdinærAndelDataLongId).isEqualTo(
            sisteAndelPerKjede.entries
                .single { it.key.type == YtelsetypeBA.ORDINÆR_BARNETRYGD }
                .value
                .tilAndelDataLongId(true),
        )
    }

    @Test
    fun `skal ikke oppdatere fom for siste utvidet andel dersom fagsak ikke er over på ny klassekode for utvidet barnetrygd`() {
        // Arrange
        val sisteAndelPerKjede =
            mapOf(
                IdentOgType("123", YtelsetypeBA.UTVIDET_BARNETRYGD) to
                    lagAndelTilkjentYtelse(
                        fom =
                            YearMonth.of(
                                2024,
                                5,
                            ),
                        tom = YearMonth.of(2033, 8),
                        periodeIdOffset = 2,
                    ),
                IdentOgType("456", YtelsetypeBA.ORDINÆR_BARNETRYGD) to
                    lagAndelTilkjentYtelse(
                        fom =
                            YearMonth.of(
                                2024,
                                1,
                            ),
                        tom = YearMonth.of(2033, 8),
                        periodeIdOffset = 1,
                    ),
            )

        // Act
        val sisteAndelPerKjedeMedOverstyrtUtvidetAndel =
            SisteUtvidetAndelOverstyrer.overstyrSisteUtvidetBarnetrygdAndel(
                sisteAndelPerKjede = sisteAndelPerKjede,
                tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag = emptyList(),
                skalBrukeNyKlassekodeForUtvidetBarnetrygd = true,
            )

        // Assert
        assertThat(sisteAndelPerKjedeMedOverstyrtUtvidetAndel).isEqualTo(sisteAndelPerKjede.mapValues { it.value.tilAndelDataLongId(true) })
    }

    @Test
    fun `skal ikke oppdatere fom for siste utvidet andel dersom det ikke er noen forskjell sammenlignet med sist oversendte kjedelement for utvidet`() {
        // Arrange
        val sisteAndelPerKjede =
            mapOf(
                IdentOgType("123", YtelsetypeBA.UTVIDET_BARNETRYGD) to
                    lagAndelTilkjentYtelse(
                        fom =
                            YearMonth.of(
                                2024,
                                5,
                            ),
                        tom = YearMonth.of(2033, 8),
                        periodeIdOffset = 0,
                    ),
                IdentOgType("456", YtelsetypeBA.ORDINÆR_BARNETRYGD) to
                    lagAndelTilkjentYtelse(
                        fom =
                            YearMonth.of(
                                2024,
                                1,
                            ),
                        tom = YearMonth.of(2033, 8),
                        periodeIdOffset = 1,
                    ),
            )

        val tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag =
            listOf(
                lagTilkjentYtelse().also {
                    it.utbetalingsoppdrag =
                        objectMapper.writeValueAsString(
                            lagUtbetalingsoppdrag(
                                utbetalingsperioder =
                                    listOf(
                                        lagUtbetalingsperiode(
                                            behandlingId = 1,
                                            periodeId = 0,
                                            forrigePeriodeId = null,
                                            ytelseTypeBa = YtelsetypeBA.UTVIDET_BARNETRYGD,
                                            fom = LocalDate.of(2024, 5, 1),
                                            tom = LocalDate.of(2033, 8, 31),
                                            opphør = null,
                                        ),
                                        lagUtbetalingsperiode(
                                            behandlingId = 1,
                                            periodeId = 1,
                                            forrigePeriodeId = null,
                                            ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                                            fom = LocalDate.of(2024, 1, 1),
                                            tom = LocalDate.of(2033, 8, 31),
                                            opphør = null,
                                        ),
                                    ),
                            ),
                        )
                },
            )

        // Act
        val sisteAndelPerKjedeMedOverstyrtUtvidetAndel =
            SisteUtvidetAndelOverstyrer.overstyrSisteUtvidetBarnetrygdAndel(
                sisteAndelPerKjede = sisteAndelPerKjede,
                tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag = tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag,
                skalBrukeNyKlassekodeForUtvidetBarnetrygd = true,
            )

        // Assert
        assertThat(sisteAndelPerKjedeMedOverstyrtUtvidetAndel).isEqualTo(sisteAndelPerKjede.mapValues { it.value.tilAndelDataLongId(true) })
    }

    @Test
    fun `skal kaste NoSuchElementException dersom periodeId til utvidet barnetrygd kjedelement oversendt til oppdrag er ulik periodeIdOffset vi har for andelen`() {
        // Arrange
        val sisteAndelPerKjede =
            mapOf(
                IdentOgType("123", YtelsetypeBA.UTVIDET_BARNETRYGD) to
                    lagAndelTilkjentYtelse(
                        fom =
                            YearMonth.of(
                                2024,
                                5,
                            ),
                        tom = YearMonth.of(2033, 8),
                        periodeIdOffset = 0,
                    ),
                IdentOgType("456", YtelsetypeBA.ORDINÆR_BARNETRYGD) to
                    lagAndelTilkjentYtelse(
                        fom =
                            YearMonth.of(
                                2024,
                                1,
                            ),
                        tom = YearMonth.of(2033, 8),
                        periodeIdOffset = 1,
                    ),
            )

        val tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag =
            listOf(
                lagTilkjentYtelse().also {
                    it.utbetalingsoppdrag =
                        objectMapper.writeValueAsString(
                            lagUtbetalingsoppdrag(
                                utbetalingsperioder =
                                    listOf(
                                        lagUtbetalingsperiode(
                                            behandlingId = 1,
                                            periodeId = 1,
                                            forrigePeriodeId = null,
                                            ytelseTypeBa = YtelsetypeBA.UTVIDET_BARNETRYGD,
                                            fom = LocalDate.of(2024, 5, 1),
                                            tom = LocalDate.of(2033, 8, 31),
                                            opphør = null,
                                        ),
                                        lagUtbetalingsperiode(
                                            behandlingId = 1,
                                            periodeId = 0,
                                            forrigePeriodeId = null,
                                            ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                                            fom = LocalDate.of(2024, 1, 1),
                                            tom = LocalDate.of(2033, 8, 31),
                                            opphør = null,
                                        ),
                                    ),
                            ),
                        )
                },
            )

        // Act & Assert
        assertThrows<NoSuchElementException> {
            SisteUtvidetAndelOverstyrer.overstyrSisteUtvidetBarnetrygdAndel(
                sisteAndelPerKjede = sisteAndelPerKjede,
                tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag = tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag,
                skalBrukeNyKlassekodeForUtvidetBarnetrygd = true,
            )
        }
    }
}
