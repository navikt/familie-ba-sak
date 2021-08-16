package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.maksimum
import no.nav.familie.ba.sak.common.minimum
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.BeløpPeriode
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.splittPeriodePå6Årsdag
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate
import java.time.YearMonth

object TilkjentYtelseUtils {

    fun beregnTilkjentYtelse(vilkårsvurdering: Vilkårsvurdering,
                             personopplysningGrunnlag: PersonopplysningGrunnlag,
                             featureToggleService: FeatureToggleService): TilkjentYtelse {
        val identBarnMap = personopplysningGrunnlag.barna
                .associateBy { it.personIdent.ident }

        val (innvilgetPeriodeResultatSøker, innvilgedePeriodeResultatBarna) = vilkårsvurdering.hentInnvilgedePerioder(
                personopplysningGrunnlag)

        val tilkjentYtelse = TilkjentYtelse(
                behandling = vilkårsvurdering.behandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now()
        )

        val andelerTilkjentYtelse = innvilgedePeriodeResultatBarna
                .flatMap { periodeResultatBarn ->
                    innvilgetPeriodeResultatSøker
                            .filter { it.overlapper(periodeResultatBarn) }
                            .flatMap { overlappendePerioderesultatSøker ->
                                val person = identBarnMap[periodeResultatBarn.personIdent]
                                             ?: error("Finner ikke barn på map over barna i behandlingen")
                                val oppfyltFom =
                                        maksimum(overlappendePerioderesultatSøker.periodeFom, periodeResultatBarn.periodeFom)

                                val påfølgendeBack2BackPeriodeSomOverlapperMedSøkerperiode =
                                        innvilgedePeriodeResultatBarna.singleOrNull { periodeResultat ->
                                            innvilgetPeriodeResultatSøker.any { periodeResultatSøker ->
                                                periodeResultatSøker.overlapper(periodeResultat)
                                            } &&
                                            periodeResultatBarn.periodeTom?.erDagenFør(periodeResultat.periodeFom) == true &&
                                            periodeResultatBarn.personIdent.equals(periodeResultat.personIdent)
                                        }

                                val foregåendeBack2BackPeriodeSomOverlapperMedSøkerperiode =
                                        innvilgedePeriodeResultatBarna.singleOrNull { periodeResultat ->
                                            innvilgetPeriodeResultatSøker.any { periodeResultatSøker ->
                                                periodeResultatSøker.overlapper(periodeResultat)
                                            } &&
                                            periodeResultat.periodeTom?.erDagenFør(periodeResultatBarn.periodeFom) == true &&
                                            periodeResultatBarn.personIdent.equals(periodeResultat.personIdent)
                                        }

                                val deltBostedEndresForPåfølgendeBack2BackPeriode =
                                        påfølgendeBack2BackPeriodeSomOverlapperMedSøkerperiode != null &&
                                        periodeResultatBarn.vilkårResultater.single {
                                            it.vilkårType == Vilkår.BOR_MED_SØKER
                                        }.erDeltBosted !=
                                        påfølgendeBack2BackPeriodeSomOverlapperMedSøkerperiode.vilkårResultater.single {
                                            it.vilkårType == Vilkår.BOR_MED_SØKER
                                        }.erDeltBosted

                                val deltBostedEndretFraForrigeBack2BackPeriode =
                                        foregåendeBack2BackPeriodeSomOverlapperMedSøkerperiode != null &&
                                        periodeResultatBarn.vilkårResultater.single {
                                            it.vilkårType == Vilkår.BOR_MED_SØKER
                                        }.erDeltBosted !=
                                        foregåendeBack2BackPeriodeSomOverlapperMedSøkerperiode.vilkårResultater.single {
                                            it.vilkårType == Vilkår.BOR_MED_SØKER
                                        }.erDeltBosted

                                val skalStarteSammeMåned =
                                        foregåendeBack2BackPeriodeSomOverlapperMedSøkerperiode != null && !deltBostedEndretFraForrigeBack2BackPeriode

                                val skalVidereføresEnMånedEkstra =
                                        påfølgendeBack2BackPeriodeSomOverlapperMedSøkerperiode != null && deltBostedEndresForPåfølgendeBack2BackPeriode

                                val minsteTom =
                                        minimum(overlappendePerioderesultatSøker.periodeTom, periodeResultatBarn.periodeTom)

                                val oppfyltTom = if (skalVidereføresEnMånedEkstra) minsteTom.plusMonths(1) else minsteTom

                                val oppfyltTomKommerFra18ÅrsVilkår =
                                        oppfyltTom == periodeResultatBarn.vilkårResultater.find {
                                            it.vilkårType == Vilkår.UNDER_18_ÅR
                                        }?.periodeTom


                                val (periodeUnder6År, periodeOver6år) = splittPeriodePå6Årsdag(person.hentSeksårsdag(),
                                                                                               oppfyltFom,
                                                                                               oppfyltTom)
                                val beløpsperioderFørFylte6År =
                                        if (featureToggleService.isEnabled(FeatureToggleConfig.BRUK_ER_DELT_BOSTED)) {

                                            if (periodeUnder6År != null) SatsService.hentGyldigSatsFor(
                                                    satstype = SatsType.TILLEGG_ORBA,
                                                    deltUtbetaling = periodeResultatBarn.erDeltBosted(),
                                                    stønadFraOgMed = settRiktigStønadFom(skalStarteSammeMåned = skalStarteSammeMåned,
                                                                                         fraOgMed = periodeUnder6År.fom),
                                                    stønadTilOgMed = settRiktigStønadTom(tilOgMed = periodeUnder6År.tom),
                                                    maxSatsGyldigFraOgMed = SatsService.tilleggEndringSeptember2021,
                                            ) else emptyList()
                                        } else {

                                            // Skal fjernes sammen med fjerning av toggle familie-ba-sak.behandling.delt_bosted
                                            if (periodeUnder6År != null) SatsService.hentGyldigSatsFor(
                                                    satstype = SatsType.TILLEGG_ORBA,
                                                    stønadFraOgMed = settRiktigStønadFom(skalStarteSammeMåned = skalStarteSammeMåned,
                                                                                         fraOgMed = periodeUnder6År.fom),
                                                    stønadTilOgMed = settRiktigStønadTom(tilOgMed = periodeUnder6År.tom),
                                                    maxSatsGyldigFraOgMed = SatsService.tilleggEndringSeptember2021,
                                            ) else emptyList()
                                        }

                                val beløpsperioderEtterFylte6År =
                                        if (featureToggleService.isEnabled(FeatureToggleConfig.BRUK_ER_DELT_BOSTED)) {
                                            if (periodeOver6år != null) SatsService.hentGyldigSatsFor(
                                                    satstype = SatsType.ORBA,
                                                    deltUtbetaling = periodeResultatBarn.erDeltBosted(),
                                                    stønadFraOgMed = settRiktigStønadFom(skalStarteSammeMåned =
                                                                                         (periodeUnder6År != null || skalStarteSammeMåned),
                                                                                         fraOgMed = periodeOver6år.fom),
                                                    stønadTilOgMed = settRiktigStønadTom(skalAvsluttesMånedenFør = oppfyltTomKommerFra18ÅrsVilkår,
                                                                                         tilOgMed = periodeOver6år.tom),
                                                    maxSatsGyldigFraOgMed = SatsService.tilleggEndringSeptember2021,
                                            ) else emptyList()
                                        } else {

                                            // Skal fjernes sammen med fjerning av toggle familie-ba-sak.behandling.delt_bosted
                                            if (periodeOver6år != null) SatsService.hentGyldigSatsFor(
                                                    satstype = SatsType.ORBA,
                                                    stønadFraOgMed = settRiktigStønadFom(skalStarteSammeMåned =
                                                                                         (periodeUnder6År != null || skalStarteSammeMåned),
                                                                                         fraOgMed = periodeOver6år.fom),
                                                    stønadTilOgMed = settRiktigStønadTom(skalAvsluttesMånedenFør = oppfyltTomKommerFra18ÅrsVilkår,
                                                                                         tilOgMed = periodeOver6år.tom),
                                                    maxSatsGyldigFraOgMed = SatsService.tilleggEndringSeptember2021,
                                            ) else emptyList()
                                        }

                                val beløpsperioder =
                                        listOf(beløpsperioderFørFylte6År, beløpsperioderEtterFylte6År).flatten()
                                                .sortedBy { it.fraOgMed }
                                                .fold(mutableListOf(), ::slåSammenEtterfølgendePerioderMedSammeBeløp)

                                beløpsperioder.map { beløpsperiode ->
                                    AndelTilkjentYtelse(
                                            behandlingId = vilkårsvurdering.behandling.id,
                                            tilkjentYtelse = tilkjentYtelse,
                                            personIdent = person.personIdent.ident,
                                            stønadFom = beløpsperiode.fraOgMed,
                                            stønadTom = beløpsperiode.tilOgMed,
                                            beløp = beløpsperiode.beløp,
                                            type = YtelseType.ORDINÆR_BARNETRYGD
                                    )
                                }
                            }
                }

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)

        return tilkjentYtelse
    }

    private fun settRiktigStønadFom(skalStarteSammeMåned: Boolean = false, fraOgMed: LocalDate): YearMonth =
            if (skalStarteSammeMåned)
                YearMonth.from(fraOgMed.withDayOfMonth(1))
            else
                YearMonth.from(fraOgMed.plusMonths(1).withDayOfMonth(1))

    private fun settRiktigStønadTom(skalAvsluttesMånedenFør: Boolean = false, tilOgMed: LocalDate): YearMonth =
            if (skalAvsluttesMånedenFør)
                YearMonth.from(tilOgMed.plusDays(1).minusMonths(1).sisteDagIMåned())
            else
                YearMonth.from(tilOgMed.sisteDagIMåned())
}

private fun slåSammenEtterfølgendePerioderMedSammeBeløp(sammenlagt: MutableList<BeløpPeriode>,
                                                        neste: BeløpPeriode): MutableList<BeløpPeriode> {
    if (sammenlagt.isNotEmpty() && sammenlagt.last().beløp == neste.beløp) {
        val forrigeOgNeste = BeløpPeriode(neste.beløp, sammenlagt.last().fraOgMed, neste.tilOgMed)
        sammenlagt.removeLast()
        sammenlagt.add(forrigeOgNeste)
    } else {
        sammenlagt.add(neste)
    }
    return sammenlagt
}