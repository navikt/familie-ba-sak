package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.common.oppfyltVilkår
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Dødsfall
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.TidspunktClosedRange
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Uendelighet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilLocalDate
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.util.UtdypendeVilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.time.LocalDate

val barn get() = PersonType.BARN
val søker get() = PersonType.SØKER
infix fun PersonType.født(tidspunkt: Tidspunkt<Dag>) =
    tilfeldigPerson(personType = this, fødselsdato = tidspunkt.tilLocalDate())

internal infix fun Person.død(tidspunkt: Tidspunkt<Dag>) = this.copy(
    dødsfall = Dødsfall(
        person = this,
        dødsfallDato = tidspunkt.tilLocalDate(),
        dødsfallAdresse = null,
        dødsfallPostnummer = null,
        dødsfallPoststed = null
    )
)

internal val uendelig: Tidspunkt<Dag> = DagTidspunkt(LocalDate.now(), Uendelighet.FREMTID)
internal fun Person.til18ÅrVilkårsdato() = DagTidspunkt.med(this.fødselsdato.til18ÅrsVilkårsdato())

val vilkårsvurdering get() = VilkårsvurderingBuilder<Dag>()
infix fun Vilkår.etter(regelverk: Regelverk) = oppfyltVilkår(this, regelverk)
infix fun <T : Tidsenhet> Vilkår.i(tidsrom: TidspunktClosedRange<T>) = oppfyltUtdypendeVilkår(this, null) i tidsrom
infix fun <T : Tidsenhet> UtdypendeVilkårRegelverkResultat.i(tidsrom: TidspunktClosedRange<T>) =
    tidsrom.tilTidslinje { this }

infix fun <T : Tidsenhet> Vilkår.oppfylt(tidsrom: TidspunktClosedRange<T>) =
    oppfyltUtdypendeVilkår(this, null) i tidsrom

infix fun <T : Tidsenhet> Tidslinje<UtdypendeVilkårRegelverkResultat, T>.etter(regelverk: Regelverk) =
    this.mapIkkeNull { it.copy(regelverk = regelverk) }

infix fun <T : Tidsenhet> Tidslinje<UtdypendeVilkårRegelverkResultat, T>.med(utdypendeVilkår: UtdypendeVilkårsvurdering) =
    this.mapIkkeNull { it.copy(utdypendeVilkårsvurderinger = it.utdypendeVilkårsvurderinger + utdypendeVilkår) }

infix fun VilkårsvurderingBuilder<Dag>.der(person: Person) = this.forPerson(person, DagTidspunkt.nå())
infix fun VilkårsvurderingBuilder.PersonResultatBuilder<Dag>.har(vilkår: Tidslinje<UtdypendeVilkårRegelverkResultat, Dag>) =
    this.medUtdypendeVilkår(vilkår)

infix fun VilkårsvurderingBuilder.PersonResultatBuilder<Dag>.og(vilkår: Tidslinje<UtdypendeVilkårRegelverkResultat, Dag>) =
    this.medUtdypendeVilkår(vilkår)

infix fun VilkårsvurderingBuilder.PersonResultatBuilder<Dag>.der(person: Person) =
    this.forPerson(person, DagTidspunkt.nå())

fun oppfyltUtdypendeVilkår(vilkår: Vilkår, regelverk: Regelverk? = null) =
    UtdypendeVilkårRegelverkResultat(
        vilkår = vilkår,
        resultat = Resultat.OPPFYLT,
        regelverk = regelverk
    )
