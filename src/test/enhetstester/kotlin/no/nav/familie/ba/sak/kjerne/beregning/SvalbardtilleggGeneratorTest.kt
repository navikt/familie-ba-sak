package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class SvalbardtilleggGeneratorTest {
    private val søker = lagPerson(type = PersonType.SØKER)
    private val barn1 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2020, 1, 1))
    private val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2021, 1, 1))

    private val behandling = lagBehandling()
    private val tilkjentYtelse =
        TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
        )

    private val barnasAndeler =
        listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                person = barn1,
                fom = YearMonth.of(2025, 1),
                tom = YearMonth.of(2025, 12),
                ytelseType = ORDINÆR_BARNETRYGD,
            ),
        )

    @ParameterizedTest
    @ValueSource(ints = [1, 15, 31])
    fun `skal generere svalbardtillegg fra og med måneden etter søker og barn flytter til Svalbard`(
        dag: Int,
    ) {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = søker,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, dag) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = barn1,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, dag) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                )
            }

        // Act
        val svalbardtilleggAndeler =
            SvalbardtilleggGenerator.lagSvalbardtilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        val svalbardtilleggAndel = svalbardtilleggAndeler.single()
        assertThat(svalbardtilleggAndel.stønadFom).isEqualTo(YearMonth.of(2025, 11))
        assertThat(svalbardtilleggAndel.stønadTom).isEqualTo(YearMonth.of(2025, 12))
        assertThat(svalbardtilleggAndel.type).isEqualTo(YtelseType.SVALBARDTILLEGG)
    }

    @Test
    fun `skal tidligst generere svalbardtillegg fra og med oktober 2025`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = søker,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 1, 1) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = barn1,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 1, 1) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                )
            }

        // Act
        val svalbardtilleggAndeler =
            SvalbardtilleggGenerator.lagSvalbardtilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        val svalbardtilleggAndel = svalbardtilleggAndeler.single()

        // TODO: Endre tilbake til oktober 2025 før vi går live
        assertThat(svalbardtilleggAndel.stønadFom).isEqualTo(YearMonth.of(2025, 8))
        assertThat(svalbardtilleggAndel.stønadTom).isEqualTo(YearMonth.of(2025, 12))
        assertThat(svalbardtilleggAndel.type).isEqualTo(YtelseType.SVALBARDTILLEGG)
    }

    @Test
    fun `skal bare generere svalbardtillegg for barn som bor i Svalbard`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = søker,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = barn1,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = barn2,
                        perioderMedUtdypendeVilkårsvurdering = emptyList(),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                )
            }

        val barnasAndeler =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    person = barn1,
                    fom = YearMonth.of(2025, 10),
                    tom = YearMonth.of(2025, 12),
                    ytelseType = ORDINÆR_BARNETRYGD,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    person = barn2,
                    fom = YearMonth.of(2025, 10),
                    tom = YearMonth.of(2025, 12),
                    ytelseType = ORDINÆR_BARNETRYGD,
                ),
            )

        // Act
        val svalbardtilleggAndeler =
            SvalbardtilleggGenerator.lagSvalbardtilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        val svalbardtilleggAndel = svalbardtilleggAndeler.single()
        assertThat(svalbardtilleggAndel.aktør).isEqualTo(barn1.aktør)
        assertThat(svalbardtilleggAndel.type).isEqualTo(YtelseType.SVALBARDTILLEGG)
    }

    @Test
    fun `skal generere svalbardtillegg med samme prosent som ordinær andel`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = søker,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = barn1,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                )
            }

        val barnasAndeler =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    person = barn1,
                    fom = YearMonth.of(2025, 1),
                    tom = YearMonth.of(2025, 12),
                    ytelseType = ORDINÆR_BARNETRYGD,
                    prosent = BigDecimal(50),
                ),
            )

        // Act
        val svalbardtilleggAndeler =
            SvalbardtilleggGenerator.lagSvalbardtilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        val svalbardtilleggAndel = svalbardtilleggAndeler.single()
        assertThat(svalbardtilleggAndel.prosent).isEqualTo(BigDecimal(50))
        assertThat(svalbardtilleggAndel.type).isEqualTo(YtelseType.SVALBARDTILLEGG)
    }

    @Test
    fun `skal ikke generere svalbardtillegg hvis ordinær andel er satt til 0 prosent`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = søker,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = barn1,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                )
            }

        val barnasAndeler =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    person = barn1,
                    fom = YearMonth.of(2025, 1),
                    tom = YearMonth.of(2025, 12),
                    ytelseType = ORDINÆR_BARNETRYGD,
                    prosent = BigDecimal.ZERO,
                ),
            )

        // Act
        val svalbardtilleggAndeler =
            SvalbardtilleggGenerator.lagSvalbardtilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(svalbardtilleggAndeler).isEmpty()
    }

    @Test
    fun `skal ikke generere svalbardtillegg hvis søker ikke bor i Svalbard`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = søker,
                        perioderMedUtdypendeVilkårsvurdering = emptyList(),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = barn1,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                )
            }

        // Act
        val svalbardtilleggAndeler =
            SvalbardtilleggGenerator.lagSvalbardtilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(svalbardtilleggAndeler).isEmpty()
    }

    @Test
    fun `skal ikke generere svalbardtillegg hvis barn ikke bor i Svalbard`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = søker,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = barn1,
                        perioderMedUtdypendeVilkårsvurdering = emptyList(),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                )
            }

        // Act
        val svalbardtilleggAndeler =
            SvalbardtilleggGenerator.lagSvalbardtilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(svalbardtilleggAndeler).isEmpty()
    }

    @Test
    fun `skal ikke generere svalbardtillegg hvis det ikke eksisterer andeler i periode`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = søker,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = barn1,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                )
            }

        // Act
        val svalbardtilleggAndeler =
            SvalbardtilleggGenerator.lagSvalbardtilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = emptyList(),
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(svalbardtilleggAndeler).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(FagsakType::class, names = ["INSTITUSJON", "BARN_ENSLIG_MINDREÅRIG"])
    fun `skal generere svalbardtillegg for institusjon og enslig mindreårig`(
        fagsakType: FagsakType,
    ) {
        // Arrange
        val behandling = lagBehandling(fagsak = lagFagsak(type = fagsakType))
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                        behandling = behandling,
                        person = barn1,
                        perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                        utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD,
                    ),
                )
            }

        // Act
        val svalbardtilleggAndeler =
            SvalbardtilleggGenerator.lagSvalbardtilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        val svalbardtilleggAndel = svalbardtilleggAndeler.single()
        assertThat(svalbardtilleggAndel.stønadFom).isEqualTo(YearMonth.of(2025, 11))
        assertThat(svalbardtilleggAndel.stønadTom).isEqualTo(YearMonth.of(2025, 12))
        assertThat(svalbardtilleggAndel.type).isEqualTo(YtelseType.SVALBARDTILLEGG)
    }
}
