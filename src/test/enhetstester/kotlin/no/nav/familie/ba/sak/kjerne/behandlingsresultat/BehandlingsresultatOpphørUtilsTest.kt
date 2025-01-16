package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.clearAllMocks
import io.mockk.clearStaticMockk
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.filtrerBortIrrelevanteAndeler
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.hentOpphørsresultatPåBehandling
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.utledOpphørsdatoForNåværendeBehandlingMedFallback
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
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
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

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

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
            )

        assertEquals(Opphørsresultat.IKKE_OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom nåværende andeler opphører mens forrige andeler ikke opphører til og med dagens dato`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

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

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
            )

        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom nåværende andeler opphører tidligere enn forrige andeler og dagens dato`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

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

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
            )

        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom vi går fra andeler på person til fullt opphør på person`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
            )

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = emptyList(),
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
            )

        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere FORTSATT_OPPHØRT dersom nåværende andeler har lik opphørsdato som forrige andeler`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

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

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
            )

        assertEquals(Opphørsresultat.FORTSATT_OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere IKKE_OPPHØRT dersom nåværende andeler har lik opphørsdato som forrige andeler men det er i fremtiden`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

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

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
            )

        assertEquals(Opphørsresultat.IKKE_OPPHØRT, opphørsresultat)
    }

    @ParameterizedTest
    @EnumSource(Årsak::class, names = ["ALLEREDE_UTBETALT", "ENDRE_MOTTAKER", "ETTERBETALING_3ÅR"])
    internal fun `filtrerBortIrrelevanteAndeler - skal filtrere andeler som har 0 i beløp og endret utbetaling andel med årsak ALLEREDE_UTBETALT, ENDRE_MOTTAKER eller ETTERBETALING_3ÅR`(årsak: Årsak) {
        val barn = lagPerson(type = PersonType.BARN)
        val barnAktør = barn.aktør

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    aktør = barnAktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om1mnd,
                    beløp = 1400,
                    aktør = barnAktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = om4mnd,
                    tom = om4mnd,
                    beløp = 0,
                    aktør = barnAktør,
                ),
            )

        val endretUtBetalingAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    årsak = årsak,
                ),
                lagEndretUtbetalingAndel(
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    fom = om4mnd,
                    tom = om4mnd,
                    årsak = årsak,
                ),
            )

        val andelerEtterFiltrering = andeler.filtrerBortIrrelevanteAndeler(endretUtBetalingAndeler)

        assertEquals(andelerEtterFiltrering.minOf { it.stønadFom }, for1mndSiden)
        assertEquals(andelerEtterFiltrering.maxOf { it.stønadTom }, om1mnd)
    }

    @Test
    internal fun `filtrerBortIrrelevanteAndeler - skal ikke filtrere andeler som har 0 i beløp og endret utbetaling andel med årsak DELT_BOSTED`() {
        val barn = lagPerson(type = PersonType.BARN)
        val barnAktør = barn.aktør

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    aktør = barnAktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om1mnd,
                    beløp = 1400,
                    aktør = barnAktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = om4mnd,
                    tom = om4mnd,
                    beløp = 0,
                    aktør = barnAktør,
                ),
            )

        val endretUtBetalingAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    årsak = Årsak.DELT_BOSTED,
                ),
                lagEndretUtbetalingAndel(
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    fom = om4mnd,
                    tom = om4mnd,
                    årsak = Årsak.DELT_BOSTED,
                ),
            )

        val andelerEtterFiltrering = andeler.filtrerBortIrrelevanteAndeler(endretUtBetalingAndeler)

        assertEquals(andelerEtterFiltrering.minOf { it.stønadFom }, for3mndSiden)
        assertEquals(andelerEtterFiltrering.maxOf { it.stønadTom }, om4mnd)
    }

    @Test
    internal fun `filtrerBortIrrelevanteAndeler - skal ikke filtrere andeler som har 0 i beløp grunnet differanseberegning`() {
        val barn = lagPerson(type = PersonType.BARN)
        val barnAktør = barn.aktør
        val søker = lagPerson(type = PersonType.SØKER)
        val søkerAktør = søker.aktør

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    differanseberegnetPeriodebeløp = 50,
                    aktør = søkerAktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om1mnd,
                    beløp = 0,
                    differanseberegnetPeriodebeløp = 50,
                    aktør = barnAktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = om4mnd,
                    tom = om4mnd,
                    beløp = 0,
                    differanseberegnetPeriodebeløp = 50,
                    aktør = barnAktør,
                ),
            )

        val andelerEtterFiltrering = andeler.filtrerBortIrrelevanteAndeler(endretAndeler = emptyList())

        assertEquals(andelerEtterFiltrering.minOf { it.stønadFom }, for3mndSiden)
        assertEquals(andelerEtterFiltrering.maxOf { it.stønadTom }, om4mnd)
    }

    @Test
    fun `utledOpphørsdatoForNåværendeBehandlingMedFallback - skal returnere null hvis det ikke finnes andeler i inneværende behandling og kun irrelevante nullutbetalinger i forrige behandling`() {
        val barn = lagPerson(type = PersonType.BARN)

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    prosent = BigDecimal.ZERO,
                    aktør = barn.aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om4mnd,
                    beløp = 0,
                    prosent = BigDecimal.ZERO,
                    aktør = barn.aktør,
                ),
            )

        val forrigeEndretAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                ),
                lagEndretUtbetalingAndel(
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    fom = for1mndSiden,
                    tom = om4mnd,
                    årsak = Årsak.ENDRE_MOTTAKER,
                ),
            )

        val opphørstidspunktInneværendeBehandling =
            emptyList<AndelTilkjentYtelse>().utledOpphørsdatoForNåværendeBehandlingMedFallback(
                forrigeAndelerIBehandling = forrigeAndeler,
                endretAndelerForForrigeBehandling = forrigeEndretAndeler,
                nåværendeEndretAndelerIBehandling = emptyList(),
            )

        assertNull(opphørstidspunktInneværendeBehandling)
    }

    @Test
    fun `utledOpphørsdatoForNåværendeBehandlingMedFallback - skal returnere tidligste fom på andeler i forrige behandling hvis det ikke finnes andeler i inneværende behandling`() {
        val barn = lagPerson(type = PersonType.BARN)

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    prosent = BigDecimal.ZERO,
                    aktør = barn.aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om4mnd,
                    prosent = BigDecimal.ZERO,
                    aktør = barn.aktør,
                ),
            )

        val forrigeEndretAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                ),
            )

        val opphørstidspunktInneværendeBehandling =
            emptyList<AndelTilkjentYtelse>().utledOpphørsdatoForNåværendeBehandlingMedFallback(
                forrigeAndelerIBehandling = forrigeAndeler,
                endretAndelerForForrigeBehandling = forrigeEndretAndeler,
                nåværendeEndretAndelerIBehandling = emptyList(),
            )

        assertEquals(for1mndSiden, opphørstidspunktInneværendeBehandling)
    }
}
