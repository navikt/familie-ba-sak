package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseUtils
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering

data class VilkårsvurderingBuilder<T : Tidsenhet>(
    val behandling: Behandling = lagBehandling(),
    private val vilkårsvurdering: Vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
) {
    val personresultater: MutableSet<PersonResultat> = mutableSetOf()
    val personer: MutableSet<Person> = mutableSetOf()

    fun forPerson(person: Person, startTidspunkt: Tidspunkt<T>): PersonResultatBuilder<T> {
        return PersonResultatBuilder(this, startTidspunkt, person)
    }

    fun byggVilkårsvurdering(): Vilkårsvurdering {
        vilkårsvurdering.personResultater = personresultater
        return vilkårsvurdering
    }

    fun byggPersonopplysningGrunnlag(): PersonopplysningGrunnlag {
        return lagTestPersonopplysningGrunnlag(behandling.id, *personer.toTypedArray())
    }

    data class PersonResultatBuilder<T : Tidsenhet>(
        val vilkårsvurderingBuilder: VilkårsvurderingBuilder<T>,
        val startTidspunkt: Tidspunkt<T>,
        private val person: Person = tilfeldigPerson(),
        private val vilkårsresultatTidslinjer: MutableList<Tidslinje<VilkårRegelverkResultat, T>> = mutableListOf()
    ) {
        fun medVilkår(v: String, vararg vilkår: Vilkår): PersonResultatBuilder<T> {
            vilkårsresultatTidslinjer.addAll(vilkår.map { v.tilVilkårRegelverkResultatTidslinje(it, startTidspunkt) })
            return this
        }

        fun forPerson(person: Person, startTidspunkt: Tidspunkt<T>): PersonResultatBuilder<T> {
            return byggPerson().forPerson(person, startTidspunkt)
        }

        fun byggVilkårsvurdering(): Vilkårsvurdering = byggPerson().byggVilkårsvurdering()
        fun byggPersonopplysningGrunnlag(): PersonopplysningGrunnlag = byggPerson().byggPersonopplysningGrunnlag()

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
            vilkårsvurderingBuilder.personer.add(person)

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
            periodeFom = this.fraOgMed.tilFørsteDagIMåneden().tilLocalDateEllerNull(),
            periodeTom = this.tilOgMed.tilSisteDagIMåneden().tilLocalDateEllerNull(),
            begrunnelse = "",
            behandlingId = personResultat.vilkårsvurdering.behandling.id
        )
    )
}

fun <T : Tidsenhet> VilkårsvurderingBuilder<T>.byggVilkårsvurderingTidslinjer() =
    VilkårsvurderingTidslinjer(this.byggVilkårsvurdering(), this.byggPersonopplysningGrunnlag())

fun <T : Tidsenhet> VilkårsvurderingBuilder<T>.byggTilkjentYtelse() =
    TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
        vilkårsvurdering = this.byggVilkårsvurdering(),
        personopplysningGrunnlag = this.byggPersonopplysningGrunnlag(),
        behandling = behandling
    )
