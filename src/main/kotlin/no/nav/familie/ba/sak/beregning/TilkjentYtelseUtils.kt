package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestBeregningDetalj
import no.nav.familie.ba.sak.behandling.restDomene.RestBeregningOversikt
import no.nav.familie.ba.sak.behandling.restDomene.RestPerson
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.beregning.SatsService.splittPeriodePå6Årsdag
import no.nav.familie.ba.sak.beregning.SatsService.BeløpPeriode
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.time.LocalDate
import java.time.YearMonth

object TilkjentYtelseUtils {

    fun beregnTilkjentYtelse(behandlingResultat: BehandlingResultat,
                             personopplysningGrunnlag: PersonopplysningGrunnlag): TilkjentYtelse {

        val identBarnMap = personopplysningGrunnlag.barna
                .associateBy { it.personIdent.ident }

        val søkerMap = personopplysningGrunnlag.søker
                .associateBy { it.personIdent.ident }

        val innvilgetPeriodeResultatSøker = behandlingResultat.periodeResultater(brukMåned = false).filter {
            søkerMap.containsKey(it.personIdent) && it.allePåkrevdeVilkårErOppfylt(
                    PersonType.SØKER
            )
        }
        val innvilgedePeriodeResultatBarna = behandlingResultat.periodeResultater(brukMåned = false).filter {
            identBarnMap.containsKey(it.personIdent) && it.allePåkrevdeVilkårErOppfylt(
                    PersonType.BARN
            )
        }

        val tilkjentYtelse = TilkjentYtelse(
                behandling = behandlingResultat.behandling,
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
                                        stønadTilOgMed = settRiktigStønadTom(skalAvsluttesMånedenFør = true,
                                                                             tilOgMed = periodeUnder6År.tom)
                                ) else emptyList()

                                val beløpsperioderEtterFylte6År = if (periodeOver6år != null) SatsService.hentGyldigSatsFor(
                                        satstype = SatsType.ORBA,
                                        stønadFraOgMed = settRiktigStønadFom(skalStarteMånedenFør = periodeUnder6År != null,
                                                                             fraOgMed = periodeOver6år.fom),
                                        stønadTilOgMed = settRiktigStønadTom(skalAvsluttesMånedenFør = oppfyltTomKommerFra18ÅrsVilkår,
                                                                             tilOgMed = periodeOver6år.tom)
                                ) else emptyList()

                                val beløpsperioder =
                                        listOf(beløpsperioderFørFylte6År, beløpsperioderEtterFylte6År).flatten()
                                                .fold(mutableListOf(), ::slåSammenEtterfølgendePerioderMedSammeBeløp)

                                beløpsperioder.map { beløpsperiode ->
                                    AndelTilkjentYtelse(
                                            behandlingId = behandlingResultat.behandling.id,
                                            tilkjentYtelse = tilkjentYtelse,
                                            personIdent = person.personIdent.ident,
                                            stønadFom = beløpsperiode.fraOgMed.atDay(1),
                                            stønadTom = beløpsperiode.tilOgMed.atEndOfMonth(),
                                            beløp = beløpsperiode.beløp,
                                            type = YtelseType.ORDINÆR_BARNETRYGD
                                    )
                                }
                            }
                }

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)

        return tilkjentYtelse
    }

    fun beregnNåværendeBeløp(beregningOversikt: List<RestBeregningOversikt>, vedtak: Vedtak): Int {
        return beregningOversikt.find {
            it.periodeFom !== null && it.periodeTom !== null
            && it.periodeFom <= vedtak.vedtaksdato && it.periodeTom > vedtak.vedtaksdato
        }?.utbetaltPerMnd
               ?: beregningOversikt.find { it.periodeTom != null && it.periodeTom > vedtak.vedtaksdato }?.utbetaltPerMnd
               ?: throw Feil("Finner ikke gjeldende beløp for virkningstidspunkt",
                             "Finner ikke gjeldende beløp for virkningstidspunkt")
    }


    private fun settRiktigStønadFom(skalStarteMånedenFør: Boolean = false, fraOgMed: LocalDate): YearMonth =
            if (skalStarteMånedenFør)
                YearMonth.from(fraOgMed.withDayOfMonth(1))
            else
                YearMonth.from(fraOgMed.plusMonths(1).withDayOfMonth(1))

    private fun settRiktigStønadTom(skalAvsluttesMånedenFør: Boolean, tilOgMed: LocalDate): YearMonth =
            if (skalAvsluttesMånedenFør)
                YearMonth.from(tilOgMed.minusMonths(1).sisteDagIMåned())
            else
                YearMonth.from(tilOgMed.sisteDagIMåned())


    fun hentBeregningOversikt(tilkjentYtelseForBehandling: TilkjentYtelse, personopplysningGrunnlag: PersonopplysningGrunnlag)
            : List<RestBeregningOversikt> {
        if (tilkjentYtelseForBehandling.andelerTilkjentYtelse.isEmpty()) return emptyList()

        val utbetalingsPerioder = beregnUtbetalingsperioderUtenKlassifisering(tilkjentYtelseForBehandling.andelerTilkjentYtelse)

        return utbetalingsPerioder.toSegments()
                .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                .map { segment ->
                    val andelerForSegment = tilkjentYtelseForBehandling.andelerTilkjentYtelse.filter {
                        segment.localDateInterval.overlaps(LocalDateInterval(it.stønadFom, it.stønadTom))
                    }
                    mapTilRestBeregningOversikt(segment,
                                                andelerForSegment,
                                                tilkjentYtelseForBehandling.behandling,
                                                personopplysningGrunnlag)
                }
    }

    private fun mapTilRestBeregningOversikt(segment: LocalDateSegment<Int>,
                                            andelerForSegment: List<AndelTilkjentYtelse>,
                                            behandling: Behandling,
                                            personopplysningGrunnlag: PersonopplysningGrunnlag): RestBeregningOversikt {
        val endret = false //TODO: Enten sammenligne med forrige beregningoversikt (fra forrige tilkjent ytelse) eller bruk andel og sett et endretflagg der
        return RestBeregningOversikt(
                periodeFom = segment.fom,
                periodeTom = segment.tom,
                ytelseTyper = andelerForSegment.map(AndelTilkjentYtelse::type),
                utbetaltPerMnd = segment.value,
                antallBarn = andelerForSegment.count { andel -> personopplysningGrunnlag.barna.any { barn -> barn.personIdent.ident == andel.personIdent } },
                sakstype = behandling.kategori,
                endring = endret,
                beregningDetaljer = andelerForSegment.map { andel ->
                    val personForAndel =
                            personopplysningGrunnlag.personer.find { person -> andel.personIdent == person.personIdent.ident }
                            ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel")
                    RestBeregningDetalj(
                            person = RestPerson(
                                    type = personForAndel.type,
                                    kjønn = personForAndel.kjønn,
                                    navn = personForAndel.navn,
                                    fødselsdato = personForAndel.fødselsdato,
                                    personIdent = personForAndel.personIdent.ident
                            ),
                            ytelseType = andel.type,
                            utbetaltPerMnd = andel.beløp
                    )
                }
        )
    }
}

private fun maksimum(periodeFomSoker: LocalDate?, periodeFomBarn: LocalDate?): LocalDate {
    if (periodeFomSoker == null && periodeFomBarn == null) {
        error("Både søker og barn kan ikke ha null i periodeFom-dato")
    }

    return maxOf(periodeFomSoker ?: LocalDate.MIN, periodeFomBarn ?: LocalDate.MIN)
}

private fun minimum(periodeTomSoker: LocalDate?, periodeTomBarn: LocalDate?): LocalDate {
    if (periodeTomSoker == null && periodeTomBarn == null) {
        error("Både søker og barn kan ikke ha null i periodeTom-dato")
    }

    return minOf(periodeTomBarn ?: LocalDate.MAX, periodeTomSoker ?: LocalDate.MAX)
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