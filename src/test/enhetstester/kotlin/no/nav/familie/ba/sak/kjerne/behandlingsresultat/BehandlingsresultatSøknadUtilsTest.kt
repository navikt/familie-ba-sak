package no.nav.familie.ba.sak.kjerne.behandlingsresultat
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatSøknadUtils.kombinerSøknadsresultater
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatSøknadUtils.utledSøknadResultatFraAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

class BehandlingsresultatSøknadUtilsTest {

    val søker = tilfeldigPerson()

    val jan22 = YearMonth.of(2022, 1)
    val aug22 = YearMonth.of(2022, 8)

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal bare utlede resultater for personer det er framstilt krav for`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(forrigeAndel.copy()),
            personerFremstiltKravFor = emptyList(),
            endretUtbetalingAndeler = emptyList()
        )

        assertThat(søknadsResultat, Is(emptyList()))
    }

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere ingen relevante endringer dersom beløpene for periodene er lik forrige behandling`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(forrigeAndel.copy()),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList()
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER))
    }

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere innvilget dersom det finnes beløp for perioder som er annerledes enn sist og større enn 0`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 0,
                aktør = barn1Aktør
            )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(
                forrigeAndel.copy(kalkulertUtbetalingsbeløp = 1054)
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList()
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INNVILGET))
    }

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere ingen relevante endringer dersom beløp på nåværende andel er 0 og det ikke finnes noen endringsperioder eller differanse beregning`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(
                forrigeAndel.copy(kalkulertUtbetalingsbeløp = 0)
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList()
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER))
    }

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere INNVILGET dersom beløp på nåværende andel er 0 og det finnes endringsperiode som DELT_BOSTED`() {
        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1Aktør = barn1Person.aktør

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            person = barn1Person,
            fom = jan22,
            tom = aug22,
            prosent = BigDecimal(100),
            behandlingId = 123L,
            årsak = Årsak.DELT_BOSTED
        )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(
                forrigeAndel.copy(kalkulertUtbetalingsbeløp = 0)
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = listOf(endretUtbetalingAndel)
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INNVILGET))
    }

    @ParameterizedTest
    @EnumSource(value = Årsak::class, mode = EnumSource.Mode.EXCLUDE, names = ["DELT_BOSTED"])
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere AVSLÅTT dersom beløp på nåværende andel er 0 og det finnes endringsperiode som ikke er DELT_BOSTED`(
        årsak: Årsak
    ) {
        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1Aktør = barn1Person.aktør

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            person = barn1Person,
            fom = jan22,
            tom = aug22,
            prosent = BigDecimal(100),
            behandlingId = 123L,
            årsak = årsak
        )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(
                forrigeAndel.copy(kalkulertUtbetalingsbeløp = 0)
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = listOf(endretUtbetalingAndel)
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.AVSLÅTT))
    }

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere INNVILGET dersom beløpet på nåværende andel er 0 men er differanseberegnet`() {
        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1Aktør = barn1Person.aktør

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(
                forrigeAndel.copy(
                    kalkulertUtbetalingsbeløp = 0,
                    differanseberegnetPeriodebeløp = 0
                )
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList()
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INNVILGET))
    }

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere INNVILGET OG AVSLÅTT dersom 1 barn får innvilget og 1 barn får avslått`() {
        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1Aktør = barn1Person.aktør
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
                tom = aug22,
                beløp = 0,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1060,
                aktør = barn2Aktør
            )
        )

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            person = barn1Person,
            fom = jan22,
            tom = aug22,
            prosent = BigDecimal(100),
            behandlingId = 123L,
            årsak = Årsak.ALLEREDE_UTBETALT
        )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = forrigeAndeler,
            nåværendeAndeler = nåværendeAndeler,
            personerFremstiltKravFor = listOf(barn1Aktør, barn2Aktør),
            endretUtbetalingAndeler = listOf(endretUtbetalingAndel)
        )

        assertThat(søknadsResultat.size, Is(2))
        assertThat(
            søknadsResultat,
            containsInAnyOrder(
                Søknadsresultat.AVSLÅTT,
                Søknadsresultat.INNVILGET
            )
        )
    }

    @Test
    fun `kombinerSøknadsresultater skal kaste feil dersom lista ikke inneholder noe som helst`() {
        val listeMedIngenSøknadsresultat = listOf<Søknadsresultat>()

        val feil = assertThrows<Feil> { listeMedIngenSøknadsresultat.kombinerSøknadsresultater() }

        assertThat(feil.message, Is("Klarer ikke utlede søknadsresultat"))
    }

    @ParameterizedTest
    @EnumSource(value = Søknadsresultat::class)
    internal fun `kombinerSøknadsresultater skal alltid returnere innholdet som det er hvis det bare 1 resultat i lista`(
        søknadsresultat: Søknadsresultat
    ) {
        val listeMedSøknadsresultat = listOf(søknadsresultat)

        val kombinertResultat = listeMedSøknadsresultat.kombinerSøknadsresultater()

        assertThat(kombinertResultat, Is(søknadsresultat))
    }

    @ParameterizedTest
    @EnumSource(value = Søknadsresultat::class, names = ["INNVILGET", "AVSLÅTT"])
    internal fun `kombinerSøknadsresultater skal ignorere INGEN_RELEVANTE_ENDRINGER dersom den er paret opp med INNVILGET eller AVSLÅTT`(
        søknadsresultat: Søknadsresultat
    ) {
        val listeMedSøknadsresultat =
            listOf(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, søknadsresultat)

        val kombinertResultat = listeMedSøknadsresultat.kombinerSøknadsresultater()

        assertThat(kombinertResultat, Is(søknadsresultat))
    }

    @Test
    fun `kombinerSøknadsresultater skal returnere DELVIS_INNVILGET dersom lista består av INNVILGET, AVSLÅTT OG INGEN_RELEVANTE_ENDRINGER`() {
        val listeMedSøknadsresultat = listOf(
            Søknadsresultat.INNVILGET,
            Søknadsresultat.AVSLÅTT,
            Søknadsresultat.INGEN_RELEVANTE_ENDRINGER
        )

        val kombinertResultat = listeMedSøknadsresultat.kombinerSøknadsresultater()

        assertThat(kombinertResultat, Is(Søknadsresultat.DELVIS_INNVILGET))
    }

    @Test
    fun `Kombiner resultater - skal returnere FORTSATT_INNVILGET hvis det er søknad og ingen relevante endringer, og ingen opphør`() {
        val behandlingsresultat = BehandlingsresultatUtils.kombinerResultaterTilBehandlingsresultat(
            Søknadsresultat.INGEN_RELEVANTE_ENDRINGER,
            Endringsresultat.INGEN_ENDRING,
            Opphørsresultat.IKKE_OPPHØRT
        )

        assertEquals(Behandlingsresultat.FORTSATT_INNVILGET, behandlingsresultat)
    }
}
