package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Dødsfall
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.util.UtdypendeVilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import java.time.LocalDate

val barn get() = PersonType.BARN
val søker get() = PersonType.SØKER

infix fun PersonType.født(tidspunkt: LocalDate) = tilfeldigPerson(personType = this, fødselsdato = tidspunkt)

internal infix fun Person.død(tidspunkt: LocalDate) =
    this.copy(
        dødsfall =
            Dødsfall(
                person = this,
                dødsfallDato = tidspunkt,
                dødsfallAdresse = null,
                dødsfallPostnummer = null,
                dødsfallPoststed = null,
            ),
    )

internal val uendelig = null

internal fun Person.under18år() = fødselsdato.rangeTo(fødselsdato.plusYears(18).minusDays(1))

val vilkårsvurdering get() = VilkårsvurderingBuilder()

infix fun Vilkår.og(vilkår: Vilkår) = listOf(this, vilkår)

infix fun List<Vilkår>.og(vilkår: Vilkår) = this + vilkår

infix fun UtdypendeVilkårRegelverkResultat.i(tidsrom: ClosedRange<LocalDate>) = Periode(this, tidsrom.start, tidsrom.endInclusive).tilTidslinje()

infix fun List<Vilkår>.oppfylt(tidsrom: ClosedRange<LocalDate>) =
    this.map {
        oppfyltUtdypendeVilkår(it, null) i tidsrom
    }

infix fun Vilkår.oppfylt(tidsrom: ClosedRange<LocalDate>) = oppfyltUtdypendeVilkår(this, null) i tidsrom

infix fun Tidslinje<UtdypendeVilkårRegelverkResultat>.etter(regelverk: Regelverk) = this.mapIkkeNull { it.copy(regelverk = regelverk) }

infix fun Tidslinje<UtdypendeVilkårRegelverkResultat>.med(utdypendeVilkår: UtdypendeVilkårsvurdering) = this.mapIkkeNull { it.copy(utdypendeVilkårsvurderinger = it.utdypendeVilkårsvurderinger + utdypendeVilkår) }

infix fun VilkårsvurderingBuilder.der(person: Person) = this.forPerson(person, LocalDate.now())

infix fun VilkårsvurderingBuilder.PersonResultatBuilder.har(vilkår: Tidslinje<UtdypendeVilkårRegelverkResultat>) = this.medUtdypendeVilkår(vilkår)

infix fun VilkårsvurderingBuilder.PersonResultatBuilder.har(vilkår: Iterable<Tidslinje<UtdypendeVilkårRegelverkResultat>>) = vilkår.map { this.medUtdypendeVilkår(it) }.last()

infix fun VilkårsvurderingBuilder.PersonResultatBuilder.og(vilkår: Tidslinje<UtdypendeVilkårRegelverkResultat>) = har(vilkår)

infix fun VilkårsvurderingBuilder.PersonResultatBuilder.og(vilkår: Iterable<Tidslinje<UtdypendeVilkårRegelverkResultat>>) = har(vilkår)

infix fun VilkårsvurderingBuilder.PersonResultatBuilder.der(person: Person) = this.forPerson(person, LocalDate.now())

fun oppfyltUtdypendeVilkår(
    vilkår: Vilkår,
    regelverk: Regelverk? = null,
) = UtdypendeVilkårRegelverkResultat(
    vilkår = vilkår,
    resultat = Resultat.OPPFYLT,
    regelverk = regelverk,
)
