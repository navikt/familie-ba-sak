package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatUtils.utledEndringsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

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
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )
        )
        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
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
                tom = mai22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
                fom = mai22.plusMonths(1),
                tom = aug22,
                beløp = 527,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
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
                tom = des22,
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
                tom = des22,
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
                tom = mai22,
                beløp = 1054,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
                fom = mai22.plusMonths(1),
                tom = aug22,
                beløp = 527,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
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
    fun `Endring i endret utbetaling andel - skal returnere true hvis årsak er endret`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel = lagEndretUtbetalingAndel(
            person = barn,
            prosent = BigDecimal.ZERO,
            fom = jan22,
            tom = aug22,
            årsak = Årsak.ETTERBETALING_3ÅR,
            søknadstidspunkt = des22.førsteDagIInneværendeMåned()
        )

        val erEndringIEndretAndeler = BehandlingsresultatUtils.erEndringIEndretUtbetalingAndeler(
            forrigeEndretAndeler = listOf(forrigeEndretAndel),
            nåværendeEndretAndeler = listOf(forrigeEndretAndel.copy(årsak = Årsak.ALLEREDE_UTBETALT))
        )

        assertTrue(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere true hvis avtaletidspunktDeltBosted er endret`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel = lagEndretUtbetalingAndel(
            person = barn,
            prosent = BigDecimal.ZERO,
            fom = jan22,
            tom = aug22,
            årsak = Årsak.DELT_BOSTED,
            søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned()
        )

        val erEndringIEndretAndeler = BehandlingsresultatUtils.erEndringIEndretUtbetalingAndeler(
            forrigeEndretAndeler = listOf(forrigeEndretAndel),
            nåværendeEndretAndeler = listOf(forrigeEndretAndel.copy(avtaletidspunktDeltBosted = feb22.førsteDagIInneværendeMåned()))
        )

        assertTrue(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere true hvis søknadstidspunkt er endret`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel = lagEndretUtbetalingAndel(
            person = barn,
            prosent = BigDecimal.ZERO,
            fom = jan22,
            tom = aug22,
            årsak = Årsak.DELT_BOSTED,
            søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned()
        )

        val erEndringIEndretAndeler = BehandlingsresultatUtils.erEndringIEndretUtbetalingAndeler(
            forrigeEndretAndeler = listOf(forrigeEndretAndel),
            nåværendeEndretAndeler = listOf(forrigeEndretAndel.copy(søknadstidspunkt = feb22.førsteDagIInneværendeMåned()))
        )

        assertTrue(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere false hvis prosent er endret`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel = lagEndretUtbetalingAndel(
            person = barn,
            prosent = BigDecimal.ZERO,
            fom = jan22,
            tom = aug22,
            årsak = Årsak.DELT_BOSTED,
            søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned()
        )

        val erEndringIEndretAndeler = BehandlingsresultatUtils.erEndringIEndretUtbetalingAndeler(
            forrigeEndretAndeler = listOf(forrigeEndretAndel),
            nåværendeEndretAndeler = listOf(forrigeEndretAndel.copy(prosent = BigDecimal(100)))
        )

        assertFalse(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere false hvis eneste endring er at perioden blir lenger`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel = lagEndretUtbetalingAndel(
            person = barn,
            prosent = BigDecimal.ZERO,
            fom = jan22,
            tom = aug22,
            årsak = Årsak.DELT_BOSTED,
            søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned()
        )

        val erEndringIEndretAndeler = BehandlingsresultatUtils.erEndringIEndretUtbetalingAndeler(
            forrigeEndretAndeler = listOf(forrigeEndretAndel),
            nåværendeEndretAndeler = listOf(forrigeEndretAndel.copy(tom = des22))
        )

        assertFalse(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere false hvis endringsperiode oppstår i nåværende behandling`() {
        val barn = lagPerson(type = PersonType.BARN)
        val nåværendeEndretAndel = lagEndretUtbetalingAndel(
            person = barn,
            prosent = BigDecimal.ZERO,
            fom = jan22,
            tom = aug22,
            årsak = Årsak.DELT_BOSTED,
            søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned()
        )

        val erEndringIEndretAndeler = BehandlingsresultatUtils.erEndringIEndretUtbetalingAndeler(
            forrigeEndretAndeler = emptyList(),
            nåværendeEndretAndeler = listOf(nåværendeEndretAndel)
        )

        assertFalse(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere true hvis et av to barn har endring på årsak`() {
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val forrigeEndretAndelBarn1 = lagEndretUtbetalingAndel(
            person = barn1,
            prosent = BigDecimal.ZERO,
            fom = jan22,
            tom = aug22,
            årsak = Årsak.DELT_BOSTED,
            søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned()
        )

        val forrigeEndretAndelBarn2 = lagEndretUtbetalingAndel(
            person = barn2,
            prosent = BigDecimal.ZERO,
            fom = jan22,
            tom = aug22,
            årsak = Årsak.ETTERBETALING_3ÅR,
            søknadstidspunkt = des22.førsteDagIInneværendeMåned()
        )

        val erEndringIEndretAndeler = BehandlingsresultatUtils.erEndringIEndretUtbetalingAndeler(
            forrigeEndretAndeler = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2),
            nåværendeEndretAndeler = listOf(
                forrigeEndretAndelBarn1,
                forrigeEndretAndelBarn2.copy(årsak = Årsak.ALLEREDE_UTBETALT)
            )
        )

        assertTrue(erEndringIEndretAndeler)
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
            nåværendeKompetanser = listOf(
                forrigeKompetanse.copy(søkersAktivitetsland = "DK").apply { behandlingId = nåværendeBehandling.id }
            ),
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
            nåværendeKompetanser = listOf(
                forrigeKompetanse.copy(søkersAktivitet = SøkersAktivitet.ARBEIDER_PÅ_NORSK_SOKKEL)
                    .apply { behandlingId = nåværendeBehandling.id }
            ),
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
            nåværendeKompetanser = listOf(
                forrigeKompetanse.copy(annenForeldersAktivitetsland = "DK")
                    .apply { behandlingId = nåværendeBehandling.id }
            ),
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
            nåværendeKompetanser = listOf(
                forrigeKompetanse.copy(annenForeldersAktivitet = AnnenForeldersAktivitet.FORSIKRET_I_BOSTEDSLAND)
                    .apply { behandlingId = nåværendeBehandling.id }
            ),
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
            nåværendeKompetanser = listOf(
                forrigeKompetanse.copy(barnetsBostedsland = "DK").apply { behandlingId = nåværendeBehandling.id }
            ),
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
            nåværendeKompetanser = listOf(
                forrigeKompetanse.copy(resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND)
                    .apply { behandlingId = nåværendeBehandling.id }
            ),
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
            nåværendeKompetanser = listOf(
                forrigeKompetanse.copy(fom = YearMonth.now().minusMonths(10))
                    .apply { behandlingId = nåværendeBehandling.id }
            ),
            forrigeKompetanser = listOf(forrigeKompetanse)
        )

        assertEquals(false, endring)
    }
    @Test
    fun `erEndringIVilkårvurderingForPerson skal returnere false dersom vilkårresultatene er helt like`() {
        val nåværendeVilkårResultat = listOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2015, 1, 1),
                periodeTom = LocalDate.of(2020, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            ),
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.of(2020, 1, 2),
                periodeTom = LocalDate.of(2022, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            )
        )

        val forrigeVilkårResultat = listOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2015, 1, 1),
                periodeTom = LocalDate.of(2020, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            ),
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.of(2020, 1, 2),
                periodeTom = LocalDate.of(2022, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            )
        )

        val erEndringIVilkårvurderingForPerson = erEndringIVilkårvurderingForPerson(
            nåværendeVilkårResultat,
            forrigeVilkårResultat
        )

        assertThat(erEndringIVilkårvurderingForPerson, Is(false))
    }

    @Test
    fun `erEndringIVilkårvurderingForPerson skal returnere true dersom det har vært endringer i regelverk`() {
        val nåværendeVilkårResultat = listOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2015, 1, 1),
                periodeTom = LocalDate.of(2020, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            ),
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.of(2020, 1, 2),
                periodeTom = LocalDate.of(2022, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            )
        )

        val forrigeVilkårResultat = listOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2015, 1, 1),
                periodeTom = LocalDate.of(2020, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            ),
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.of(2020, 1, 2),
                periodeTom = LocalDate.of(2022, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
                ),
                vurderesEtter = Regelverk.EØS_FORORDNINGEN
            )
        )

        val erEndringIVilkårvurderingForPerson = erEndringIVilkårvurderingForPerson(
            nåværendeVilkårResultat,
            forrigeVilkårResultat
        )

        assertThat(erEndringIVilkårvurderingForPerson, Is(true))
    }

    @Test
    fun `erEndringIVilkårvurderingForPerson skal returnere true dersom det har vært endringer i utdypendevilkårsvurdering`() {
        val nåværendeVilkårResultat = listOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2015, 1, 1),
                periodeTom = LocalDate.of(2020, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            ),
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.of(2020, 1, 2),
                periodeTom = LocalDate.of(2022, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            )
        )

        val forrigeVilkårResultat = listOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2015, 1, 1),
                periodeTom = LocalDate.of(2020, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            ),
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.of(2020, 1, 2),
                periodeTom = LocalDate.of(2022, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERING_ANNET_GRUNNLAG
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            )
        )

        val erEndringIVilkårvurderingForPerson = erEndringIVilkårvurderingForPerson(
            nåværendeVilkårResultat,
            forrigeVilkårResultat
        )

        assertThat(erEndringIVilkårvurderingForPerson, Is(true))
    }

    @Test
    fun `erEndringIVilkårvurderingForPerson skal returnere true dersom det har oppstått splitt i vilkårsvurderingen`() {
        val nåværendeVilkårResultat = listOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2015, 1, 1),
                periodeTom = LocalDate.of(2020, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            ),
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.of(2020, 1, 2),
                periodeTom = LocalDate.of(2022, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            )
        )

        val forrigeVilkårResultat = listOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2015, 1, 1),
                periodeTom = LocalDate.of(2020, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            ),
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.of(2020, 1, 2),
                periodeTom = LocalDate.of(2021, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            ),
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.of(2021, 1, 2),
                periodeTom = LocalDate.of(2022, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            )
        )

        val erEndringIVilkårvurderingForPerson = erEndringIVilkårvurderingForPerson(
            nåværendeVilkårResultat,
            forrigeVilkårResultat
        )

        assertThat(erEndringIVilkårvurderingForPerson, Is(true))
    }

    @Test
    fun `utledEndringsresultat skal returnere INGEN_ENDRING dersom det ikke finnes noe endringer i behandling`() {
        val endringsresultat = utledEndringsresultat(
            nåværendeAndeler = emptyList(),
            forrigeAndeler = emptyList(),
            personerFremstiltKravFor = emptyList(),
            nåværendeKompetanser = emptyList(),
            forrigeKompetanser = emptyList(),
            nåværendePersonResultat = emptySet(),
            forrigePersonResultat = emptySet(),
            nåværendeEndretAndeler = emptyList(),
            forrigeEndretAndeler = emptyList()
        )

        assertThat(endringsresultat, Is(BehandlingsresultatUtils.Endringsresultat.INGEN_ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere ENDRING dersom det finnes endringer i beløp`() {
        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val endringsresultat = utledEndringsresultat(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(forrigeAndel.copy(kalkulertUtbetalingsbeløp = 40)),
            personerFremstiltKravFor = emptyList(),
            forrigeKompetanser = emptyList(),
            nåværendeKompetanser = emptyList(),
            nåværendePersonResultat = emptySet(),
            forrigePersonResultat = emptySet(),
            nåværendeEndretAndeler = emptyList(),
            forrigeEndretAndeler = emptyList()
        )

        assertThat(endringsresultat, Is(BehandlingsresultatUtils.Endringsresultat.ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere ENDRING dersom det finnes endringer i vilkårsvurderingen`() {
        val forrigeVilkårResultater = listOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2015, 1, 1),
                periodeTom = LocalDate.of(2020, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER
            )
        )

        val nåværendeVilkårResultater = listOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2015, 1, 1),
                periodeTom = LocalDate.of(2020, 1, 1),
                begrunnelse = "begrunnelse",
                behandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
                ),
                vurderesEtter = Regelverk.EØS_FORORDNINGEN
            )
        )

        val forrigePersonResultat = PersonResultat(
            id = 0,
            vilkårsvurdering = mockk(),
            aktør = barn1Aktør,
            vilkårResultater = forrigeVilkårResultater.toMutableSet()
        )

        val nåværendePersonResultat =
            PersonResultat(
                id = 0,
                vilkårsvurdering = mockk(),
                aktør = barn1Aktør,
                vilkårResultater = nåværendeVilkårResultater.toMutableSet()
            )

        val endringsresultat = utledEndringsresultat(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = emptyList(),
            personerFremstiltKravFor = emptyList(),
            forrigeKompetanser = emptyList(),
            nåværendeKompetanser = emptyList(),
            forrigePersonResultat = setOf(forrigePersonResultat),
            nåværendePersonResultat = setOf(nåværendePersonResultat),
            nåværendeEndretAndeler = emptyList(),
            forrigeEndretAndeler = emptyList()
        )

        assertThat(endringsresultat, Is(BehandlingsresultatUtils.Endringsresultat.ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere ENDRING dersom det finnes endringer i kompetanse`() {
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

        val endringsresultat = utledEndringsresultat(
            nåværendeAndeler = emptyList(),
            forrigeAndeler = emptyList(),
            personerFremstiltKravFor = emptyList(),
            forrigeKompetanser = listOf(forrigeKompetanse),
            nåværendeKompetanser = listOf(forrigeKompetanse.copy(søkersAktivitet = SøkersAktivitet.ARBEIDER_PÅ_NORSK_SOKKEL).apply { behandlingId = nåværendeBehandling.id }),
            nåværendePersonResultat = emptySet(),
            forrigePersonResultat = emptySet(),
            nåværendeEndretAndeler = emptyList(),
            forrigeEndretAndeler = emptyList()
        )

        assertThat(endringsresultat, Is(BehandlingsresultatUtils.Endringsresultat.ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere ENDRING dersom det finnes endringer i endret utbetaling andeler`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel = lagEndretUtbetalingAndel(
            person = barn,
            prosent = BigDecimal.ZERO,
            fom = jan22,
            tom = aug22,
            årsak = Årsak.ETTERBETALING_3ÅR,
            søknadstidspunkt = des22.førsteDagIInneværendeMåned()
        )

        val endringsresultat = utledEndringsresultat(
            nåværendeAndeler = emptyList(),
            forrigeAndeler = emptyList(),
            personerFremstiltKravFor = emptyList(),
            forrigeKompetanser = emptyList(),
            nåværendeKompetanser = emptyList(),
            nåværendePersonResultat = emptySet(),
            forrigePersonResultat = emptySet(),
            forrigeEndretAndeler = listOf(forrigeEndretAndel),
            nåværendeEndretAndeler = listOf(forrigeEndretAndel.copy(årsak = Årsak.ALLEREDE_UTBETALT))
        )

        assertThat(endringsresultat, Is(BehandlingsresultatUtils.Endringsresultat.ENDRING))
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
