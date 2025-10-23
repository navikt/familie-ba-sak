package no.nav.familie.ba.sak.kjerne.tidslinje.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseGenerator
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad.OvergangsstønadService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.LocalDate
import java.time.YearMonth

data class VilkårsvurderingBuilder(
    val behandling: Behandling = lagBehandling(),
    private val vilkårsvurdering: Vilkårsvurdering = Vilkårsvurdering(behandling = behandling),
) {
    val personresultater: MutableSet<PersonResultat> = mutableSetOf()
    val personer: MutableSet<Person> = mutableSetOf()

    fun forPerson(
        person: Person,
        startTidspunkt: LocalDate,
    ): PersonResultatBuilder = PersonResultatBuilder(this, startTidspunkt, person)

    fun forPerson(
        person: Person,
        startTidspunkt: YearMonth,
    ): PersonResultatBuilder = PersonResultatBuilder(this, startTidspunkt.førsteDagIInneværendeMåned(), person)

    fun byggVilkårsvurdering(): Vilkårsvurdering {
        vilkårsvurdering.personResultater = personresultater
        return vilkårsvurdering
    }

    fun byggPersonopplysningGrunnlag(): PersonopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, *personer.toTypedArray())

    data class PersonResultatBuilder(
        val vilkårsvurderingBuilder: VilkårsvurderingBuilder,
        val startTidspunkt: LocalDate,
        private val person: Person = tilfeldigPerson(),
        private val vilkårsresultatTidslinjer: MutableList<Tidslinje<UtdypendeVilkårRegelverkResultat>> = mutableListOf(),
    ) {
        fun medVilkår(
            v: String,
            vararg vilkår: Vilkår,
        ): PersonResultatBuilder {
            vilkårsresultatTidslinjer.addAll(
                vilkår.map { v.tilUtdypendeVilkårRegelverkResultatTidslinje(it, startTidspunkt.toYearMonth()) },
            )
            return this
        }

        fun medVilkår(tidslinje: Tidslinje<VilkårRegelverkResultat>): PersonResultatBuilder {
            vilkårsresultatTidslinjer.add(
                tidslinje.mapIkkeNull { UtdypendeVilkårRegelverkResultat(it.vilkår, it.resultat, it.regelverk) },
            )
            return this
        }

        fun medUtdypendeVilkår(tidslinje: Tidslinje<UtdypendeVilkårRegelverkResultat>): PersonResultatBuilder {
            vilkårsresultatTidslinjer.add(tidslinje)
            return this
        }

        fun medUtdypendeVilkårsvurdering(
            v: String,
            vilkår: Vilkår,
            utdypendeVilkårsvurdering: UtdypendeVilkårsvurdering,
        ): PersonResultatBuilder {
            vilkårsresultatTidslinjer.add(
                v.tilCharTidslinje(startTidspunkt).mapVerdi {
                    UtdypendeVilkårRegelverkResultat(vilkår, Resultat.OPPFYLT, Regelverk.EØS_FORORDNINGEN, utdypendeVilkårsvurdering)
                },
            )
            return this
        }

        fun forPerson(
            person: Person,
            startTidspunkt: LocalDate,
        ): PersonResultatBuilder = byggPerson().forPerson(person, startTidspunkt)

        fun forPerson(
            person: Person,
            startTidspunkt: YearMonth,
        ): PersonResultatBuilder = byggPerson().forPerson(person, startTidspunkt)

        fun byggVilkårsvurdering(): Vilkårsvurdering = byggPerson().byggVilkårsvurdering()

        fun byggPersonopplysningGrunnlag(): PersonopplysningGrunnlag = byggPerson().byggPersonopplysningGrunnlag()

        fun byggPerson(): VilkårsvurderingBuilder {
            val personResultat =
                PersonResultat(
                    vilkårsvurdering = vilkårsvurderingBuilder.vilkårsvurdering,
                    aktør = person.aktør,
                )

            val vilkårresultater =
                vilkårsresultatTidslinjer.flatMap {
                    it
                        .tilPerioderIkkeNull()
                        .flatMap { periode -> periode.tilVilkårResultater(personResultat) }
                }

            personResultat.vilkårResultater.addAll(vilkårresultater)
            vilkårsvurderingBuilder.personresultater.add(personResultat)
            vilkårsvurderingBuilder.personer.add(person)

            return vilkårsvurderingBuilder
        }
    }
}

internal fun Periode<UtdypendeVilkårRegelverkResultat>.tilVilkårResultater(personResultat: PersonResultat): Collection<VilkårResultat> =
    listOf(
        VilkårResultat(
            personResultat = personResultat,
            vilkårType = this.verdi.vilkår,
            resultat = this.verdi.resultat!!,
            vurderesEtter = this.verdi.regelverk,
            periodeFom = this.fom,
            periodeTom = this.tom,
            begrunnelse = "En begrunnelse",
            sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = this.verdi.utdypendeVilkårsvurderinger,
        ),
    )

fun VilkårsvurderingBuilder.byggVilkårsvurderingTidslinjer() = VilkårsvurderingTidslinjer(this.byggVilkårsvurdering(), this.byggPersonopplysningGrunnlag().tilPersonEnkelSøkerOgBarn())

fun VilkårsvurderingBuilder.PersonResultatBuilder.byggVilkårsvurderingTidslinjer() = this.byggPerson().byggVilkårsvurderingTidslinjer()

fun VilkårsvurderingBuilder.byggTilkjentYtelse(): TilkjentYtelse {
    val vilkårsvurdering = this.byggVilkårsvurdering()

    val overgangsstønadServiceMock: OvergangsstønadService = mockk()
    val vilkårsvurderingServiceMock: VilkårsvurderingService = mockk()
    val featureToggleServiceMock: FeatureToggleService = mockk()
    val tilkjentYtelseGenerator = TilkjentYtelseGenerator(overgangsstønadServiceMock, vilkårsvurderingServiceMock, featureToggleServiceMock)

    every { overgangsstønadServiceMock.hentOgLagrePerioderMedOvergangsstønadForBehandling(any(), any()) } returns mockkObject()
    every { overgangsstønadServiceMock.hentPerioderMedFullOvergangsstønad(any<Behandling>()) } answers { emptyList() }
    every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering
    every { featureToggleServiceMock.isEnabled(FeatureToggle.SKAL_INKLUDERE_ÅRSAK_ENDRE_MOTTAKER_I_INITIELL_GENERERING_AV_ANDELER) } returns true

    return tilkjentYtelseGenerator.genererTilkjentYtelse(
        behandling = vilkårsvurdering.behandling,
        personopplysningGrunnlag = this.byggPersonopplysningGrunnlag(),
    )
}

data class UtdypendeVilkårRegelverkResultat(
    val vilkår: Vilkår,
    val resultat: Resultat?,
    val regelverk: Regelverk?,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
) {
    constructor(
        vilkår: Vilkår,
        resultat: Resultat?,
        regelverk: Regelverk?,
        vararg utdypendeVilkårsvurdering: UtdypendeVilkårsvurdering,
    ) : this(vilkår, resultat, regelverk, utdypendeVilkårsvurdering.toList())
}
