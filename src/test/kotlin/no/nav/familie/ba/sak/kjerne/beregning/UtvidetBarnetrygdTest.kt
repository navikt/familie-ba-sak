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

    data class TestPerson(val oppfyltFom: LocalDate,
                          val oppfyltTom: LocalDate,
                          val ident: String = randomFnr(),
                          val erDeltBosted: Boolean = false)

    @Test
    fun `Barn som fyller 6 år i det vilkårene er oppfylt får andel måneden etter`() {

        // Først barn A og B
        // Så kommer utvidet for søker med barn B-sats
        // Så fjernes barn B og man bruker barn A-sats
        val søker = TestPerson(oppfyltFom = LocalDate.of(2019, 2, 1), oppfyltTom = LocalDate.of(2020, 6, 30))
        val barnB = TestPerson(oppfyltFom = LocalDate.of(2019, 1, 1), oppfyltTom = LocalDate.of(2020, 6, 30), erDeltBosted = true)
        val barnA = TestPerson(oppfyltFom = LocalDate.of(2019, 1, 1), oppfyltTom = LocalDate.of(2020, 2, 28))

        val (vilkårsvurdering, personopplysningGrunnlag) =
                genererVilkårsvurderingOgPersonopplysningGrunnlag(søkerTestPerson = søker,
                                                                  barnTestPersoner = listOf(barnA, barnB))

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(vilkårsvurdering = vilkårsvurdering,
                                                                      personopplysningGrunnlag = personopplysningGrunnlag,
                                                                      featureToggleService = featureToggleService)

        assertEquals(4, tilkjentYtelse.andelerTilkjentYtelse.size)
        assertEquals(4, tilkjentYtelse.andelerTilkjentYtelse.size)

    }

    private fun genererVilkårsvurderingOgPersonopplysningGrunnlag(søkerTestPerson: TestPerson,
                                                                  barnTestPersoner: List<TestPerson>): Pair<Vilkårsvurdering, PersonopplysningGrunnlag> {
        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søkerTestPerson.ident)
                .apply {
                    setSortedVilkårResultater(oppfylteVilkårFor(personResultat = this,
                                                                vilkårOppfyltFom = søkerTestPerson.oppfyltFom,
                                                                vilkårOppfyltTom = søkerTestPerson.oppfyltTom,
                                                                personType = PersonType.SØKER))
                }

        val barnResultater = barnTestPersoner.map {
            PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = it.ident)
                    .apply {
                        setSortedVilkårResultater(oppfylteVilkårFor(personResultat = this,
                                                                    vilkårOppfyltFom = it.oppfyltFom,
                                                                    vilkårOppfyltTom = it.oppfyltTom,
                                                                    personType = PersonType.BARN,
                                                                    erDeltBosted = it.erDeltBosted))
                    }
        }
        vilkårsvurdering.apply { personResultater = (listOf(søkerResultat) + barnResultater).toSet() }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
        val søker = Person(aktørId = randomAktørId(),
                           personIdent = PersonIdent(søkerTestPerson.ident),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = LocalDate.of(1990, 1, 1),
                           navn = "Pappa Pappasen",
                           kjønn = Kjønn.MANN)
        val barn = barnTestPersoner.map {
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

        return Pair(vilkårsvurdering, personopplysningGrunnlag)
    }

    fun oppfylteVilkårFor(personResultat: PersonResultat,
                          vilkårOppfyltFom: LocalDate?,
                          vilkårOppfyltTom: LocalDate?,
                          personType: PersonType,
                          erDeltBosted: Boolean = false): Set<VilkårResultat> {
        val vilkårSomSkalVurderes = Vilkår.hentVilkårFor(personType = personType).toMutableList()
                .also {
                    if (personType == PersonType.SØKER) it.addAll(Vilkår.hentVilkårFor(personType = personType,
                                                                                       ytelseType = YtelseType.UTVIDET_BARNETRYGD))
                }
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