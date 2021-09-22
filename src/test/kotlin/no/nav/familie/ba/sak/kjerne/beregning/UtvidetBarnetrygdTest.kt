package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtvidetBarnetrygdTest {

    // TODO: Stedene i testen som ser på beløp, må starte å se på prosent

    private val featureToggleService = mockk<FeatureToggleService>()

    private val fødselsdato = LocalDate.of(2014, 1, 1)

    @BeforeEach
    fun setUp() {
        every { featureToggleService.isEnabled(any()) } answers { true }
    }

    @Test
    fun `Utvidet andeler får høyeste beløp når det utbetales til flere barn med ulike beløp`() {

        val søker =
                OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 6, 15))
        val barnA =
                OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 6, 15), rolle = PersonType.BARN, erDeltBosted = true)
        val barnB =
                OppfyltPeriode(fom = LocalDate.of(2019, 4, 1), tom = LocalDate.of(2020, 2, 15), rolle = PersonType.BARN)

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val søkerResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søker.ident)
                .apply {
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søker.fom,
                                                              vilkårOppfyltTom = søker.tom,
                                                              personType = PersonType.SØKER))
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søker.fom,
                                                              vilkårOppfyltTom = søker.tom,
                                                              personType = PersonType.SØKER,
                                                              erUtvidet = true))
                }
        val barnResultater = listOf(barnA, barnB).map {
            PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = it.ident)
                    .apply {
                        vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                                  vilkårOppfyltFom = it.fom,
                                                                  vilkårOppfyltTom = it.tom,
                                                                  personType = PersonType.BARN,
                                                                  erDeltBosted = it.erDeltBosted))
                    }
        }
        vilkårsvurdering.apply { personResultater = (listOf(søkerResultat) + barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søker, barnA, barnB).lagGrunnlagPersoner(this))
                }

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(vilkårsvurdering = vilkårsvurdering,
                                                               personopplysningGrunnlag = personopplysningGrunnlag,
                                                               behandling = behandling,
                                                               featureToggleService = featureToggleService)
                .andelerTilkjentYtelse.toList().sortedWith(compareBy({ it.stønadFom }, { it.type }, { it.kalkulertUtbetalingsbeløp }))

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
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerOrdinær.fom,
                                                              vilkårOppfyltTom = søkerOrdinær.tom,
                                                              personType = PersonType.SØKER))
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerUtvidet.fom,
                                                              vilkårOppfyltTom = søkerUtvidet.tom,
                                                              personType = PersonType.SØKER,
                                                              erUtvidet = søkerUtvidet.erUtvidet))
                }

        val barnResultater =
                PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnOppfylt.ident)
                        .apply {
                            vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                                      vilkårOppfyltFom = barnOppfylt.fom,
                                                                      vilkårOppfyltTom = barnOppfylt.tom,
                                                                      personType = PersonType.BARN,
                                                                      erDeltBosted = barnOppfylt.erDeltBosted))
                        }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
                }

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(vilkårsvurdering = vilkårsvurdering,
                                                               personopplysningGrunnlag = personopplysningGrunnlag,
                                                               behandling = behandling,
                                                               featureToggleService = featureToggleService)
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
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerOrdinær.fom,
                                                              vilkårOppfyltTom = søkerOrdinær.tom,
                                                              personType = PersonType.SØKER))
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerUtvidet.fom,
                                                              vilkårOppfyltTom = søkerUtvidet.tom,
                                                              personType = PersonType.SØKER,
                                                              erUtvidet = søkerUtvidet.erUtvidet))
                }

        val barnResultater =
                PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnOppfylt.ident)
                        .apply {
                            vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                                      vilkårOppfyltFom = barnOppfylt.fom,
                                                                      vilkårOppfyltTom = barnOppfylt.tom,
                                                                      personType = PersonType.BARN,
                                                                      erDeltBosted = barnOppfylt.erDeltBosted))
                        }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
                }

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(vilkårsvurdering = vilkårsvurdering,
                                                               personopplysningGrunnlag = personopplysningGrunnlag,
                                                               behandling = behandling,
                                                               featureToggleService = featureToggleService)
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
    fun `Utvidet andeler slutter måneden etter vilkår ikke er innfridd lenger, ikke samme slik som ellers`() {

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
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerOrdinær.fom,
                                                              vilkårOppfyltTom = søkerOrdinær.tom,
                                                              personType = PersonType.SØKER))
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerUtvidet.fom,
                                                              vilkårOppfyltTom = søkerUtvidet.tom,
                                                              personType = PersonType.SØKER,
                                                              erUtvidet = søkerUtvidet.erUtvidet))
                }

        val barnResultater =
                PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnOppfylt.ident)
                        .apply {
                            vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                                      vilkårOppfyltFom = barnOppfylt.fom,
                                                                      vilkårOppfyltTom = barnOppfylt.tom,
                                                                      personType = PersonType.BARN,
                                                                      erDeltBosted = barnOppfylt.erDeltBosted))
                        }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
                .apply {
                    personer.addAll(listOf(søkerOrdinær, barnOppfylt).lagGrunnlagPersoner(this))
                }

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(vilkårsvurdering = vilkårsvurdering,
                                                               personopplysningGrunnlag = personopplysningGrunnlag,
                                                               behandling = behandling,
                                                               featureToggleService = featureToggleService)
                .andelerTilkjentYtelse.toList().sortedBy { it.type }

        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(barnOppfylt.ident, andelBarn.personIdent)
        assertEquals(barnOppfylt.tom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerUtvidet.ident, andelUtvidet.personIdent)
        assertEquals(søkerUtvidet.tom.nesteMåned(), andelUtvidet.stønadTom)
    }

    private data class OppfyltPeriode(val fom: LocalDate,
                                      val tom: LocalDate,
                                      val ident: String = randomFnr(),
                                      val rolle: PersonType = PersonType.SØKER,
                                      val erUtvidet: Boolean = false,
                                      val erDeltBosted: Boolean = false)

    private fun oppfylteVilkårFor(personResultat: PersonResultat,
                                  vilkårOppfyltFom: LocalDate?,
                                  vilkårOppfyltTom: LocalDate?,
                                  personType: PersonType,
                                  erUtvidet: Boolean = false,
                                  erDeltBosted: Boolean = false): Set<VilkårResultat> {
        val vilkårSomSkalVurderes = if (erUtvidet)
            listOf(Vilkår.UTVIDET_BARNETRYGD)
        else
            Vilkår.hentVilkårFor(personType = personType)

        return vilkårSomSkalVurderes.map {
            VilkårResultat(personResultat = personResultat,
                           vilkårType = it,
                           resultat = Resultat.OPPFYLT,
                           periodeFom = if (it == Vilkår.UNDER_18_ÅR) fødselsdato else vilkårOppfyltFom,
                           periodeTom = if (it == Vilkår.UNDER_18_ÅR) fødselsdato.plusYears(18) else vilkårOppfyltTom,
                           begrunnelse = "",
                           behandlingId = personResultat.vilkårsvurdering.behandling.id,
                           erDeltBosted = erDeltBosted)
        }.toSet()
    }

    private fun List<OppfyltPeriode>.lagGrunnlagPersoner(personopplysningGrunnlag: PersonopplysningGrunnlag): List<Person> = this.map {
        Person(aktørId = randomAktørId(),
               personIdent = PersonIdent(it.ident),
               type = it.rolle,
               personopplysningGrunnlag = personopplysningGrunnlag,
               fødselsdato = fødselsdato,
               navn = "Test Testesen",
               kjønn = Kjønn.KVINNE)
                .apply {
                    sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this))
                }
    }
}