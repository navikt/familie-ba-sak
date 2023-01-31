package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockkStatic
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.hentOpphørsresultatPåBehandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

class BehandlingsresultatOpphørUtilsTest {

    val søker = tilfeldigPerson()

    val jan22 = YearMonth.of(2022, 1)
    val feb22 = YearMonth.of(2022, 2)
    val mar22 = YearMonth.of(2022, 3)
    val mai22 = YearMonth.of(2022, 5)
    val aug22 = YearMonth.of(2022, 8)

    @BeforeEach
    fun reset() {
        clearStaticMockk(YearMonth::class)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere IKKE_OPPHØRT dersom nåværende andeler strekker seg lengre enn dagens dato`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        mockkStatic(YearMonth::class)
        every { YearMonth.now() } returns YearMonth.of(2022, 4)

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mai22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mai22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mai22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val opphørsresultat = hentOpphørsresultatPåBehandling(nåværendeAndeler, forrigeAndeler)

        assertEquals(Opphørsresultat.IKKE_OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom nåværende andeler opphører mens forrige andeler ikke opphører til og med dagens dato`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        mockkStatic(YearMonth::class)
        every { YearMonth.now() } returns YearMonth.of(2022, 4)

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = feb22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = feb22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val opphørsresultat = hentOpphørsresultatPåBehandling(nåværendeAndeler, forrigeAndeler)

        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom nåværende andeler opphører tidligere enn forrige andeler og dagens dato`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør
        val apr22 = YearMonth.of(2022, 4)

        mockkStatic(YearMonth::class)
        every { YearMonth.now() } returns apr22

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = feb22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = feb22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val opphørsresultat = hentOpphørsresultatPåBehandling(nåværendeAndeler, forrigeAndeler)

        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere FORTSATT_OPPHØRT dersom nåværende andeler har lik opphørsdato som forrige andeler`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør
        val apr22 = YearMonth.of(2022, 4)

        mockkStatic(YearMonth::class)
        every { YearMonth.now() } returns apr22

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val opphørsresultat = hentOpphørsresultatPåBehandling(nåværendeAndeler, forrigeAndeler)

        assertEquals(Opphørsresultat.FORTSATT_OPPHØRT, opphørsresultat)
    }
}
