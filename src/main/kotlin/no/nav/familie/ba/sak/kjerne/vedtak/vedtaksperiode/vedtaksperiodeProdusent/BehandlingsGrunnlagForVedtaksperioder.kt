package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilAndelForVedtaksbegrunnelseTidslinjerPerAktørOgType
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
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.tilAndelerForVedtaksbegrunnelseTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvedeVilkårTidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilTidslinjeForSplittForPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull

typealias AktørId = String

data class GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag(
    val overlappendeGenerelleAvslagVedtaksperiodeGrunnlagForPerson: Tidslinje<VedtaksperiodeGrunnlagForPerson>,
    val vedtaksperiodeGrunnlagForPerson: Tidslinje<VedtaksperiodeGrunnlagForPerson>,
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
    val personerFremstiltKravFor: List<Aktør>,
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
        skalSplittePåValutakursendringer: Boolean = true,
        featureToggleService: FeatureToggleService,
    ): Map<AktørOgRolleBegrunnelseGrunnlag, GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag> {
        val søker = persongrunnlag.søker
        val ordinæreVilkårForSøkerForskjøvetTidslinje =
            hentOrdinæreVilkårForSøkerForskjøvetTidslinje(søker, personResultater)

        val erMinstEttBarnMedUtbetalingTidslinje =
            hentErMinstEttBarnMedUtbetalingTidslinje(personResultater, behandling.fagsak.type, persongrunnlag)

        val personresultaterOgRolleForVilkår =
            if (behandling.fagsak.type.erBarnSøker()) {
                personResultater.single().splittOppVilkårForBarnOgSøkerRolle()
            } else {
                personResultater.map { personResultat ->
                    Pair(persongrunnlag.personer.single { person -> personResultat.aktør == person.aktør }.type, personResultat)
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

                val forskjøvedeVilkårResultaterForPersonsAndeler: Tidslinje<List<VilkårResultat>> =
                    vilkårResultaterUtenGenerelleAvslag
                        .hentForskjøvedeVilkårResultaterForPersonsAndelerTidslinje(
                            person = person,
                            erMinstEttBarnMedUtbetalingTidslinje = erMinstEttBarnMedUtbetalingTidslinje,
                            ordinæreVilkårForSøkerTidslinje = ordinæreVilkårForSøkerForskjøvetTidslinje,
                            fagsakType = behandling.fagsak.type,
                            vilkårRolle = vilkårRolle,
                            bareSøkerOgUregistrertBarn = bareSøkerOgUregistrertBarn,
                        ).slåSammenSplitterPåUtdypendeVilkår()

                AktørOgRolleBegrunnelseGrunnlag(aktør, vilkårRolle) to
                    GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag(
                        overlappendeGenerelleAvslagVedtaksperiodeGrunnlagForPerson =
                            overlappendeGenerelleAvslag.generelleAvslagTilGrunnlagForPersonTidslinje(
                                person,
                            ),
                        vedtaksperiodeGrunnlagForPerson =
                            forskjøvedeVilkårResultaterForPersonsAndeler.tilGrunnlagForPersonTidslinje(
                                person = person,
                                vilkårRolle = vilkårRolle,
                                skalSplittePåValutakursendringer = skalSplittePåValutakursendringer,
                            ),
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
    ): Tidslinje<VedtaksperiodeGrunnlagForPerson> =
        this
            .map { Periode(it, null, null).tilTidslinje() }
            .kombinerUtenNull { it.toList() }
            .mapVerdi { vilkårResultater ->
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

    private fun Tidslinje<List<VilkårResultat>>.tilGrunnlagForPersonTidslinje(
        person: Person,
        vilkårRolle: PersonType,
        skalSplittePåValutakursendringer: Boolean,
    ): Tidslinje<VedtaksperiodeGrunnlagForPerson> {
        val småbarnstilleggTidslinje = andelerTilkjentYtelse.hentErUtbetalingSmåbarnstilleggTidslinje()
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
                .tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje(småbarnstilleggTidslinje)

        val andelTilkjentYtelseTidslinje =
            andelerTilkjentYtelse
                .filtrerPåAktør(person.aktør)
                .filtrerPåRolle(vilkårRolle)
                .tilAndelerTidslinje(skalSplittePåValutakursendringer = skalSplittePåValutakursendringer)

        val grunnlagTidslinje =
            this
                .tilVilkårResultaterForVedtaksPeriodeTidslinje()
                .kombinerMed(andelTilkjentYtelseTidslinje) { vilkårResultater, andeler ->
                    lagGrunnlagForVilkårOgAndel(
                        vilkårResultater = vilkårResultater,
                        person = person,
                        andeler = andeler,
                    )
                }.kombinerMed(kompetanseTidslinje) { grunnlagForPerson, kompetanse ->
                    lagGrunnlagMedKompetanse(grunnlagForPerson, kompetanse)
                }.kombinerMed(utenlandskPeriodebeløpTidslinje) { grunnlagForPerson, utenlandskPeriodebeløp ->
                    lagGrunnlagMedUtenlandskPeriodebeløp(grunnlagForPerson, utenlandskPeriodebeløp)
                }.kombinerMed(endredeUtbetalingerTidslinje) { grunnlagForPerson, endretUtbetalingAndel ->
                    lagGrunnlagMedEndretUtbetalingAndel(grunnlagForPerson, endretUtbetalingAndel)
                }.kombinerMed(overgangsstønadTidslinje) { grunnlagForPerson, overgangsstønad ->
                    lagGrunnlagMedOvergangsstønad(grunnlagForPerson, overgangsstønad)
                }.filtrerIkkeNull()

        return grunnlagTidslinje
            .slåSammenLikePerioder()
            .tilPerioderIkkeNull()
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
): Tidslinje<List<VilkårResultat>> {
    val søkerPersonResultater = personResultater.single { it.aktør == søker.aktør }

    val (_, vilkårResultaterUtenOverlappendeGenerelleAvslag) =
        splittOppPåErOverlappendeGenerelleAvslag(
            søkerPersonResultater,
        )

    return vilkårResultaterUtenOverlappendeGenerelleAvslag
        .tilForskjøvedeVilkårTidslinjer(søker.fødselsdato)
        .kombiner { vilkårResultater -> vilkårResultater.toList().takeIf { it.isNotEmpty() } }
        .mapVerdi { it?.toList()?.filtrerVilkårErOrdinærtFor(søker) }
}

fun VilkårResultat.erGenereltAvslag() = periodeFom == null && periodeTom == null && erEksplisittAvslagPåSøknad == true

private fun hentErMinstEttBarnMedUtbetalingTidslinje(
    personResultater: Set<PersonResultat>,
    fagsakType: FagsakType,
    persongrunnlag: PersonopplysningGrunnlag,
): Tidslinje<Boolean> {
    val søker = persongrunnlag.søker
    val søkerSineOrdinæreVilkårErOppfyltTidslinje =
        personResultater
            .single { it.aktør == søker.aktør }
            .tilTidslinjeForSplittForPerson(
                person = søker,
                fagsakType = fagsakType,
            ).mapVerdi { it != null }

    val barnSineVilkårErOppfyltTidslinjer =
        personResultater
            .mapNotNull { personResultat ->
                val person = persongrunnlag.personer.single { it.aktør == personResultat.aktør }

                if (person.type == PersonType.BARN) {
                    personResultat
                        .tilTidslinjeForSplittForPerson(
                            person = persongrunnlag.personer.single { it.aktør == personResultat.aktør },
                            fagsakType = fagsakType,
                        ).mapVerdi { it != null }
                } else {
                    null
                }
            }

    return barnSineVilkårErOppfyltTidslinjer
        .map {
            it.kombinerMed(søkerSineOrdinæreVilkårErOppfyltTidslinje) { barnetHarAlleOrdinæreVilkårOppfylt, søkerHarAlleOrdinæreVilkårOppfylt ->
                barnetHarAlleOrdinæreVilkårOppfylt == true && søkerHarAlleOrdinæreVilkårOppfylt == true
            }
        }.kombiner { erOrdinæreVilkårOppfyltForSøkerOgBarn ->
            erOrdinæreVilkårOppfyltForSøkerOgBarn.any { it }
        }
}

private fun List<VilkårResultat>.hentForskjøvedeVilkårResultaterForPersonsAndelerTidslinje(
    person: Person,
    erMinstEttBarnMedUtbetalingTidslinje: Tidslinje<Boolean>,
    ordinæreVilkårForSøkerTidslinje: Tidslinje<List<VilkårResultat>>,
    fagsakType: FagsakType,
    vilkårRolle: PersonType,
    bareSøkerOgUregistrertBarn: Boolean,
): Tidslinje<List<VilkårResultat>> {
    val forskjøvedeVilkårResultaterForPerson = this.tilForskjøvedeVilkårTidslinjer(person.fødselsdato).kombiner()

    return when (vilkårRolle) {
        PersonType.SØKER ->
            forskjøvedeVilkårResultaterForPerson
                .mapVerdi { vilkårResultater ->
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
                forskjøvedeVilkårResultaterForPerson.mapVerdi { it?.toList() }
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
                tomTidslinje()
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

private fun lagGrunnlagForVilkårOgAndel(
    vilkårResultater: List<VilkårResultatForVedtaksperiode>?,
    person: Person,
    andeler: Iterable<AndelForVedtaksobjekt>?,
): VedtaksperiodeGrunnlagForPerson {
    val andelerListe = andeler?.toList()

    return if (!andelerListe.isNullOrEmpty()) {
        VedtaksperiodeGrunnlagForPersonVilkårInnvilget(
            vilkårResultaterForVedtaksperiode =
                vilkårResultater
                    ?: throw Feil("vilkårResultatene burde alltid finnes om vi har andeler."),
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
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilget -> vedtaksperiodeGrunnlagForPerson.copy(kompetanse = kompetanse)
    is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> vedtaksperiodeGrunnlagForPerson
    null -> null
}

private fun lagGrunnlagMedUtenlandskPeriodebeløp(
    vedtaksperiodeGrunnlagForPerson: VedtaksperiodeGrunnlagForPerson?,
    utenlandskPeriodebeløp: UtenlandskPeriodebeløpForVedtaksperiode?,
) = when (vedtaksperiodeGrunnlagForPerson) {
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilget -> vedtaksperiodeGrunnlagForPerson.copy(utenlandskPeriodebeløp = utenlandskPeriodebeløp)
    is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> vedtaksperiodeGrunnlagForPerson
    null -> null
}

private fun lagGrunnlagMedEndretUtbetalingAndel(
    vedtaksperiodeGrunnlagForPerson: VedtaksperiodeGrunnlagForPerson?,
    endretUtbetalingAndel: IEndretUtbetalingAndelForVedtaksperiode?,
) = when (vedtaksperiodeGrunnlagForPerson) {
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilget -> vedtaksperiodeGrunnlagForPerson.copy(endretUtbetalingAndel = endretUtbetalingAndel)
    is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> vedtaksperiodeGrunnlagForPerson
    null -> null
}

private fun lagGrunnlagMedOvergangsstønad(
    vedtaksperiodeGrunnlagForPerson: VedtaksperiodeGrunnlagForPerson?,
    overgangsstønad: OvergangsstønadForVedtaksperiode?,
) = when (vedtaksperiodeGrunnlagForPerson) {
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilget -> vedtaksperiodeGrunnlagForPerson.copy(overgangsstønad = overgangsstønad)
    is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> vedtaksperiodeGrunnlagForPerson
    null -> null
}

fun List<AndelTilkjentYtelse>.tilAndelerTidslinje(skalSplittePåValutakursendringer: Boolean): Tidslinje<out Iterable<AndelForVedtaksobjekt>> =
    if (skalSplittePåValutakursendringer) {
        this
            .tilAndelForVedtaksbegrunnelseTidslinjerPerAktørOgType()
            .values
            .map { tidslinje -> tidslinje.mapIkkeNull { it }.slåSammenLikePerioder() }
            .kombiner()
    } else {
        this
            .tilAndelForVedtaksperiodeTidslinjerPerAktørOgType()
            .values
            .map { tidslinje -> tidslinje.mapIkkeNull { it }.slåSammenLikePerioder() }
            .kombiner()
    }

// Vi trenger dette for å kunne begrunne nye perioder med småbarnstillegg som vi ikke hadde i forrige behandling
fun List<InternPeriodeOvergangsstønad>.tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje(
    erUtbetalingSmåbarnstilleggTidslinje: Tidslinje<Boolean>,
) = this
    .map { OvergangsstønadForVedtaksperiode(it) }
    .map { Periode(it, it.fom.førsteDagIInneværendeMåned(), it.tom.sisteDagIMåned()) }
    .tilTidslinje()
    .kombinerMed(erUtbetalingSmåbarnstilleggTidslinje) { overgangsstønad, erUtbetalingSmåbarnstillegg ->
        overgangsstønad.takeIf { erUtbetalingSmåbarnstillegg == true }
    }

private fun Tidslinje<List<VilkårResultat>>.tilVilkårResultaterForVedtaksPeriodeTidslinje() = this.mapVerdi { vilkårResultater -> vilkårResultater?.map { VilkårResultatForVedtaksperiode(it) } }

@JvmName("internPeriodeOvergangsstønaderFiltrerPåAktør")
fun List<InternPeriodeOvergangsstønad>.filtrerPåAktør(aktør: Aktør) = this.filter { it.personIdent == aktør.aktivFødselsnummer() }

@JvmName("andelerTilkjentYtelserFiltrerPåAktør")
fun List<AndelTilkjentYtelse>.filtrerPåAktør(aktør: Aktør) = this.filter { andelTilkjentYtelse -> andelTilkjentYtelse.aktør == aktør }

@JvmName("endredeUtbetalingerFiltrerPåAktør")
fun List<IUtfyltEndretUtbetalingAndel>.filtrerPåAktør(aktør: Aktør) = this.filter { it.personer.any { person -> person.aktør == aktør } }

@JvmName("utfyltKompetanseFiltrerPåAktør")
fun List<UtfyltKompetanse>.filtrerPåAktør(aktør: Aktør) = this.filter { it.barnAktører.contains(aktør) }

@JvmName("utfyltValutakursFiltrerPåAktør")
fun List<UtfyltValutakurs>.filtrerPåAktør(aktør: Aktør) = this.filter { it.barnAktører.contains(aktør) }

@JvmName("utfyltUtenlandskPeriodebeløpFiltrerPåAktør")
fun List<UtfyltUtenlandskPeriodebeløp>.filtrerPåAktør(aktør: Aktør) = this.filter { it.barnAktører.contains(aktør) }

private fun Periode<VedtaksperiodeGrunnlagForPerson>.erInnvilgetEllerEksplisittAvslag(): Boolean {
    val erInnvilget = verdi is VedtaksperiodeGrunnlagForPersonVilkårInnvilget
    val erEksplisittAvslag = verdi.vilkårResultaterForVedtaksperiode.any { it.erEksplisittAvslagPåSøknad }

    return erInnvilget || erEksplisittAvslag
}

private fun List<AndelTilkjentYtelse>.hentErUtbetalingSmåbarnstilleggTidslinje(): Tidslinje<Boolean> = this.tilAndelerForVedtaksbegrunnelseTidslinje().hentErUtbetalingSmåbarnstilleggTidslinje()

fun Tidslinje<Iterable<AndelForVedtaksbegrunnelse>>.hentErUtbetalingSmåbarnstilleggTidslinje() =
    this.mapIkkeNull { andelerIPeriode ->
        andelerIPeriode.any {
            it.type == YtelseType.SMÅBARNSTILLEGG && it.kalkulertUtbetalingsbeløp > 0
        }
    }
