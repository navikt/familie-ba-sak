package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class AutovedtakSøknadBegrunnelseUtilTest {
    private val behandling = lagBehandling()
    private val tidligereBarn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2006, 5, 10))
    private val nyttBarn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2025, 10, 20))

    private fun lagAndel(
        person: Person,
        fom: YearMonth,
        tom: YearMonth,
        sats: Int = 1968,
        ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
        utenUtbetaling: Boolean = false,
    ) = lagAndelTilkjentYtelse(
        fom = fom,
        tom = tom,
        person = person,
        behandling = behandling,
        ytelseType = ytelseType,
        sats = sats,
        beløp = if (utenUtbetaling) 0 else sats,
        prosent = if (utenUtbetaling) BigDecimal.ZERO else BigDecimal(100),
    )

    private val andelerForTidligereBarn =
        listOf(
            lagAndel(tidligereBarn, fom = YearMonth.of(2006, 6), tom = YearMonth.of(2006, 8), utenUtbetaling = true),
            lagAndel(tidligereBarn, fom = YearMonth.of(2006, 9), tom = YearMonth.of(2019, 2), sats = 970),
            lagAndel(tidligereBarn, fom = YearMonth.of(2019, 3), tom = YearMonth.of(2024, 4), sats = 1054),
        )

    private val andelerForNyttBarn =
        listOf(
            lagAndel(nyttBarn, fom = YearMonth.of(2025, 11), tom = YearMonth.of(2026, 1), utenUtbetaling = true),
            lagAndel(nyttBarn, fom = YearMonth.of(2026, 2), tom = YearMonth.of(2026, 4), sats = 1968),
            lagAndel(nyttBarn, fom = YearMonth.of(2026, 5), tom = YearMonth.of(2031, 12), sats = 2012),
            lagAndel(nyttBarn, fom = YearMonth.of(2032, 1), tom = YearMonth.of(2043, 9), sats = 1900),
            lagAndel(nyttBarn, fom = YearMonth.of(2026, 2), tom = YearMonth.of(2026, 4), sats = 500, ytelseType = YtelseType.FINNMARKSTILLEGG),
            lagAndel(nyttBarn, fom = YearMonth.of(2026, 5), tom = YearMonth.of(2043, 9), sats = 512, ytelseType = YtelseType.FINNMARKSTILLEGG),
        )

    private val nyeAndeler = finnNyeAndeler(forrigeAndeler = andelerForTidligereBarn, nåværendeAndeler = andelerForTidligereBarn + andelerForNyttBarn)

    @Test
    fun `skal gi første måned med utbetaling for barn som ikke hadde andeler i forrige behandling`() {
        // Act & Assert
        assertThat(nyeAndeler.finnFørsteMånederMedUtbetaling(YtelseType.ORDINÆR_BARNETRYGD)).containsExactly(YearMonth.of(2026, 2))
    }

    @Test
    fun `skal gi alle barns første måned med utbetaling når det ikke finnes forrige behandling`() {
        // Act
        val nyeAndelerUtenForrigeBehandling = finnNyeAndeler(forrigeAndeler = emptyList(), nåværendeAndeler = andelerForTidligereBarn + andelerForNyttBarn)

        // Assert
        assertThat(nyeAndelerUtenForrigeBehandling.finnFørsteMånederMedUtbetaling(YtelseType.ORDINÆR_BARNETRYGD)).containsExactlyInAnyOrder(YearMonth.of(2006, 9), YearMonth.of(2026, 2))
    }

    @Test
    fun `skal gi måneden satsen øker, men ikke når satsen synker`() {
        // Act & Assert
        assertThat(nyeAndeler.finnMånederMedSatsøkning()).containsExactly(YearMonth.of(2026, 5))
    }

    @Test
    fun `skal gi første måned med tillegg, og ikke måneden satsen for tillegget endres`() {
        // Act & Assert
        assertThat(nyeAndeler.finnFørsteMånederMedUtbetaling(YtelseType.FINNMARKSTILLEGG)).containsExactly(YearMonth.of(2026, 2))
        assertThat(nyeAndeler.finnFørsteMånederMedUtbetaling(YtelseType.SVALBARDTILLEGG)).isEmpty()
    }

    @Test
    fun `skal gi første måned i hver sammenhengende utbetaling når det er opphold mellom dem`() {
        // Arrange
        val andelerMedOpphold =
            listOf(
                lagAndel(nyttBarn, fom = YearMonth.of(2026, 2), tom = YearMonth.of(2026, 4), sats = 1968),
                lagAndel(nyttBarn, fom = YearMonth.of(2026, 5), tom = YearMonth.of(2026, 7), utenUtbetaling = true),
                lagAndel(nyttBarn, fom = YearMonth.of(2026, 8), tom = YearMonth.of(2043, 9), sats = 2012),
            )

        // Act
        val nyeAndelerMedOpphold = finnNyeAndeler(forrigeAndeler = emptyList(), nåværendeAndeler = andelerMedOpphold)

        // Assert
        assertThat(nyeAndelerMedOpphold.finnFørsteMånederMedUtbetaling(YtelseType.ORDINÆR_BARNETRYGD)).containsExactlyInAnyOrder(YearMonth.of(2026, 2), YearMonth.of(2026, 8))
        assertThat(nyeAndelerMedOpphold.finnMånederMedSatsøkning()).isEmpty()
    }

    @Test
    fun `skal bare regne månedene etter forrige behandling som nye når andelen fortsetter`() {
        // Arrange
        val forrigeAndeler = listOf(lagAndel(nyttBarn, fom = YearMonth.of(2026, 2), tom = YearMonth.of(2026, 4)))
        val nåværendeAndeler = listOf(lagAndel(nyttBarn, fom = YearMonth.of(2026, 2), tom = YearMonth.of(2043, 9)))

        // Act
        val nyeAndelerEtterForrigeBehandling = finnNyeAndeler(forrigeAndeler = forrigeAndeler, nåværendeAndeler = nåværendeAndeler)

        // Assert
        assertThat(nyeAndelerEtterForrigeBehandling.finnFørsteMånederMedUtbetaling(YtelseType.ORDINÆR_BARNETRYGD)).containsExactly(YearMonth.of(2026, 5))
    }

    @Test
    fun `skal ikke regne måneder som nye når forrige behandling hadde andel uten utbetaling`() {
        // Arrange
        val forrigeAndeler = listOf(lagAndel(nyttBarn, fom = YearMonth.of(2026, 2), tom = YearMonth.of(2026, 4), utenUtbetaling = true))
        val nåværendeAndeler = listOf(lagAndel(nyttBarn, fom = YearMonth.of(2026, 2), tom = YearMonth.of(2026, 4)))

        // Act
        val nyeAndelerUtenEndring = finnNyeAndeler(forrigeAndeler = forrigeAndeler, nåværendeAndeler = nåværendeAndeler)

        // Assert
        assertThat(nyeAndelerUtenEndring.finnFørsteMånederMedUtbetaling(YtelseType.ORDINÆR_BARNETRYGD)).isEmpty()
    }

    @Test
    fun `skal gjenkjenne måneder med andel uten utbetaling`() {
        // Act & Assert
        assertThat(nyeAndeler.harAndelUtenUtbetalingI(YearMonth.of(2025, 11))).isTrue()
        assertThat(nyeAndeler.harAndelUtenUtbetalingI(YearMonth.of(2026, 1))).isTrue()
        assertThat(nyeAndeler.harAndelUtenUtbetalingI(YearMonth.of(2026, 2))).isFalse()
        assertThat(nyeAndeler.harAndelUtenUtbetalingI(YearMonth.of(2006, 7))).isFalse()
    }
}
