package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.lagVertikaleSegmenter
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerPerson
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertEndretAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertRestPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.erGyldigForKompetanseMedData
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.leftJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.MinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.tilMinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.tilMinimertePersoner
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.endretUtbetalingsperiodeBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.landkodeTilBarnetsBostedsland
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.triggesForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.time.LocalDate

@Deprecated("Skal utfases. Bruk hentPerioderMedUtbetaling")
fun hentPerioderMedUtbetalingGammel(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak
) = andelerTilkjentYtelse.lagVertikaleSegmenter()
    .map { (segmenter, _) ->
        VedtaksperiodeMedBegrunnelser(
            fom = segmenter.fom,
            tom = segmenter.tom,
            vedtak = vedtak,
            type = Vedtaksperiodetype.UTBETALING
        )
    }

fun hentPerioderMedUtbetaling(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak,
    forskjøvetVilkårResultatTidslinjeMap: Map<Aktør, Tidslinje<Iterable<VilkårResultat>, Måned>>
): List<VedtaksperiodeMedBegrunnelser> {
    val utdypendeVilkårsvurderingTidslinje =
        forskjøvetVilkårResultatTidslinjeMap
            .tilUtdypendeVilkårsvurderingTidslinjeMap().values
            .kombinerUtenNull { it.flatten().toSet().ifEmpty { null } }
            .filtrerIkkeNull()
            .slåSammenLike()

    return andelerTilkjentYtelse
        .tilTidslinjerPerPerson().values
        .kombinerUtenNull { it }
        .leftJoin(utdypendeVilkårsvurderingTidslinje) { andelerTilkjentYtelseIPeriode, utdypendeVilkårIPeriode ->
            Pair(andelerTilkjentYtelseIPeriode, utdypendeVilkårIPeriode)
        }
        .perioder()
        .map {
            VedtaksperiodeMedBegrunnelser(
                fom = it.fraOgMed.tilYearMonth().førsteDagIInneværendeMåned(),
                tom = it.tilOgMed.tilYearMonth().sisteDagIInneværendeMåned(),
                vedtak = vedtak,
                type = Vedtaksperiodetype.UTBETALING
            )
        }
}

private fun Map<Aktør, Tidslinje<Iterable<VilkårResultat>, Måned>>.tilUtdypendeVilkårsvurderingTidslinjeMap():
    Map<Aktør, Tidslinje<Set<UtdypendeVilkårsvurdering>, Måned>> = this
    .mapValues { (_, vilkårsvurderingTidslinje) ->
        vilkårsvurderingTidslinje.map { vilkårResultater ->
            vilkårResultater?.flatMap { it.utdypendeVilkårsvurderinger }?.toSet()
        }
    }

fun oppdaterUtbetalingsperioderMedReduksjonFraForrigeBehandling(
    utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser>,
    reduksjonsperioder: List<VedtaksperiodeMedBegrunnelser>
): List<VedtaksperiodeMedBegrunnelser> {
    if (reduksjonsperioder.isNotEmpty()) {
        val utbetalingsperioderTidslinje = VedtaksperiodeMedBegrunnelserTidslinje(utbetalingsperioder)
        val reduksjonsperioderTidslinje = ReduksjonsperioderFraForrigeBehandlingTidslinje(reduksjonsperioder)

        val kombinertTidslinje = utbetalingsperioderTidslinje.kombinerMed(
            reduksjonsperioderTidslinje
        ) { utbetalingsperiode, reduksjonsperiode ->
            when {
                reduksjonsperiode != null && utbetalingsperiode == null -> reduksjonsperiode
                reduksjonsperiode != null && utbetalingsperiode != null -> utbetalingsperiode.copy(type = reduksjonsperiode.type)
                else -> utbetalingsperiode
            }
        }
        return kombinertTidslinje.lagVedtaksperioderMedBegrunnelser()
    }
    return utbetalingsperioder
}

fun validerSatsendring(fom: LocalDate?, harBarnMedSeksårsdagPåFom: Boolean) {
    val satsendring = SatsService
        .finnSatsendring(fom ?: TIDENES_MORGEN)

    if (satsendring.isEmpty() && !harBarnMedSeksårsdagPåFom) {
        throw FunksjonellFeil(
            melding = "Begrunnelsen stemmer ikke med satsendring.",
            frontendFeilmelding = "Begrunnelsen stemmer ikke med satsendring. Vennligst velg en annen begrunnelse."
        )
    }
}

fun validerVedtaksperiodeMedBegrunnelser(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser) {
    if ((
        vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.OPPHØR ||
            vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.AVSLAG
        ) &&
        vedtaksperiodeMedBegrunnelser.harFriteksterUtenStandardbegrunnelser()
    ) {
        val fritekstUtenStandardbegrunnelserFeilmelding =
            "Fritekst kan kun brukes i kombinasjon med en eller flere begrunnelser. " +
                "Legg først til en ny begrunnelse eller fjern friteksten(e)."
        throw FunksjonellFeil(
            melding = fritekstUtenStandardbegrunnelserFeilmelding,
            frontendFeilmelding = fritekstUtenStandardbegrunnelserFeilmelding
        )
    }

    if (vedtaksperiodeMedBegrunnelser.vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET &&
        vedtaksperiodeMedBegrunnelser.harFriteksterOgStandardbegrunnelser()
    ) {
        throw FunksjonellFeil(
            "Det ble sendt med både fritekst og begrunnelse. " +
                "Vedtaket skal enten ha fritekst eller bregrunnelse, men ikke begge deler."
        )
    }
}

/**
 * Brukes for opphør som har egen logikk dersom det er første periode.
 */
fun erFørsteVedtaksperiodePåFagsak(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    periodeFom: LocalDate?
): Boolean = !andelerTilkjentYtelse.any {
    it.stønadFom.isBefore(
        periodeFom?.toYearMonth() ?: TIDENES_MORGEN.toYearMonth()
    )
}

fun identifiserReduksjonsperioderFraSistIverksatteBehandling(
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak,
    utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser>,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    opphørsperioder: List<VedtaksperiodeMedBegrunnelser>,
    aktørerIForrigePersonopplysningGrunnlag: List<Aktør>,
): List<VedtaksperiodeMedBegrunnelser> {
    val forrigeSegmenter = forrigeAndelerTilkjentYtelse.lagVertikaleSegmenter()

    // henter segmenter for personer som finnes i forrige behandling
    val nåværendeSegmenter = andelerTilkjentYtelse.filter {
        aktørerIForrigePersonopplysningGrunnlag.any { forrigeAktør -> forrigeAktør == it.aktør }
    }.lagVertikaleSegmenter()

    val segmenter = forrigeSegmenter.filterNot { (forrigeSegment, _) ->
        nåværendeSegmenter.any { (nyttSegment, _) ->
            forrigeSegment.fom == nyttSegment.fom &&
                forrigeSegment.tom == nyttSegment.tom &&
                forrigeSegment.value == nyttSegment.value
        }
    }
    val reduksjonsperioderFraInnvilgelsesTidspunkt =
        segmenter.filter { (forrigeSegment, _) ->
            nåværendeSegmenter.any { (nyttSegment, _) ->
                nyttSegment.overlapper(
                    forrigeSegment
                )
            }
        }.toList()
            .fold(emptyList<VedtaksperiodeMedBegrunnelser>()) { acc, (gammeltSegment, gammeltAndelerTyForSegment) ->
                val overlappendePerioder = nåværendeSegmenter.filter { (nåSegment, nåAndelTilkjentYtelserForSegment) ->
                    nåSegment.overlapper(gammeltSegment) && gammeltAndelerTyForSegment.any { gammelAndelTyForSegment ->
                        val fom = nåSegment.fom
                        nåAndelTilkjentYtelserForSegment.all { nåAndelTyForSegment ->
                            // Når en person mister utbetaling på et segment i behandling
                            !(nåAndelTyForSegment.aktør.aktørId == gammelAndelTyForSegment.aktør.aktørId && nåAndelTyForSegment.type == gammelAndelTyForSegment.type) &&
                                // Når den personen som mister utbetaling ikke har en utbetaling av samme type i forrige måned
                                utbetalingsperioder.none { utbetalingsperiode ->
                                    utbetalingsperiode.tom == fom.minusDays(1) &&
                                        utbetalingsperiode.hentUtbetalingsperiodeDetaljer(
                                            andelerTilkjentYtelse = andelerTilkjentYtelse,
                                            personopplysningGrunnlag = personopplysningGrunnlag,
                                        )
                                            .any {
                                                it.person.personIdent ==
                                                    gammelAndelTyForSegment.aktør.aktivFødselsnummer() && it.ytelseType == gammelAndelTyForSegment.type
                                            }
                                }
                        }
                    }
                }.keys

                acc + overlappendePerioder.map { overlappendePeriode ->
                    VedtaksperiodeMedBegrunnelser(
                        vedtak = vedtak,
                        fom = utledFom(gammeltSegment, overlappendePeriode),
                        tom = utledTom(gammeltSegment, overlappendePeriode),
                        type = Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
                    )
                }
            }
    // opphørsperioder kan ikke være inkludert i reduksjonsperioder
    return reduksjonsperioderFraInnvilgelsesTidspunkt.filterNot { reduksjonsperiode ->
        opphørsperioder.any { it.fom == reduksjonsperiode.fom || it.tom == reduksjonsperiode.tom }
    }
}

private fun utledFom(
    gammeltSegment: LocalDateSegment<Int>,
    overlappendePeriode: LocalDateSegment<Int>
) = if (gammeltSegment.fom > overlappendePeriode.fom) gammeltSegment.fom else overlappendePeriode.fom

private fun utledTom(
    gammeltSegment: LocalDateSegment<Int>,
    overlappendePeriode: LocalDateSegment<Int>
) = if (gammeltSegment.tom > overlappendePeriode.tom) overlappendePeriode.tom else gammeltSegment.tom

fun hentGyldigeBegrunnelserForPeriode(
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    persongrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering,
    aktørIderMedUtbetaling: List<String>,
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    kanBehandleEØS: Boolean,
    sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>,
    kompetanserIPeriode: List<Kompetanse>,
    kompetanserSomStopperRettFørPeriode: List<Kompetanse>
): List<IVedtakBegrunnelse> {
    val standardbegrunnelser = hentGyldigeStandardbegrunnelserForVedtaksperiode(
        utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
        sanityBegrunnelser = sanityBegrunnelser,
        persongrunnlag = persongrunnlag,
        vilkårsvurdering = vilkårsvurdering,
        aktørIderMedUtbetaling = aktørIderMedUtbetaling,
        endretUtbetalingAndeler = endretUtbetalingAndeler,
        andelerTilkjentYtelse = andelerTilkjentYtelse
    )
    val eøsBegrunnelser = if (kanBehandleEØS) hentGyldigeEØSBegrunnelserForPeriode(
        sanityEØSBegrunnelser = sanityEØSBegrunnelser,
        kompetanserIPeriode = kompetanserIPeriode,
        kompetanserSomStopperRettFørPeriode = kompetanserSomStopperRettFørPeriode
    ) else emptyList()

    return standardbegrunnelser + eøsBegrunnelser
}

fun hentGyldigeStandardbegrunnelserForVedtaksperiode(
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    persongrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering,
    aktørIderMedUtbetaling: List<String>,
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
) = hentGyldigeBegrunnelserForVedtaksperiodeMinimert(
    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
    sanityBegrunnelser = sanityBegrunnelser,
    minimertePersoner = persongrunnlag.tilMinimertePersoner(),
    minimertePersonresultater = vilkårsvurdering.personResultater
        .map { it.tilMinimertPersonResultat() },
    aktørIderMedUtbetaling = aktørIderMedUtbetaling,
    minimerteEndredeUtbetalingAndeler = endretUtbetalingAndeler
        .map { it.tilMinimertEndretUtbetalingAndel() },
    erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak(
        andelerTilkjentYtelse,
        utvidetVedtaksperiodeMedBegrunnelser.fom
    ),
    ytelserForSøkerForrigeMåned = hentYtelserForSøkerForrigeMåned(
        andelerTilkjentYtelse,
        utvidetVedtaksperiodeMedBegrunnelser
    ),
    ytelserForrigePerioder = andelerTilkjentYtelse.filter { ytelseErFraForrigePeriode(it, utvidetVedtaksperiodeMedBegrunnelser) }
)

fun hentGyldigeEØSBegrunnelserForPeriode(
    sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>,
    kompetanserIPeriode: List<Kompetanse>,
    kompetanserSomStopperRettFørPeriode: List<Kompetanse>
) = EØSStandardbegrunnelse.values()
    .mapNotNull { it.tilEØSBegrunnelseMedTriggere(sanityEØSBegrunnelser) }
    .filter { begrunnelse ->
        when (begrunnelse.eøsBegrunnelse.vedtakBegrunnelseType) {
            VedtakBegrunnelseType.EØS_INNVILGET -> kompetanserIPeriode.any { kompetanse ->
                kompetanse.validerFelterErSatt()
                begrunnelse.erGyldigForKompetanseMedData(
                    annenForeldersAktivitetFraKompetanse = kompetanse.annenForeldersAktivitet!!,
                    barnetsBostedslandFraKompetanse = landkodeTilBarnetsBostedsland(kompetanse.barnetsBostedsland!!),
                    resultatFraKompetanse = kompetanse.resultat!!
                )
            }
            VedtakBegrunnelseType.EØS_OPPHØR -> kompetanserSomStopperRettFørPeriode.any { kompetanse ->
                kompetanse.validerFelterErSatt()
                begrunnelse.erGyldigForKompetanseMedData(
                    annenForeldersAktivitetFraKompetanse = kompetanse.annenForeldersAktivitet!!,
                    barnetsBostedslandFraKompetanse = landkodeTilBarnetsBostedsland(kompetanse.barnetsBostedsland!!),
                    resultatFraKompetanse = kompetanse.resultat!!
                )
            }
            else -> false
        }
    }.map { it.eøsBegrunnelse }

fun hentGyldigeBegrunnelserForVedtaksperiodeMinimert(
    minimertVedtaksperiode: MinimertVedtaksperiode,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    minimertePersoner: List<MinimertPerson>,
    minimertePersonresultater: List<MinimertRestPersonResultat>,
    aktørIderMedUtbetaling: List<String>,
    minimerteEndredeUtbetalingAndeler: List<MinimertEndretAndel>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForSøkerForrigeMåned: List<YtelseType>,
    ytelserForrigePerioder: List<AndelTilkjentYtelse>
): List<Standardbegrunnelse> {
    val tillateBegrunnelserForVedtakstype = Standardbegrunnelse.values()
        .filter {
            minimertVedtaksperiode
                .type
                .tillatteBegrunnelsestyper
                .contains(it.vedtakBegrunnelseType)
        }.filter {
            if (it.vedtakBegrunnelseType == VedtakBegrunnelseType.ENDRET_UTBETALING) {
                endretUtbetalingsperiodeBegrunnelser.contains(it)
            } else true
        }

    return when (minimertVedtaksperiode.type) {
        Vedtaksperiodetype.FORTSATT_INNVILGET,
        Vedtaksperiodetype.AVSLAG -> tillateBegrunnelserForVedtakstype
        Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> velgRedusertBegrunnelser(
            tillateBegrunnelserForVedtakstype,
            sanityBegrunnelser,
            minimertVedtaksperiode,
            minimertePersonresultater,
            minimertePersoner,
            aktørIderMedUtbetaling,
            minimerteEndredeUtbetalingAndeler,
            erFørsteVedtaksperiodePåFagsak,
            ytelserForSøkerForrigeMåned,
            ytelserForrigePerioder
        )
        else -> {
            velgUtbetalingsbegrunnelser(
                tillateBegrunnelserForVedtakstype,
                sanityBegrunnelser,
                minimertVedtaksperiode,
                minimertePersonresultater,
                minimertePersoner,
                aktørIderMedUtbetaling,
                minimerteEndredeUtbetalingAndeler,
                erFørsteVedtaksperiodePåFagsak,
                ytelserForSøkerForrigeMåned,
                ytelserForrigePerioder
            )
        }
    }
}

private fun velgRedusertBegrunnelser(
    tillateBegrunnelserForVedtakstype: List<Standardbegrunnelse>,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    minimertVedtaksperiode: MinimertVedtaksperiode,
    minimertePersonresultater: List<MinimertRestPersonResultat>,
    minimertePersoner: List<MinimertPerson>,
    aktørIderMedUtbetaling: List<String>,
    minimerteEndredeUtbetalingAndeler: List<MinimertEndretAndel>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForSøkerForrigeMåned: List<YtelseType>,
    ytelserForrigePeriode: List<AndelTilkjentYtelse>
): List<Standardbegrunnelse> {
    val redusertBegrunnelser = tillateBegrunnelserForVedtakstype.filter {
        it.tilSanityBegrunnelse(sanityBegrunnelser)?.tilTriggesAv()?.gjelderFraInnvilgelsestidspunkt ?: false
    }
    if (minimertVedtaksperiode.utbetalingsperioder.any { it.utbetaltPerMnd > 0 }) {
        val utbetalingsbegrunnelser = velgUtbetalingsbegrunnelser(
            Standardbegrunnelse.values().toList(),
            sanityBegrunnelser,
            minimertVedtaksperiode,
            minimertePersonresultater,
            minimertePersoner,
            aktørIderMedUtbetaling,
            minimerteEndredeUtbetalingAndeler,
            erFørsteVedtaksperiodePåFagsak,
            ytelserForSøkerForrigeMåned,
            ytelserForrigePeriode
        )
        return redusertBegrunnelser + utbetalingsbegrunnelser
    }
    return redusertBegrunnelser
}

private fun velgUtbetalingsbegrunnelser(
    tillateBegrunnelserForVedtakstype: List<Standardbegrunnelse>,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    minimertVedtaksperiode: MinimertVedtaksperiode,
    minimertePersonresultater: List<MinimertRestPersonResultat>,
    minimertePersoner: List<MinimertPerson>,
    aktørIderMedUtbetaling: List<String>,
    minimerteEndredeUtbetalingAndeler: List<MinimertEndretAndel>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForSøkerForrigeMåned: List<YtelseType>,
    ytelserForrigePeriode: List<AndelTilkjentYtelse>
): List<Standardbegrunnelse> {
    val standardbegrunnelser: MutableSet<Standardbegrunnelse> =
        tillateBegrunnelserForVedtakstype
            .filter { it.vedtakBegrunnelseType != VedtakBegrunnelseType.FORTSATT_INNVILGET }
            .filter { it.tilSanityBegrunnelse(sanityBegrunnelser)?.tilTriggesAv()?.valgbar ?: false }
            .fold(mutableSetOf()) { acc, standardBegrunnelse ->
                if (standardBegrunnelse.triggesForPeriode(
                        minimertVedtaksperiode = minimertVedtaksperiode,
                        minimertePersonResultater = minimertePersonresultater,
                        minimertePersoner = minimertePersoner,
                        aktørIderMedUtbetaling = aktørIderMedUtbetaling,
                        minimerteEndredeUtbetalingAndeler = minimerteEndredeUtbetalingAndeler,
                        sanityBegrunnelser = sanityBegrunnelser,
                        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
                        ytelserForSøkerForrigeMåned = ytelserForSøkerForrigeMåned,
                        ytelserForrigePeriode = ytelserForrigePeriode
                    )
                ) {
                    acc.add(standardBegrunnelse)
                }

                acc
            }

    val fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget =
        minimertVedtaksperiode.type == Vedtaksperiodetype.UTBETALING &&
            standardbegrunnelser.isEmpty()

    return if (fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget) {
        tillateBegrunnelserForVedtakstype
            .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET }
    } else {
        standardbegrunnelser.toList()
    }
}

fun hentYtelserForSøkerForrigeMåned(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser
) = andelerTilkjentYtelse.filter {
    it.type.erKnyttetTilSøker() &&
        ytelseErFraForrigePeriode(it, utvidetVedtaksperiodeMedBegrunnelser)
}.map { it.type }

fun ytelseErFraForrigePeriode(ytelse: AndelTilkjentYtelse, utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser) = ytelse.stønadTom.sisteDagIInneværendeMåned().erDagenFør(utvidetVedtaksperiodeMedBegrunnelser.fom)
