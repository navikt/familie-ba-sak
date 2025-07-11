package no.nav.familie.ba.sak.kjerne.endretutbetaling

import io.mockk.clearAllMocks
import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtAlleOpprettedeEndringerErUtfylt
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtEndringerErTilknyttetAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerDeltBosted
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerIngenOverlappendeEndring
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerPeriodeInnenforTilkjentytelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerÅrsak
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndretUtbetalingAndelValideringTest {
    val søker = lagPerson(type = PersonType.SØKER)
    val barn = lagPerson(type = PersonType.BARN)
    val endretUtbetalingAndelUtvidetNullutbetaling =
        endretUtbetalingAndel(setOf(søker), YtelseType.UTVIDET_BARNETRYGD, BigDecimal.ZERO)
    val endretUtbetalingAndelDeltBostedNullutbetaling =
        endretUtbetalingAndel(setOf(barn), YtelseType.ORDINÆR_BARNETRYGD, BigDecimal.ZERO)

    @AfterAll
    fun clearMocks() {
        clearAllMocks()
    }

    @ParameterizedTest
    @EnumSource(Årsak::class, mode = EnumSource.Mode.EXCLUDE, names = ["DELT_BOSTED"])
    fun `skal sjekke at en endret periode ikke overlapper med eksisterende endringsperioder`(
        årsak: Årsak,
    ) {
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()
        val endretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = 1,
                personer = mutableSetOf(barn1),
                fom = YearMonth.of(2020, 2),
                tom = YearMonth.of(2020, 6),
                årsak = Årsak.DELT_BOSTED,
                begrunnelse = "begrunnelse",
                prosent = BigDecimal(100),
                søknadstidspunkt = LocalDate.now(),
                avtaletidspunktDeltBosted = LocalDate.now(),
            )

        val feil =
            assertThrows<FunksjonellFeil> {
                validerIngenOverlappendeEndring(
                    endretUtbetalingAndel,
                    listOf(
                        endretUtbetalingAndel.copy(
                            fom = YearMonth.of(2018, 4),
                            tom = YearMonth.of(2019, 2),
                            årsak = årsak,
                        ),
                        endretUtbetalingAndel.copy(
                            fom = YearMonth.of(2020, 4),
                            tom = YearMonth.of(2021, 2),
                            årsak = årsak,
                        ),
                    ),
                )
            }
        assertEquals(
            "Perioden som blir forsøkt lagt til overlapper med eksisterende periode for en av personene.",
            feil.melding,
        )

        // Resterende kall skal validere ok.
        validerIngenOverlappendeEndring(
            endretUtbetalingAndel,
            listOf(
                endretUtbetalingAndel.copy(
                    fom = endretUtbetalingAndel.tom!!.plusMonths(1),
                    tom = endretUtbetalingAndel.tom!!.plusMonths(10),
                ),
            ),
        )
        validerIngenOverlappendeEndring(
            endretUtbetalingAndel,
            listOf(endretUtbetalingAndel.copy(personer = mutableSetOf(barn2))),
        )
    }

    @Test
    fun `skal sjekke at en endret periode ikke strekker seg utover ytterpunktene for tilkjent ytelse`() {
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 2),
                    tom = YearMonth.of(2020, 4),
                    person = barn1,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 7),
                    tom = YearMonth.of(2020, 10),
                    person = barn1,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2018, 10),
                    tom = YearMonth.of(2021, 10),
                    person = barn2,
                ),
            )

        val endretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = 1,
                personer = mutableSetOf(barn1),
                fom = YearMonth.of(2020, 2),
                tom = YearMonth.of(2020, 6),
                årsak = Årsak.DELT_BOSTED,
                begrunnelse = "begrunnelse",
                prosent = BigDecimal(100),
                søknadstidspunkt = LocalDate.now(),
                avtaletidspunktDeltBosted = LocalDate.now(),
            )

        var feil =
            assertThrows<FunksjonellFeil> {
                validerPeriodeInnenforTilkjentytelse(endretUtbetalingAndel, emptyList())
            }
        assertEquals(
            "Det er ingen tilkjent ytelse for en av personene det blir forsøkt lagt til en endret periode for.",
            feil.melding,
        )

        val endretUtbetalingAndelerSomIkkeValiderer =
            listOf(
                endretUtbetalingAndel.copy(fom = YearMonth.of(2020, 1), tom = YearMonth.of(2020, 11)),
                endretUtbetalingAndel.copy(fom = YearMonth.of(2020, 1), tom = YearMonth.of(2020, 4)),
                endretUtbetalingAndel.copy(fom = YearMonth.of(2020, 2), tom = YearMonth.of(2020, 11)),
            )

        endretUtbetalingAndelerSomIkkeValiderer.forEach {
            feil =
                assertThrows {
                    validerPeriodeInnenforTilkjentytelse(it, andelTilkjentYtelser)
                }
            assertEquals(
                "Det er ingen tilkjent ytelse for en av personene det blir forsøkt lagt til en endret periode for.",
                feil.melding,
            )
        }

        val endretUtbetalingAndelerSomValiderer =
            listOf(
                endretUtbetalingAndel,
                endretUtbetalingAndel.copy(fom = YearMonth.of(2020, 2), tom = YearMonth.of(2020, 10)),
                endretUtbetalingAndel.copy(fom = YearMonth.of(2018, 10), tom = YearMonth.of(2021, 10), personer = mutableSetOf(barn2)),
            )

        endretUtbetalingAndelerSomValiderer.forEach { validerPeriodeInnenforTilkjentytelse(it, andelTilkjentYtelser) }
    }

    @Test
    fun `skal sjekke at en endret periode ikke strekker seg utover ytterpunktene for tilkjent ytelse for flere barn`() {
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2020, 2),
                    person = barn1,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 2),
                    tom = YearMonth.of(2020, 3),
                    person = barn2,
                ),
            )

        val gyldigEndretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = 1,
                personer = mutableSetOf(barn1, barn2),
                fom = YearMonth.of(2020, 2),
                tom = YearMonth.of(2020, 2),
                årsak = Årsak.ALLEREDE_UTBETALT,
                begrunnelse = "Allerede utbetalt",
                prosent = BigDecimal.ZERO,
                søknadstidspunkt = LocalDate.now(),
            )

        assertDoesNotThrow {
            validerPeriodeInnenforTilkjentytelse(gyldigEndretUtbetalingAndel, andelTilkjentYtelser)
        }

        val endretUtbetalingAndelSomErUgyldigForBarn1 =
            gyldigEndretUtbetalingAndel.copy(
                fom = YearMonth.of(2020, 2),
                tom = YearMonth.of(2020, 3),
            )

        assertThrows<FunksjonellFeil> {
            validerPeriodeInnenforTilkjentytelse(endretUtbetalingAndelSomErUgyldigForBarn1, andelTilkjentYtelser)
        }.also {
            assertThat(it.melding).isEqualTo("Det er ingen tilkjent ytelse for en av personene det blir forsøkt lagt til en endret periode for.")
        }

        val endretUtbetalingAndelSomErUgyldigForBarn2 =
            gyldigEndretUtbetalingAndel.copy(
                fom = YearMonth.of(2020, 1),
                tom = YearMonth.of(2020, 2),
            )

        assertThrows<FunksjonellFeil> {
            validerPeriodeInnenforTilkjentytelse(endretUtbetalingAndelSomErUgyldigForBarn2, andelTilkjentYtelser)
        }.also {
            assertThat(it.melding).isEqualTo("Det er ingen tilkjent ytelse for en av personene det blir forsøkt lagt til en endret periode for.")
        }
    }

    @Test
    fun `Skal kaste feil hvis endringsperiode med årsak delt bosted ikke overlapper helt med delt bosted periode`() {
        val endretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = 1,
                personer = mutableSetOf(tilfeldigPerson()),
                fom = YearMonth.of(2020, 2),
                tom = YearMonth.of(2020, 6),
                årsak = Årsak.DELT_BOSTED,
                begrunnelse = "begrunnelse",
                prosent = BigDecimal(100),
                søknadstidspunkt = LocalDate.now(),
                avtaletidspunktDeltBosted = LocalDate.now(),
            )
        assertThrows<FunksjonellFeil> {
            validerDeltBosted(
                endretUtbetalingAndel = endretUtbetalingAndel,
                deltBostedPerioder =
                    listOf(
                        MånedPeriode(
                            fom = YearMonth.of(2020, 2),
                            tom = YearMonth.of(2020, 4),
                        ),
                    ),
            )
        }

        assertThrows<FunksjonellFeil> {
            validerDeltBosted(
                endretUtbetalingAndel = endretUtbetalingAndel,
                deltBostedPerioder =
                    listOf(
                        MånedPeriode(
                            fom = YearMonth.of(2020, 7),
                            tom = YearMonth.of(2020, 10),
                        ),
                    ),
            )
        }
    }

    @Test
    fun `Skal kaste feil hvis endringsårsak er delt bosted og det ikke eksisterer delt bosted perioder`() {
        val endretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = 1,
                personer = mutableSetOf(tilfeldigPerson()),
                fom = YearMonth.of(2020, 2),
                tom = YearMonth.of(2020, 6),
                årsak = Årsak.DELT_BOSTED,
                begrunnelse = "begrunnelse",
                prosent = BigDecimal(100),
                søknadstidspunkt = LocalDate.now(),
                avtaletidspunktDeltBosted = LocalDate.now(),
            )
        assertThrows<FunksjonellFeil> {
            validerDeltBosted(
                endretUtbetalingAndel = endretUtbetalingAndel,
                deltBostedPerioder = emptyList(),
            )
        }
    }

    @Test
    fun `Skal ikke kaste feil hvis endringsperiode med årsak delt bosted overlapper helt med delt bosted periode`() {
        val endretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = 1,
                personer = mutableSetOf(tilfeldigPerson()),
                fom = YearMonth.of(2020, 2),
                tom = YearMonth.of(2020, 6),
                årsak = Årsak.DELT_BOSTED,
                begrunnelse = "begrunnelse",
                prosent = BigDecimal(100),
                søknadstidspunkt = LocalDate.now(),
                avtaletidspunktDeltBosted = LocalDate.now(),
            )
        assertDoesNotThrow {
            validerDeltBosted(
                endretUtbetalingAndel = endretUtbetalingAndel,
                deltBostedPerioder =
                    listOf(
                        MånedPeriode(
                            fom = YearMonth.of(2020, 2),
                            tom = YearMonth.of(2020, 6),
                        ),
                    ),
            )
        }
        assertDoesNotThrow {
            validerDeltBosted(
                endretUtbetalingAndel = endretUtbetalingAndel,
                deltBostedPerioder =
                    listOf(
                        MånedPeriode(
                            fom = YearMonth.of(2020, 1),
                            tom = YearMonth.of(2020, 7),
                        ),
                    ),
            )
        }
    }

    @Test
    fun `sjekk at alle endrede utbetalingsandeler validerer`() {
        val endretUtbetalingAndel1 = lagEndretUtbetalingAndel(personer = setOf(tilfeldigPerson()))
        val endretUtbetalingAndel2 = lagEndretUtbetalingAndel(personer = setOf(tilfeldigPerson()))
        validerAtAlleOpprettedeEndringerErUtfylt(listOf(endretUtbetalingAndel1, endretUtbetalingAndel2))

        val feil =
            assertThrows<FunksjonellFeil> {
                validerAtAlleOpprettedeEndringerErUtfylt(
                    listOf(
                        endretUtbetalingAndel1,
                        endretUtbetalingAndel2.copy(fom = null),
                    ),
                )
            }
        assertEquals(
            "Det er opprettet instanser av EndretUtbetalingandel som ikke er fylt ut før navigering til neste steg.",
            feil.melding,
        )
    }

    @Test
    fun `sjekk at alle endrede utbetalingsandeler er tilknyttet andeltilkjentytelser`() {
        val endretUtbetalingAndel1 = lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(personer = setOf(tilfeldigPerson()))
        val feil =
            assertThrows<FunksjonellFeil> {
                validerAtEndringerErTilknyttetAndelTilkjentYtelse(listOf(endretUtbetalingAndel1))
            }
        assertEquals(
            "Det er opprettet instanser av EndretUtbetalingandel som ikke er tilknyttet noen andeler. De må enten lagres eller slettes av SB.",
            feil.melding,
        )

        val andelTilkjentYtelse: AndelTilkjentYtelse = mockk()
        validerAtEndringerErTilknyttetAndelTilkjentYtelse(
            listOf(
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    personer = setOf(tilfeldigPerson()),
                    andelTilkjentYtelser = mutableListOf(andelTilkjentYtelse),
                ),
            ),
        )
    }

    @Test
    fun `Skal finne riktige delt bosted perioder for barn, og slå sammen de som er sammenhengende`() {
        val behandling = lagBehandling()

        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().plusMonths(7)

        val barn = lagPerson(type = PersonType.BARN, fødselsdato = fom)
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val personResultatForPerson =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn.aktør,
            )

        val vilkårResultaterForPerson = mutableSetOf<VilkårResultat>()
        Vilkår
            .hentVilkårFor(
                personType = PersonType.BARN,
                fagsakType = FagsakType.NORMAL,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            ).forEach {
                if (it == Vilkår.BOR_MED_SØKER) {
                    vilkårResultaterForPerson.addAll(
                        listOf(
                            VilkårResultat(
                                personResultat = personResultatForPerson,
                                periodeFom = fom,
                                periodeTom = LocalDate.now().minusMonths(1).sisteDagIMåned(),
                                vilkårType = it,
                                resultat = Resultat.OPPFYLT,
                                begrunnelse = "",
                                sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                            ),
                            VilkårResultat(
                                personResultat = personResultatForPerson,
                                periodeFom = LocalDate.now().førsteDagIInneværendeMåned(),
                                periodeTom = tom,
                                vilkårType = it,
                                resultat = Resultat.OPPFYLT,
                                begrunnelse = "",
                                sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                            ),
                        ),
                    )
                } else {
                    VilkårResultat(
                        personResultat = personResultatForPerson,
                        periodeFom = fom,
                        periodeTom = tom,
                        vilkårType = it,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "",
                        sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                        utdypendeVilkårsvurderinger = emptyList(),
                    )
                }
            }
        personResultatForPerson.setSortedVilkårResultater(vilkårResultaterForPerson)

        vilkårsvurdering.personResultater =
            setOf(
                personResultatForPerson,
                lagPersonResultat(
                    vilkårsvurdering = vilkårsvurdering,
                    person = lagPerson(type = PersonType.BARN, fødselsdato = fom.minusYears(4)),
                    resultat = Resultat.OPPFYLT,
                    personType = PersonType.BARN,
                    periodeFom = fom.minusMonths(3),
                    periodeTom = tom.plusMonths(4),
                    erDeltBosted = true,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                ),
            )

        val deltBostedPerioder = finnDeltBostedPerioder(person = barn, vilkårsvurdering = vilkårsvurdering)

        assertTrue(deltBostedPerioder.size == 1)
        assertEquals(fom.plusMonths(1).førsteDagIInneværendeMåned(), deltBostedPerioder.single().fom)
        assertEquals(tom.sisteDagIMåned(), deltBostedPerioder.single().tom)
    }

    @Test
    fun `Skal finne riktige delt bosted perioder for barn og ikke slå de sammen når de ikke er sammenhengde`() {
        val behandling = lagBehandling()
        val barn = lagPerson(type = PersonType.BARN)
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val fom1 = LocalDate.now().minusMonths(5)
        val tom1 = LocalDate.now().minusMonths(2)
        val fom2 = LocalDate.now()
        val tom2 = LocalDate.now().plusMonths(7)
        val personResultatForPerson =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn.aktør,
            )

        val vilkårResultaterForPerson = mutableSetOf<VilkårResultat>()
        Vilkår
            .hentVilkårFor(
                personType = PersonType.BARN,
                fagsakType = FagsakType.NORMAL,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            ).forEach {
                if (it == Vilkår.BOR_MED_SØKER) {
                    vilkårResultaterForPerson.addAll(
                        listOf(
                            VilkårResultat(
                                personResultat = personResultatForPerson,
                                periodeFom = fom1,
                                periodeTom = tom1,
                                vilkårType = it,
                                resultat = Resultat.OPPFYLT,
                                begrunnelse = "",
                                sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                            ),
                            VilkårResultat(
                                personResultat = personResultatForPerson,
                                periodeFom = fom2,
                                periodeTom = tom2,
                                vilkårType = it,
                                resultat = Resultat.OPPFYLT,
                                begrunnelse = "",
                                sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                            ),
                        ),
                    )
                } else {
                    VilkårResultat(
                        personResultat = personResultatForPerson,
                        periodeFom = fom1,
                        periodeTom = tom2,
                        vilkårType = it,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "",
                        sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                        utdypendeVilkårsvurderinger = emptyList(),
                    )
                }
            }
        personResultatForPerson.setSortedVilkårResultater(vilkårResultaterForPerson)

        vilkårsvurdering.personResultater =
            setOf(
                personResultatForPerson,
                lagPersonResultat(
                    vilkårsvurdering = vilkårsvurdering,
                    person = lagPerson(type = PersonType.BARN, fødselsdato = fom1.minusYears(5)),
                    resultat = Resultat.OPPFYLT,
                    personType = PersonType.BARN,
                    periodeFom = fom1.minusMonths(3),
                    periodeTom = tom2.plusMonths(4),
                    erDeltBosted = true,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    lagFullstendigVilkårResultat = true,
                ),
            )

        val deltBostedPerioder = finnDeltBostedPerioder(person = barn, vilkårsvurdering = vilkårsvurdering)

        assertTrue(deltBostedPerioder.size == 2)

        val førstePeriode = deltBostedPerioder.get(0)
        val andrePeriode = deltBostedPerioder.get(1)

        assertEquals(fom1.plusMonths(1).førsteDagIInneværendeMåned(), førstePeriode.fom)
        assertEquals(tom1.sisteDagIMåned(), førstePeriode.tom)
        assertEquals(fom2.plusMonths(1).førsteDagIInneværendeMåned(), andrePeriode.fom)
        assertEquals(tom2.sisteDagIMåned(), andrePeriode.tom)
    }

    @Test
    fun `Skal finne riktige delt bosted perioder for søker, og slå sammen de som er sammenhengende`() {
        val behandling = lagBehandling()
        val barn = lagPerson(type = PersonType.BARN)
        val søker = lagPerson(type = PersonType.SØKER)
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val fomBarn1 = LocalDate.now().minusMonths(5)
        val tomBarn1 = LocalDate.now().plusMonths(7)
        val fomBarn2 = fomBarn1.minusMonths(5)
        val personResultatForPerson =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn.aktør,
            )

        val vilkårResultaterForPerson = mutableSetOf<VilkårResultat>()
        Vilkår
            .hentVilkårFor(
                personType = PersonType.BARN,
                fagsakType = FagsakType.NORMAL,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            ).forEach {
                if (it == Vilkår.BOR_MED_SØKER) {
                    vilkårResultaterForPerson.addAll(
                        listOf(
                            VilkårResultat(
                                personResultat = personResultatForPerson,
                                periodeFom = fomBarn1,
                                periodeTom = LocalDate.now().minusMonths(1).sisteDagIMåned(),
                                vilkårType = it,
                                resultat = Resultat.OPPFYLT,
                                begrunnelse = "",
                                sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                            ),
                            VilkårResultat(
                                personResultat = personResultatForPerson,
                                periodeFom = LocalDate.now().førsteDagIInneværendeMåned(),
                                periodeTom = tomBarn1,
                                vilkårType = it,
                                resultat = Resultat.OPPFYLT,
                                begrunnelse = "",
                                sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                            ),
                        ),
                    )
                } else {
                    VilkårResultat(
                        personResultat = personResultatForPerson,
                        periodeFom = fomBarn1,
                        periodeTom = tomBarn1,
                        vilkårType = it,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "",
                        sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                        utdypendeVilkårsvurderinger = emptyList(),
                    )
                }
            }
        personResultatForPerson.setSortedVilkårResultater(vilkårResultaterForPerson)

        vilkårsvurdering.personResultater =
            setOf(
                personResultatForPerson,
                lagPersonResultat(
                    vilkårsvurdering = vilkårsvurdering,
                    person = lagPerson(type = PersonType.BARN, fødselsdato = fomBarn2),
                    resultat = Resultat.OPPFYLT,
                    personType = PersonType.BARN,
                    periodeFom = fomBarn2,
                    periodeTom = fomBarn1,
                    erDeltBosted = true,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    lagFullstendigVilkårResultat = true,
                ),
            )

        val deltBostedPerioder = finnDeltBostedPerioder(person = søker, vilkårsvurdering = vilkårsvurdering)

        assertTrue(deltBostedPerioder.size == 1)
        assertEquals(fomBarn2.plusMonths(1).førsteDagIInneværendeMåned(), deltBostedPerioder.single().fom)
        assertEquals(tomBarn1.sisteDagIMåned(), deltBostedPerioder.single().tom)
    }

    @Test
    fun `Skal finne riktige delt bosted perioder for søker, og slå sammen de som overlapper`() {
        val behandling = lagBehandling()
        val barn = lagPerson(type = PersonType.BARN)
        val søker = lagPerson(type = PersonType.SØKER)
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val fomBarn1 = LocalDate.now().minusMonths(5)
        val tomBarn1 = LocalDate.now().plusMonths(7)
        val fomBarn2 = fomBarn1.minusMonths(5)
        val personResultatForPerson =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn.aktør,
            )

        val vilkårResultaterForPerson = mutableSetOf<VilkårResultat>()
        Vilkår
            .hentVilkårFor(
                personType = PersonType.BARN,
                fagsakType = FagsakType.NORMAL,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            ).forEach {
                if (it == Vilkår.BOR_MED_SØKER) {
                    vilkårResultaterForPerson.addAll(
                        listOf(
                            VilkårResultat(
                                personResultat = personResultatForPerson,
                                periodeFom = fomBarn1,
                                periodeTom = LocalDate.now().minusMonths(1).sisteDagIMåned(),
                                vilkårType = it,
                                resultat = Resultat.OPPFYLT,
                                begrunnelse = "",
                                sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                            ),
                            VilkårResultat(
                                personResultat = personResultatForPerson,
                                periodeFom = LocalDate.now().førsteDagIInneværendeMåned(),
                                periodeTom = tomBarn1,
                                vilkårType = it,
                                resultat = Resultat.OPPFYLT,
                                begrunnelse = "",
                                sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                            ),
                        ),
                    )
                } else {
                    VilkårResultat(
                        personResultat = personResultatForPerson,
                        periodeFom = fomBarn1,
                        periodeTom = tomBarn1,
                        vilkårType = it,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "",
                        sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                        utdypendeVilkårsvurderinger = emptyList(),
                    )
                }
            }
        personResultatForPerson.setSortedVilkårResultater(vilkårResultaterForPerson)

        vilkårsvurdering.personResultater =
            setOf(
                personResultatForPerson,
                lagPersonResultat(
                    vilkårsvurdering = vilkårsvurdering,
                    person = lagPerson(type = PersonType.BARN, fødselsdato = fomBarn2),
                    resultat = Resultat.OPPFYLT,
                    personType = PersonType.BARN,
                    periodeFom = fomBarn2,
                    periodeTom = tomBarn1,
                    erDeltBosted = true,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    lagFullstendigVilkårResultat = true,
                ),
            )

        val deltBostedPerioder = finnDeltBostedPerioder(person = søker, vilkårsvurdering = vilkårsvurdering)

        assertTrue(deltBostedPerioder.size == 1)
        assertEquals(fomBarn2.plusMonths(1).førsteDagIInneværendeMåned(), deltBostedPerioder.single().fom)
        assertEquals(tomBarn1.sisteDagIMåned(), deltBostedPerioder.single().tom)
    }

    @Test
    fun `Skal returnere tom liste hvis det ikke finnes noen delt bosted perioder på person`() {
        val behandling = lagBehandling()
        val barn1 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(5))
        val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(4))
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        vilkårsvurdering.personResultater =
            setOf(
                lagPersonResultat(
                    vilkårsvurdering = vilkårsvurdering,
                    person = barn1,
                    periodeFom = LocalDate.now().minusMonths(7),
                    periodeTom = LocalDate.now(),
                    resultat = Resultat.OPPFYLT,
                ),
                lagPersonResultat(
                    vilkårsvurdering = vilkårsvurdering,
                    person = barn2,
                    periodeFom = LocalDate.now().minusMonths(4),
                    periodeTom = LocalDate.now(),
                    resultat = Resultat.OPPFYLT,
                ),
            )

        val deltBostedPerioder = finnDeltBostedPerioder(person = barn1, vilkårsvurdering = vilkårsvurdering)

        assertTrue(deltBostedPerioder.isEmpty())
    }

    @Test
    fun `skal ikke feile dersom det er en utvidet endring og delt bosted endring med samme periode og prosent`() {
        validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
            listOf(endretUtbetalingAndelUtvidetNullutbetaling, endretUtbetalingAndelDeltBostedNullutbetaling),
        )
        Assertions.assertDoesNotThrow {
            validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
                listOf(endretUtbetalingAndelUtvidetNullutbetaling, endretUtbetalingAndelDeltBostedNullutbetaling),
            )
        }
    }

    @Test
    fun `skal ikke feile dersom det er en delt bosted endring som inneholder barn og søker`() {
        val andeler =
            lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                fom = inneværendeMåned().minusMonths(1),
                tom = inneværendeMåned().minusMonths(1),
                personer = setOf(barn, søker),
                årsak = Årsak.DELT_BOSTED,
                andelTilkjentYtelser =
                    mutableListOf(
                        lagAndelTilkjentYtelse(
                            fom = inneværendeMåned().minusMonths(1),
                            tom = inneværendeMåned().minusMonths(1),
                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = inneværendeMåned().minusMonths(1),
                            tom = inneværendeMåned().minusMonths(1),
                            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        ),
                    ),
            )

        assertDoesNotThrow {
            validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(listOf(andeler))
        }
    }

    @Test
    fun `skal kaste feil dersom det er en endring på utvidet ytelse uten en endring på delt bosted i samme periode`() {
        Assertions.assertThrows(FunksjonellFeil::class.java) {
            validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
                listOf(endretUtbetalingAndelUtvidetNullutbetaling),
            )
        }
    }

    private fun endretUtbetalingAndel(
        personer: Set<Person>,
        ytelsestype: YtelseType,
        prosent: BigDecimal,
        fomUtvidet: YearMonth = inneværendeMåned().minusMonths(1),
        tomUtvidet: YearMonth = inneværendeMåned().minusMonths(1),
    ): EndretUtbetalingAndelMedAndelerTilkjentYtelse =
        lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
            id = Random.nextLong(),
            fom = fomUtvidet,
            tom = tomUtvidet,
            personer = personer,
            årsak = Årsak.DELT_BOSTED,
            prosent = prosent,
            andelTilkjentYtelser =
                mutableListOf(
                    lagAndelTilkjentYtelse(
                        fom = fomUtvidet,
                        tom = tomUtvidet,
                        ytelseType = ytelsestype,
                    ),
                ),
        )

    @Test
    fun `Skal kaste feil hvis endringsårsak=allerede utbetalt og tom-dato er i fremtiden selv om tom er samme som gyldigTomIFremtiden`() {
        assertThrows<FunksjonellFeil> {
            validerTomDato(
                tomDato = YearMonth.now().plusMonths(3),
                årsak = Årsak.ALLEREDE_UTBETALT,
                gyldigTomEtterDagensDato = YearMonth.now().plusMonths(3),
            )
        }
    }

    @ParameterizedTest
    @EnumSource(value = Årsak::class, names = ["ENDRE_MOTTAKER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Skal kaste feil hvis årsak ikke er ENDRE_MOTTAKER og tom-dato er null`(
        årsak: Årsak,
    ) {
        assertThrows<FunksjonellFeil> {
            validerTomDato(
                tomDato = null,
                årsak = årsak,
                gyldigTomEtterDagensDato = YearMonth.now(),
            )
        }.also {
            assertThat(it.frontendFeilmelding).startsWith("Til og med-dato kan ikke være tom")
        }
    }

    @Test
    fun `Skal ikke kaste feil hvis tom-dato er i fremtiden, men lik gyldig dato i fremtiden`() {
        val tom = YearMonth.now().plusMonths(4)
        assertDoesNotThrow { validerTomDato(tomDato = tom, gyldigTomEtterDagensDato = tom, årsak = Årsak.DELT_BOSTED) }
    }

    @Test
    fun `Skal kaste feil hvis tom-dato er i fremtiden, men ikke lik gyldig dato i fremtiden`() {
        assertThrows<FunksjonellFeil> {
            validerTomDato(
                tomDato = YearMonth.now().plusMonths(6),
                gyldigTomEtterDagensDato = YearMonth.now().plusMonths(9),
                årsak = Årsak.ENDRE_MOTTAKER,
            )
        }
    }

    @Test
    fun `Skal kaste feil hvis perioden skal utbetales, men årsak er 'endre mottaker' eller 'allerede utbetalt'`() {
        assertThrows<FunksjonellFeil> {
            validerUtbetalingMotÅrsak(
                årsak = Årsak.ALLEREDE_UTBETALT,
                skalUtbetales = true,
            )
        }
        assertThrows<FunksjonellFeil> { validerUtbetalingMotÅrsak(årsak = Årsak.ENDRE_MOTTAKER, skalUtbetales = true) }
    }

    @Test
    fun `Skal ikke kaste feil hvis perioden skal utbetales, men årsak er 'delt bosted'`() {
        assertDoesNotThrow { validerUtbetalingMotÅrsak(årsak = Årsak.DELT_BOSTED, skalUtbetales = true) }
    }

    @Test
    fun `Skal kaste feil dersom endringsårsak er 'Allerede utbetalt' men tom dato er satt til etter inneværende måned`() {
        val endretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = 1,
                personer = mutableSetOf(tilfeldigPerson()),
                fom = YearMonth.now().minusMonths(3),
                tom = YearMonth.now().plusMonths(1),
                årsak = Årsak.ALLEREDE_UTBETALT,
                begrunnelse = "begrunnelse",
                prosent = BigDecimal(100),
                søknadstidspunkt = LocalDate.now(),
                avtaletidspunktDeltBosted = LocalDate.now(),
            )

        val feilmelding =
            assertThrows<FunksjonellFeil> {
                validerÅrsak(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    vilkårsvurdering = mockk(),
                )
            }.frontendFeilmelding

        assertEquals(
            "Du har valgt årsaken allerede utbetalt. Du kan ikke velge denne årsaken og en til og med dato frem i tid. Ta kontakt med superbruker om du er usikker på hva du skal gjøre.",
            feilmelding,
        )
    }

    @Test
    fun `Skal kaste ikke feil dersom endringsårsak er 'Allerede utbetalt' og tom dato er satt til å være lik eller før inneværende måned`() {
        val endretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = 1,
                personer = mutableSetOf(tilfeldigPerson()),
                fom = YearMonth.now().minusMonths(4),
                tom = YearMonth.now(),
                årsak = Årsak.ALLEREDE_UTBETALT,
                begrunnelse = "begrunnelse",
                prosent = BigDecimal(100),
                søknadstidspunkt = LocalDate.now(),
                avtaletidspunktDeltBosted = LocalDate.now(),
            )

        assertDoesNotThrow {
            validerÅrsak(
                endretUtbetalingAndel = endretUtbetalingAndel,
                vilkårsvurdering = mockk(),
            )
        }
    }

    @Test
    fun `Skal kaste feil dersom endringsårsak er 'Etterbetaling 3 år' og øker til hundre prosent utbetaling`() {
        val endretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = 1,
                fom = YearMonth.now().minusYears(4),
                tom = YearMonth.now().minusYears(3),
                årsak = Årsak.ETTERBETALING_3ÅR,
                prosent = BigDecimal(100),
                søknadstidspunkt = LocalDate.now(),
            )

        val feil =
            assertThrows<FunksjonellFeil> {
                validerÅrsak(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    vilkårsvurdering = null,
                )
            }

        assertThat(feil.frontendFeilmelding).isEqualTo("Du kan ikke endre til full utbetaling når det er mer enn tre år siden søknadstidspunktet.")
    }

    @Test
    fun `Skal kaste feil dersom endringsårsak er 'Etterbetaling 3 år' og perioden er mindre er 3 år siden`() {
        val endretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = 1,
                fom = YearMonth.now().minusYears(5),
                tom = YearMonth.now().minusYears(3),
                årsak = Årsak.ETTERBETALING_3ÅR,
                prosent = BigDecimal(0),
                søknadstidspunkt = LocalDate.now(),
            )

        val feil =
            assertThrows<FunksjonellFeil> {
                validerÅrsak(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    vilkårsvurdering = null,
                )
            }

        assertThat(feil.frontendFeilmelding).isEqualTo("Du kan kun stoppe etterbetaling for en periode som strekker seg mer enn tre år tilbake i tid.")
    }

    @Test
    fun `Skal ikke kaste feil dersom endringsårsak er 'Etterbetaling 3 år' og perioden er mer enn 3 år siden`() {
        val endretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = 1,
                fom = YearMonth.now().minusYears(5),
                tom = YearMonth.now().minusYears(3).minusMonths(1),
                årsak = Årsak.ETTERBETALING_3ÅR,
                prosent = BigDecimal(0),
                søknadstidspunkt = LocalDate.now(),
            )

        assertDoesNotThrow {
            validerÅrsak(
                endretUtbetalingAndel = endretUtbetalingAndel,
                vilkårsvurdering = null,
            )
        }
    }

    @Test
    fun `Skal kaste feil dersom endringsårsak er 'Etterbetaling 3 måneder' og perioden er mindre er 3 måneder siden`() {
        val endretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = 1,
                fom = YearMonth.now().minusMonths(5),
                tom = YearMonth.now().minusMonths(3),
                årsak = Årsak.ETTERBETALING_3MND,
                prosent = BigDecimal(0),
                søknadstidspunkt = LocalDate.now(),
            )

        val feil =
            assertThrows<FunksjonellFeil> {
                validerÅrsak(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    vilkårsvurdering = null,
                )
            }

        assertThat(feil.frontendFeilmelding).isEqualTo("Du kan kun stoppe etterbetaling for en periode som strekker seg mer enn tre måneder tilbake i tid.")
    }

    @Test
    fun `Skal ikke kaste feil dersom endringsårsak er 'Etterbetaling 3 måneder' og perioden er mer enn 3 måneder`() {
        val endretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = 1,
                fom = YearMonth.now().minusMonths(5),
                tom = YearMonth.now().minusMonths(4),
                årsak = Årsak.ETTERBETALING_3MND,
                prosent = BigDecimal(0),
                søknadstidspunkt = LocalDate.now(),
            )

        assertDoesNotThrow {
            validerÅrsak(
                endretUtbetalingAndel = endretUtbetalingAndel,
                vilkårsvurdering = null,
            )
        }
    }
}
