package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.common.oppfyltVilkår
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.medRegelverk
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.TidspunktClosedRange
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilLocalDate
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

val barn get() = PersonType.BARN
val søker get() = PersonType.SØKER
infix fun PersonType.født(tidspunkt: Tidspunkt<Dag>) =
    tilfeldigPerson(personType = this, fødselsdato = tidspunkt.tilLocalDate())

val vilkårsvurdering get() = VilkårsvurderingBuilder<Dag>()
infix fun Vilkår.etter(regelverk: Regelverk) = oppfyltVilkår(this, regelverk)
infix fun <T : Tidsenhet> Vilkår.i(tidsrom: TidspunktClosedRange<T>) = oppfyltVilkår(this, null) i tidsrom
infix fun <T : Tidsenhet> VilkårRegelverkResultat.i(tidsrom: TidspunktClosedRange<T>) = tidsrom.tilTidslinje { this }
infix fun <T : Tidsenhet> Vilkår.oppfylt(tidsrom: TidspunktClosedRange<T>) = oppfyltVilkår(this, null) i tidsrom
infix fun <T : Tidsenhet> Tidslinje<VilkårRegelverkResultat, T>.etter(regelverk: Regelverk) =
    this.map { it?.medRegelverk(regelverk) }

infix fun VilkårsvurderingBuilder<Dag>.der(person: Person) = this.forPerson(person, DagTidspunkt.nå())
infix fun VilkårsvurderingBuilder.PersonResultatBuilder<Dag>.har(vilkår: Tidslinje<VilkårRegelverkResultat, Dag>) =
    this.medVilkår(vilkår)

infix fun VilkårsvurderingBuilder.PersonResultatBuilder<Dag>.og(vilkår: Tidslinje<VilkårRegelverkResultat, Dag>) =
    this.medVilkår(vilkår)

infix fun VilkårsvurderingBuilder.PersonResultatBuilder<Dag>.der(person: Person) =
    this.forPerson(person, DagTidspunkt.nå())
