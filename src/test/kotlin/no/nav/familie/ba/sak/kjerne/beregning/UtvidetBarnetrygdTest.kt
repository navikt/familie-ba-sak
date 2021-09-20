package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtvidetBarnetrygdTest {

    private val featureToggleService = mockk<FeatureToggleService>()

    val fødselsdatoBarn = LocalDate.of(2014, 1, 1)

    @BeforeEach
    fun setUp() {
        every { featureToggleService.isEnabled(any()) } answers { true }
    }

    data class OppfyltPeriode(val oppfyltFom: LocalDate,
                              val oppfyltTom: LocalDate,
                              val ident: String = randomFnr(),
                              val erUtvidet: Boolean = false,
                              val erDeltBosted: Boolean = false)

    @Test
    fun `Velger høyeste beløp i periode med utvidet barnetrygd`() {

        val søkerOrdinær =
                OppfyltPeriode(oppfyltFom = LocalDate.of(2019, 4, 1), oppfyltTom = LocalDate.of(2020, 6, 15))
        val søkerUtvidet =
                OppfyltPeriode(oppfyltFom = LocalDate.of(2019, 6, 15), oppfyltTom = søkerOrdinær.oppfyltTom, erUtvidet = true)
        val barnA =
                OppfyltPeriode(oppfyltFom = LocalDate.of(2019, 4, 1), oppfyltTom = LocalDate.of(2020, 6, 15), erDeltBosted = true)
        val barnB =
                OppfyltPeriode(oppfyltFom = LocalDate.of(2019, 4, 1), oppfyltTom = LocalDate.of(2020, 2, 15))

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søkerOrdinær.ident)
                .apply {
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerOrdinær.oppfyltFom,
                                                              vilkårOppfyltTom = søkerOrdinær.oppfyltTom,
                                                              personType = PersonType.SØKER))
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerUtvidet.oppfyltFom,
                                                              vilkårOppfyltTom = søkerUtvidet.oppfyltTom,
                                                              personType = PersonType.SØKER,
                                                              erUtvidet = søkerUtvidet.erUtvidet))
                }

        val barnResultater = listOf(barnA, barnB).map {
            PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = it.ident)
                    .apply {
                        vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                                  vilkårOppfyltFom = it.oppfyltFom,
                                                                  vilkårOppfyltTom = it.oppfyltTom,
                                                                  personType = PersonType.BARN,
                                                                  erDeltBosted = it.erDeltBosted))
                    }
        }
        vilkårsvurdering.apply { personResultater = (listOf(søkerResultat) + barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
        val søker = Person(aktørId = randomAktørId(),
                           personIdent = PersonIdent(søkerOrdinær.ident),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = LocalDate.of(1990, 1, 1),
                           navn = "Pappa Pappasen",
                           kjønn = Kjønn.MANN)
        val barn = listOf(barnA, barnB).map {
            Person(aktørId = randomAktørId(),
                   personIdent = PersonIdent(it.ident),
                   type = PersonType.BARN,
                   personopplysningGrunnlag = personopplysningGrunnlag,
                   fødselsdato = fødselsdatoBarn,
                   navn = "Barn Barnesen",
                   kjønn = Kjønn.MANN)
                    .apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) }
        }
        personopplysningGrunnlag.personer.add(søker)
        personopplysningGrunnlag.personer.addAll(barn)

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(vilkårsvurdering = vilkårsvurdering,
                                                                      personopplysningGrunnlag = personopplysningGrunnlag,
                                                                      featureToggleService = featureToggleService)

        val andeler =
                tilkjentYtelse.andelerTilkjentYtelse.toList().sortedWith(compareBy({ it.stønadFom }, { it.type }, { it.beløp }))

        // TODO: Må oppdatere disse til å se på prosent
        assertEquals(4, andeler.size)

        val andelBarnA = andeler[0]
        val andelBarnB = andeler[1]
        val andelUtvidetA = andeler[2]
        val andelUtvidetB = andeler[3]

        assertEquals(barnA.ident, andelBarnA.personIdent)
        assertEquals(barnA.oppfyltFom.nesteMåned(), andelBarnA.stønadFom)
        assertEquals(barnA.oppfyltTom.toYearMonth(), andelBarnA.stønadTom)
        assertEquals(527, andelBarnA.beløp)

        assertEquals(barnB.ident, andelBarnB.personIdent)
        assertEquals(barnB.oppfyltFom.nesteMåned(), andelBarnB.stønadFom)
        assertEquals(barnB.oppfyltTom.toYearMonth(), andelBarnB.stønadTom)
        assertEquals(1054, andelBarnB.beløp)

        assertEquals(søkerOrdinær.ident, andelUtvidetA.personIdent)
        assertEquals(søkerUtvidet.oppfyltFom.nesteMåned(), andelUtvidetA.stønadFom)
        assertEquals(barnB.oppfyltTom.toYearMonth(), andelUtvidetA.stønadTom)
        assertEquals(andelBarnB.beløp, andelUtvidetA.beløp)

        assertEquals(søkerOrdinær.ident, andelUtvidetB.personIdent)
        assertEquals(barnB.oppfyltTom.nesteMåned(), andelUtvidetB.stønadFom)
        assertEquals(søkerUtvidet.oppfyltTom.toYearMonth(), andelUtvidetB.stønadTom)
        assertEquals(andelBarnA.beløp, andelUtvidetB.beløp)
    }

    @Test
    fun `Lager kun andeler for barna hvis ikke utvidet-vilkår er innfridd`() {
        val søkerOrdinær =
                OppfyltPeriode(oppfyltFom = LocalDate.of(2019, 4, 1), oppfyltTom = LocalDate.of(2020, 6, 15))
        val søkerUtvidet =
                OppfyltPeriode(oppfyltFom = LocalDate.of(2019, 6, 15), oppfyltTom = søkerOrdinær.oppfyltTom, erUtvidet = true)
        val barnOppfylt =
                OppfyltPeriode(oppfyltFom = LocalDate.of(2019, 4, 1), oppfyltTom = LocalDate.of(2020, 6, 15))

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søkerOrdinær.ident)
                .apply {
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerOrdinær.oppfyltFom,
                                                              vilkårOppfyltTom = søkerOrdinær.oppfyltTom,
                                                              personType = PersonType.SØKER))
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerUtvidet.oppfyltFom,
                                                              vilkårOppfyltTom = søkerUtvidet.oppfyltTom,
                                                              personType = PersonType.SØKER,
                                                              erUtvidet = søkerUtvidet.erUtvidet))
                }

        val barnResultater =
                PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnOppfylt.ident)
                        .apply {
                            vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                                      vilkårOppfyltFom = barnOppfylt.oppfyltFom,
                                                                      vilkårOppfyltTom = barnOppfylt.oppfyltTom,
                                                                      personType = PersonType.BARN,
                                                                      erDeltBosted = barnOppfylt.erDeltBosted))
                        }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
        val søker = Person(aktørId = randomAktørId(),
                           personIdent = PersonIdent(søkerOrdinær.ident),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = LocalDate.of(1990, 1, 1),
                           navn = "Pappa Pappasen",
                           kjønn = Kjønn.MANN)
        val barn = Person(aktørId = randomAktørId(),
                          personIdent = PersonIdent(barnOppfylt.ident),
                          type = PersonType.BARN,
                          personopplysningGrunnlag = personopplysningGrunnlag,
                          fødselsdato = fødselsdatoBarn,
                          navn = "Barn Barnesen",
                          kjønn = Kjønn.MANN)
                .apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) }

        personopplysningGrunnlag.personer.add(søker)
        personopplysningGrunnlag.personer.add(barn)

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(vilkårsvurdering = vilkårsvurdering,
                                                                      personopplysningGrunnlag = personopplysningGrunnlag,
                                                                      featureToggleService = featureToggleService)

        val andeler =
                tilkjentYtelse.andelerTilkjentYtelse.toList().sortedBy { it.type }

        // TODO: Må oppdatere disse til å se på prosent
        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(barnOppfylt.ident, andelBarn.personIdent)
        assertEquals(barnOppfylt.oppfyltFom.nesteMåned(), andelBarn.stønadFom)
        assertEquals(barnOppfylt.oppfyltTom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerOrdinær.ident, andelUtvidet.personIdent)
        assertEquals(søkerUtvidet.oppfyltFom.nesteMåned(), andelUtvidet.stønadFom)
        assertEquals(søkerUtvidet.oppfyltTom.toYearMonth(), andelUtvidet.stønadTom)
    }

    @Test
    fun `Lager kun andeler for periodene hvor barn også har andeler`() {
        val søkerOrdinær = OppfyltPeriode(oppfyltFom = LocalDate.of(2019, 4, 1), oppfyltTom = LocalDate.of(2020, 6, 15))
        val søkerUtvidet = søkerOrdinær.copy(erUtvidet = true)
        val barnOppfylt = OppfyltPeriode(oppfyltFom = LocalDate.of(2019, 6, 1), oppfyltTom = LocalDate.of(2019, 8, 15))

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søkerOrdinær.ident)
                .apply {
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerOrdinær.oppfyltFom,
                                                              vilkårOppfyltTom = søkerOrdinær.oppfyltTom,
                                                              personType = PersonType.SØKER))
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerUtvidet.oppfyltFom,
                                                              vilkårOppfyltTom = søkerUtvidet.oppfyltTom,
                                                              personType = PersonType.SØKER,
                                                              erUtvidet = søkerUtvidet.erUtvidet))
                }

        val barnResultater =
                PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnOppfylt.ident)
                        .apply {
                            vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                                      vilkårOppfyltFom = barnOppfylt.oppfyltFom,
                                                                      vilkårOppfyltTom = barnOppfylt.oppfyltTom,
                                                                      personType = PersonType.BARN,
                                                                      erDeltBosted = barnOppfylt.erDeltBosted))
                        }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
        val søker = Person(aktørId = randomAktørId(),
                           personIdent = PersonIdent(søkerOrdinær.ident),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = LocalDate.of(1990, 1, 1),
                           navn = "Pappa Pappasen",
                           kjønn = Kjønn.MANN)
        val barn = Person(aktørId = randomAktørId(),
                          personIdent = PersonIdent(barnOppfylt.ident),
                          type = PersonType.BARN,
                          personopplysningGrunnlag = personopplysningGrunnlag,
                          fødselsdato = fødselsdatoBarn,
                          navn = "Barn Barnesen",
                          kjønn = Kjønn.MANN)
                .apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) }

        personopplysningGrunnlag.personer.add(søker)
        personopplysningGrunnlag.personer.add(barn)

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(vilkårsvurdering = vilkårsvurdering,
                                                                      personopplysningGrunnlag = personopplysningGrunnlag,
                                                                      featureToggleService = featureToggleService)

        val andeler =
                tilkjentYtelse.andelerTilkjentYtelse.toList().sortedBy { it.type }

        // TODO: Må oppdatere disse til å se på prosent
        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(barnOppfylt.ident, andelBarn.personIdent)
        assertEquals(barnOppfylt.oppfyltFom.nesteMåned(), andelBarn.stønadFom)
        assertEquals(barnOppfylt.oppfyltTom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerOrdinær.ident, andelUtvidet.personIdent)
        assertEquals(barnOppfylt.oppfyltFom.nesteMåned(), andelUtvidet.stønadFom)
        assertEquals(barnOppfylt.oppfyltTom.toYearMonth(), andelUtvidet.stønadTom)
    }

    @Test
    fun `Utvida opphører måneden etter vilkår ikke er innfridd, ikke inneværende slik som vanlige vilkår`() {

        val søkerOrdinær =
                OppfyltPeriode(oppfyltFom = LocalDate.of(2019, 4, 1), oppfyltTom = LocalDate.of(2020, 6, 15))
        val søkerUtvidet =
                OppfyltPeriode(oppfyltFom = søkerOrdinær.oppfyltFom, oppfyltTom = LocalDate.of(2020, 4, 15), erUtvidet = true)
        val barnOppfylt =
                OppfyltPeriode(oppfyltFom = søkerOrdinær.oppfyltFom, oppfyltTom = søkerOrdinær.oppfyltTom)

        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søkerOrdinær.ident)
                .apply {
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerOrdinær.oppfyltFom,
                                                              vilkårOppfyltTom = søkerOrdinær.oppfyltTom,
                                                              personType = PersonType.SØKER))
                    vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                              vilkårOppfyltFom = søkerUtvidet.oppfyltFom,
                                                              vilkårOppfyltTom = søkerUtvidet.oppfyltTom,
                                                              personType = PersonType.SØKER,
                                                              erUtvidet = søkerUtvidet.erUtvidet))
                }

        val barnResultater =
                PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnOppfylt.ident)
                        .apply {
                            vilkårResultater.addAll(oppfylteVilkårFor(personResultat = this,
                                                                      vilkårOppfyltFom = barnOppfylt.oppfyltFom,
                                                                      vilkårOppfyltTom = barnOppfylt.oppfyltTom,
                                                                      personType = PersonType.BARN,
                                                                      erDeltBosted = barnOppfylt.erDeltBosted))
                        }
        vilkårsvurdering.apply { personResultater = listOf(søkerResultat, barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
        val søker = Person(aktørId = randomAktørId(),
                           personIdent = PersonIdent(søkerOrdinær.ident),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = LocalDate.of(1990, 1, 1),
                           navn = "Pappa Pappasen",
                           kjønn = Kjønn.MANN)
        val barn = Person(aktørId = randomAktørId(),
                          personIdent = PersonIdent(barnOppfylt.ident),
                          type = PersonType.BARN,
                          personopplysningGrunnlag = personopplysningGrunnlag,
                          fødselsdato = fødselsdatoBarn,
                          navn = "Barn Barnesen",
                          kjønn = Kjønn.MANN)
                .apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) }

        personopplysningGrunnlag.personer.add(søker)
        personopplysningGrunnlag.personer.add(barn)

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(vilkårsvurdering = vilkårsvurdering,
                                                                      personopplysningGrunnlag = personopplysningGrunnlag,
                                                                      featureToggleService = featureToggleService)

        val andeler =
                tilkjentYtelse.andelerTilkjentYtelse.toList().sortedBy { it.type }

        // TODO: Må oppdatere disse til å se på prosent
        assertEquals(2, andeler.size)

        val andelBarn = andeler[0]
        val andelUtvidet = andeler[1]

        assertEquals(barnOppfylt.ident, andelBarn.personIdent)
        assertEquals(barnOppfylt.oppfyltTom.toYearMonth(), andelBarn.stønadTom)

        assertEquals(søkerOrdinær.ident, andelUtvidet.personIdent)
        assertEquals(søkerUtvidet.oppfyltTom.nesteMåned(), andelUtvidet.stønadTom)
    }


    private fun LocalDate.nesteMåned() = this.toYearMonth().plusMonths(1)

    private fun oppfylteVilkårFor(personResultat: PersonResultat,
                                  vilkårOppfyltFom: LocalDate?,
                                  vilkårOppfyltTom: LocalDate?,
                                  personType: PersonType,
                                  erUtvidet: Boolean = false,
                                  erDeltBosted: Boolean = false): Set<VilkårResultat> {
        val vilkårSomSkalVurderes = if (erUtvidet)
            Vilkår.hentVilkårFor(personType = PersonType.SØKER,
                                 ytelseType = YtelseType.UTVIDET_BARNETRYGD)
        else
            Vilkår.hentVilkårFor(personType = personType)

        return vilkårSomSkalVurderes.map {
            VilkårResultat(personResultat = personResultat,
                           vilkårType = it,
                           resultat = Resultat.OPPFYLT,
                           periodeFom = if (it == Vilkår.UNDER_18_ÅR) fødselsdatoBarn else vilkårOppfyltFom,
                           periodeTom = if (it == Vilkår.UNDER_18_ÅR) fødselsdatoBarn.plusYears(18) else vilkårOppfyltTom,
                           begrunnelse = "",
                           behandlingId = personResultat.vilkårsvurdering.behandling.id,
                           erDeltBosted = erDeltBosted)
        }.toSet()
    }
}