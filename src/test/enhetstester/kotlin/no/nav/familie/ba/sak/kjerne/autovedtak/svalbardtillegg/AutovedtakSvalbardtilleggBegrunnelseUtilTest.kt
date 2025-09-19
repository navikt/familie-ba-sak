package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

internal class AutovedtakSvalbardtilleggBegrunnelseUtilTest {
    @Nested
    inner class FinnInnvilgedeOgReduserteSvalbardtilleggPerioderTest {
        @Test
        fun `Skal returnere alle perioder der det har kommet nye svalbardtillegg andeler`() {
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
                        ytelseType = YtelseType.SVALBARDTILLEGG,
                    ),
                )

            val innvilgedeOgReduserteSvalbardtilleggPerioder =
                finnInnvilgedeOgReduserteSvalbardtilleggPerioder(
                    forrigeAndeler = emptyList(),
                    nåværendeAndeler = nåværendeAndeler,
                )

            val innvilgedePerioder = innvilgedeOgReduserteSvalbardtilleggPerioder.first
            val redusertePerioder = innvilgedeOgReduserteSvalbardtilleggPerioder.second

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
                        ytelseType = YtelseType.SVALBARDTILLEGG,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 10),
                        tom = YearMonth.of(2025, 12),
                        person = barn2,
                        behandling = behandling,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.SVALBARDTILLEGG,
                    ),
                )

            val innvilgedeOgReduserteSvalbardtilleggPerioder =
                finnInnvilgedeOgReduserteSvalbardtilleggPerioder(
                    forrigeAndeler = emptyList(),
                    nåværendeAndeler = nåværendeAndeler,
                )

            val innvilgedePerioder = innvilgedeOgReduserteSvalbardtilleggPerioder.first
            val redusertePerioder = innvilgedeOgReduserteSvalbardtilleggPerioder.second

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
                        ytelseType = YtelseType.SVALBARDTILLEGG,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn2,
                        behandling = behandling,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.SVALBARDTILLEGG,
                    ),
                )

            val innvilgedeOgReduserteSvalbardtilleggPerioder =
                finnInnvilgedeOgReduserteSvalbardtilleggPerioder(
                    forrigeAndeler = emptyList(),
                    nåværendeAndeler = nåværendeAndeler,
                )

            val innvilgedePerioder = innvilgedeOgReduserteSvalbardtilleggPerioder.first
            val redusertePerioder = innvilgedeOgReduserteSvalbardtilleggPerioder.second

            assertThat(redusertePerioder).isEmpty()
            assertThat(innvilgedePerioder).hasSize(1)
            assertThat(innvilgedePerioder.single()).isEqualTo(YearMonth.of(2025, 11))
        }

        @Test
        fun `Ved flere barn som både får og mister svalbardtillegg for samme periode skal det dannes innvilget og reduksjonsperiode`() {
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
                        ytelseType = YtelseType.SVALBARDTILLEGG,
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
                        ytelseType = YtelseType.SVALBARDTILLEGG,
                    ),
                )

            val innvilgedeOgReduserteSvalbardtilleggPerioder =
                finnInnvilgedeOgReduserteSvalbardtilleggPerioder(
                    forrigeAndeler = forrigeAndeler,
                    nåværendeAndeler = nåværendeAndeler,
                )

            val innvilgedePerioder = innvilgedeOgReduserteSvalbardtilleggPerioder.first
            val redusertePerioder = innvilgedeOgReduserteSvalbardtilleggPerioder.second

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
                        ytelseType = YtelseType.SVALBARDTILLEGG,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn2,
                        behandling = behandling,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.SVALBARDTILLEGG,
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
                        ytelseType = YtelseType.SVALBARDTILLEGG,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2025, 11),
                        tom = YearMonth.of(2025, 12),
                        person = barn2,
                        behandling = behandling2,
                        beløp = 500,
                        sats = 500,
                        ytelseType = YtelseType.SVALBARDTILLEGG,
                    ),
                )

            val innvilgedeOgReduserteSvalbardtilleggPerioder =
                finnInnvilgedeOgReduserteSvalbardtilleggPerioder(
                    forrigeAndeler = forrigeAndeler,
                    nåværendeAndeler = nåværendeAndeler,
                )

            val innvilgedePerioder = innvilgedeOgReduserteSvalbardtilleggPerioder.first
            val redusertePerioder = innvilgedeOgReduserteSvalbardtilleggPerioder.second

            assertThat(redusertePerioder).isEmpty()
            assertThat(innvilgedePerioder).isEmpty()
        }

        @Test
        fun `Skal returnere alle perioder der man har mistet svalbardtillegg andeler siden forrige behandling`() {
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
                        ytelseType = YtelseType.SVALBARDTILLEGG,
                    ),
                )

            val innvilgedeOgReduserteSvalbardtilleggPerioder =
                finnInnvilgedeOgReduserteSvalbardtilleggPerioder(
                    forrigeAndeler = forrigeAndeler,
                    nåværendeAndeler = emptyList(),
                )

            val innvilgedePerioder = innvilgedeOgReduserteSvalbardtilleggPerioder.first
            val redusertePerioder = innvilgedeOgReduserteSvalbardtilleggPerioder.second

            assertThat(innvilgedePerioder).isEmpty()
            assertThat(redusertePerioder).hasSize(1)
            assertThat(redusertePerioder.single()).isEqualTo(YearMonth.of(2025, 11))
        }
    }

    @Nested
    inner class LeggTilBegrunnelseIVedtaksperiodeTest {
        @Test
        fun `Skal kaste feil dersom det ikke finnes noe vedtaksperioder som passer noe utbetalings perioden begrunnelsen skal legges i`() {
            // Arrange
            val vedtaksperioderIBehandling =
                listOf(
                    lagVedtaksperiodeMedBegrunnelser(
                        fom = LocalDate.of(2025, 10, 1),
                        tom = LocalDate.of(2025, 10, 1),
                        type = Vedtaksperiodetype.UTBETALING,
                    ),
                    lagVedtaksperiodeMedBegrunnelser(
                        fom = LocalDate.of(2025, 11, 1),
                        tom = LocalDate.of(2025, 11, 1),
                        type = Vedtaksperiodetype.OPPHØR,
                    ),
                    lagVedtaksperiodeMedBegrunnelser(
                        fom = LocalDate.of(2025, 12, 1),
                        tom = LocalDate.of(2025, 12, 1),
                        type = Vedtaksperiodetype.UTBETALING,
                    ),
                )

            // Act && Assert
            val feilmelding =
                assertThrows<Feil> {
                    leggTilBegrunnelseIVedtaksperiode(
                        vedtaksperiodeStartDato = YearMonth.of(2025, 11),
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SVALBARDTILLEGG,
                        vedtaksperioder = vedtaksperioderIBehandling,
                    )
                }.message

            assertThat(feilmelding).isEqualTo("Finner ikke aktuell periode å begrunne ved autovedtak svalbardtillegg. Se securelogger for detaljer.")
        }

        @Test
        fun `Skal legge til begrunnelse i riktig vedtaksperiode`() {
            // Arrange
            val vedtaksperioderIBehandling =
                listOf(
                    lagVedtaksperiodeMedBegrunnelser(
                        fom = LocalDate.of(2025, 10, 1),
                        tom = LocalDate.of(2025, 10, 1),
                        type = Vedtaksperiodetype.UTBETALING,
                    ),
                    lagVedtaksperiodeMedBegrunnelser(
                        fom = LocalDate.of(2025, 11, 1),
                        tom = LocalDate.of(2025, 11, 1),
                        type = Vedtaksperiodetype.UTBETALING,
                    ),
                    lagVedtaksperiodeMedBegrunnelser(
                        fom = LocalDate.of(2025, 12, 1),
                        tom = LocalDate.of(2025, 12, 1),
                        type = Vedtaksperiodetype.UTBETALING,
                    ),
                )

            // Act
            leggTilBegrunnelseIVedtaksperiode(
                vedtaksperiodeStartDato = YearMonth.of(2025, 11),
                standardbegrunnelse = Standardbegrunnelse.INNVILGET_SVALBARDTILLEGG,
                vedtaksperioder = vedtaksperioderIBehandling,
            )

            // Assert
            val vedtaksperiode = vedtaksperioderIBehandling.single { it.fom == LocalDate.of(2025, 11, 1) }
            assertThat(vedtaksperiode.begrunnelser.map { it.standardbegrunnelse }).contains(Standardbegrunnelse.INNVILGET_SVALBARDTILLEGG)
        }
    }
}
