package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilAndelForVedtaksperiodeTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilIEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtfyltUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.tilIUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.UtfyltValutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.tilIValutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.tilTidslinje
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMedNullable
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.tilAndelerForVedtaksbegrunnelseTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvedeVilkårTidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilTidslinjeForSplittForPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

typealias AktørId = String

data class GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag(
    val overlappendeGenerelleAvslagVedtaksperiodeGrunnlagForPerson: Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned>,
    val vedtaksperiodeGrunnlagForPerson: Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned>,
)

data class AktørOgRolleBegrunnelseGrunnlag(
    val aktør: Aktør,
    val rolleBegrunnelseGrunnlag: PersonType,
)

data class BehandlingsGrunnlagForVedtaksperioder(
    val persongrunnlag: PersonopplysningGrunnlag,
    val personResultater: Set<PersonResultat>,
    val behandling: Behandling,
    val kompetanser: List<Kompetanse>,
    val endredeUtbetalinger: List<EndretUtbetalingAndel>,
    val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    val perioderOvergangsstønad: List<InternPeriodeOvergangsstønad>,
    val uregistrerteBarn: List<BarnMedOpplysninger>,
    val utenlandskPeriodebeløp: List<UtenlandskPeriodebeløp>,
    val valutakurs: List<Valutakurs>,
) {
    val utfylteEndredeUtbetalinger =
        endredeUtbetalinger
            .map { it.tilIEndretUtbetalingAndel() }
            .filterIsInstance<IUtfyltEndretUtbetalingAndel>()

    val utfylteKompetanser =
        kompetanser
            .map { it.tilIKompetanse() }
            .filterIsInstance<UtfyltKompetanse>()

    val utfylteValutakurs =
        valutakurs
            .map { it.tilIValutakurs() }
            .filterIsInstance<UtfyltValutakurs>()

    val utfylteUtenlandskPeriodebeløp =
        utenlandskPeriodebeløp
            .map { it.tilIUtenlandskPeriodebeløp() }
            .filterIsInstance<UtfyltUtenlandskPeriodebeløp>()

    fun utledGrunnlagTidslinjePerPerson(
        erToggleForÅIkkeSplittePåValutakursendringerPå: Boolean,
    ): Map<AktørOgRolleBegrunnelseGrunnlag, GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag> {
        val søker = persongrunnlag.søker
        val ordinæreVilkårForSøkerForskjøvetTidslinje =
            hentOrdinæreVilkårForSøkerForskjøvetTidslinje(søker, personResultater)

        val erMinstEttBarnMedUtbetalingTidslinje =
            hentErMinstEttBarnMedUtbetalingTidslinje(personResultater, behandling.fagsak.type, persongrunnlag)

        val erUtbetalingSmåbarnstilleggTidslinje = this.andelerTilkjentYtelse.hentErUtbetalingSmåbarnstilleggTidslinje()

        val personresultaterOgRolleForVilkår =
            if (behandling.fagsak.type.erBarnSøker()) {
                personResultater.single().splittOppVilkårForBarnOgSøkerRolle()
            } else {
                personResultater.map {
                    Pair(persongrunnlag.personer.single { person -> it.aktør == person.aktør }.type, it)
                }
            }

        val bareSøkerOgUregistrertBarn = uregistrerteBarn.isNotEmpty() && personResultater.size == 1

        val grunnlagForPersonTidslinjer =
            personresultaterOgRolleForVilkår.associate { (vilkårRolle, personResultat) ->
                val aktør = personResultat.aktør
                val person = persongrunnlag.personer.single { person -> aktør == person.aktør }

                val (overlappendeGenerelleAvslag, vilkårResultaterUtenGenerelleAvslag) =
                    splittOppPåErOverlappendeGenerelleAvslag(
                        personResultat,
                    )

                val forskjøvedeVilkårResultaterForPersonsAndeler: Tidslinje<List<VilkårResultat>, Måned> =
                    vilkårResultaterUtenGenerelleAvslag.hentForskjøvedeVilkårResultaterForPersonsAndelerTidslinje(
                        person = person,
                        erMinstEttBarnMedUtbetalingTidslinje = erMinstEttBarnMedUtbetalingTidslinje,
                        ordinæreVilkårForSøkerTidslinje = ordinæreVilkårForSøkerForskjøvetTidslinje,
                        fagsakType = behandling.fagsak.type,
                        vilkårRolle = vilkårRolle,
                        bareSøkerOgUregistrertBarn = bareSøkerOgUregistrertBarn,
                    )

                AktørOgRolleBegrunnelseGrunnlag(aktør, vilkårRolle) to
                    GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag(
                        overlappendeGenerelleAvslagVedtaksperiodeGrunnlagForPerson =
                            overlappendeGenerelleAvslag.generelleAvslagTilGrunnlagForPersonTidslinje(
                                person,
                            ),
                        vedtaksperiodeGrunnlagForPerson =
                            if (erToggleForÅIkkeSplittePåValutakursendringerPå) {
                                forskjøvedeVilkårResultaterForPersonsAndeler.tilGrunnlagForPersonTidslinjeNy(
                                    person = person,
                                    erUtbetalingSmåbarnstilleggTidslinje = erUtbetalingSmåbarnstilleggTidslinje,
                                    vilkårRolle = vilkårRolle,
                                )
                            } else {
                                forskjøvedeVilkårResultaterForPersonsAndeler.tilGrunnlagForPersonTidslinjeGammel(
                                    person = person,
                                    erUtbetalingSmåbarnstilleggTidslinje = erUtbetalingSmåbarnstilleggTidslinje,
                                    vilkårRolle = vilkårRolle,
                                )
                            },
                    )
            }

        return grunnlagForPersonTidslinjer
    }

    private fun PersonResultat.splittOppVilkårForBarnOgSøkerRolle(): List<Pair<PersonType, PersonResultat>> {
        val personResultaterVilkårForSøker = hentDelAvPersonResultatForRolle(rolle = PersonType.SØKER)

        val personResultaterVilkårForBarn = hentDelAvPersonResultatForRolle(rolle = PersonType.BARN)

        return listOf(
            Pair(PersonType.SØKER, personResultaterVilkårForSøker),
            Pair(PersonType.BARN, personResultaterVilkårForBarn),
        )
    }

    private fun PersonResultat.hentDelAvPersonResultatForRolle(
        rolle: PersonType,
    ): PersonResultat {
        val personResultaterVilkårForSøker = this.kopierMedParent(this.vilkårsvurdering, true)
        personResultaterVilkårForSøker.setSortedVilkårResultater(
            personResultaterVilkårForSøker.vilkårResultater
                .filter { it.vilkårType.gjelder(rolle) }
                .toSet(),
        )
        return personResultaterVilkårForSøker
    }

    private fun Vilkår.gjelder(persontype: PersonType) =
        when (this) {
            Vilkår.UNDER_18_ÅR -> listOf(PersonType.BARN).contains(persontype)
            Vilkår.BOR_MED_SØKER -> listOf(PersonType.BARN).contains(persontype)
            Vilkår.GIFT_PARTNERSKAP -> listOf(PersonType.BARN).contains(persontype)
            Vilkår.BOSATT_I_RIKET -> listOf(PersonType.BARN, PersonType.SØKER).contains(persontype)
            Vilkår.LOVLIG_OPPHOLD -> listOf(PersonType.BARN, PersonType.SØKER).contains(persontype)
            Vilkår.UTVIDET_BARNETRYGD -> listOf(PersonType.SØKER).contains(persontype)
        }

    private fun List<VilkårResultat>.generelleAvslagTilGrunnlagForPersonTidslinje(
        person: Person,
    ): Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned> =
        this
            .map {
                listOf(månedPeriodeAv(null, null, it))
                    .tilTidslinje()
            }.kombinerUtenNull { it.toList() }
            .map { vilkårResultater ->
                vilkårResultater?.let {
                    VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget(
                        person = person,
                        vilkårResultaterForVedtaksperiode =
                            it.map { vilkårResultat ->
                                VilkårResultatForVedtaksperiode(
                                    vilkårResultat,
                                )
                            },
                    )
                }
            }

    private fun Tidslinje<List<VilkårResultat>, Måned>.tilGrunnlagForPersonTidslinjeNy(
        person: Person,
        erUtbetalingSmåbarnstilleggTidslinje: Tidslinje<Boolean, Måned>,
        vilkårRolle: PersonType,
    ): Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned> {
        val kompetanseTidslinje =
            utfylteKompetanser
                .filtrerPåAktør(person.aktør)
                .tilTidslinje()
                .mapIkkeNull { KompetanseForVedtaksperiode(it) }

        val utenlandskPeriodebeløpTidslinje =
            utfylteUtenlandskPeriodebeløp
                .filtrerPåAktør(person.aktør)
                .tilTidslinje()
                .mapIkkeNull { UtenlandskPeriodebeløpForVedtaksperiode(it) }

        val endredeUtbetalingerTidslinje =
            utfylteEndredeUtbetalinger
                .filtrerPåAktør(person.aktør)
                .tilTidslinje()
                .mapIkkeNull { it.tilEndretUtbetalingAndelForVedtaksperiode() }

        val overgangsstønadTidslinje =
            perioderOvergangsstønad
                .filtrerPåAktør(person.aktør)
                .tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje(erUtbetalingSmåbarnstilleggTidslinje)

        val andelTilkjentYtelseTidslinje =
            andelerTilkjentYtelse
                .filtrerPåAktør(person.aktør)
                .filtrerPåRolle(vilkårRolle)
                .tilAndelerForVedtaksPeriodeTidslinje()

        val grunnlagTidslinje =
            this
                .tilVilkårResultaterForVedtaksPeriodeTidslinje()
                .kombinerMed(andelTilkjentYtelseTidslinje) { vilkårResultater, andeler ->
                    lagGrunnlagForVilkårOgAndelNy(
                        vilkårResultater = vilkårResultater,
                        person = person,
                        andeler = andeler,
                    )
                }.kombinerMedNullable(kompetanseTidslinje) { grunnlagForPerson, kompetanse ->
                    lagGrunnlagMedKompetanse(grunnlagForPerson, kompetanse)
                }.kombinerMedNullable(utenlandskPeriodebeløpTidslinje) { grunnlagForPerson, utenlandskPeriodebeløp ->
                    lagGrunnlagMedUtenlandskPeriodebeløp(grunnlagForPerson, utenlandskPeriodebeløp)
                }.kombinerMedNullable(endredeUtbetalingerTidslinje) { grunnlagForPerson, endretUtbetalingAndel ->
                    lagGrunnlagMedEndretUtbetalingAndel(grunnlagForPerson, endretUtbetalingAndel)
                }.kombinerMedNullable(overgangsstønadTidslinje) { grunnlagForPerson, overgangsstønad ->
                    lagGrunnlagMedOvergangsstønad(grunnlagForPerson, overgangsstønad)
                }.filtrerIkkeNull()

        return grunnlagTidslinje
            .slåSammenLike()
            .perioder()
            .dropWhile { !it.erInnvilgetEllerEksplisittAvslag() }
            .tilTidslinje()
    }

    private fun Tidslinje<List<VilkårResultat>, Måned>.tilGrunnlagForPersonTidslinjeGammel(
        person: Person,
        erUtbetalingSmåbarnstilleggTidslinje: Tidslinje<Boolean, Måned>,
        vilkårRolle: PersonType,
    ): Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned> {
        val kompetanseTidslinje =
            utfylteKompetanser
                .filtrerPåAktør(person.aktør)
                .tilTidslinje()
                .mapIkkeNull { KompetanseForVedtaksperiode(it) }

        val utenlandskPeriodebeløpTidslinje =
            utfylteUtenlandskPeriodebeløp
                .filtrerPåAktør(person.aktør)
                .tilTidslinje()
                .mapIkkeNull { UtenlandskPeriodebeløpForVedtaksperiode(it) }

        val endredeUtbetalingerTidslinje =
            utfylteEndredeUtbetalinger
                .filtrerPåAktør(person.aktør)
                .tilTidslinje()
                .mapIkkeNull { it.tilEndretUtbetalingAndelForVedtaksperiode() }

        val valutakursTidslinje =
            utfylteValutakurs
                .filtrerPåAktør(person.aktør)
                .tilTidslinje()
                .mapIkkeNull { ValutakursForVedtaksperiode(it) }

        val overgangsstønadTidslinje =
            perioderOvergangsstønad
                .filtrerPåAktør(person.aktør)
                .tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje(erUtbetalingSmåbarnstilleggTidslinje)

        val andelTilkjentYtelseTidslinje =
            andelerTilkjentYtelse
                .filtrerPåAktør(person.aktør)
                .filtrerPåRolle(vilkårRolle)
                .tilAndelerForVedtaksbegrunnelseTidslinje()

        val grunnlagTidslinje =
            this
                .tilVilkårResultaterForVedtaksPeriodeTidslinje()
                .kombinerMed(andelTilkjentYtelseTidslinje) { vilkårResultater, andeler ->
                    lagGrunnlagForVilkårOgAndelGammel(
                        vilkårResultater = vilkårResultater,
                        person = person,
                        andeler = andeler,
                    )
                }.kombinerMedNullable(kompetanseTidslinje) { grunnlagForPerson, kompetanse ->
                    lagGrunnlagMedKompetanse(grunnlagForPerson, kompetanse)
                }.kombinerMedNullable(valutakursTidslinje) { grunnlagForPerson, valutakurs ->
                    lagGrunnlagMedValutakurs(grunnlagForPerson, valutakurs)
                }.kombinerMedNullable(utenlandskPeriodebeløpTidslinje) { grunnlagForPerson, utenlandskPeriodebeløp ->
                    lagGrunnlagMedUtenlandskPeriodebeløp(grunnlagForPerson, utenlandskPeriodebeløp)
                }.kombinerMedNullable(endredeUtbetalingerTidslinje) { grunnlagForPerson, endretUtbetalingAndel ->
                    lagGrunnlagMedEndretUtbetalingAndel(grunnlagForPerson, endretUtbetalingAndel)
                }.kombinerMedNullable(overgangsstønadTidslinje) { grunnlagForPerson, overgangsstønad ->
                    lagGrunnlagMedOvergangsstønad(grunnlagForPerson, overgangsstønad)
                }.filtrerIkkeNull()

        return grunnlagTidslinje
            .slåSammenLike()
            .perioder()
            .dropWhile { !it.erInnvilgetEllerEksplisittAvslag() }
            .tilTidslinje()
    }
}

private fun List<AndelTilkjentYtelse>.filtrerPåRolle(
    vilkårRolle: PersonType,
) = filter {
    if (vilkårRolle == PersonType.SØKER) {
        it.erSøkersAndel()
    } else {
        !it.erSøkersAndel()
    }
}

private fun splittOppPåErOverlappendeGenerelleAvslag(personResultat: PersonResultat): Pair<List<VilkårResultat>, List<VilkårResultat>> {
    val overlappendeGenerelleAvslag =
        personResultat.vilkårResultater
            .groupBy { it.vilkårType }
            .mapNotNull { (_, resultat) ->
                if (resultat.size > 1) {
                    resultat.filter { it.erGenereltAvslag() }
                } else {
                    null
                }
            }.flatten()

    val vilkårResultaterUtenGenerelleAvslag =
        personResultat.vilkårResultater.filterNot { overlappendeGenerelleAvslag.contains(it) }
    return Pair(overlappendeGenerelleAvslag, vilkårResultaterUtenGenerelleAvslag)
}

private fun List<VilkårResultat>.filtrerVilkårErOrdinærtFor(
    søker: Person,
): List<VilkårResultat>? {
    val ordinæreVilkårForSøker = Vilkår.hentOrdinæreVilkårFor(søker.type)

    return this
        .filter { ordinæreVilkårForSøker.contains(it.vilkårType) }
        .takeIf { it.isNotEmpty() }
}

fun hentOrdinæreVilkårForSøkerForskjøvetTidslinje(
    søker: Person,
    personResultater: Set<PersonResultat>,
): Tidslinje<List<VilkårResultat>, Måned> {
    val søkerPersonResultater = personResultater.single { it.aktør == søker.aktør }

    val (_, vilkårResultaterUtenOverlappendeGenerelleAvslag) =
        splittOppPåErOverlappendeGenerelleAvslag(
            søkerPersonResultater,
        )

    return vilkårResultaterUtenOverlappendeGenerelleAvslag
        .tilForskjøvedeVilkårTidslinjer(søker.fødselsdato)
        .kombiner { vilkårResultater -> vilkårResultater.toList().takeIf { it.isNotEmpty() } }
        .map { it?.toList()?.filtrerVilkårErOrdinærtFor(søker) }
}

fun VilkårResultat.erGenereltAvslag() =
    periodeFom == null && periodeTom == null && erEksplisittAvslagPåSøknad == true

private fun hentErMinstEttBarnMedUtbetalingTidslinje(
    personResultater: Set<PersonResultat>,
    fagsakType: FagsakType,
    persongrunnlag: PersonopplysningGrunnlag,
): Tidslinje<Boolean, Måned> {
    val søker = persongrunnlag.søker
    val søkerSinerOrdinæreVilkårErOppfyltTidslinje =
        personResultater
            .single { it.aktør == søker.aktør }
            .tilTidslinjeForSplittForPerson(
                person = søker,
                fagsakType = fagsakType,
            ).map { it != null }

    val barnSineVilkårErOppfyltTidslinjer =
        personResultater
            .mapNotNull { personResultat ->
                val person = persongrunnlag.personer.single { it.aktør == personResultat.aktør }

                if (person.type == PersonType.BARN) {
                    personResultat
                        .tilTidslinjeForSplittForPerson(
                            person = persongrunnlag.personer.single { it.aktør == personResultat.aktør },
                            fagsakType = fagsakType,
                        ).map { it != null }
                } else {
                    null
                }
            }

    return barnSineVilkårErOppfyltTidslinjer
        .map {
            it.kombinerMed(søkerSinerOrdinæreVilkårErOppfyltTidslinje) { barnetHarAlleOrdinæreVilkårOppfylt, søkerHarAlleOrdinæreVilkårOppfylt ->
                barnetHarAlleOrdinæreVilkårOppfylt == true && søkerHarAlleOrdinæreVilkårOppfylt == true
            }
        }.kombiner { erOrdinæreVilkårOppfyltForSøkerOgBarn ->
            erOrdinæreVilkårOppfyltForSøkerOgBarn.any { it }
        }
}

private fun List<VilkårResultat>.hentForskjøvedeVilkårResultaterForPersonsAndelerTidslinje(
    person: Person,
    erMinstEttBarnMedUtbetalingTidslinje: Tidslinje<Boolean, Måned>,
    ordinæreVilkårForSøkerTidslinje: Tidslinje<List<VilkårResultat>, Måned>,
    fagsakType: FagsakType,
    vilkårRolle: PersonType,
    bareSøkerOgUregistrertBarn: Boolean,
): Tidslinje<List<VilkårResultat>, Måned> {
    val forskjøvedeVilkårResultaterForPerson = this.tilForskjøvedeVilkårTidslinjer(person.fødselsdato).kombiner()

    return when (vilkårRolle) {
        PersonType.SØKER ->
            forskjøvedeVilkårResultaterForPerson
                .map { vilkårResultater ->
                    if (bareSøkerOgUregistrertBarn) {
                        vilkårResultater?.toList()?.takeIf { it.isNotEmpty() }
                    } else {
                        vilkårResultater?.filtrerErIkkeOrdinærtFor(vilkårRolle)?.takeIf { it.isNotEmpty() }
                    }
                }.kombinerMed(erMinstEttBarnMedUtbetalingTidslinje) { vilkårResultaterForSøker, erMinstEttBarnMedUtbetaling ->
                    vilkårResultaterForSøker?.takeIf { erMinstEttBarnMedUtbetaling == true || vilkårResultaterForSøker.any { it.erEksplisittAvslagPåSøknad == true } }
                }

        PersonType.BARN ->
            if (fagsakType == FagsakType.BARN_ENSLIG_MINDREÅRIG || fagsakType == FagsakType.INSTITUSJON) {
                forskjøvedeVilkårResultaterForPerson.map { it?.toList() }
            } else {
                forskjøvedeVilkårResultaterForPerson
                    .kombinerMed(ordinæreVilkårForSøkerTidslinje) { vilkårResultaterBarn, vilkårResultaterSøker ->
                        slåSammenHvisMulig(vilkårResultaterBarn, vilkårResultaterSøker)?.toList()
                    }
            }

        PersonType.ANNENPART ->
            if (this.isNotEmpty()) {
                throw Feil("Ikke implementert for annenpart")
            } else {
                emptyList<Periode<List<VilkårResultat>, Måned>>().tilTidslinje()
            }
    }
}

private fun slåSammenHvisMulig(
    venstre: Iterable<VilkårResultat>?,
    høyre: Iterable<VilkårResultat>?,
) = when {
    venstre == null -> høyre
    høyre == null -> venstre
    else -> høyre + venstre
}

private fun Iterable<VilkårResultat>.filtrerErIkkeOrdinærtFor(persontype: PersonType): List<VilkårResultat> {
    val ordinæreVilkårForPerson = Vilkår.hentOrdinæreVilkårFor(persontype)

    return this.filterNot { ordinæreVilkårForPerson.contains(it.vilkårType) }
}

private fun lagGrunnlagForVilkårOgAndelNy(
    vilkårResultater: List<VilkårResultatForVedtaksperiode>?,
    person: Person,
    andeler: Iterable<AndelForVedtaksperiode>?,
): VedtaksperiodeGrunnlagForPerson {
    val andelerListe = andeler?.toList()

    return if (!andelerListe.isNullOrEmpty()) {
        VedtaksperiodeGrunnlagForPersonVilkårInnvilgetNy(
            vilkårResultaterForVedtaksperiode =
                vilkårResultater
                    ?: error("vilkårResultatene burde alltid finnes om vi har andeler."),
            person = person,
            andeler = andeler,
        )
    } else {
        VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget(
            vilkårResultaterForVedtaksperiode = vilkårResultater ?: emptyList(),
            person = person,
        )
    }
}

private fun lagGrunnlagForVilkårOgAndelGammel(
    vilkårResultater: List<VilkårResultatForVedtaksperiode>?,
    person: Person,
    andeler: Iterable<AndelForVedtaksbegrunnelse>?,
): VedtaksperiodeGrunnlagForPerson {
    val andelerListe = andeler?.toList()

    return if (!andelerListe.isNullOrEmpty()) {
        VedtaksperiodeGrunnlagForPersonVilkårInnvilgetGammel(
            vilkårResultaterForVedtaksperiode =
                vilkårResultater
                    ?: error("vilkårResultatene burde alltid finnes om vi har andeler."),
            person = person,
            andeler = andeler,
        )
    } else {
        VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget(
            vilkårResultaterForVedtaksperiode = vilkårResultater ?: emptyList(),
            person = person,
        )
    }
}

private fun lagGrunnlagMedKompetanse(
    vedtaksperiodeGrunnlagForPerson: VedtaksperiodeGrunnlagForPerson?,
    kompetanse: KompetanseForVedtaksperiode?,
) = when (vedtaksperiodeGrunnlagForPerson) {
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilgetNy -> vedtaksperiodeGrunnlagForPerson.copy(kompetanse = kompetanse)
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilgetGammel -> vedtaksperiodeGrunnlagForPerson.copy(kompetanse = kompetanse)
    is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> vedtaksperiodeGrunnlagForPerson
    null -> null
}

private fun lagGrunnlagMedValutakurs(
    vedtaksperiodeGrunnlagForPerson: VedtaksperiodeGrunnlagForPerson?,
    valutakursForVedtaksperiode: ValutakursForVedtaksperiode?,
) = when (vedtaksperiodeGrunnlagForPerson) {
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilgetNy -> vedtaksperiodeGrunnlagForPerson.copy(valutakurs = valutakursForVedtaksperiode)
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilgetGammel -> vedtaksperiodeGrunnlagForPerson.copy(valutakurs = valutakursForVedtaksperiode)
    is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> vedtaksperiodeGrunnlagForPerson
    null -> null
}

private fun lagGrunnlagMedUtenlandskPeriodebeløp(
    vedtaksperiodeGrunnlagForPerson: VedtaksperiodeGrunnlagForPerson?,
    utenlandskPeriodebeløp: UtenlandskPeriodebeløpForVedtaksperiode?,
) = when (vedtaksperiodeGrunnlagForPerson) {
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilgetNy -> vedtaksperiodeGrunnlagForPerson.copy(utenlandskPeriodebeløp = utenlandskPeriodebeløp)
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilgetGammel -> vedtaksperiodeGrunnlagForPerson.copy(utenlandskPeriodebeløp = utenlandskPeriodebeløp)
    is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> vedtaksperiodeGrunnlagForPerson
    null -> null
}

private fun lagGrunnlagMedEndretUtbetalingAndel(
    vedtaksperiodeGrunnlagForPerson: VedtaksperiodeGrunnlagForPerson?,
    endretUtbetalingAndel: IEndretUtbetalingAndelForVedtaksperiode?,
) = when (vedtaksperiodeGrunnlagForPerson) {
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilgetNy -> vedtaksperiodeGrunnlagForPerson.copy(endretUtbetalingAndel = endretUtbetalingAndel)
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilgetGammel -> vedtaksperiodeGrunnlagForPerson.copy(endretUtbetalingAndel = endretUtbetalingAndel)
    is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> vedtaksperiodeGrunnlagForPerson
    null -> null
}

private fun lagGrunnlagMedOvergangsstønad(
    vedtaksperiodeGrunnlagForPerson: VedtaksperiodeGrunnlagForPerson?,
    overgangsstønad: OvergangsstønadForVedtaksperiode?,
) = when (vedtaksperiodeGrunnlagForPerson) {
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilgetNy -> vedtaksperiodeGrunnlagForPerson.copy(overgangsstønad = overgangsstønad)
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilgetGammel -> vedtaksperiodeGrunnlagForPerson.copy(overgangsstønad = overgangsstønad)
    is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> vedtaksperiodeGrunnlagForPerson
    null -> null
}

fun List<AndelTilkjentYtelse>.tilAndelerForVedtaksPeriodeTidslinje(): Tidslinje<Iterable<AndelForVedtaksperiode>, Måned> =
    this
        .tilAndelForVedtaksperiodeTidslinjerPerAktørOgType()
        .values
        .map { tidslinje -> tidslinje.mapIkkeNull { it }.slåSammenLike() }
        .kombiner()

// Vi trenger dette for å kunne begrunne nye perioder med småbarnstillegg som vi ikke hadde i forrige behandling
fun List<InternPeriodeOvergangsstønad>.tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje(
    erUtbetalingSmåbarnstilleggTidslinje: Tidslinje<Boolean, Måned>,
) = this
    .map { OvergangsstønadForVedtaksperiode(it) }
    .map { Periode(it.fom.tilMånedTidspunkt(), it.tom.tilMånedTidspunkt(), it) }
    .tilTidslinje()
    .kombinerMed(erUtbetalingSmåbarnstilleggTidslinje) { overgangsstønad, erUtbetalingSmåbarnstillegg ->
        overgangsstønad.takeIf { erUtbetalingSmåbarnstillegg == true }
    }

private fun Tidslinje<List<VilkårResultat>, Måned>.tilVilkårResultaterForVedtaksPeriodeTidslinje() =
    this.map { vilkårResultater -> vilkårResultater?.map { VilkårResultatForVedtaksperiode(it) } }

@JvmName("internPeriodeOvergangsstønaderFiltrerPåAktør")
fun List<InternPeriodeOvergangsstønad>.filtrerPåAktør(aktør: Aktør) =
    this.filter { it.personIdent == aktør.aktivFødselsnummer() }

@JvmName("andelerTilkjentYtelserFiltrerPåAktør")
fun List<AndelTilkjentYtelse>.filtrerPåAktør(aktør: Aktør) =
    this.filter { andelTilkjentYtelse -> andelTilkjentYtelse.aktør == aktør }

@JvmName("endredeUtbetalingerFiltrerPåAktør")
fun List<IUtfyltEndretUtbetalingAndel>.filtrerPåAktør(aktør: Aktør) =
    this.filter { endretUtbetaling -> endretUtbetaling.person.aktør == aktør }

@JvmName("utfyltKompetanseFiltrerPåAktør")
fun List<UtfyltKompetanse>.filtrerPåAktør(aktør: Aktør) =
    this.filter { it.barnAktører.contains(aktør) }

@JvmName("utfyltValutakursFiltrerPåAktør")
fun List<UtfyltValutakurs>.filtrerPåAktør(aktør: Aktør) =
    this.filter { it.barnAktører.contains(aktør) }

@JvmName("utfyltUtenlandskPeriodebeløpFiltrerPåAktør")
fun List<UtfyltUtenlandskPeriodebeløp>.filtrerPåAktør(aktør: Aktør) =
    this.filter { it.barnAktører.contains(aktør) }

private fun Periode<VedtaksperiodeGrunnlagForPerson, Måned>.erInnvilgetEllerEksplisittAvslag(): Boolean {
    val grunnlagForPerson = innhold ?: return false

    val erInnvilget = grunnlagForPerson is VedtaksperiodeGrunnlagForPersonVilkårInnvilget
    val erEksplisittAvslag =
        grunnlagForPerson.vilkårResultaterForVedtaksperiode.any { it.erEksplisittAvslagPåSøknad }

    return erInnvilget || erEksplisittAvslag
}

private fun List<AndelTilkjentYtelse>.hentErUtbetalingSmåbarnstilleggTidslinje(): Tidslinje<Boolean, Måned> = this.tilAndelerForVedtaksbegrunnelseTidslinje().hentErUtbetalingSmåbarnstilleggTidslinje()

fun Tidslinje<Iterable<AndelForVedtaksbegrunnelse>, Måned>.hentErUtbetalingSmåbarnstilleggTidslinje() =
    this.mapIkkeNull { andelerIPeriode ->
        andelerIPeriode.any {
            it.type == YtelseType.SMÅBARNSTILLEGG && it.kalkulertUtbetalingsbeløp > 0
        }
    }
