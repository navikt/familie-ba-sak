package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestBeregningDetalj
import no.nav.familie.ba.sak.behandling.restDomene.RestBeregningOversikt
import no.nav.familie.ba.sak.behandling.restDomene.RestPerson
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.SakType
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.time.LocalDate
import java.time.YearMonth

object TilkjentYtelseUtils {

    fun beregnTilkjentYtelse(behandlingResultat: BehandlingResultat,
                             sakType: SakType,
                             personopplysningGrunnlag: PersonopplysningGrunnlag): TilkjentYtelse {

        val identBarnMap = personopplysningGrunnlag.barna
                .associateBy { it.personIdent.ident }

        val søkerMap = personopplysningGrunnlag.søker
                .associateBy { it.personIdent.ident }

        val innvilgetPeriodeResultatSøker = behandlingResultat.periodeResultater(brukMåned = true).filter {
            søkerMap.containsKey(it.personIdent) && it.allePåkrevdeVilkårErOppfylt(
                    PersonType.SØKER,
                    sakType
            )
        }
        val innvilgedePeriodeResultatBarna = behandlingResultat.periodeResultater(brukMåned = true).filter {
            identBarnMap.containsKey(it.personIdent) && it.allePåkrevdeVilkårErOppfylt(
                    PersonType.BARN,
                    sakType
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
                                val stønadFom =
                                        maksimum(overlappendePerioderesultatSøker.periodeFom, periodeResultatBarn.periodeFom)
                                val stønadTom =
                                        minimum(overlappendePerioderesultatSøker.periodeTom, periodeResultatBarn.periodeTom)
                                val stønadTomKommerFra18ÅrsVilkår =
                                        stønadTom == periodeResultatBarn.vilkårResultater.find { it.vilkårType == Vilkår.UNDER_18_ÅR }?.periodeTom
                                val beløpsperioder = SatsService.hentGyldigSatsFor(
                                        satstype = SatsType.ORBA,
                                        stønadFraOgMed = settRiktigStønadFom(stønadFom),
                                        stønadTilOgMed = settRiktigStønadTom(stønadTomKommerFra18ÅrsVilkår,
                                                                             stønadTom)
                                )

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

    private fun settRiktigStønadFom(fraOgMed: LocalDate): YearMonth =
            YearMonth.from(fraOgMed.plusMonths(1))

    private fun settRiktigStønadTom(erBarnetrygdTil18ÅrsDag: Boolean, tilOgMed: LocalDate): YearMonth {
        return if (erBarnetrygdTil18ÅrsDag)
            YearMonth.from(tilOgMed.minusMonths(1))
        else
            YearMonth.from(tilOgMed)
    }

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
        return RestBeregningOversikt(
                periodeFom = segment.fom,
                periodeTom = segment.tom,
                ytelseTyper = andelerForSegment.map(AndelTilkjentYtelse::type),
                utbetaltPerMnd = segment.value,
                antallBarn = andelerForSegment.count { andel -> personopplysningGrunnlag.barna.any { barn -> barn.personIdent.ident == andel.personIdent } },
                sakstype = behandling.kategori,
                beregningDetaljer = andelerForSegment.map { andel ->
                    val personForAndel = personopplysningGrunnlag.personer.find { person -> andel.personIdent == person.personIdent.ident }
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
