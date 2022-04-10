package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering

data class VilkårsvurderingBuilder<T : Tidsenhet>(
    private val behandling: Behandling = lagBehandling(),
    private val vilkårsvurdering: Vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
) {
    val personresultater: MutableSet<PersonResultat> = mutableSetOf()

    fun forPerson(person: Person, startTidspunkt: Tidspunkt<T>): PersonResultatBuilder<T> {
        return PersonResultatBuilder(this, startTidspunkt, person)
    }

    fun byggVilkårsvurdering(): Vilkårsvurdering {
        vilkårsvurdering.personResultater = personresultater
        return vilkårsvurdering
    }

    data class PersonResultatBuilder<T : Tidsenhet>(
        val vilkårsvurderingBuilder: VilkårsvurderingBuilder<T>,
        val startTidspunkt: Tidspunkt<T>,
        private val person: Person = tilfeldigPerson(),
        private val vilkårsresultatTidslinjer: List<Tidslinje<VilkårRegelverkResultat, T>> = emptyList(),
    ) {
        fun medVilkår(v: String, vilkår: Vilkår): PersonResultatBuilder<T> {
            return copy(
                vilkårsresultatTidslinjer = this.vilkårsresultatTidslinjer +
                    v.tilVilkårRegelverkResultatTidslinje(vilkår, startTidspunkt)
            )
        }

        fun forPerson(person: Person, startTidspunkt: Tidspunkt<T>): PersonResultatBuilder<T> {
            return byggPerson().forPerson(person, startTidspunkt)
        }

        fun byggVilkårsvurdering(): Vilkårsvurdering = byggPerson().byggVilkårsvurdering()

        fun byggPerson(): VilkårsvurderingBuilder<T> {

            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurderingBuilder.vilkårsvurdering,
                aktør = person.aktør
            )

            val vilkårresultater = vilkårsresultatTidslinjer.flatMap {
                it.perioder()
                    .filter { it.innhold != null }
                    .flatMap { periode -> periode.tilVilkårResultater(personResultat) }
            }

            personResultat.vilkårResultater.addAll(vilkårresultater)
            vilkårsvurderingBuilder.personresultater.add(personResultat)

            return vilkårsvurderingBuilder
        }
    }
}

internal fun <T : Tidsenhet> Periode<VilkårRegelverkResultat, T>.tilVilkårResultater(personResultat: PersonResultat): Collection<VilkårResultat> {
    return listOf(
        VilkårResultat(
            personResultat = personResultat,
            vilkårType = this.innhold?.vilkår!!,
            resultat = this.innhold?.resultat!!,
            vurderesEtter = this.innhold?.regelverk,
            periodeFom = this.fraOgMed.tilLocalDateEllerNull(),
            periodeTom = this.tilOgMed.tilLocalDateEllerNull(),
            begrunnelse = "",
            behandlingId = 0
        )
    )
}
