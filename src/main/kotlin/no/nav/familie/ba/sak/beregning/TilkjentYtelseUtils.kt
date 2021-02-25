package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.Vilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.beregning.SatsService.BeløpPeriode
import no.nav.familie.ba.sak.beregning.SatsService.splittPeriodePå6Årsdag
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.*
import java.time.LocalDate
import java.time.YearMonth

object TilkjentYtelseUtils {

    fun beregnTilkjentYtelse(vilkårsvurdering: Vilkårsvurdering,
                             personopplysningGrunnlag: PersonopplysningGrunnlag): TilkjentYtelse {

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
                                val oppfyltTom =
                                        minimum(overlappendePerioderesultatSøker.periodeTom, periodeResultatBarn.periodeTom)
                                val oppfyltTomKommerFra18ÅrsVilkår =
                                        oppfyltTom == periodeResultatBarn.vilkårResultater.find { it.vilkårType == Vilkår.UNDER_18_ÅR }?.periodeTom


                                val (periodeUnder6År, periodeOver6år) = splittPeriodePå6Årsdag(person.hentSeksårsdag(),
                                                                                               oppfyltFom,
                                                                                               oppfyltTom)

                                val beløpsperioderFørFylte6År = if (periodeUnder6År != null) SatsService.hentGyldigSatsFor(
                                        satstype = SatsType.TILLEGG_ORBA,
                                        stønadFraOgMed = settRiktigStønadFom(fraOgMed = periodeUnder6År.fom),
                                        stønadTilOgMed = settRiktigStønadTom(tilOgMed = periodeUnder6År.tom)
                                ) else emptyList()

                                val beløpsperioderEtterFylte6År = if (periodeOver6år != null) SatsService.hentGyldigSatsFor(
                                        satstype = SatsType.ORBA,
                                        stønadFraOgMed = settRiktigStønadFom(skalStarteSammeMåned = periodeUnder6År != null,
                                                                             fraOgMed = periodeOver6år.fom),
                                        stønadTilOgMed = settRiktigStønadTom(skalAvsluttesMånedenFør = oppfyltTomKommerFra18ÅrsVilkår,
                                                                             tilOgMed = periodeOver6år.tom)
                                ) else emptyList()

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

    fun beregnNåværendeBeløp(utbetalingsperiode: List<Utbetalingsperiode>, vedtak: Vedtak): Int {
        return utbetalingsperiode.find {
            it.periodeFom <= vedtak.vedtaksdato?.toLocalDate() && it.periodeTom > vedtak.vedtaksdato?.toLocalDate()
        }?.utbetaltPerMnd
               ?: utbetalingsperiode.find { it.periodeTom > vedtak.vedtaksdato?.toLocalDate() }?.utbetaltPerMnd
               ?: throw Feil("Finner ikke gjeldende beløp for virkningstidspunkt",
                             "Finner ikke gjeldende beløp for virkningstidspunkt")
    }


    private fun settRiktigStønadFom(skalStarteSammeMåned: Boolean = false, fraOgMed: LocalDate): YearMonth =
            if (skalStarteSammeMåned)
                YearMonth.from(fraOgMed.withDayOfMonth(1))
            else
                YearMonth.from(fraOgMed.plusMonths(1).withDayOfMonth(1))

    private fun settRiktigStønadTom(skalAvsluttesMånedenFør: Boolean = false, tilOgMed: LocalDate): YearMonth =
            if (skalAvsluttesMånedenFør)
                YearMonth.from(tilOgMed.minusMonths(1).sisteDagIMåned())
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