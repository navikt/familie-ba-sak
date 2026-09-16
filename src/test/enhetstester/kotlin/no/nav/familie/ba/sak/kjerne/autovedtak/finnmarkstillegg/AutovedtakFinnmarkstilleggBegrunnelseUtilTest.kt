package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class AutovedtakFinnmarkstilleggBegrunnelseUtilTest {
    @Nested
    inner class FinnInnvilgedeOgReduserteFinnmarkstilleggPerioderTest {
        @Test
        fun `Skal returnere alle perioder der det har kommet nye finnmarkstillegg andeler`() {
            val barn = lagPerson(type = PersonType.BARN)
            val behandling = lagBehandling()

            val nåværendeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn,
                        behandling = behandling,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.FINNMARKSTILLEGG,
                    ),
                )

            val innvilgedeOgReduserteFinnmarkstilleggPerioder =
                finnInnvilgedeOgReduserteFinnmarkstilleggPerioder(
                    forrigeAndeler = emptyList(),
                    nåværendeAndeler = nåværendeAndeler,
                )

            val innvilgedePerioder = innvilgedeOgReduserteFinnmarkstilleggPerioder.first
            val redusertePerioder = innvilgedeOgReduserteFinnmarkstilleggPerioder.second

            assertThat(redusertePerioder).isEmpty()
            assertThat(innvilgedePerioder).hasSize(1)
            assertThat(innvilgedePerioder.single()).isEqualTo(YearMonth.of(2025, 11))
        }

        @Test
        fun `Skal differensiere mellom barn og returnere alle innvilgede perioder`() {
            val barn = lagPerson(type = PersonType.BARN)
            val barn2 = lagPerson(type = PersonType.BARN)
            val behandling = lagBehandling()

            val nåværendeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn,
                        behandling = behandling,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.FINNMARKSTILLEGG,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 10),
                        tom = YearMonth.of(2025, 12),
                        person = barn2,
                        behandling = behandling,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.FINNMARKSTILLEGG,
                    ),
                )

            val innvilgedeOgReduserteFinnmarkstilleggPerioder =
                finnInnvilgedeOgReduserteFinnmarkstilleggPerioder(
                    forrigeAndeler = emptyList(),
                    nåværendeAndeler = nåværendeAndeler,
                )

            val innvilgedePerioder = innvilgedeOgReduserteFinnmarkstilleggPerioder.first
            val redusertePerioder = innvilgedeOgReduserteFinnmarkstilleggPerioder.second

            assertThat(redusertePerioder).isEmpty()
            assertThat(innvilgedePerioder).hasSize(2)
            assertThat(innvilgedePerioder).anySatisfy { assertThat(it).isEqualTo(YearMonth.of(2025, 10)) }
            assertThat(innvilgedePerioder).anySatisfy { assertThat(it).isEqualTo(YearMonth.of(2025, 11)) }
        }

        @Test
        fun `Ved flere barn som har likt innvilgelsestidspunkt returneres bare 1 periode`() {
            val barn = lagPerson(type = PersonType.BARN)
            val barn2 = lagPerson(type = PersonType.BARN)
            val behandling = lagBehandling()

            val nåværendeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn,
                        behandling = behandling,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.FINNMARKSTILLEGG,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn2,
                        behandling = behandling,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.FINNMARKSTILLEGG,
                    ),
                )

            val innvilgedeOgReduserteFinnmarkstilleggPerioder =
                finnInnvilgedeOgReduserteFinnmarkstilleggPerioder(
                    forrigeAndeler = emptyList(),
                    nåværendeAndeler = nåværendeAndeler,
                )

            val innvilgedePerioder = innvilgedeOgReduserteFinnmarkstilleggPerioder.first
            val redusertePerioder = innvilgedeOgReduserteFinnmarkstilleggPerioder.second

            assertThat(redusertePerioder).isEmpty()
            assertThat(innvilgedePerioder).hasSize(1)
            assertThat(innvilgedePerioder.single()).isEqualTo(YearMonth.of(2025, 11))
        }

        @Test
        fun `Ved flere barn som både får og mister finnmarkstillegg for samme periode skal det dannes innvilget og reduksjonsperiode`() {
            val barn = lagPerson(type = PersonType.BARN)
            val barn2 = lagPerson(type = PersonType.BARN)
            val behandling = lagBehandling()
            val forrigeBehandling = lagBehandling()

            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn,
                        behandling = forrigeBehandling,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.FINNMARKSTILLEGG,
                    ),
                )

            val nåværendeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn2,
                        behandling = behandling,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.FINNMARKSTILLEGG,
                    ),
                )

            val innvilgedeOgReduserteFinnmarkstilleggPerioder =
                finnInnvilgedeOgReduserteFinnmarkstilleggPerioder(
                    forrigeAndeler = forrigeAndeler,
                    nåværendeAndeler = nåværendeAndeler,
                )

            val innvilgedePerioder = innvilgedeOgReduserteFinnmarkstilleggPerioder.first
            val redusertePerioder = innvilgedeOgReduserteFinnmarkstilleggPerioder.second

            assertThat(redusertePerioder).hasSize(1)
            assertThat(redusertePerioder.single()).isEqualTo(YearMonth.of(2025, 11))
            assertThat(innvilgedePerioder).hasSize(1)
            assertThat(innvilgedePerioder.single()).isEqualTo(YearMonth.of(2025, 11))
        }

        @Test
        fun `Ikke returner noe innvilgelsesperioder dersom andelene allerede fantes i forrige behandling`() {
            val barn = lagPerson(type = PersonType.BARN)
            val barn2 = lagPerson(type = PersonType.BARN)
            val behandling = lagBehandling()
            val behandling2 = lagBehandling()

            val nåværendeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn,
                        behandling = behandling,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.FINNMARKSTILLEGG,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn2,
                        behandling = behandling,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.FINNMARKSTILLEGG,
                    ),
                )

            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn,
                        behandling = behandling2,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.FINNMARKSTILLEGG,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn2,
                        behandling = behandling2,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.FINNMARKSTILLEGG,
                    ),
                )

            val innvilgedeOgReduserteFinnmarkstilleggPerioder =
                finnInnvilgedeOgReduserteFinnmarkstilleggPerioder(
                    forrigeAndeler = forrigeAndeler,
                    nåværendeAndeler = nåværendeAndeler,
                )

            val innvilgedePerioder = innvilgedeOgReduserteFinnmarkstilleggPerioder.first
            val redusertePerioder = innvilgedeOgReduserteFinnmarkstilleggPerioder.second

            assertThat(redusertePerioder).isEmpty()
            assertThat(innvilgedePerioder).isEmpty()
        }

        @Test
        fun `Skal returnere alle perioder der man har mistet finnmarkstillegg andeler siden forrige behandling`() {
            val barn = lagPerson(type = PersonType.BARN)
            val behandling = lagBehandling()

            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn,
                        behandling = behandling,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.FINNMARKSTILLEGG,
                    ),
                )

            val innvilgedeOgReduserteFinnmarkstilleggPerioder =
                finnInnvilgedeOgReduserteFinnmarkstilleggPerioder(
                    forrigeAndeler = forrigeAndeler,
                    nåværendeAndeler = emptyList(),
                )

            val innvilgedePerioder = innvilgedeOgReduserteFinnmarkstilleggPerioder.first
            val redusertePerioder = innvilgedeOgReduserteFinnmarkstilleggPerioder.second

            assertThat(innvilgedePerioder).isEmpty()
            assertThat(redusertePerioder).hasSize(1)
            assertThat(redusertePerioder.single()).isEqualTo(YearMonth.of(2025, 11))
        }
    }
}
