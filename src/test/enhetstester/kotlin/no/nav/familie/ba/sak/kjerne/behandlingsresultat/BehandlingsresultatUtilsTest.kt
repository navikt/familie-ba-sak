package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockkStatic
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

class BehandlingsresultatUtilsTest {

    val søker = tilfeldigPerson()

    private val barn1Aktør = randomAktør()

    val jan22 = YearMonth.of(2022, 1)
    val feb22 = YearMonth.of(2022, 2)
    val mar22 = YearMonth.of(2022, 3)
    val mai22 = YearMonth.of(2022, 5)
    val aug22 = YearMonth.of(2022, 8)
    val des22 = YearMonth.of(2022, 12)

    @BeforeEach
    fun reset() {
        clearStaticMockk(YearMonth::class)
    }

    @Test
    fun `Skal kaste feil dersom det finnes uvurderte ytelsepersoner`() {
        val feil = assertThrows<Feil> {
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                    YtelsePerson(
                        aktør = barn1Aktør,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                        resultater = setOf(YtelsePersonResultat.IKKE_VURDERT)
                    )
                )
            )
        }

        assertEquals("Minst én ytelseperson er ikke vurdert", feil.message)
    }

    @Test
    fun `Kaster feil ved ugyldig resultat på førstegangsbehandling`() {
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        setOf(
            Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
            Behandlingsresultat.ENDRET_UTEN_UTBETALING,
            Behandlingsresultat.ENDRET_OG_OPPHØRT,
            Behandlingsresultat.OPPHØRT,
            Behandlingsresultat.FORTSATT_INNVILGET,
            Behandlingsresultat.IKKE_VURDERT
        ).forEach {
            val feil = assertThrows<FunksjonellFeil> {
                validerBehandlingsresultat(behandling, it)
            }
            assertTrue(feil.message?.contains("ugyldig") ?: false)
        }
    }

    @Test
    fun `Kaster feil ved ugyldig resultat på revurdering`() {
        val behandling = lagBehandling(behandlingType = BehandlingType.REVURDERING)

        val feil = assertThrows<FunksjonellFeil> {
            validerBehandlingsresultat(behandling, Behandlingsresultat.IKKE_VURDERT)
        }
        assertTrue(feil.message?.contains("ugyldig") ?: false)
    }

    @Test
    fun `skal returnere AVSLÅTT_OG_OPPHØRT behandlingsresultat`() {
        val personer = listOf(
            lagYtelsePerson(YtelsePersonResultat.AVSLÅTT),
            lagYtelsePerson(YtelsePersonResultat.OPPHØRT),
            lagYtelsePerson(YtelsePersonResultat.FORTSATT_OPPHØRT)
        )

        assertEquals(
            Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
    }

    @Test
    fun `skal returnere AVSLÅTT_OG_OPPHØRT behandlingsresultat når et barn har fortsatt opphørt og søker har avslått`() {
        val personer = listOf(
            lagYtelsePerson(YtelsePersonResultat.AVSLÅTT),
            lagYtelsePerson(YtelsePersonResultat.FORTSATT_OPPHØRT)
        )

        assertEquals(
            Behandlingsresultat.AVSLÅTT,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
    }

    @Test
    fun `skal returnere ENDRET_OG_OPPHØRT behandlingsresultat`() {
        val personer = listOf(
            lagYtelsePerson(YtelsePersonResultat.ENDRET_UTBETALING),
            lagYtelsePerson(YtelsePersonResultat.OPPHØRT),
            lagYtelsePerson(YtelsePersonResultat.FORTSATT_OPPHØRT)
        )

        assertEquals(
            Behandlingsresultat.ENDRET_OG_OPPHØRT,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
    }

    @Test
    fun `skal returnere AVSLÅTT_ENDRET_OG_OPPHØRT behandlingsresultat`() {
        val personer = listOf(
            lagYtelsePerson(YtelsePersonResultat.ENDRET_UTBETALING),
            lagYtelsePerson(YtelsePersonResultat.AVSLÅTT),
            lagYtelsePerson(YtelsePersonResultat.FORTSATT_OPPHØRT)
        )

        assertEquals(
            Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
    }

    @Test
    fun `Skal returnere FORTSATT_OPPHØRT behandlingsresultat hvis ytelsen opphører på forskjellig tidspunkt`() {
        val personer = listOf(
            lagYtelsePerson(
                resultat = YtelsePersonResultat.FORTSATT_OPPHØRT,
                ytelseSlutt = YearMonth.now().minusMonths(1)
            ),
            lagYtelsePerson(
                resultat = YtelsePersonResultat.FORTSATT_OPPHØRT,
                ytelseSlutt = YearMonth.now()
            )
        )

        assertEquals(
            Behandlingsresultat.FORTSATT_OPPHØRT,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
    }

    @Test
    fun `både ENDRET_UTBETALING og ENDRET_UTEN_UTBETALING som samlede resultater gir behandlingsresultat ENDRET_UTBETALING`() {
        val personer = listOf(
            lagYtelsePerson(
                resultat = YtelsePersonResultat.ENDRET_UTBETALING,
                ytelseSlutt = YearMonth.now().minusMonths(1)
            ),
            lagYtelsePerson(
                resultat = YtelsePersonResultat.ENDRET_UTEN_UTBETALING,
                ytelseSlutt = YearMonth.now()
            )
        )

        assertEquals(
            Behandlingsresultat.ENDRET_UTBETALING,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
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

        val opphørsresultat = BehandlingsresultatUtils.hentOpphørsresultatPåBehandling(nåværendeAndeler, forrigeAndeler)

        assertEquals(BehandlingsresultatUtils.Opphørsresultat.IKKE_OPPHØRT, opphørsresultat)
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

        val opphørsresultat = BehandlingsresultatUtils.hentOpphørsresultatPåBehandling(nåværendeAndeler, forrigeAndeler)

        assertEquals(BehandlingsresultatUtils.Opphørsresultat.OPPHØRT, opphørsresultat)
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

        val opphørsresultat = BehandlingsresultatUtils.hentOpphørsresultatPåBehandling(nåværendeAndeler, forrigeAndeler)

        assertEquals(BehandlingsresultatUtils.Opphørsresultat.OPPHØRT, opphørsresultat)
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

        val opphørsresultat = BehandlingsresultatUtils.hentOpphørsresultatPåBehandling(nåværendeAndeler, forrigeAndeler)

        assertEquals(BehandlingsresultatUtils.Opphørsresultat.FORTSATT_OPPHØRT, opphørsresultat)
    }

    private fun lagYtelsePerson(
        resultat: YtelsePersonResultat,
        ytelseSlutt: YearMonth? = YearMonth.now().minusMonths(2)
    ): YtelsePerson {
        val ytelseType = YtelseType.ORDINÆR_BARNETRYGD
        val kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE, KravOpprinnelse.INNEVÆRENDE)
        return YtelsePerson(
            aktør = randomAktør(),
            ytelseType = ytelseType,
            kravOpprinnelse = kravOpprinnelse,
            resultater = setOf(resultat),
            ytelseSlutt = ytelseSlutt
        )
    }
}
