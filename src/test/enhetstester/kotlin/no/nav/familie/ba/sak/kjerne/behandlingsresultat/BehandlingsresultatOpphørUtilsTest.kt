package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.clearAllMocks
import io.mockk.clearStaticMockk
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.filtrerBortIrrelevanteAndeler
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.hentOpphørsresultatPåBehandling
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.utledOpphørsdatoForNåværendeBehandlingMedFallback
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlingsresultatOpphørUtilsTest {
    val søker = tilfeldigPerson()

    val for3mndSiden = YearMonth.now().minusMonths(3)
    val for2mndSiden = YearMonth.now().minusMonths(2)
    val for1mndSiden = YearMonth.now().minusMonths(1)
    val om1mnd = YearMonth.now().plusMonths(1)
    val om4mnd = YearMonth.now().plusMonths(4)

    @BeforeEach
    fun reset() {
        clearStaticMockk(YearMonth::class)
    }

    @AfterAll
    fun clearMocks() {
        clearAllMocks()
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere IKKE_OPPHØRT dersom nåværende andeler strekker seg lengre enn dagens dato`() {
        // Arrange
        val barn1Aktør = lagAktør()
        val barn2Aktør = lagAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om1mnd,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om1mnd,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om1mnd,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om4mnd,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        // Act
        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
            )

        // Assert
        assertEquals(Opphørsresultat.IKKE_OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom nåværende andeler opphører mens forrige andeler ikke opphører til og med dagens dato`() {
        // Arrange
        val barn1Aktør = lagAktør()
        val barn2Aktør = lagAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om4mnd,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om4mnd,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        // Act
        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
            )

        // Assert
        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom nåværende andeler opphører tidligere enn forrige andeler og dagens dato`() {
        // Arrange
        val barn1Aktør = lagAktør()
        val barn2Aktør = lagAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        // Act
        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
            )

        // Assert
        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom vi går fra andeler på person til fullt opphør på person`() {
        // Arrange
        val barn1Aktør = lagAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
            )

        // Act
        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = emptyList(),
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
            )

        // Assert
        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere FORTSATT_OPPHØRT dersom nåværende andeler har lik opphørsdato som forrige andeler`() {
        // Arrange
        val barn1Aktør = lagAktør()
        val barn2Aktør = lagAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        // Act
        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
            )

        // Assert
        assertEquals(Opphørsresultat.FORTSATT_OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere IKKE_OPPHØRT dersom nåværende andeler har lik opphørsdato som forrige andeler men det er i fremtiden`() {
        // Arrange
        val barn1Aktør = lagAktør()
        val barn2Aktør = lagAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om4mnd,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om4mnd,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        // Act
        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
            )

        // Assert
        assertEquals(Opphørsresultat.IKKE_OPPHØRT, opphørsresultat)
    }

    @ParameterizedTest
    @EnumSource(Årsak::class, names = ["ALLEREDE_UTBETALT", "ENDRE_MOTTAKER", "ETTERBETALING_3ÅR"])
    internal fun `filtrerBortIrrelevanteAndeler - skal filtrere andeler som har 0 i beløp og endret utbetaling andel med årsak ALLEREDE_UTBETALT, ENDRE_MOTTAKER eller ETTERBETALING_3ÅR`(årsak: Årsak) {
        // Arrange
        val barn = lagAktør()

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    aktør = barn,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om1mnd,
                    beløp = 1400,
                    aktør = barn,
                ),
                lagAndelTilkjentYtelse(
                    fom = om4mnd,
                    tom = om4mnd,
                    beløp = 0,
                    aktør = barn,
                ),
            )

        val endretUtBetalingAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    aktører = setOf(barn),
                    prosent = BigDecimal.ZERO,
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    årsak = årsak,
                ),
                lagEndretUtbetalingAndel(
                    aktører = setOf(barn),
                    prosent = BigDecimal.ZERO,
                    fom = om4mnd,
                    tom = om4mnd,
                    årsak = årsak,
                ),
            )

        // Act
        val andelerEtterFiltrering = andeler.filtrerBortIrrelevanteAndeler(endretUtBetalingAndeler)

        // Assert
        assertEquals(andelerEtterFiltrering.minOf { it.stønadFom }, for1mndSiden)
        assertEquals(andelerEtterFiltrering.maxOf { it.stønadTom }, om1mnd)
    }

    @Test
    internal fun `filtrerBortIrrelevanteAndeler - skal ikke filtrere andeler som har 0 i beløp og endret utbetaling andel med årsak DELT_BOSTED`() {
        // Arrange
        val barn = lagAktør()

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    aktør = barn,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om1mnd,
                    beløp = 1400,
                    aktør = barn,
                ),
                lagAndelTilkjentYtelse(
                    fom = om4mnd,
                    tom = om4mnd,
                    beløp = 0,
                    aktør = barn,
                ),
            )

        val endretUtBetalingAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    aktører = setOf(barn),
                    prosent = BigDecimal.ZERO,
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    årsak = Årsak.DELT_BOSTED,
                ),
                lagEndretUtbetalingAndel(
                    aktører = setOf(barn),
                    prosent = BigDecimal.ZERO,
                    fom = om4mnd,
                    tom = om4mnd,
                    årsak = Årsak.DELT_BOSTED,
                ),
            )

        // Act
        val andelerEtterFiltrering = andeler.filtrerBortIrrelevanteAndeler(endretUtBetalingAndeler)

        // Assert
        assertEquals(andelerEtterFiltrering.minOf { it.stønadFom }, for3mndSiden)
        assertEquals(andelerEtterFiltrering.maxOf { it.stønadTom }, om4mnd)
    }

    @Test
    internal fun `filtrerBortIrrelevanteAndeler - skal ikke filtrere andeler som har 0 i beløp grunnet differanseberegning`() {
        // Arrange
        val barn = lagAktør()
        val søker = lagAktør()

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    differanseberegnetPeriodebeløp = 50,
                    aktør = søker,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om1mnd,
                    beløp = 0,
                    differanseberegnetPeriodebeløp = 50,
                    aktør = barn,
                ),
                lagAndelTilkjentYtelse(
                    fom = om4mnd,
                    tom = om4mnd,
                    beløp = 0,
                    differanseberegnetPeriodebeløp = 50,
                    aktør = barn,
                ),
            )

        // Act
        val andelerEtterFiltrering = andeler.filtrerBortIrrelevanteAndeler(endretAndeler = emptyList())

        // Assert
        assertEquals(andelerEtterFiltrering.minOf { it.stønadFom }, for3mndSiden)
        assertEquals(andelerEtterFiltrering.maxOf { it.stønadTom }, om4mnd)
    }

    @Test
    fun `utledOpphørsdatoForNåværendeBehandlingMedFallback - skal returnere null hvis det ikke finnes andeler i inneværende behandling og kun irrelevante nullutbetalinger i forrige behandling`() {
        // Arrange
        val barn = lagAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    prosent = BigDecimal.ZERO,
                    aktør = barn,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om4mnd,
                    beløp = 0,
                    prosent = BigDecimal.ZERO,
                    aktør = barn,
                ),
            )

        val forrigeEndretAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    aktører = setOf(barn),
                    prosent = BigDecimal.ZERO,
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                ),
                lagEndretUtbetalingAndel(
                    aktører = setOf(barn),
                    prosent = BigDecimal.ZERO,
                    fom = for1mndSiden,
                    tom = om4mnd,
                    årsak = Årsak.ENDRE_MOTTAKER,
                ),
            )

        // Act
        val opphørstidspunktInneværendeBehandling =
            emptyList<AndelTilkjentYtelse>().utledOpphørsdatoForNåværendeBehandlingMedFallback(
                forrigeAndelerIBehandling = forrigeAndeler,
                endretAndelerForForrigeBehandling = forrigeEndretAndeler,
                nåværendeEndretAndelerIBehandling = emptyList(),
            )

        // Assert
        assertNull(opphørstidspunktInneværendeBehandling)
    }

    @Test
    fun `utledOpphørsdatoForNåværendeBehandlingMedFallback - skal returnere tidligste fom på andeler i forrige behandling hvis det ikke finnes andeler i inneværende behandling`() {
        // Arrange
        val barn = lagAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    prosent = BigDecimal.ZERO,
                    aktør = barn,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om4mnd,
                    prosent = BigDecimal.ZERO,
                    aktør = barn,
                ),
            )

        val forrigeEndretAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    aktører = setOf(barn),
                    prosent = BigDecimal.ZERO,
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                ),
            )

        // Act
        val opphørstidspunktInneværendeBehandling =
            emptyList<AndelTilkjentYtelse>().utledOpphørsdatoForNåværendeBehandlingMedFallback(
                forrigeAndelerIBehandling = forrigeAndeler,
                endretAndelerForForrigeBehandling = forrigeEndretAndeler,
                nåværendeEndretAndelerIBehandling = emptyList(),
            )

        // Assert
        assertEquals(for1mndSiden, opphørstidspunktInneværendeBehandling)
    }
}
