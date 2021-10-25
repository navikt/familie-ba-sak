package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtvidetBarnetrygdTest {

    private val fødselsdatoOver6År = LocalDate.of(2014, 1, 1)
    private val fødselsdatoUnder6År = LocalDate.of(2021, 1, 15)

    @Test
    fun `Utvidet andeler får høyeste beløp når det utbetales til flere barn med ulike beløp`() {

        val søker =
            OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 6, 15))
        val barnA =
            OppfyltPeriode(
                fom = LocalDate.of(2019, 4, 1),
                tom = LocalDate.of(2020, 6, 15),
                rolle = PersonType.BARN,
                erDeltBosted = true
            )
        val barnB =
            OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 2, 15), rolle = PersonType.BARN)

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val søkerResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søker.ident)
            .apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søker.fom,
                        vilkårOppfyltTom = søker.tom,
                        personType = PersonType.SØKER
                    )
                )
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søker.fom,
                        vilkårOppfyltTom = søker.tom,
                        personType = PersonType.SØKER,
                        erUtvidet = true
                    )
                )
            }
        val barnResultater = listOf(barnA, barnB).map {
            PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = it.ident)
                .apply {
                    vilkårResultater.addAll(
                        oppfylteVilkårFor(
                            personResultat = this,
                            vilkårOppfyltFom = it.fom,
                            vilkårOppfyltTom = it.tom,
                            personType = PersonType.BARN,
                            erDeltBosted = it.erDeltBosted
                        )
                    )
                }
        }
        vilkårsvurdering.apply { personResultater = (listOf(søkerResultat) + barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
            .apply {
                personer.addAll(listOf(søker, barnA, barnB).lagGrunnlagPersoner(this))
            }

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = behandling
        )
            .andelerTilkjentYtelse.toList()
            .sortedWith(compareBy({ it.stønadFom }, { it.type }, { it.kalkulertUtbetalingsbeløp }))

        assertEquals(4, andeler.size)

        val andelBarnA = andeler[0]
        val andelBarnB = andeler[1]
        val andelUtvidetA = andeler[2]
        val andelUtvidetB = andeler[3]

        assertEquals(barnA.ident, andelBarnA.personIdent)
        assertEquals(barnA.fom.nesteMåned(), andelBarnA.stønadFom)
        assertEquals(barnA.tom.toYearMonth(), andelBarnA.stønadTom)
        assertEquals(527, andelBarnA.kalkulertUtbetalingsbeløp)

        assertEquals(barnB.ident, andelBarnB.personIdent)
        assertEquals(barnB.fom.nesteMåned(), andelBarnB.stønadFom)
        assertEquals(barnB.tom.toYearMonth(), andelBarnB.stønadTom)
        assertEquals(1054, andelBarnB.kalkulertUtbetalingsbeløp)

        assertEquals(søker.ident, andelUtvidetA.personIdent)
        assertEquals(søker.fom.nesteMåned(), andelUtvidetA.stønadFom)
        assertEquals(barnB.tom.toYearMonth(), andelUtvidetA.stønadTom)
        assertEquals(andelBarnB.kalkulertUtbetalingsbeløp, andelUtvidetA.kalkulertUtbetalingsbeløp)

        assertEquals(søker.ident, andelUtvidetB.personIdent)
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

        val søkerResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søker.ident)
            .apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søker.fom,
                        vilkårOppfyltTom = søker.tom,
                        personType = PersonType.SØKER
                    )
                )
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søker.fom,
                        vilkårOppfyltTom = søker.tom,
                        personType = PersonType.SØKER,
                        erUtvidet = true
                    )
                )
            }
        val barnResultater = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = oppfyltBarn.ident)
            .apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = oppfyltBarn.fom,
                        vilkårOppfyltTom = oppfyltBarn.tom,
                        personType = PersonType.BARN,
                        fødselsdato = fødselsdatoUnder6År
                    )
                )
            }

        vilkårsvurdering.apply { personResultater = (listOf(søkerResultat) + barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
            .apply {
                personer.addAll(listOf(søker, oppfyltBarn).lagGrunnlagPersoner(this, fødselsdatoUnder6År))
            }

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = behandling
        )
            .andelerTilkjentYtelse.toList()
            .sortedWith(compareBy({ it.stønadFom }, { it.type }, { it.kalkulertUtbetalingsbeløp }))

        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(oppfyltBarn.ident, andelBarn.personIdent)
        assertEquals(oppfyltBarn.fom.nesteMåned(), andelBarn.stønadFom)
        assertEquals(oppfyltBarn.tom.toYearMonth(), andelBarn.stønadTom)
        assertEquals(1354, andelBarn.kalkulertUtbetalingsbeløp)

        assertEquals(søker.ident, andelUtvidet.personIdent)
        assertEquals(søker.fom.nesteMåned(), andelUtvidet.stønadFom)
        assertEquals(søker.tom.toYearMonth(), andelUtvidet.stønadTom)
        assertEquals(1054, andelUtvidet.kalkulertUtbetalingsbeløp)
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
        val søkerResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søkerOrdinær.ident)
            .apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerOrdinær.fom,
                        vilkårOppfyltTom = søkerOrdinær.tom,
                        personType = PersonType.SØKER
                    )
                )
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerUtvidet.fom,
                        vilkårOppfyltTom = søkerUtvidet.tom,
                        personType = PersonType.SØKER,
                        erUtvidet = søkerUtvidet.erUtvidet
                    )
                )
            }

        val barnResultater =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnOppfylt.ident)
                .apply {
                    vilkårResultater.addAll(
                        oppfylteVilkårFor(
                            personResultat = this,
                            vilkårOppfyltFom = barnOppfylt.fom,
                            vilkårOppfyltTom = barnOppfylt.tom,
                            personType = PersonType.BARN,
                            erDeltBosted = barnOppfylt.erDeltBosted
                        )
                    )
                }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
            .apply {
                personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
            }

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = behandling
        )
            .andelerTilkjentYtelse.toList().sortedBy { it.type }

        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(barnOppfylt.ident, andelBarn.personIdent)
        assertEquals(barnOppfylt.fom.nesteMåned(), andelBarn.stønadFom)
        assertEquals(barnOppfylt.tom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerUtvidet.ident, andelUtvidet.personIdent)
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
        val søkerResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søkerOrdinær.ident)
            .apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerOrdinær.fom,
                        vilkårOppfyltTom = søkerOrdinær.tom,
                        personType = PersonType.SØKER
                    )
                )
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerUtvidet.fom,
                        vilkårOppfyltTom = søkerUtvidet.tom,
                        personType = PersonType.SØKER,
                        erUtvidet = søkerUtvidet.erUtvidet
                    )
                )
            }

        val barnResultater =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnOppfylt.ident)
                .apply {
                    vilkårResultater.addAll(
                        oppfylteVilkårFor(
                            personResultat = this,
                            vilkårOppfyltFom = barnOppfylt.fom,
                            vilkårOppfyltTom = barnOppfylt.tom,
                            personType = PersonType.BARN,
                            erDeltBosted = barnOppfylt.erDeltBosted
                        )
                    )
                }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
            .apply {
                personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
            }

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = behandling
        )
            .andelerTilkjentYtelse.toList().sortedBy { it.type }

        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(barnOppfylt.ident, andelBarn.personIdent)
        assertEquals(barnOppfylt.fom.nesteMåned(), andelBarn.stønadFom)
        assertEquals(barnOppfylt.tom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerUtvidet.ident, andelUtvidet.personIdent)
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
        val søkerResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søkerOrdinær.ident)
            .apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerOrdinær.fom,
                        vilkårOppfyltTom = søkerOrdinær.tom,
                        personType = PersonType.SØKER
                    )
                )
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerUtvidet.fom,
                        vilkårOppfyltTom = søkerUtvidet.tom,
                        personType = PersonType.SØKER,
                        erUtvidet = søkerUtvidet.erUtvidet
                    )
                )
            }

        val barnResultater =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnOppfylt.ident)
                .apply {
                    vilkårResultater.addAll(
                        oppfylteVilkårFor(
                            personResultat = this,
                            vilkårOppfyltFom = barnOppfylt.fom,
                            vilkårOppfyltTom = barnOppfylt.tom,
                            personType = PersonType.BARN,
                            erDeltBosted = barnOppfylt.erDeltBosted
                        )
                    )
                }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
            .apply {
                personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
            }

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = behandling
        )
            .andelerTilkjentYtelse.toList().sortedBy { it.type }

        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(barnOppfylt.ident, andelBarn.personIdent)
        assertEquals(barnOppfylt.tom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerUtvidet.ident, andelUtvidet.personIdent)
        assertEquals(søkerUtvidet.tom.toYearMonth(), andelUtvidet.stønadTom)
    }

    @Test
    fun `Utvidet andel blir videreført en måned ekstra hvis det er back2back i månedskifte`() {
        val søkerOrdinær =
            OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 10, 15))
        val barnOppfylt =
            OppfyltPeriode(fom = søkerOrdinær.fom, tom = søkerOrdinær.tom, rolle = PersonType.BARN)

        val b2bTom = LocalDate.of(2020, 2, 29)
        val b2bFom = LocalDate.of(2020, 3, 1)

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søkerOrdinær.ident)
            .apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerOrdinær.fom,
                        vilkårOppfyltTom = søkerOrdinær.tom,
                        personType = PersonType.SØKER
                    )
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
                            behandlingId = this.vilkårsvurdering.behandling.id,
                            erDeltBosted = false
                        ),
                        VilkårResultat(
                            personResultat = this,
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = b2bFom,
                            periodeTom = null,
                            begrunnelse = "",
                            behandlingId = this.vilkårsvurdering.behandling.id,
                            erDeltBosted = false
                        )
                    )
                )
            }

        val barnResultater =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnOppfylt.ident)
                .apply {
                    vilkårResultater.addAll(
                        oppfylteVilkårFor(
                            personResultat = this,
                            vilkårOppfyltFom = barnOppfylt.fom,
                            vilkårOppfyltTom = barnOppfylt.tom,
                            personType = PersonType.BARN,
                            erDeltBosted = barnOppfylt.erDeltBosted
                        )
                    )
                }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
            .apply {
                personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
            }

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = behandling
        )
            .andelerTilkjentYtelse.toList().sortedBy { it.type }

        assertEquals(3, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet1 = andeler[1]
        val andelUtvidet2 = andeler[2]

        assertEquals(barnOppfylt.ident, andelBarn.personIdent)
        assertEquals(barnOppfylt.tom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerOrdinær.ident, andelUtvidet1.personIdent)
        assertEquals(b2bTom.plusMonths(1).toYearMonth(), andelUtvidet1.stønadTom)

        assertEquals(søkerOrdinær.ident, andelUtvidet2.personIdent)
        assertEquals(b2bFom.plusMonths(1).toYearMonth(), andelUtvidet2.stønadFom)
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
        val søkerResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søkerOrdinær.ident)
            .apply {
                vilkårResultater.addAll(
                    oppfylteVilkårFor(
                        personResultat = this,
                        vilkårOppfyltFom = søkerOrdinær.fom,
                        vilkårOppfyltTom = søkerOrdinær.tom,
                        personType = PersonType.SØKER
                    )
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
                            behandlingId = this.vilkårsvurdering.behandling.id,
                            erDeltBosted = false
                        ),
                        VilkårResultat(
                            personResultat = this,
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = utvidetAndrePeriodeFom,
                            periodeTom = null,
                            begrunnelse = "",
                            behandlingId = this.vilkårsvurdering.behandling.id,
                            erDeltBosted = false
                        )
                    )
                )
            }

        val barnResultater =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnOppfylt.ident)
                .apply {
                    vilkårResultater.addAll(
                        oppfylteVilkårFor(
                            personResultat = this,
                            vilkårOppfyltFom = barnOppfylt.fom,
                            vilkårOppfyltTom = barnOppfylt.tom,
                            personType = PersonType.BARN,
                            erDeltBosted = barnOppfylt.erDeltBosted
                        )
                    )
                }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
            .apply {
                personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
            }

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = behandling
        )
            .andelerTilkjentYtelse.toList().sortedBy { it.type }

        assertEquals(3, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet1 = andeler[1]
        val andelUtvidet2 = andeler[2]

        assertEquals(barnOppfylt.ident, andelBarn.personIdent)
        assertEquals(barnOppfylt.tom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerOrdinær.ident, andelUtvidet1.personIdent)
        assertEquals(utvidetFørstePeriodeTom.toYearMonth(), andelUtvidet1.stønadTom)

        assertEquals(søkerOrdinær.ident, andelUtvidet2.personIdent)
        assertEquals(utvidetAndrePeriodeFom.plusMonths(1).toYearMonth(), andelUtvidet2.stønadFom)
    }

    private data class OppfyltPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
        val ident: String = randomFnr(),
        val rolle: PersonType = PersonType.SØKER,
        val erUtvidet: Boolean = false,
        val erDeltBosted: Boolean = false
    )

    private fun oppfylteVilkårFor(
        personResultat: PersonResultat,
        vilkårOppfyltFom: LocalDate?,
        vilkårOppfyltTom: LocalDate?,
        personType: PersonType,
        erUtvidet: Boolean = false,
        erDeltBosted: Boolean = false,
        fødselsdato: LocalDate = fødselsdatoOver6År
    ): Set<VilkårResultat> {
        val vilkårSomSkalVurderes = if (erUtvidet)
            listOf(Vilkår.UTVIDET_BARNETRYGD)
        else
            Vilkår.hentVilkårFor(personType = personType)

        return vilkårSomSkalVurderes.map {
            VilkårResultat(
                personResultat = personResultat,
                vilkårType = it,
                resultat = Resultat.OPPFYLT,
                periodeFom = if (it == Vilkår.UNDER_18_ÅR) fødselsdato else vilkårOppfyltFom,
                periodeTom = if (it == Vilkår.UNDER_18_ÅR) fødselsdato.plusYears(18) else vilkårOppfyltTom,
                begrunnelse = "",
                behandlingId = personResultat.vilkårsvurdering.behandling.id,
                erDeltBosted = erDeltBosted
            )
        }.toSet()
    }

    private fun List<OppfyltPeriode>.lagGrunnlagPersoner(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        fødselsdato: LocalDate = fødselsdatoOver6År
    ): List<Person> = this.map {
        Person(
            aktørId = randomAktørId(),
            personIdent = PersonIdent(it.ident),
            type = it.rolle,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = fødselsdato,
            navn = "Test Testesen",
            kjønn = Kjønn.KVINNE
        )
            .apply {
                sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this))
            }
    }
}
