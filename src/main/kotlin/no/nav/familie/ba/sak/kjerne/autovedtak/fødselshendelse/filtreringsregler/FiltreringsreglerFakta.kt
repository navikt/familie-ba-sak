package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.time.LocalDate

sealed class FiltreringsreglerFakta {
    abstract val søker: Person
    abstract val søkerMottarLøpendeUtvidet: Boolean
    abstract val søkerOppfyllerVilkårForUtvidetBarnetrygd: Boolean
    abstract val søkerMottarEøsBarnetrygd: Boolean
    abstract val barnaSomSkalVurderes: List<Person>
    abstract val søkerLever: Boolean
    abstract val barnaLever: Boolean
    abstract val søkerHarVerge: Boolean
    abstract val løperBarnetrygdForBarnetPåAnnenForelder: Boolean
}

data class FiltreringsreglerFaktaFødselshendelse(
    override val søker: Person,
    override val søkerMottarLøpendeUtvidet: Boolean = false,
    override val søkerOppfyllerVilkårForUtvidetBarnetrygd: Boolean,
    override val søkerMottarEøsBarnetrygd: Boolean = false,
    override val barnaSomSkalVurderes: List<Person>,
    override val søkerLever: Boolean,
    override val barnaLever: Boolean,
    override val søkerHarVerge: Boolean,
    override val løperBarnetrygdForBarnetPåAnnenForelder: Boolean,
    val restenAvBarna: List<PersonInfo>,
    val erFagsakenMigrertEtterBarnFødt: Boolean, // TODO : Er det relevant?
    val morHarIkkeOpphørtBarnetrygd: Boolean,
    @JsonIgnore val dagensDato: LocalDate = LocalDate.now(),
) : FiltreringsreglerFakta()

data class FiltreringsreglerFaktaSøknad(
    override val søker: Person,
    override val søkerMottarLøpendeUtvidet: Boolean = false,
    override val søkerOppfyllerVilkårForUtvidetBarnetrygd: Boolean,
    override val søkerMottarEøsBarnetrygd: Boolean = false,
    override val barnaSomSkalVurderes: List<Person>,
    override val søkerLever: Boolean,
    override val barnaLever: Boolean,
    override val søkerHarVerge: Boolean,
    override val løperBarnetrygdForBarnetPåAnnenForelder: Boolean,
    val søkerHarIkkeLøpendeUtbetalingOgHarAldriHattUtbetaling: Boolean // TODO : Skal vi gjøre denne sjekken i baks-mottak? Sjekk med Anna
) : FiltreringsreglerFakta()
