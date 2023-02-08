package no.nav.familie.ba.sak.kjerne.behandlingsresultat
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatSøknadUtils.kombinerSøknadsresultater
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatSøknadUtils.utledSøknadsresultatForPersonerFremstiltKravFor
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

internal class BehandlingsresultatSøknadUtilsTest {

    val søker = tilfeldigPerson()
    val barn1Aktør = lagPerson(type = PersonType.BARN).aktør

    val des21 = LocalDate.of(2021, 12, 1)
    val jan22 = YearMonth.of(2022, 1)
    val aug22 = YearMonth.of(2022, 8)

    val personResultatBarn1FraDes21TilAug22 = lagPersonResultatForAktør(aktør = barn1Aktør)

    @Test
    fun `Søknadsresultat pr person - skal bare utlede resultater for personer det er framstilt krav for`() {
        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val søknadsResultat = utledSøknadsresultatForPersonerFremstiltKravFor(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(forrigeAndel.copy()),
            personerFremstiltKravFor = emptyList(),
            endretUtbetalingAndeler = emptyList(),
            nåværendePersonResultater = setOf(personResultatBarn1FraDes21TilAug22)
        )

        assertThat(søknadsResultat, Is(emptyList()))
    }

    @Test
    fun `Søknadsresultat pr person - skal returnere ingen relevante endringer dersom beløpene for periodene er lik forrige behandling`() {
        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val søknadsResultat = utledSøknadsresultatForPersonerFremstiltKravFor(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(forrigeAndel.copy()),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList(),
            nåværendePersonResultater = setOf(personResultatBarn1FraDes21TilAug22)
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER))
    }

    @Test
    fun `Søknadsresultat pr person - skal returnere innvilget dersom det finnes beløp for perioder som er annerledes enn sist og større enn 0`() {
        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 0,
                aktør = barn1Aktør
            )

        val søknadsResultat = utledSøknadsresultatForPersonerFremstiltKravFor(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(
                forrigeAndel.copy(kalkulertUtbetalingsbeløp = 1054)
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList(),
            nåværendePersonResultater = setOf(personResultatBarn1FraDes21TilAug22)
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INNVILGET))
    }

    @Test
    fun `Søknadsresultat pr person - skal returnere ingen relevante endringer dersom beløp på nåværende andel er 0 og det ikke finnes noen endringsperioder eller differanse beregning`() {
        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val søknadsResultat = utledSøknadsresultatForPersonerFremstiltKravFor(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(
                forrigeAndel.copy(kalkulertUtbetalingsbeløp = 0)
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList(),
            nåværendePersonResultater = setOf(personResultatBarn1FraDes21TilAug22)
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER))
    }

    @Test
    fun `Søknadsresultat pr person - skal returnere INNVILGET dersom beløp på nåværende andel er 0 og det finnes endringsperiode som DELT_BOSTED`() {
        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            person = lagPerson(aktør = barn1Aktør),
            fom = jan22,
            tom = aug22,
            prosent = BigDecimal(100),
            behandlingId = 123L,
            årsak = Årsak.DELT_BOSTED
        )

        val søknadsResultat = utledSøknadsresultatForPersonerFremstiltKravFor(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(
                forrigeAndel.copy(kalkulertUtbetalingsbeløp = 0)
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
            nåværendePersonResultater = setOf(personResultatBarn1FraDes21TilAug22)
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INNVILGET))
    }

    @ParameterizedTest
    @EnumSource(value = Årsak::class, mode = EnumSource.Mode.EXCLUDE, names = ["DELT_BOSTED"])
    fun `Søknadsresultat pr person - skal returnere AVSLÅTT dersom beløp på nåværende andel er 0 og det finnes endringsperiode som ikke er DELT_BOSTED`(
        årsak: Årsak
    ) {
        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            person = lagPerson(aktør = barn1Aktør),
            fom = jan22,
            tom = aug22,
            prosent = BigDecimal(100),
            behandlingId = 123L,
            årsak = årsak
        )

        val søknadsResultat = utledSøknadsresultatForPersonerFremstiltKravFor(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(
                forrigeAndel.copy(kalkulertUtbetalingsbeløp = 0)
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
            nåværendePersonResultater = setOf(personResultatBarn1FraDes21TilAug22)
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.AVSLÅTT))
    }

    @Test
    fun `Søknadsresultat pr person - skal returnere INNVILGET dersom beløpet på nåværende andel er 0 men er differanseberegnet`() {
        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val søknadsResultat = utledSøknadsresultatForPersonerFremstiltKravFor(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(
                forrigeAndel.copy(
                    kalkulertUtbetalingsbeløp = 0,
                    differanseberegnetPeriodebeløp = 0
                )
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList(),
            nåværendePersonResultater = setOf(personResultatBarn1FraDes21TilAug22)
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INNVILGET))
    }

    @Test
    fun `Søknadsresultat pr person - skal returnere INNVILGET OG AVSLÅTT dersom 1 barn får innvilget og 1 barn får avslått`() {
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
                beløp = 1060,
                aktør = barn1Aktør
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 0,
                aktør = barn2Aktør
            )
        )

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            person = lagPerson(aktør = barn2Aktør),
            fom = jan22,
            tom = aug22,
            prosent = BigDecimal(100),
            behandlingId = 123L,
            årsak = Årsak.ALLEREDE_UTBETALT
        )

        val personResultatBarn2 = lagPersonResultatForAktør(barn2Aktør)

        val søknadsResultat = utledSøknadsresultatForPersonerFremstiltKravFor(
            forrigeAndeler = forrigeAndeler,
            nåværendeAndeler = nåværendeAndeler,
            personerFremstiltKravFor = listOf(barn1Aktør, barn2Aktør),
            endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
            nåværendePersonResultater = setOf(personResultatBarn1FraDes21TilAug22, personResultatBarn2)
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
    fun `Søknadsresultat pr person - skal returnere INNVILGET dersom småbarnstillegg blir lagt til`() {
        val søker = lagPerson(type = PersonType.SØKER)

        val forrigeAndelBarn =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør
            )

        val forrigeAndelUtvidet = lagAndelTilkjentYtelse(
            fom = jan22,
            tom = aug22,
            beløp = 1054,
            aktør = søker.aktør,
            ytelseType = YtelseType.UTVIDET_BARNETRYGD
        )

        val personResultatSøker = lagPersonResultatForAktør(søker.aktør)

        val søknadsResultat = utledSøknadsresultatForPersonerFremstiltKravFor(
            forrigeAndeler = listOf(forrigeAndelBarn, forrigeAndelUtvidet),
            nåværendeAndeler = listOf(
                forrigeAndelBarn,
                forrigeAndelUtvidet,
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 630,
                    aktør = søker.aktør,
                    ytelseType = YtelseType.SMÅBARNSTILLEGG
                )
            ),
            personerFremstiltKravFor = listOf(søker.aktør),
            endretUtbetalingAndeler = emptyList(),
            nåværendePersonResultater = setOf(personResultatBarn1FraDes21TilAug22, personResultatSøker)
        ).filter { it != Søknadsresultat.INGEN_RELEVANTE_ENDRINGER }

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INNVILGET))
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

    @Test
    fun `utledResultatPåSøknad - skal kaste feil dersom man har endt opp med ingen resultater`() {
        assertThrows<Feil> {
            BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
                forrigeAndeler = emptyList(),
                nåværendeAndeler = emptyList(),
                nåværendePersonResultater = emptySet(),
                personerFremstiltKravFor = emptyList(),
                endretUtbetalingAndeler = emptyList(),
                behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                finnesUregistrerteBarn = false
            )
        }
    }

    @Test
    fun `utledResultatPåSøknad - skal returnere AVSLÅTT dersom det er søkt for barn som ikke er registrert`() {
        val resultatPåSøknad = BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = emptyList(),
            nåværendePersonResultater = emptySet(),
            personerFremstiltKravFor = emptyList(),
            endretUtbetalingAndeler = emptyList(),
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            finnesUregistrerteBarn = true
        )

        assertThat(resultatPåSøknad, Is(Søknadsresultat.AVSLÅTT))
    }

    @ParameterizedTest
    @EnumSource(value = Resultat::class, names = ["IKKE_OPPFYLT", "IKKE_VURDERT"])
    fun `utledResultatPåSøknad - skal returnere AVSLÅTT dersom behandlingen er en fødselshendelse og det finnes vilkårsvurdering som ikke er oppfylt eller vurdert`(resultat: Resultat) {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)

        val barnPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = randomAktør(),
            resultat = resultat,
            periodeFom = des21,
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN
        )

        val resultatPåSøknad = BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = emptyList(),
            nåværendePersonResultater = setOf(barnPersonResultat),
            personerFremstiltKravFor = emptyList(),
            endretUtbetalingAndeler = emptyList(),
            behandlingÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
            finnesUregistrerteBarn = false
        )

        assertThat(resultatPåSøknad, Is(Søknadsresultat.AVSLÅTT))
    }

    @Test
    fun `utledResultatPåSøknad - skal returnere AVSLÅTT dersom er eksplisitt avslag på minst en person det er framstilt krav for`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)

        val barnAktør = randomAktør()

        val barnPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = barnAktør,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = des21,
            periodeTom = LocalDate.now(),
            personType = PersonType.BARN,
            erEksplisittAvslagPåSøknad = true

        )

        val resultatPåSøknad = BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = emptyList(),
            nåværendePersonResultater = setOf(barnPersonResultat),
            personerFremstiltKravFor = listOf(barnAktør),
            endretUtbetalingAndeler = emptyList(),
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            finnesUregistrerteBarn = false
        )

        assertThat(resultatPåSøknad, Is(Søknadsresultat.AVSLÅTT))
    }

    @Test
    fun `utledResultatPåSøknad - skal returnere INNVILGET dersom barnet det er søkt for har fått andeler med positive beløp som er annerledes enn forrige gang`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)

        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1Aktør = barn1Person.aktør

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør
                )
            )

        val barnPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = barn1Aktør,
            resultat = Resultat.OPPFYLT,
            periodeFom = des21,
            periodeTom = LocalDate.now(),
            personType = PersonType.BARN
        )

        val resultatPåSøknad = BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = nåværendeAndeler,
            nåværendePersonResultater = setOf(barnPersonResultat),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList(),
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            finnesUregistrerteBarn = false
        )

        assertThat(resultatPåSøknad, Is(Søknadsresultat.INNVILGET))
    }

    @Test
    fun `utledResultatPåSøknad - skal returnere DELVIS_INNVILGET dersom det finnes et barn som har fått innvilget men også et barn som ikke er registrert`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)

        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1Aktør = barn1Person.aktør

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør
                )
            )

        val barnPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = barn1Aktør,
            resultat = Resultat.OPPFYLT,
            periodeFom = des21,
            periodeTom = LocalDate.now(),
            personType = PersonType.BARN
        )

        val resultatPåSøknad = BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = nåværendeAndeler,
            nåværendePersonResultater = setOf(barnPersonResultat),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList(),
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            finnesUregistrerteBarn = true
        )

        assertThat(resultatPåSøknad, Is(Søknadsresultat.DELVIS_INNVILGET))
    }

    @Test
    fun `utledResultatPåSøknad - skal returnere INGEN_RELEVANTE_ENDRINGER dersom barnet det er søkt for har fått helt lik andel som forrige behandling`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)

        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1Aktør = barn1Person.aktør

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør
                )
            )

        val barnPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = barn1Aktør,
            resultat = Resultat.OPPFYLT,
            periodeFom = des21,
            periodeTom = LocalDate.now(),
            personType = PersonType.BARN
        )

        val resultatPåSøknad = BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
            forrigeAndeler = andeler,
            nåværendeAndeler = andeler,
            nåværendePersonResultater = setOf(barnPersonResultat),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList(),
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            finnesUregistrerteBarn = false
        )

        assertThat(resultatPåSøknad, Is(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER))
    }

    private fun lagPersonResultatForAktør(
        aktør: Aktør,
        periodeFom: LocalDate = des21,
        periodeTom: LocalDate? = aug22.sisteDagIInneværendeMåned()
    ): PersonResultat = lagPersonResultat(aktør = aktør, vilkårsvurdering = lagVilkårsvurdering(behandling = lagBehandling(), søkerAktør = søker.aktør, resultat = Resultat.OPPFYLT), periodeFom = periodeFom, periodeTom = periodeTom, resultat = Resultat.OPPFYLT, erEksplisittAvslagPåSøknad = false)
}
