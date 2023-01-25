package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockkStatic
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
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
    fun `Endring i beløp - Skal returnere false dersom eneste endring er opphør`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )
        )
        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = mai22,
                beløp = 1054,
                aktør = barn1Aktør
            )
        )

        val erEndringIBeløp = BehandlingsresultatUtils.erEndringIBeløp(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            personerFremstiltKravFor = listOf()
        )

        assertEquals(false, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere true når beløp i periode har gått fra større enn 0 til null og det er søkt for person`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )
        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = mai22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val erEndringIBeløp = BehandlingsresultatUtils.erEndringIBeløp(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            personerFremstiltKravFor = listOf(barn1Aktør)
        )

        assertEquals(true, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere false når beløp i periode har gått fra større enn 0 til at annet tall større enn 0 og det er søkt for person`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )
        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = mai22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = mai22.plusMonths(1),
                tom = aug22,
                beløp = 527,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val erEndringIBeløp = BehandlingsresultatUtils.erEndringIBeløp(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            personerFremstiltKravFor = listOf(barn1Aktør)
        )

        assertEquals(false, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere true når beløp i periode har gått fra null til et tall større enn 0 og det ikke er søkt for person`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )
        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = des22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val erEndringIBeløp = BehandlingsresultatUtils.erEndringIBeløp(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            personerFremstiltKravFor = listOf()
        )

        assertEquals(true, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere false når beløp i periode har gått fra null til et tall større enn 0 og det er søkt for person`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )
        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = des22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val erEndringIBeløp = BehandlingsresultatUtils.erEndringIBeløp(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            personerFremstiltKravFor = listOf(barn1Aktør)
        )

        assertEquals(false, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere true når beløp i periode har gått fra større enn 0 til at annet tall større enn 0 og det ikke er søkt for person`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )
        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = mai22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = mai22.plusMonths(1),
                tom = aug22,
                beløp = 527,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val erEndringIBeløp = BehandlingsresultatUtils.erEndringIBeløp(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            personerFremstiltKravFor = listOf()
        )

        assertEquals(true, erEndringIBeløp)
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
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = mai22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = mai22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = mai22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
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
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = feb22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
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
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = feb22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
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
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = jan22,
                tom = mar22,
                beløp = 1054,
                aktør = barn2Aktør
            )
        )

        val opphørsresultat = hentOpphørsresultatPåBehandling(nåværendeAndeler, forrigeAndeler)

        assertEquals(Opphørsresultat.FORTSATT_OPPHØRT, opphørsresultat)
    }

    @Test
    fun `Endring i kompetanse - skal returnere false når ingenting endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null
            )

        val endring = BehandlingsresultatUtils.erEndringIKompetanse(
            nåværendeKompetanser = listOf(forrigeKompetanse.copy().apply { behandlingId = nåværendeBehandling.id }),
            forrigeKompetanser = listOf(forrigeKompetanse)
        )

        assertEquals(false, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere true når søkers aktivitetsland endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null
            )

        val endring = BehandlingsresultatUtils.erEndringIKompetanse(
            nåværendeKompetanser = listOf(forrigeKompetanse.copy(søkersAktivitetsland = "DK").apply { behandlingId = nåværendeBehandling.id }),
            forrigeKompetanser = listOf(forrigeKompetanse)
        )

        assertEquals(true, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere true når søkers aktivitet endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null
            )

        val endring = BehandlingsresultatUtils.erEndringIKompetanse(
            nåværendeKompetanser = listOf(forrigeKompetanse.copy(søkersAktivitet = SøkersAktivitet.ARBEIDER_PÅ_NORSK_SOKKEL).apply { behandlingId = nåværendeBehandling.id }),
            forrigeKompetanser = listOf(forrigeKompetanse)
        )

        assertEquals(true, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere true når annen forelders aktivitetsland endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null
            )

        val endring = BehandlingsresultatUtils.erEndringIKompetanse(
            nåværendeKompetanser = listOf(forrigeKompetanse.copy(annenForeldersAktivitetsland = "DK").apply { behandlingId = nåværendeBehandling.id }),
            forrigeKompetanser = listOf(forrigeKompetanse)
        )

        assertEquals(true, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere true når annen forelders aktivitet endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null
            )

        val endring = BehandlingsresultatUtils.erEndringIKompetanse(
            nåværendeKompetanser = listOf(forrigeKompetanse.copy(annenForeldersAktivitet = AnnenForeldersAktivitet.FORSIKRET_I_BOSTEDSLAND).apply { behandlingId = nåværendeBehandling.id }),
            forrigeKompetanser = listOf(forrigeKompetanse)
        )

        assertEquals(true, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere true når barnets bostedsland endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null
            )

        val endring = BehandlingsresultatUtils.erEndringIKompetanse(
            nåværendeKompetanser = listOf(forrigeKompetanse.copy(barnetsBostedsland = "DK").apply { behandlingId = nåværendeBehandling.id }),
            forrigeKompetanser = listOf(forrigeKompetanse)
        )

        assertEquals(true, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere true når resultat på kompetansen endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null
            )

        val endring = BehandlingsresultatUtils.erEndringIKompetanse(
            nåværendeKompetanser = listOf(forrigeKompetanse.copy(resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND).apply { behandlingId = nåværendeBehandling.id }),
            forrigeKompetanser = listOf(forrigeKompetanse)
        )

        assertEquals(true, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere false når det kun blir lagt på en ekstra kompetanseperiode`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null
            )

        val endring = BehandlingsresultatUtils.erEndringIKompetanse(
            nåværendeKompetanser = listOf(forrigeKompetanse.copy(fom = YearMonth.now().minusMonths(10)).apply { behandlingId = nåværendeBehandling.id }),
            forrigeKompetanser = listOf(forrigeKompetanse)
        )

        assertEquals(false, endring)
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
