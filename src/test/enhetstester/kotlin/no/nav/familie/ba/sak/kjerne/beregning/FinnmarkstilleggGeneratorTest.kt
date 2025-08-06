package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.OPPFYLT
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class FinnmarkstilleggGeneratorTest {
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
    fun `skal generere finnmarkstillegg fra og med måneden etter søker og barn flytter til Finnmark`(
        dag: Int,
    ) {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    personResultat(
                        person = søker,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 10, dag) to null),
                        vilkårsvurdering = it,
                    ),
                    personResultat(
                        person = barn1,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 10, dag) to null),
                        vilkårsvurdering = it,
                    ),
                )
            }

        // Act
        val finnmarkstilleggAndeler =
            FinnmarkstilleggGenerator.lagFinnmarkstilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        val finnmarkstilleggAndel = finnmarkstilleggAndeler.single()
        assertThat(finnmarkstilleggAndel.stønadFom).isEqualTo(YearMonth.of(2025, 11))
        assertThat(finnmarkstilleggAndel.stønadTom).isEqualTo(YearMonth.of(2025, 12))
    }

    @Test
    fun `skal tidligst generere finnmarkstillegg fra og med oktober 2025`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    personResultat(
                        person = søker,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 1, 1) to null),
                        vilkårsvurdering = it,
                    ),
                    personResultat(
                        person = barn1,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 1, 1) to null),
                        vilkårsvurdering = it,
                    ),
                )
            }

        // Act
        val finnmarkstilleggAndeler =
            FinnmarkstilleggGenerator.lagFinnmarkstilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        val finnmarkstilleggAndel = finnmarkstilleggAndeler.single()
        assertThat(finnmarkstilleggAndel.stønadFom).isEqualTo(YearMonth.of(2025, 10))
        assertThat(finnmarkstilleggAndel.stønadTom).isEqualTo(YearMonth.of(2025, 12))
    }

    @Test
    fun `skal bare generere finnmarkstillegg for barn som bor i Finnmark`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    personResultat(
                        person = søker,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                    ),
                    personResultat(
                        person = barn1,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                    ),
                    personResultat(
                        person = barn2,
                        bosattIFinnmarkPerioder = emptyList(),
                        vilkårsvurdering = it,
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
        val finnmarkstilleggAndeler =
            FinnmarkstilleggGenerator.lagFinnmarkstilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        val finnmarkstilleggAndel = finnmarkstilleggAndeler.single()
        assertThat(finnmarkstilleggAndel.aktør).isEqualTo(barn1.aktør)
    }

    @Test
    fun `skal generere finnmarkstillegg med samme prosent som ordinær andel`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    personResultat(
                        person = søker,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                    ),
                    personResultat(
                        person = barn1,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
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
        val finnmarkstilleggAndeler =
            FinnmarkstilleggGenerator.lagFinnmarkstilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        val finnmarkstilleggAndel = finnmarkstilleggAndeler.single()
        assertThat(finnmarkstilleggAndel.prosent).isEqualTo(BigDecimal(50))
    }

    @Test
    fun `skal ikke generere finnmarkstillegg hvis ordinær andel er satt til 0 prosent`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    personResultat(
                        person = søker,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                    ),
                    personResultat(
                        person = barn1,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
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
        val finnmarkstilleggAndeler =
            FinnmarkstilleggGenerator.lagFinnmarkstilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(finnmarkstilleggAndeler).isEmpty()
    }

    @Test
    fun `skal ikke generere finnmarkstillegg hvis søker ikke bor i Finnmark`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    personResultat(
                        person = søker,
                        bosattIFinnmarkPerioder = emptyList(),
                        vilkårsvurdering = it,
                    ),
                    personResultat(
                        person = barn1,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                    ),
                )
            }

        // Act
        val finnmarkstilleggAndeler =
            FinnmarkstilleggGenerator.lagFinnmarkstilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(finnmarkstilleggAndeler).isEmpty()
    }

    @Test
    fun `skal ikke generere finnmarkstillegg hvis barn ikke bor i Finnmark`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    personResultat(
                        person = søker,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                    ),
                    personResultat(
                        person = barn1,
                        bosattIFinnmarkPerioder = emptyList(),
                        vilkårsvurdering = it,
                    ),
                )
            }

        // Act
        val finnmarkstilleggAndeler =
            FinnmarkstilleggGenerator.lagFinnmarkstilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(finnmarkstilleggAndeler).isEmpty()
    }

    @Test
    fun `skal ikke generere finnmarkstillegg hvis det ikke eksisterer andeler i periode`() {
        // Arrange
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    personResultat(
                        person = søker,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                    ),
                    personResultat(
                        person = barn1,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                    ),
                )
            }

        // Act
        val finnmarkstilleggAndeler =
            FinnmarkstilleggGenerator.lagFinnmarkstilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = emptyList(),
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(finnmarkstilleggAndeler).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(FagsakType::class, names = ["INSTITUSJON", "BARN_ENSLIG_MINDREÅRIG"])
    fun `skal generere finnmarkstillegg for institusjon og enslig mindreårig`(
        fagsakType: FagsakType,
    ) {
        // Arrange
        val behandling = lagBehandling(fagsak = lagFagsak(type = fagsakType))
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) {
                setOf(
                    personResultat(
                        person = barn1,
                        bosattIFinnmarkPerioder = listOf(LocalDate.of(2025, 10, 1) to null),
                        vilkårsvurdering = it,
                    ),
                )
            }

        // Act
        val finnmarkstilleggAndeler =
            FinnmarkstilleggGenerator.lagFinnmarkstilleggAndeler(
                behandling = behandling,
                vilkårsvurdering = vilkårsvurdering,
                barnasAndeler = barnasAndeler,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        val finnmarkstilleggAndel = finnmarkstilleggAndeler.single()
        assertThat(finnmarkstilleggAndel.stønadFom).isEqualTo(YearMonth.of(2025, 11))
        assertThat(finnmarkstilleggAndel.stønadTom).isEqualTo(YearMonth.of(2025, 12))
    }

    private fun personResultat(
        person: Person,
        bosattIFinnmarkPerioder: List<Pair<LocalDate, LocalDate?>>,
        vilkårsvurdering: Vilkårsvurdering,
    ): PersonResultat =
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = person.aktør,
            lagVilkårResultater = { personResultat ->
                setOfNotNull(
                    *bosattIFinnmarkPerioder
                        .map {
                            lagVilkårResultat(
                                personResultat = personResultat,
                                vilkårType = BOSATT_I_RIKET,
                                resultat = OPPFYLT,
                                periodeFom = it.first,
                                periodeTom = it.second,
                                utdypendeVilkårsvurderinger = listOf(BOSATT_I_FINNMARK_NORD_TROMS),
                                behandlingId = behandling.id,
                            )
                        }.toTypedArray(),
                    if (person.type == PersonType.BARN) {
                        lagVilkårResultat(
                            personResultat = personResultat,
                            vilkårType = UNDER_18_ÅR,
                            resultat = OPPFYLT,
                            periodeFom = person.fødselsdato,
                            periodeTom = person.fødselsdato.plusYears(18),
                            behandlingId = behandling.id,
                        )
                    } else {
                        null
                    },
                )
            },
        )
}
