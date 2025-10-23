package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad.OvergangsstønadService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.tidslinje.mapVerdi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class UtvidetBarnetrygdTest {
    private val fødselsdatoOver6År = LocalDate.of(2014, 1, 1)
    private val fødselsdatoUnder6År = LocalDate.of(2021, 1, 15)

    private val overgangsstønadServiceMock: OvergangsstønadService = mockk()
    private val vilkårsvurderingServiceMock: VilkårsvurderingService = mockk()
    private val featureToggleServiceMock: FeatureToggleService = mockk()
    private val tilkjentYtelseGenerator = TilkjentYtelseGenerator(overgangsstønadServiceMock, vilkårsvurderingServiceMock, featureToggleServiceMock)

    @BeforeEach
    fun setup() {
        every { overgangsstønadServiceMock.hentOgLagrePerioderMedOvergangsstønadForBehandling(any(), any()) } returns mockkObject()
        every { overgangsstønadServiceMock.hentPerioderMedFullOvergangsstønad(any<Behandling>()) } answers { emptyList() }
        every { featureToggleServiceMock.isEnabled(FeatureToggle.SKAL_INKLUDERE_ÅRSAK_ENDRE_MOTTAKER_I_INITIELL_GENERERING_AV_ANDELER) } returns true
    }

    @Test
    fun `Utvidet andeler får høyeste beløp når det utbetales til flere barn med ulike beløp`() {
        val søker =
            OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 6, 15))
        val barnA =
            OppfyltPeriode(
                fom = LocalDate.of(2019, 4, 1),
                tom = LocalDate.of(2020, 6, 15),
                rolle = PersonType.BARN,
                erDeltBosted = true,
            )
        val barnB =
            OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 2, 15), rolle = PersonType.BARN)

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val søkerResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
                .apply {
                    vilkårResultater.addAll(
                        oppfylteVilkårFor(
                            personResultat = this,
                            vilkårOppfyltFom = søker.fom,
                            vilkårOppfyltTom = søker.tom,
                            personType = PersonType.SØKER,
                        ),
                    )
                    vilkårResultater.addAll(
                        oppfylteVilkårFor(
                            personResultat = this,
                            vilkårOppfyltFom = søker.fom,
                            vilkårOppfyltTom = søker.tom,
                            personType = PersonType.SØKER,
                            erUtvidet = true,
                        ),
                    )
                }
        val barnResultater =
            listOf(barnA, barnB).map {
                PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = it.aktør)
                    .apply {
                        vilkårResultater.addAll(
                            oppfylteVilkårFor(
                                personResultat = this,
                                vilkårOppfyltFom = it.fom,
                                vilkårOppfyltTom = it.tom,
                                personType = PersonType.BARN,
                                erDeltBosted = it.erDeltBosted,
                            ),
                        )
                    }
            }
        vilkårsvurdering.apply { personResultater = (listOf(søkerResultat) + barnResultater).toSet() }

        val personopplysningGrunnlag =
            PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søker, barnA, barnB).lagGrunnlagPersoner(this))
                }

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = vilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                ).andelerTilkjentYtelse
                .toList()
                .sortedWith(compareBy({ it.stønadFom }, { it.type }, { it.kalkulertUtbetalingsbeløp }))

        assertEquals(4, andeler.size)

        val andelBarnA = andeler[0]
        val andelBarnB = andeler[1]
        val andelUtvidetA = andeler[2]
        val andelUtvidetB = andeler[3]

        assertEquals(barnA.ident, andelBarnA.aktør.aktivFødselsnummer())
        assertEquals(barnA.fom.nesteMåned(), andelBarnA.stønadFom)
        assertEquals(barnA.tom.toYearMonth(), andelBarnA.stønadTom)
        assertEquals(527, andelBarnA.kalkulertUtbetalingsbeløp)

        assertEquals(barnB.ident, andelBarnB.aktør.aktivFødselsnummer())
        assertEquals(barnB.fom.nesteMåned(), andelBarnB.stønadFom)
        assertEquals(barnB.tom.toYearMonth(), andelBarnB.stønadTom)
        assertEquals(1054, andelBarnB.kalkulertUtbetalingsbeløp)

        assertEquals(søker.ident, andelUtvidetA.aktør.aktivFødselsnummer())
        assertEquals(søker.fom.nesteMåned(), andelUtvidetA.stønadFom)
        assertEquals(barnB.tom.toYearMonth(), andelUtvidetA.stønadTom)
        assertEquals(andelBarnB.kalkulertUtbetalingsbeløp, andelUtvidetA.kalkulertUtbetalingsbeløp)

        assertEquals(søker.ident, andelUtvidetB.aktør.aktivFødselsnummer())
        assertEquals(barnB.tom.nesteMåned(), andelUtvidetB.stønadFom)
        assertEquals(søker.tom.toYearMonth(), andelUtvidetB.stønadTom)
        assertEquals(andelBarnA.kalkulertUtbetalingsbeløp, andelUtvidetB.kalkulertUtbetalingsbeløp)
    }

    @Test
    fun `Utvidet andeler får høyeste ordinærsats når søker har tillegg for barn under 6 år`() {
        val søker =
            OppfyltPeriode(fom = fødselsdatoUnder6År, tom = LocalDate.of(2021, 6, 15))
        val oppfyltBarn =
            OppfyltPeriode(fom = fødselsdatoUnder6År, tom = LocalDate.of(2021, 6, 15), rolle = PersonType.BARN)

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val søkerResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
                .apply {
                    vilkårResultater.addAll(
                        oppfylteVilkårFor(
                            personResultat = this,
                            vilkårOppfyltFom = søker.fom,
                            vilkårOppfyltTom = søker.tom,
                            personType = PersonType.SØKER,
                        ),
                    )
                    vilkårResultater.addAll(
                        oppfylteVilkårFor(
                            personResultat = this,
                            vilkårOppfyltFom = søker.fom,
                            vilkårOppfyltTom = søker.tom,
                            personType = PersonType.SØKER,
                            erUtvidet = true,
                        ),
                    )
                }
        val barnResultater =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = oppfyltBarn.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = oppfyltBarn.fom,
                        vilkårOppfyltTom = oppfyltBarn.tom,
                        personType = PersonType.BARN,
                        fødselsdato = fødselsdatoUnder6År,
                    ),
                )
            }

        vilkårsvurdering.apply { personResultater = (listOf(søkerResultat) + barnResultater).toSet() }

        val personopplysningGrunnlag =
            PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søker, oppfyltBarn).lagGrunnlagPersoner(this, fødselsdatoUnder6År))
                }

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = vilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                ).andelerTilkjentYtelse
                .toList()
                .sortedWith(compareBy({ it.stønadFom }, { it.type }, { it.kalkulertUtbetalingsbeløp }))

        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(oppfyltBarn.ident, andelBarn.aktør.aktivFødselsnummer())
        assertEquals(oppfyltBarn.fom.nesteMåned(), andelBarn.stønadFom)
        assertEquals(oppfyltBarn.tom.toYearMonth(), andelBarn.stønadTom)
        assertEquals(1354, andelBarn.kalkulertUtbetalingsbeløp)

        assertEquals(søker.ident, andelUtvidet.aktør.aktivFødselsnummer())
        assertEquals(søker.fom.nesteMåned(), andelUtvidet.stønadFom)
        assertEquals(søker.tom.toYearMonth(), andelUtvidet.stønadTom)
        assertEquals(1054, andelUtvidet.kalkulertUtbetalingsbeløp)
    }

    @Test
    fun `Utvidet andeler får største prosent funnet blant andelene til barna som bor med søker`() {
        val behandling = lagBehandling()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling)
        val søkerAktør = randomAktør()

        val utvidetVilkår =
            lagVilkårResultat(
                vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                periodeFom = LocalDate.of(2021, 10, 1),
                periodeTom = LocalDate.of(2022, 2, 28),
                personResultat =
                    PersonResultat(
                        aktør = søkerAktør,
                        vilkårsvurdering =
                            lagVilkårsvurdering(
                                søkerAktør = søkerAktør,
                                behandling = behandling,
                                resultat = Resultat.OPPFYLT,
                            ),
                    ),
            )

        val barnasAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2021, 10),
                    tom = YearMonth.of(2022, 2),
                    person = tilfeldigPerson(personType = PersonType.BARN),
                    prosent = BigDecimal(50),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    tilkjentYtelse = tilkjentYtelse,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2021, 10),
                    tom = YearMonth.of(2022, 2),
                    person = tilfeldigPerson(personType = PersonType.BARN),
                    prosent = BigDecimal(100),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    tilkjentYtelse = tilkjentYtelse,
                ),
            )

        val utvidetAndelerNårBarnMed100ProsentBorMedSøker =
            UtvidetBarnetrygdGenerator(
                behandlingId = behandling.id,
                tilkjentYtelse = tilkjentYtelse,
            ).lagUtvidetBarnetrygdAndeler(
                utvidetVilkår = listOf(utvidetVilkår),
                andelerBarna = barnasAndeler,
                perioderBarnaBorMedSøkerTidslinje =
                    barnasAndeler
                        .tilSeparateTidslinjerForBarna()
                        .mapValues { it.value.mapVerdi { true } },
            )

        val utvidetAndelerNårKunBarnMed50ProsentBorMedSøker =
            UtvidetBarnetrygdGenerator(
                behandlingId = behandling.id,
                tilkjentYtelse = tilkjentYtelse,
            ).lagUtvidetBarnetrygdAndeler(
                utvidetVilkår = listOf(utvidetVilkår),
                andelerBarna = barnasAndeler,
                perioderBarnaBorMedSøkerTidslinje =
                    barnasAndeler
                        .tilSeparateTidslinjerForBarna()
                        .mapValues { it.value.mapVerdi { andel -> andel?.prosent == BigDecimal(50) } },
            )

        assertEquals(BigDecimal(100), utvidetAndelerNårBarnMed100ProsentBorMedSøker.minOf { it.prosent })
        assertEquals(BigDecimal(50), utvidetAndelerNårKunBarnMed50ProsentBorMedSøker.maxOf { it.prosent })
    }

    @Test
    fun `Utvidet andeler lages kun når vilkåret er innfridd`() {
        val søkerOrdinær =
            OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 6, 15))
        val søkerUtvidet =
            søkerOrdinær.copy(fom = LocalDate.of(2019, 6, 15), erUtvidet = true)
        val barnOppfylt =
            OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 6, 15), rolle = PersonType.BARN)

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søkerOrdinær.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerOrdinær.fom,
                        vilkårOppfyltTom = søkerOrdinær.tom,
                        personType = PersonType.SØKER,
                    ),
                )
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerUtvidet.fom,
                        vilkårOppfyltTom = søkerUtvidet.tom,
                        personType = PersonType.SØKER,
                        erUtvidet = søkerUtvidet.erUtvidet,
                    ),
                )
            }

        val barnResultater =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barnOppfylt.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = barnOppfylt.fom,
                        vilkårOppfyltTom = barnOppfylt.tom,
                        personType = PersonType.BARN,
                        erDeltBosted = barnOppfylt.erDeltBosted,
                    ),
                )
            }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag =
            PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
                }

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = vilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                ).andelerTilkjentYtelse
                .toList()
                .sortedBy { it.type }

        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(barnOppfylt.ident, andelBarn.aktør.aktivFødselsnummer())
        assertEquals(barnOppfylt.fom.nesteMåned(), andelBarn.stønadFom)
        assertEquals(barnOppfylt.tom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerUtvidet.ident, andelUtvidet.aktør.aktivFødselsnummer())
        assertEquals(søkerUtvidet.fom.nesteMåned(), andelUtvidet.stønadFom)
        assertEquals(søkerUtvidet.tom.toYearMonth(), andelUtvidet.stønadTom)
    }

    @Test
    fun `Utvidet andeler lages kun når det finnes andel for barn`() {
        val søkerOrdinær =
            OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 6, 15))
        val søkerUtvidet =
            søkerOrdinær.copy(erUtvidet = true)
        val barnOppfylt =
            OppfyltPeriode(fom = LocalDate.of(2019, 6, 1), tom = LocalDate.of(2019, 8, 15), rolle = PersonType.BARN)

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søkerOrdinær.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerOrdinær.fom,
                        vilkårOppfyltTom = søkerOrdinær.tom,
                        personType = PersonType.SØKER,
                    ),
                )
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerUtvidet.fom,
                        vilkårOppfyltTom = søkerUtvidet.tom,
                        personType = PersonType.SØKER,
                        erUtvidet = søkerUtvidet.erUtvidet,
                    ),
                )
            }

        val barnResultater =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barnOppfylt.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = barnOppfylt.fom,
                        vilkårOppfyltTom = barnOppfylt.tom,
                        personType = PersonType.BARN,
                        erDeltBosted = barnOppfylt.erDeltBosted,
                    ),
                )
            }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag =
            PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
                }

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = vilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                ).andelerTilkjentYtelse
                .toList()
                .sortedBy { it.type }

        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(barnOppfylt.ident, andelBarn.aktør.aktivFødselsnummer())
        assertEquals(barnOppfylt.fom.nesteMåned(), andelBarn.stønadFom)
        assertEquals(barnOppfylt.tom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerUtvidet.ident, andelUtvidet.aktør.aktivFødselsnummer())
        assertEquals(barnOppfylt.fom.nesteMåned(), andelUtvidet.stønadFom)
        assertEquals(barnOppfylt.tom.toYearMonth(), andelUtvidet.stønadTom)
    }

    @Test
    fun `Utvidet andeler slutter siste dag i  måneden som vilkår ikke er innfridd lenger`() {
        val søkerOrdinær =
            OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 6, 15))
        val søkerUtvidet =
            søkerOrdinær.copy(tom = LocalDate.of(2020, 4, 15), erUtvidet = true)
        val barnOppfylt =
            OppfyltPeriode(fom = søkerOrdinær.fom, tom = søkerOrdinær.tom, rolle = PersonType.BARN)

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søkerOrdinær.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerOrdinær.fom,
                        vilkårOppfyltTom = søkerOrdinær.tom,
                        personType = PersonType.SØKER,
                    ),
                )
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerUtvidet.fom,
                        vilkårOppfyltTom = søkerUtvidet.tom,
                        personType = PersonType.SØKER,
                        erUtvidet = søkerUtvidet.erUtvidet,
                    ),
                )
            }

        val barnResultater =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barnOppfylt.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = barnOppfylt.fom,
                        vilkårOppfyltTom = barnOppfylt.tom,
                        personType = PersonType.BARN,
                        erDeltBosted = barnOppfylt.erDeltBosted,
                    ),
                )
            }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag =
            PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
                }

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = vilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                ).andelerTilkjentYtelse
                .toList()
                .sortedBy { it.type }

        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(barnOppfylt.ident, andelBarn.aktør.aktivFødselsnummer())
        assertEquals(barnOppfylt.tom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerUtvidet.ident, andelUtvidet.aktør.aktivFødselsnummer())
        assertEquals(søkerUtvidet.tom.toYearMonth(), andelUtvidet.stønadTom)
    }

    @Test
    fun `Utvidet andel blir IKKE splittet opp på endring i utvidet vilkåret ved back-to-back, men utbetalingsperiodene blir det`() {
        val søkerOrdinær =
            OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 10, 15))
        val barnOppfylt =
            OppfyltPeriode(fom = søkerOrdinær.fom, tom = søkerOrdinær.tom, rolle = PersonType.BARN)
        val b2bTom = LocalDate.of(2020, 2, 29)
        val b2bFom = LocalDate.of(2020, 3, 1)
        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søkerOrdinær.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerOrdinær.fom,
                        vilkårOppfyltTom = søkerOrdinær.tom,
                        personType = PersonType.SØKER,
                    ),
                )
                vilkårResultater.addAll(
                    setOf(
                        VilkårResultat(
                            personResultat = this,
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = søkerOrdinær.fom,
                            periodeTom = b2bTom,
                            begrunnelse = "",
                            sistEndretIBehandlingId = this.vilkårsvurdering.behandling.id,
                            utdypendeVilkårsvurderinger = emptyList(),
                        ),
                        VilkårResultat(
                            personResultat = this,
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = b2bFom,
                            periodeTom = null,
                            begrunnelse = "",
                            sistEndretIBehandlingId = this.vilkårsvurdering.behandling.id,
                            utdypendeVilkårsvurderinger = emptyList(),
                        ),
                    ),
                )
            }
        val barnResultater =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barnOppfylt.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = barnOppfylt.fom,
                        vilkårOppfyltTom = barnOppfylt.tom,
                        personType = PersonType.BARN,
                        erDeltBosted = barnOppfylt.erDeltBosted,
                    ),
                )
            }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }
        val personopplysningGrunnlag =
            PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
                }

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = vilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                ).andelerTilkjentYtelse
                .toList()
                .sortedBy { it.type }

        // Én  andel for barnet og én andel for utvidet barnetrygd. Utvidet-andelen splittes IKKE
        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(barnOppfylt.ident, andelBarn.aktør.aktivFødselsnummer())
        assertEquals(YearMonth.of(2019, 5), andelBarn.stønadFom)
        assertEquals(YearMonth.of(2020, 10), andelBarn.stønadTom)

        assertEquals(søkerOrdinær.ident, andelUtvidet.aktør.aktivFødselsnummer())
        assertEquals(YearMonth.of(2019, 5), andelUtvidet.stønadFom)
        assertEquals(YearMonth.of(2020, 10), andelUtvidet.stønadTom)
    }

    @Test
    fun `Utvidet andel blir ikke splittet opp på endring i barnas vilkår som ikke er delt bosted`() {
        val søkerOrdinær =
            OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 10, 15))
        val barnOppfylt =
            OppfyltPeriode(fom = søkerOrdinær.fom, tom = søkerOrdinær.tom, rolle = PersonType.BARN)

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søkerOrdinær.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerOrdinær.fom,
                        vilkårOppfyltTom = søkerOrdinær.tom,
                        personType = PersonType.SØKER,
                    ),
                )
                vilkårResultater.addAll(
                    setOf(
                        VilkårResultat(
                            personResultat = this,
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = søkerOrdinær.fom,
                            periodeTom = søkerOrdinær.tom,
                            begrunnelse = "",
                            sistEndretIBehandlingId = this.vilkårsvurdering.behandling.id,
                            utdypendeVilkårsvurderinger = emptyList(),
                        ),
                    ),
                )
            }

        val b2bTom = LocalDate.of(2020, 2, 29)
        val b2bFom = LocalDate.of(2020, 3, 1)

        val barnResultater =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barnOppfylt.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = barnOppfylt.fom,
                        vilkårOppfyltTom = barnOppfylt.tom,
                        personType = PersonType.BARN,
                        erDeltBosted = barnOppfylt.erDeltBosted,
                    ),
                )

                vilkårResultater.removeIf { it.vilkårType == Vilkår.BOR_MED_SØKER }
                vilkårResultater.addAll(
                    setOf(
                        VilkårResultat(
                            personResultat = this,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = søkerOrdinær.fom,
                            periodeTom = b2bTom,
                            begrunnelse = "",
                            sistEndretIBehandlingId = this.vilkårsvurdering.behandling.id,
                            utdypendeVilkårsvurderinger = emptyList(),
                        ),
                        VilkårResultat(
                            personResultat = this,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = b2bFom,
                            periodeTom = søkerOrdinær.tom,
                            begrunnelse = "",
                            sistEndretIBehandlingId = this.vilkårsvurdering.behandling.id,
                            utdypendeVilkårsvurderinger = emptyList(),
                        ),
                    ),
                )
            }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag =
            PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
                }

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = vilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                ).andelerTilkjentYtelse
                .toList()
                .sortedBy { it.type }

        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(barnOppfylt.ident, andelBarn.aktør.aktivFødselsnummer())
        assertEquals(barnOppfylt.fom.plusMonths(1).toYearMonth(), andelBarn.stønadFom)
        assertEquals(barnOppfylt.tom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerOrdinær.ident, andelUtvidet.aktør.aktivFødselsnummer())
        assertEquals(søkerOrdinær.fom.plusMonths(1).toYearMonth(), andelUtvidet.stønadFom)
        assertEquals(søkerOrdinær.tom.toYearMonth(), andelUtvidet.stønadTom)
    }

    @Test
    fun `Utvidet andel blir splittet opp på endring i barnas vilkår som er delt bosted`() {
        val søkerOrdinær =
            OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 10, 15))
        val barnOppfylt =
            OppfyltPeriode(fom = søkerOrdinær.fom, tom = søkerOrdinær.tom, rolle = PersonType.BARN)

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søkerOrdinær.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerOrdinær.fom,
                        vilkårOppfyltTom = søkerOrdinær.tom,
                        personType = PersonType.SØKER,
                    ),
                )
                vilkårResultater.addAll(
                    setOf(
                        VilkårResultat(
                            personResultat = this,
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = søkerOrdinær.fom,
                            periodeTom = søkerOrdinær.tom,
                            begrunnelse = "",
                            sistEndretIBehandlingId = this.vilkårsvurdering.behandling.id,
                            utdypendeVilkårsvurderinger = emptyList(),
                        ),
                    ),
                )
            }

        val b2bTom = LocalDate.of(2020, 2, 29)
        val b2bFom = LocalDate.of(2020, 3, 1)

        val barnResultater =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barnOppfylt.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = barnOppfylt.fom,
                        vilkårOppfyltTom = barnOppfylt.tom,
                        personType = PersonType.BARN,
                        erDeltBosted = barnOppfylt.erDeltBosted,
                    ),
                )
                vilkårResultater.removeIf { it.vilkårType == Vilkår.BOR_MED_SØKER }
                vilkårResultater.addAll(
                    setOf(
                        VilkårResultat(
                            personResultat = this,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = søkerOrdinær.fom,
                            periodeTom = b2bTom,
                            begrunnelse = "",
                            sistEndretIBehandlingId = this.vilkårsvurdering.behandling.id,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                        ),
                        VilkårResultat(
                            personResultat = this,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = b2bFom,
                            periodeTom = søkerOrdinær.tom,
                            begrunnelse = "",
                            sistEndretIBehandlingId = this.vilkårsvurdering.behandling.id,
                            utdypendeVilkårsvurderinger = emptyList(),
                        ),
                    ),
                )
            }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag =
            PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
                }

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = vilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                ).andelerTilkjentYtelse
                .toList()
                .sortedBy { it.type }

        assertEquals(4, andeler.size)

        val andelBarn1 = andeler[0]
        val andelBarn2 = andeler[1]
        val andelUtvidet1 = andeler[2]
        val andelUtvidet2 = andeler[3]

        assertEquals(barnOppfylt.ident, andelBarn1.aktør.aktivFødselsnummer())
        assertEquals(barnOppfylt.fom.plusMonths(1).toYearMonth(), andelBarn1.stønadFom)
        assertEquals(b2bTom.toYearMonth(), andelBarn1.stønadTom)
        assertEquals(BigDecimal(50), andelBarn1.prosent)

        assertEquals(barnOppfylt.ident, andelBarn2.aktør.aktivFødselsnummer())
        assertEquals(b2bFom.toYearMonth(), andelBarn2.stønadFom)
        assertEquals(barnOppfylt.tom.toYearMonth(), andelBarn2.stønadTom)
        assertEquals(BigDecimal(100), andelBarn2.prosent)

        assertEquals(søkerOrdinær.ident, andelUtvidet1.aktør.aktivFødselsnummer())
        assertEquals(søkerOrdinær.fom.plusMonths(1).toYearMonth(), andelUtvidet1.stønadFom)
        assertEquals(b2bTom.toYearMonth(), andelUtvidet1.stønadTom)
        assertEquals(BigDecimal(50), andelUtvidet1.prosent)

        assertEquals(søkerOrdinær.ident, andelUtvidet2.aktør.aktivFødselsnummer())
        assertEquals(b2bFom.toYearMonth(), andelUtvidet2.stønadFom)
        assertEquals(søkerOrdinær.tom.toYearMonth(), andelUtvidet2.stønadTom)
        assertEquals(BigDecimal(100), andelUtvidet2.prosent)
    }

    @Test
    fun `Utvidet andel starter og opphører riktig når det er to perioder som ikke er back2back`() {
        val søkerOrdinær =
            OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 10, 15))
        val barnOppfylt =
            OppfyltPeriode(fom = søkerOrdinær.fom, tom = søkerOrdinær.tom, rolle = PersonType.BARN)

        val utvidetFørstePeriodeTom = LocalDate.of(2020, 2, 20)
        val utvidetAndrePeriodeFom = LocalDate.of(2020, 3, 15)

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søkerOrdinær.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerOrdinær.fom,
                        vilkårOppfyltTom = søkerOrdinær.tom,
                        personType = PersonType.SØKER,
                    ),
                )
                vilkårResultater.addAll(
                    setOf(
                        VilkårResultat(
                            personResultat = this,
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = søkerOrdinær.fom,
                            periodeTom = utvidetFørstePeriodeTom,
                            begrunnelse = "",
                            sistEndretIBehandlingId = this.vilkårsvurdering.behandling.id,
                            utdypendeVilkårsvurderinger = emptyList(),
                        ),
                        VilkårResultat(
                            personResultat = this,
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = utvidetAndrePeriodeFom,
                            periodeTom = null,
                            begrunnelse = "",
                            sistEndretIBehandlingId = this.vilkårsvurdering.behandling.id,
                            utdypendeVilkårsvurderinger = emptyList(),
                        ),
                    ),
                )
            }

        val barnResultater =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barnOppfylt.aktør,
            ).apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = barnOppfylt.fom,
                        vilkårOppfyltTom = barnOppfylt.tom,
                        personType = PersonType.BARN,
                        erDeltBosted = barnOppfylt.erDeltBosted,
                    ),
                )
            }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag =
            PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
                }

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = vilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                ).andelerTilkjentYtelse
                .toList()
                .sortedBy { it.type }

        assertEquals(3, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet1 = andeler[1]
        val andelUtvidet2 = andeler[2]

        assertEquals(barnOppfylt.ident, andelBarn.aktør.aktivFødselsnummer())
        assertEquals(barnOppfylt.tom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerOrdinær.ident, andelUtvidet1.aktør.aktivFødselsnummer())
        assertEquals(utvidetFørstePeriodeTom.toYearMonth(), andelUtvidet1.stønadTom)

        assertEquals(søkerOrdinær.ident, andelUtvidet2.aktør.aktivFødselsnummer())
        assertEquals(utvidetAndrePeriodeFom.plusMonths(1).toYearMonth(), andelUtvidet2.stønadFom)
    }

    @Test
    fun `Skal kaste feil hvis utvidet-andeler ikke overlapper med noen av barnas andeler`() {
        val behandling = lagBehandling()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling)
        val søkerAktør = randomAktør()

        val utvidetVilkår =
            lagVilkårResultat(
                vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                periodeFom = LocalDate.of(2018, 2, 1),
                periodeTom = LocalDate.of(2019, 2, 28),
                personResultat =
                    PersonResultat(
                        aktør = søkerAktør,
                        vilkårsvurdering =
                            lagVilkårsvurdering(
                                søkerAktør = søkerAktør,
                                behandling = behandling,
                                resultat = Resultat.OPPFYLT,
                            ),
                    ),
            )

        val barnasAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2021, 10),
                    tom = YearMonth.of(2022, 2),
                    person = tilfeldigPerson(personType = PersonType.BARN),
                    prosent = BigDecimal(100),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    tilkjentYtelse = tilkjentYtelse,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2021, 10),
                    tom = YearMonth.of(2022, 1),
                    person = tilfeldigPerson(personType = PersonType.BARN),
                    prosent = BigDecimal(100),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    tilkjentYtelse = tilkjentYtelse,
                ),
            )

        assertThrows<FunksjonellFeil> {
            UtvidetBarnetrygdGenerator(
                behandlingId = behandling.id,
                tilkjentYtelse = tilkjentYtelse,
            ).lagUtvidetBarnetrygdAndeler(
                utvidetVilkår = listOf(utvidetVilkår),
                andelerBarna = barnasAndeler,
                perioderBarnaBorMedSøkerTidslinje =
                    barnasAndeler.tilSeparateTidslinjerForBarna().mapValues { it.value.mapVerdi { true } },
            )
        }
    }

    @Test
    fun `Skal dele opp utvidet-segment ved endring i sats`() {
        val behandling = lagBehandling()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling)
        val søkerAktør = randomAktør()

        val utvidetVilkår =
            lagVilkårResultat(
                vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                periodeFom = LocalDate.of(2016, 2, 1),
                periodeTom = LocalDate.of(2022, 2, 28),
                personResultat =
                    PersonResultat(
                        aktør = søkerAktør,
                        vilkårsvurdering =
                            lagVilkårsvurdering(
                                søkerAktør = søkerAktør,
                                behandling = behandling,
                                resultat = Resultat.OPPFYLT,
                            ),
                    ),
            )

        val barnasAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2015, 10),
                    tom = YearMonth.of(2022, 2),
                    person = tilfeldigPerson(personType = PersonType.BARN),
                    prosent = BigDecimal(100),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    tilkjentYtelse = tilkjentYtelse,
                ),
            )

        val utvidetAndeler =
            UtvidetBarnetrygdGenerator(
                behandlingId = behandling.id,
                tilkjentYtelse = tilkjentYtelse,
            ).lagUtvidetBarnetrygdAndeler(
                utvidetVilkår = listOf(utvidetVilkår),
                andelerBarna = barnasAndeler,
                perioderBarnaBorMedSøkerTidslinje =
                    barnasAndeler
                        .tilSeparateTidslinjerForBarna()
                        .mapValues { it.value.mapVerdi { true } },
            ).sortedBy { it.stønadFom }

        assertEquals(2, utvidetAndeler.size)

        val andelFørSatsendring = utvidetAndeler[0]
        val andelEtterSatsendring = utvidetAndeler[1]

        val datoForSatsendring =
            SatsService.hentDatoForSatsendring(
                satstype = SatsType.UTVIDET_BARNETRYGD,
                oppdatertBeløp = 1054,
            )

        assertEquals(970, andelFørSatsendring.sats)
        assertEquals(datoForSatsendring?.minusDays(1)?.toYearMonth(), andelFørSatsendring.stønadTom)

        assertEquals(1054, andelEtterSatsendring.sats)
        assertEquals(datoForSatsendring?.toYearMonth(), andelEtterSatsendring.stønadFom)
    }

    @Test
    fun `Skal endre utbetaling for søker og barn på endret utbetaling andel`() {
        val søker = OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 6, 15))
        val barn = OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 6, 15), rolle = PersonType.BARN)

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val søkerResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
                .apply {
                    vilkårResultater.addAll(
                        oppfylteVilkårFor(
                            personResultat = this,
                            vilkårOppfyltFom = søker.fom,
                            vilkårOppfyltTom = søker.tom,
                            personType = PersonType.SØKER,
                        ),
                    )
                    vilkårResultater.addAll(
                        oppfylteVilkårFor(
                            personResultat = this,
                            vilkårOppfyltFom = søker.fom,
                            vilkårOppfyltTom = søker.tom,
                            personType = PersonType.SØKER,
                            erUtvidet = true,
                        ),
                    )
                }
        val barnResultater =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
                .apply {
                    vilkårResultater.addAll(
                        oppfylteVilkårFor(
                            personResultat = this,
                            vilkårOppfyltFom = barn.fom,
                            vilkårOppfyltTom = barn.tom,
                            personType = PersonType.BARN,
                        ),
                    )
                }

        vilkårsvurdering.apply { personResultater = setOf(søkerResultat, barnResultater) }

        val personopplysningGrunnlag =
            PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søker, barn).lagGrunnlagPersoner(this))
                }

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val endretUtbetalingAndeler =
            listOf(
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    behandlingId = behandling.id,
                    personer = personopplysningGrunnlag.personer,
                    fom = YearMonth.of(2019, 5),
                    tom = YearMonth.of(2019, 5),
                    prosent = BigDecimal.ZERO,
                ),
            )

        val andeler =
            tilkjentYtelseGenerator
                .genererTilkjentYtelse(
                    behandling = vilkårsvurdering.behandling,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    endretUtbetalingAndeler = endretUtbetalingAndeler,
                ).andelerTilkjentYtelse

        assertThat(andeler)
            .anySatisfy {
                assertThat(it.aktør).isEqualTo(søker.aktør)
                assertThat(it.stønadFom).isEqualTo(YearMonth.of(2019, 5))
                assertThat(it.stønadTom).isEqualTo(YearMonth.of(2019, 5))
                assertThat(it.prosent).isEqualTo(BigDecimal.ZERO)
            }.anySatisfy {
                assertThat(it.aktør).isEqualTo(søker.aktør)
                assertThat(it.stønadFom).isEqualTo(YearMonth.of(2019, 6))
                assertThat(it.stønadTom).isEqualTo(YearMonth.of(2020, 6))
                assertThat(it.prosent).isEqualTo(BigDecimal(100))
            }.anySatisfy {
                assertThat(it.aktør).isEqualTo(barn.aktør)
                assertThat(it.stønadFom).isEqualTo(YearMonth.of(2019, 5))
                assertThat(it.stønadTom).isEqualTo(YearMonth.of(2019, 5))
                assertThat(it.prosent).isEqualTo(BigDecimal.ZERO)
            }.anySatisfy {
                assertThat(it.aktør).isEqualTo(barn.aktør)
                assertThat(it.stønadFom).isEqualTo(YearMonth.of(2019, 6))
                assertThat(it.stønadTom).isEqualTo(YearMonth.of(2020, 6))
                assertThat(it.prosent).isEqualTo(BigDecimal(100))
            }
    }

    private data class OppfyltPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
        val ident: String = randomFnr(),
        val aktør: Aktør = lagAktør(ident),
        val rolle: PersonType = PersonType.SØKER,
        val erUtvidet: Boolean = false,
        val erDeltBosted: Boolean = false,
    )

    private fun oppfylteVilkårFor(
        personResultat: PersonResultat,
        vilkårOppfyltFom: LocalDate?,
        vilkårOppfyltTom: LocalDate?,
        personType: PersonType,
        erUtvidet: Boolean = false,
        erDeltBosted: Boolean = false,
        fødselsdato: LocalDate = fødselsdatoOver6År,
    ): Set<VilkårResultat> {
        val vilkårSomSkalVurderes =
            if (erUtvidet) {
                listOf(Vilkår.UTVIDET_BARNETRYGD)
            } else {
                Vilkår.hentVilkårFor(
                    personType = personType,
                    fagsakType = FagsakType.NORMAL,
                    behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
                )
            }

        return vilkårSomSkalVurderes
            .map {
                VilkårResultat(
                    personResultat = personResultat,
                    vilkårType = it,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = if (it == Vilkår.UNDER_18_ÅR) fødselsdato else vilkårOppfyltFom,
                    periodeTom = if (it == Vilkår.UNDER_18_ÅR) fødselsdato.plusYears(18) else vilkårOppfyltTom,
                    begrunnelse = "",
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                    utdypendeVilkårsvurderinger =
                        listOfNotNull(
                            if (erDeltBosted) UtdypendeVilkårsvurdering.DELT_BOSTED else null,
                        ),
                )
            }.toSet()
    }

    private fun List<OppfyltPeriode>.lagGrunnlagPersoner(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        fødselsdato: LocalDate = fødselsdatoOver6År,
    ): List<Person> =
        this.map {
            Person(
                aktør = lagAktør(it.ident),
                type = it.rolle,
                personopplysningGrunnlag = personopplysningGrunnlag,
                fødselsdato = fødselsdato,
                navn = "Test Testesen",
                kjønn = Kjønn.KVINNE,
            ).apply {
                sivilstander = mutableListOf(GrSivilstand(type = SIVILSTANDTYPE.UGIFT, person = this))
            }
        }
}
