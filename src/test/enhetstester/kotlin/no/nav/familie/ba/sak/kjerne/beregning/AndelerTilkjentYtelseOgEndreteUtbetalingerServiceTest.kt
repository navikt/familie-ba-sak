package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import lagAndelTilkjentYtelse
import lagBehandling
import lagEndretUtbetalingAndel
import lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.YearMonth

class AndelerTilkjentYtelseOgEndreteUtbetalingerServiceTest {
    val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    val endretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()

    val andelerTilkjentYtelseOgEndreteUtbetalingerService =
        AndelerTilkjentYtelseOgEndreteUtbetalingerService(
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            endretUtbetalingAndelRepository = endretUtbetalingAndelRepository,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        )

    @BeforeEach
    fun setup() {
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(any()) } returns null
    }

    @Nested
    inner class FinnEndreteUtbetalingerMedAndelerTilkjentYtelse {
        @ParameterizedTest
        @EnumSource(value = BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING"])
        fun `For behandling med årsak satsendring og månedlig valutajustering blir ikke endrete utbetalinger filtrert bort`(
            årsak: BehandlingÅrsak,
        ) {
            val behandling =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = årsak,
                )

            val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

            every { behandlingHentOgPersisterService.hent(any()) } returns behandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns emptyList()
            every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } returns listOf(endretUtbetalingAndel)

            val endreteUtbetalinger = andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id)

            assertThat(endreteUtbetalinger).hasSize(1)
        }

        @Test
        fun `For behandling med årsak nye opplysinger blir endrete utbetalinger uten overlappende andeler filtrert bort`() {
            val behandling =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                )

            val aktør = lagPerson()

            every { behandlingHentOgPersisterService.hent(any()) } returns behandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        person = aktør,
                        fom = YearMonth.now().minusMonths(2),
                        tom = YearMonth.now().minusMonths(1),
                    ),
                )
            every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } returns
                listOf(
                    lagEndretUtbetalingAndel(
                        person = aktør,
                        fom = YearMonth.now().minusMonths(3),
                        tom = YearMonth.now().minusMonths(2),
                        årsak = Årsak.ENDRE_MOTTAKER,
                    ),
                )

            val endreteUtbetalinger = andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id)

            assertThat(endreteUtbetalinger)
                .singleElement()
                .extracting { it.andelerTilkjentYtelse }
                .satisfies({ assertThat(it).isEmpty() })
        }

        @Test
        fun `For behandling med årsak nye opplsyninger blir endrete utbetalinger med overlappende andeler kombinert`() {
            val behandling =
                lagBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                )

            val aktør = lagPerson()

            every { behandlingHentOgPersisterService.hent(any()) } returns behandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        person = aktør,
                        fom = YearMonth.now().minusMonths(3),
                        tom = YearMonth.now().minusMonths(2),
                    ),
                    lagAndelTilkjentYtelse(
                        person = aktør,
                        fom = YearMonth.now().minusMonths(2),
                        tom = YearMonth.now().minusMonths(1),
                    ),
                )
            every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } returns
                listOf(
                    lagEndretUtbetalingAndel(
                        person = aktør,
                        fom = YearMonth.now().minusMonths(3),
                        tom = YearMonth.now().minusMonths(1),
                        årsak = Årsak.ENDRE_MOTTAKER,
                    ),
                )

            val endreteUtbetalinger = andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id)

            assertThat(endreteUtbetalinger)
                .singleElement()
                .extracting { it.andelerTilkjentYtelse }
                .satisfies({
                    assertThat(it)
                        .hasSize(2)
                        .anySatisfy {
                            assertThat(it.stønadFom).isEqualTo(YearMonth.now().minusMonths(3))
                            assertThat(it.stønadTom).isEqualTo(YearMonth.now().minusMonths(2))
                        }.anySatisfy {
                            assertThat(it.stønadFom).isEqualTo(YearMonth.now().minusMonths(2))
                            assertThat(it.stønadTom).isEqualTo(YearMonth.now().minusMonths(1))
                        }
                })
        }
    }
}
