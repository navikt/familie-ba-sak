package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.mockk
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockFeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagKompetanse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatEndringUtils.erEndringIBeløpForPerson
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatEndringUtils.utledEndringsresultat
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.utledOpphørsdatoForNåværendeBehandlingMedFallback
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

class BehandlingsresultatEndringUtilsTest {
    val søker = tilfeldigPerson()

    private val barn1Aktør = randomAktør()

    val jan22 = YearMonth.of(2022, 1)
    val feb22 = YearMonth.of(2022, 2)
    val mai22 = YearMonth.of(2022, 5)
    val aug22 = YearMonth.of(2022, 8)
    val des22 = YearMonth.of(2022, 12)

    @Test
    fun `utledEndringsresultat skal returnere INGEN_ENDRING dersom det ikke finnes noe endringer i behandling`() {
        val endringsresultat =
            utledEndringsresultat(
                behandling = lagBehandling(),
                nåværendeAndeler = emptyList(),
                forrigeAndeler = emptyList(),
                personerFremstiltKravFor = emptyList(),
                nåværendeKompetanser = emptyList(),
                forrigeKompetanser = emptyList(),
                nåværendePersonResultat = emptySet(),
                forrigePersonResultat = emptySet(),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                personerIBehandling = emptySet(),
                personerIForrigeBehandling = emptySet(),
                nåMåned = YearMonth.now(),
                nåværendeUtenlandskPeriodebeløp = emptyList(),
                forrigeUtenlandskPeriodebeløp = emptyList(),
                featureToggleService = mockFeatureToggleService(),
            )

        assertThat(endringsresultat, Is(Endringsresultat.INGEN_ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere ENDRING dersom det finnes endringer i beløp`() {
        val person = lagPerson()

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = person.aktør,
            )

        val endringsresultat =
            utledEndringsresultat(
                behandling = lagBehandling(),
                nåværendeAndeler = listOf(forrigeAndel.copy(kalkulertUtbetalingsbeløp = 40)),
                forrigeAndeler = listOf(forrigeAndel),
                personerFremstiltKravFor = emptyList(),
                nåværendeKompetanser = emptyList(),
                forrigeKompetanser = emptyList(),
                nåværendePersonResultat = emptySet(),
                forrigePersonResultat = emptySet(),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                personerIBehandling = setOf(person),
                personerIForrigeBehandling = setOf(person),
                nåMåned = YearMonth.now(),
                nåværendeUtenlandskPeriodebeløp = emptyList(),
                forrigeUtenlandskPeriodebeløp = emptyList(),
                featureToggleService = mockFeatureToggleService(),
            )

        assertThat(endringsresultat, Is(Endringsresultat.ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere ENDRING dersom det finnes endringer i vilkårsvurderingen`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val barn = lagPerson(aktør = barn1Aktør, fødselsdato = fødselsdato, type = PersonType.BARN)
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2015, 2),
                    tom = YearMonth.of(2020, 1),
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2015, 2),
                    tom = YearMonth.of(2020, 1),
                ),
            )

        val forrigeVilkårResultater =
            listOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val nåværendeVilkårResultater =
            listOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                ),
            )

        val forrigePersonResultat =
            PersonResultat(
                id = 0,
                vilkårsvurdering = mockk(),
                aktør = barn1Aktør,
                vilkårResultater = forrigeVilkårResultater.toMutableSet(),
            )

        val nåværendePersonResultat =
            PersonResultat(
                id = 0,
                vilkårsvurdering = mockk(),
                aktør = barn1Aktør,
                vilkårResultater = nåværendeVilkårResultater.toMutableSet(),
            )

        val endringsresultat =
            utledEndringsresultat(
                behandling = lagBehandling(),
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                personerFremstiltKravFor = emptyList(),
                nåværendeKompetanser = emptyList(),
                forrigeKompetanser = emptyList(),
                nåværendePersonResultat = setOf(nåværendePersonResultat),
                forrigePersonResultat = setOf(forrigePersonResultat),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                personerIBehandling = setOf(barn),
                personerIForrigeBehandling = setOf(barn),
                nåMåned = YearMonth.now(),
                nåværendeUtenlandskPeriodebeløp = emptyList(),
                forrigeUtenlandskPeriodebeløp = emptyList(),
                featureToggleService = mockFeatureToggleService(),
            )

        assertThat(endringsresultat, Is(Endringsresultat.ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere ENDRING dersom det finnes endringer i kompetanse`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()

        val barnPerson = lagPerson(aktør = barn1Aktør)

        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endringsresultat =
            utledEndringsresultat(
                behandling = lagBehandling(),
                nåværendeAndeler = emptyList(),
                forrigeAndeler = emptyList(),
                personerFremstiltKravFor = emptyList(),
                nåværendeKompetanser =
                    listOf(
                        forrigeKompetanse
                            .copy(søkersAktivitet = KompetanseAktivitet.ARBEIDER_PÅ_NORSK_SOKKEL)
                            .apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanser = listOf(forrigeKompetanse),
                nåværendePersonResultat = emptySet(),
                forrigePersonResultat = emptySet(),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                personerIBehandling = setOf(barnPerson),
                personerIForrigeBehandling = setOf(barnPerson),
                nåMåned = YearMonth.now(),
                nåværendeUtenlandskPeriodebeløp = emptyList(),
                forrigeUtenlandskPeriodebeløp = emptyList(),
                featureToggleService = mockFeatureToggleService(),
            )

        assertThat(endringsresultat, Is(Endringsresultat.ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere ENDRING dersom det finnes endringer i endret utbetaling andeler`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                personer = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.ETTERBETALING_3ÅR,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            )

        val endringsresultat =
            utledEndringsresultat(
                behandling = lagBehandling(),
                nåværendeAndeler = emptyList(),
                forrigeAndeler = emptyList(),
                personerFremstiltKravFor = emptyList(),
                nåværendeKompetanser = emptyList(),
                forrigeKompetanser = emptyList(),
                nåværendePersonResultat = emptySet(),
                forrigePersonResultat = emptySet(),
                nåværendeEndretAndeler = listOf(forrigeEndretAndel.copy(årsak = Årsak.ALLEREDE_UTBETALT)),
                forrigeEndretAndeler = listOf(forrigeEndretAndel),
                personerIBehandling = setOf(barn),
                personerIForrigeBehandling = setOf(barn),
                nåMåned = YearMonth.now(),
                nåværendeUtenlandskPeriodebeløp = emptyList(),
                forrigeUtenlandskPeriodebeløp = emptyList(),
                featureToggleService = mockFeatureToggleService(),
            )

        assertThat(endringsresultat, Is(Endringsresultat.ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere ENDRING dersom det finnes endringer i utenlandsk periodebeløp`() {
        val barnPerson = lagPerson(aktør = barn1Aktør)
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()

        val forrigeUtenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                valutakode = "NOK",
                beløp = BigDecimal(500),
                intervall = Intervall.MÅNEDLIG,
                utbetalingsland = "NORGE",
                fom = jan22,
                tom = mai22,
            )

        val endringsresultat =
            utledEndringsresultat(
                behandling = lagBehandling(),
                nåværendeAndeler = emptyList(),
                forrigeAndeler = emptyList(),
                personerFremstiltKravFor = emptyList(),
                nåværendeKompetanser = emptyList(),
                forrigeKompetanser = emptyList(),
                nåværendePersonResultat = emptySet(),
                forrigePersonResultat = emptySet(),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                personerIBehandling = setOf(barnPerson),
                personerIForrigeBehandling = setOf(barnPerson),
                nåMåned = YearMonth.now(),
                nåværendeUtenlandskPeriodebeløp = listOf(forrigeUtenlandskPeriodebeløp.copy(valutakode = "SEK").apply { behandlingId = nåværendeBehandling.id }),
                forrigeUtenlandskPeriodebeløp = listOf(forrigeUtenlandskPeriodebeløp),
                featureToggleService = mockFeatureToggleService(),
            )

        assertThat(endringsresultat, Is(Endringsresultat.ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere INGEN_ENDRING hvis behandlingsårsak er FINNMARKSTILLEGG og eneste endring er i vilkårsvurdering`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)

        val forrigePersonResultat =
            PersonResultat(
                id = 0,
                vilkårsvurdering = mockk(),
                aktør = barn1Aktør,
                vilkårResultater =
                    mutableSetOf(
                        VilkårResultat(
                            personResultat = null,
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = fødselsdato,
                            periodeTom = null,
                            begrunnelse = "begrunnelse",
                            sistEndretIBehandlingId = 0,
                            utdypendeVilkårsvurderinger = listOf(),
                            vurderesEtter = Regelverk.NASJONALE_REGLER,
                        ),
                    ),
            )

        val nåværendePersonResultat =
            PersonResultat(
                id = 0,
                vilkårsvurdering = mockk(),
                aktør = barn1Aktør,
                vilkårResultater =
                    mutableSetOf(
                        VilkårResultat(
                            personResultat = null,
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = fødselsdato,
                            periodeTom = LocalDate.of(2024, 12, 31),
                            begrunnelse = "begrunnelse",
                            sistEndretIBehandlingId = 0,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS),
                            vurderesEtter = Regelverk.NASJONALE_REGLER,
                        ),
                        VilkårResultat(
                            personResultat = null,
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2025, 1, 1),
                            periodeTom = null,
                            begrunnelse = "begrunnelse",
                            sistEndretIBehandlingId = 0,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS),
                            vurderesEtter = Regelverk.NASJONALE_REGLER,
                        ),
                    ),
            )

        val barn = lagPerson(aktør = barn1Aktør, fødselsdato = fødselsdato, type = PersonType.BARN)

        val endringsresultat =
            utledEndringsresultat(
                behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG),
                nåværendeAndeler = emptyList(),
                forrigeAndeler = emptyList(),
                personerFremstiltKravFor = emptyList(),
                nåværendeKompetanser = emptyList(),
                forrigeKompetanser = emptyList(),
                nåværendePersonResultat = setOf(nåværendePersonResultat),
                forrigePersonResultat = setOf(forrigePersonResultat),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                personerIBehandling = setOf(barn),
                personerIForrigeBehandling = setOf(barn),
                nåMåned = YearMonth.now(),
                nåværendeUtenlandskPeriodebeløp = emptyList(),
                forrigeUtenlandskPeriodebeløp = emptyList(),
                featureToggleService = mockFeatureToggleService(),
            )

        assertThat(endringsresultat, Is(Endringsresultat.INGEN_ENDRING))
    }

    @Test
    fun `Endring i beløp - Skal returnere false dersom eneste endring er opphør`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = mai22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
            )

        val opphørstidspunktForBehandling =
            nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                forrigeAndelerIBehandling = forrigeAndeler,
                nåværendeEndretAndelerIBehandling = emptyList(),
                endretAndelerForForrigeBehandling = emptyList(),
            )

        val erEndringIBeløp =
            erEndringIBeløpForPerson(
                nåværendeAndelerForPerson = nåværendeAndeler,
                forrigeAndelerForPerson = forrigeAndeler,
                opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                erFremstiltKravForPerson = false,
                nåMåned = YearMonth.now(),
            )

        assertEquals(false, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere true når beløp i periode har gått fra større enn 0 til null og det er søkt for person`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør
        val personerFramstiltKravFor = listOf(barn1Aktør)

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = mai22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val erEndringIBeløp =
            listOf(barn1Aktør, barn2Aktør).any { aktør ->
                val erFremstiltKravForPerson = personerFramstiltKravFor.contains(aktør)

                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = forrigeAndeler,
                        nåværendeEndretAndelerIBehandling = emptyList(),
                        endretAndelerForForrigeBehandling = emptyList(),
                    )

                val erEndringIBeløpForPerson =
                    erEndringIBeløpForPerson(
                        nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør },
                        forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør },
                        opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                        erFremstiltKravForPerson = erFremstiltKravForPerson,
                        nåMåned = YearMonth.now(),
                    )

                erEndringIBeløpForPerson
            }

        assertEquals(true, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere false når beløp i periode har gått fra større enn 0 til at annet tall større enn 0 og det er søkt for person`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val personerFramstiltKravFor = listOf(barn1Aktør)

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = mai22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = mai22.plusMonths(1),
                    tom = aug22,
                    beløp = 527,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val erEndringIBeløp =
            listOf(barn1Aktør, barn2Aktør).any { aktør ->
                val erFremstiltKravForPerson = personerFramstiltKravFor.contains(aktør)

                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = forrigeAndeler,
                        nåværendeEndretAndelerIBehandling = emptyList(),
                        endretAndelerForForrigeBehandling = emptyList(),
                    )

                val erEndringIBeløpForPerson =
                    erEndringIBeløpForPerson(
                        nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør },
                        forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør },
                        opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                        erFremstiltKravForPerson = erFremstiltKravForPerson,
                        nåMåned = YearMonth.now(),
                    )

                erEndringIBeløpForPerson
            }

        assertEquals(false, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere true når beløp i periode har gått fra null til et tall større enn 0 og det ikke er søkt for person`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = des22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val erEndringIBeløp =
            listOf(barn1Aktør, barn2Aktør).any { aktør ->
                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = forrigeAndeler,
                        nåværendeEndretAndelerIBehandling = emptyList(),
                        endretAndelerForForrigeBehandling = emptyList(),
                    )

                val erEndringIBeløpForPerson =
                    erEndringIBeløpForPerson(
                        nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør },
                        forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør },
                        opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                        erFremstiltKravForPerson = false,
                        nåMåned = YearMonth.now(),
                    )

                erEndringIBeløpForPerson
            }

        assertEquals(true, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere false når beløp i periode har gått fra null til et tall større enn 0 og det er søkt for person`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val personerFramstiltKravFor = listOf(barn1Aktør)

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = des22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val erEndringIBeløp =
            listOf(barn1Aktør, barn2Aktør).any { aktør ->
                val erFremstiltKravForPerson = personerFramstiltKravFor.contains(aktør)

                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = forrigeAndeler,
                        nåværendeEndretAndelerIBehandling = emptyList(),
                        endretAndelerForForrigeBehandling = emptyList(),
                    )

                val erEndringIBeløpForPerson =
                    erEndringIBeløpForPerson(
                        nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør },
                        forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør },
                        opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                        erFremstiltKravForPerson = erFremstiltKravForPerson,
                        nåMåned = YearMonth.now(),
                    )

                erEndringIBeløpForPerson
            }

        assertEquals(false, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere true når beløp i periode har gått fra større enn 0 til at annet tall større enn 0 og det ikke er søkt for person`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = mai22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = mai22.plusMonths(1),
                    tom = aug22,
                    beløp = 527,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val erEndringIBeløp =
            listOf(barn1Aktør, barn2Aktør).any { aktør ->

                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = forrigeAndeler,
                        nåværendeEndretAndelerIBehandling = emptyList(),
                        endretAndelerForForrigeBehandling = emptyList(),
                    )

                val erEndringIBeløpForPerson =
                    erEndringIBeløpForPerson(
                        nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør },
                        forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør },
                        opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                        erFremstiltKravForPerson = false,
                        nåMåned = YearMonth.now(),
                    )

                erEndringIBeløpForPerson
            }

        assertEquals(true, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere true når beløp i periode har gått fra null til 0kr og det ikke er søkt for person`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 0,
                    aktør = barn1Aktør,
                ),
            )

        val erEndringIBeløp =
            listOf(barn1Aktør).any { aktør ->

                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = emptyList(),
                        nåværendeEndretAndelerIBehandling = emptyList(),
                        endretAndelerForForrigeBehandling = emptyList(),
                    )

                val erEndringIBeløpForPerson =
                    erEndringIBeløpForPerson(
                        nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør },
                        forrigeAndelerForPerson = emptyList(),
                        opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                        erFremstiltKravForPerson = false,
                        nåMåned = YearMonth.now(),
                    )

                erEndringIBeløpForPerson
            }

        assertEquals(true, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere true hvis utvidet ikke er endret men småbarnstillegg kun er lagt på`() {
        val søker = lagPerson(type = PersonType.SØKER).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = søker,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = søker,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
                lagAndelTilkjentYtelse(
                    fom = mai22,
                    tom = aug22,
                    beløp = 630,
                    aktør = søker,
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val erEndringIBeløp =
            listOf(søker, barn2Aktør).any { aktør ->

                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = forrigeAndeler,
                        nåværendeEndretAndelerIBehandling = emptyList(),
                        endretAndelerForForrigeBehandling = emptyList(),
                    )

                val erEndringIBeløpForPerson =
                    erEndringIBeløpForPerson(
                        nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør },
                        forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør },
                        opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                        erFremstiltKravForPerson = false,
                        nåMåned = YearMonth.now(),
                    )

                erEndringIBeløpForPerson
            }

        assertEquals(true, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal ikke bry seg om endringer lengre enn 1 måneder fram i tid`() {
        val søker = lagPerson(type = PersonType.SØKER).aktør
        val barnAktør = lagPerson(type = PersonType.BARN).aktør
        val denneMåned = YearMonth.now()
        val enMånedFramITid = denneMåned.plusMonths(1)
        val toMånederFramITid = enMånedFramITid.plusMonths(1)

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = denneMåned,
                    tom = enMånedFramITid,
                    beløp = 1054,
                    aktør = søker,
                ),
                lagAndelTilkjentYtelse(
                    fom = toMånederFramITid,
                    tom = toMånederFramITid,
                    beløp = 1054,
                    aktør = barnAktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = denneMåned,
                    tom = enMånedFramITid,
                    beløp = 1054,
                    aktør = søker,
                ),
                lagAndelTilkjentYtelse(
                    fom = toMånederFramITid,
                    tom = toMånederFramITid,
                    beløp = 1070,
                    aktør = barnAktør,
                ),
            )

        val opphørstidspunktForBehandling =
            nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                forrigeAndelerIBehandling = forrigeAndeler,
                nåværendeEndretAndelerIBehandling = emptyList(),
                endretAndelerForForrigeBehandling = emptyList(),
            )

        val erEndringIBeløp =
            erEndringIBeløpForPerson(
                nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == barnAktør },
                forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == barn1Aktør },
                opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                erFremstiltKravForPerson = false,
                nåMåned = YearMonth.now(),
            )

        assertEquals(false, erEndringIBeløp)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere true hvis årsak er endret`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                personer = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.ETTERBETALING_3ÅR,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            )

        val erEndringIEndretAndeler =
            erEndringIEndretUtbetalingAndelerForPerson(
                forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndel.copy(årsak = Årsak.ALLEREDE_UTBETALT)),
            )

        assertTrue(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere true hvis avtaletidspunktDeltBosted er endret`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                personer = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val erEndringIEndretAndeler =
            erEndringIEndretUtbetalingAndelerForPerson(
                forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndel.copy(avtaletidspunktDeltBosted = feb22.førsteDagIInneværendeMåned())),
            )

        assertTrue(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere true hvis søknadstidspunkt er endret og det ikke var satt før`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                personer = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = null,
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val erEndringIEndretAndeler =
            erEndringIEndretUtbetalingAndelerForPerson(
                forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndel.copy(søknadstidspunkt = feb22.førsteDagIInneværendeMåned())),
            )

        assertTrue(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere false hvis prosent er endret`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                personer = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val erEndringIEndretAndeler =
            erEndringIEndretUtbetalingAndelerForPerson(
                forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndel.copy(prosent = BigDecimal(100))),
            )

        assertFalse(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere true hvis eneste endring er at perioden blir lenger`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                personer = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val erEndringIEndretAndeler =
            erEndringIEndretUtbetalingAndelerForPerson(
                forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndel.copy(tom = des22)),
            )

        assertTrue(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere true hvis endringsperiode oppstår i nåværende behandling`() {
        val barn = lagPerson(type = PersonType.BARN)
        val nåværendeEndretAndel =
            lagEndretUtbetalingAndel(
                personer = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val erEndringIEndretAndeler =
            erEndringIEndretUtbetalingAndelerForPerson(
                forrigeEndretAndelerForPerson = emptyList(),
                nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
            )

        assertTrue(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere true hvis et av to barn har endring på årsak`() {
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val forrigeEndretAndelBarn1 =
            lagEndretUtbetalingAndel(
                personer = setOf(barn1),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val forrigeEndretAndelBarn2 =
            lagEndretUtbetalingAndel(
                personer = setOf(barn2),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.ETTERBETALING_3ÅR,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            )

        val erEndringIEndretAndeler =
            listOf(barn1, barn2).any {
                erEndringIEndretUtbetalingAndelerForPerson(
                    forrigeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2).filter { endretAndel -> endretAndel.personer.contains(it) },
                    nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2.copy(årsak = Årsak.ALLEREDE_UTBETALT)).filter { endretAndel -> endretAndel.personer.contains(it) },
                )
            }

        assertTrue(erEndringIEndretAndeler)
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
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson = listOf(forrigeKompetanse.copy().apply { behandlingId = nåværendeBehandling.id }),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
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
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse.copy(søkersAktivitetsland = "DK").apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
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
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse
                            .copy(søkersAktivitet = KompetanseAktivitet.ARBEIDER_PÅ_NORSK_SOKKEL)
                            .apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
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
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse
                            .copy(annenForeldersAktivitetsland = "DK")
                            .apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
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
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse
                            .copy(annenForeldersAktivitet = KompetanseAktivitet.FORSIKRET_I_BOSTEDSLAND)
                            .apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
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
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse.copy(barnetsBostedsland = "DK").apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
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
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse
                            .copy(resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND)
                            .apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
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
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse
                            .copy(fom = YearMonth.now().minusMonths(10))
                            .apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
            )

        assertEquals(false, endring)
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere false dersom vilkårresultatene er helt like`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.of(2020, 1, 2),
                    periodeTom = LocalDate.of(2022, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.of(2020, 1, 2),
                    periodeTom = LocalDate.of(2022, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()
        val barn = lagPerson(aktør = aktør, fødselsdato = fødselsdato, type = PersonType.BARN)

        val erEndringIVilkårvurderingForPerson =
            erEndringIVilkårsvurderingForPerson(
                nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                forrigePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                personIBehandling = barn,
                personIForrigeBehandling = barn,
                tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = TIDENES_MORGEN.toYearMonth(),
                featureToggleService = mockFeatureToggleService(),
            )

        assertThat(erEndringIVilkårvurderingForPerson, Is(false))
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere true dersom det har vært endringer i regelverk`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                ),
            )

        val aktør = randomAktør()
        val barn = lagPerson(aktør = aktør, fødselsdato = fødselsdato, type = PersonType.BARN)

        val erEndringIVilkårvurderingForPerson =
            erEndringIVilkårsvurderingForPerson(
                nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                forrigePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                personIBehandling = barn,
                personIForrigeBehandling = barn,
                tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = TIDENES_MORGEN.toYearMonth(),
                featureToggleService = mockFeatureToggleService(),
            )

        assertThat(erEndringIVilkårvurderingForPerson, Is(true))
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere true dersom det har vært endringer i utdypendevilkårsvurdering`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.VURDERING_ANNET_GRUNNLAG,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()
        val barn = lagPerson(aktør = aktør, fødselsdato = fødselsdato, type = PersonType.BARN)

        val erEndringIVilkårvurderingForPerson =
            erEndringIVilkårsvurderingForPerson(
                nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                forrigePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                personIBehandling = barn,
                personIForrigeBehandling = barn,
                tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = TIDENES_MORGEN.toYearMonth(),
                featureToggleService = mockFeatureToggleService(),
            )

        assertThat(erEndringIVilkårvurderingForPerson, Is(true))
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere true dersom det har oppstått splitt i vilkårsvurderingen`() {
        val fødselsdato = jan22.førsteDagIInneværendeMåned()
        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = null,
                    begrunnelse = "",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger = listOf(),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = mai22.atDay(7),
                    begrunnelse = "",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger = listOf(),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = mai22.atDay(8),
                    periodeTom = null,
                    begrunnelse = "",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger = listOf(),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()
        val barn = lagPerson(aktør = aktør, fødselsdato = fødselsdato, type = PersonType.BARN)

        val erEndringIVilkårvurderingForPerson =
            erEndringIVilkårsvurderingForPerson(
                nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                forrigePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                personIBehandling = barn,
                personIForrigeBehandling = barn,
                tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = TIDENES_MORGEN.toYearMonth(),
                featureToggleService = mockFeatureToggleService(),
            )

        assertThat(erEndringIVilkårvurderingForPerson, Is(true))
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere false hvis det kun er opphørt`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = null,
                    begrunnelse = "begrunnelse",
                    sistEndretIBehandlingId = 0,
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                            UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()
        val barn = lagPerson(aktør = aktør, fødselsdato = fødselsdato, type = PersonType.BARN)

        val erEndringIVilkårvurderingForPerson =
            erEndringIVilkårsvurderingForPerson(
                nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                forrigePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                personIBehandling = barn,
                personIForrigeBehandling = barn,
                tidligsteRelevanteFomDatoForPersonIVilkårsvurdering = TIDENES_MORGEN.toYearMonth(),
                featureToggleService = mockFeatureToggleService(),
            )

        assertThat(erEndringIVilkårvurderingForPerson, Is(false))
    }

    private fun lagPersonResultatFraVilkårResultater(
        vilkårResultater: Set<VilkårResultat>,
        aktør: Aktør,
    ): PersonResultat {
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = lagBehandling(), resultat = Resultat.OPPFYLT, søkerAktør = randomAktør())
        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = aktør)

        personResultat.setSortedVilkårResultater(vilkårResultater)

        return personResultat
    }
}
