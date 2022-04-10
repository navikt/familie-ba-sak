package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.YearMonth

data class VilkårsvurderingBuilder(
    private val behandling: Behandling = lagBehandling(),
    private val vilkårsvurdering: Vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
) {
    val personresultater: MutableSet<PersonResultat> = mutableSetOf()

    fun forPerson(person: Person, startMåned: YearMonth = YearMonth.of(2020, 1)): PersonResultatBuilder {
        return PersonResultatBuilder(this, startMåned, person)
    }

    fun byggVilkårsvurdering(): Vilkårsvurdering {
        vilkårsvurdering.personResultater = personresultater
        return vilkårsvurdering
    }

    data class PersonResultatBuilder(
        val vilkårsvurderingBuilder: VilkårsvurderingBuilder,
        val startMåned: YearMonth,
        private val person: Person = tilfeldigPerson(),
        private val vilkårsresultatTidslinjer: List<Tidslinje<VilkårRegelverkResultat, Måned>> = emptyList(),
    ) {
        fun medVilkår(v: String, vilkår: Vilkår): PersonResultatBuilder {
            return copy(
                vilkårsresultatTidslinjer = this.vilkårsresultatTidslinjer +
                    v.tilVilkårRegelverkResultatTidslinje(vilkår, Tidspunkt.med(startMåned))
            )
        }

        fun forPerson(person: Person, startMåned: YearMonth = YearMonth.of(2020, 1)): PersonResultatBuilder {
            return byggPerson().forPerson(person, startMåned)
        }

        fun byggVilkårsvurdering(): Vilkårsvurdering = byggPerson().byggVilkårsvurdering()

        fun byggPerson(): VilkårsvurderingBuilder {

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

internal fun Periode<VilkårRegelverkResultat, Måned>.tilVilkårResultater(personResultat: PersonResultat): Collection<VilkårResultat> {
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
