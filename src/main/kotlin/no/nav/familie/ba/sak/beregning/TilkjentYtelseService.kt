package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelse
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class TilkjentYtelseService(private val satsService: SatsService) {

    fun beregnTilkjentYtelse(behandlingResultat: BehandlingResultat,
                             personopplysningGrunnlag: PersonopplysningGrunnlag): TilkjentYtelse {

        val identBarnMap = personopplysningGrunnlag.barna
                .associateBy { it.personIdent.ident }

        val søkerMap = personopplysningGrunnlag.søker
                .associateBy { it.personIdent.ident }

        val innvilgetPeriodeResultatSøker = behandlingResultat.periodeResultater.filter {
            søkerMap.containsKey(it.personIdent) && it.hentSamletResultat() == BehandlingResultatType.INNVILGET
        }
        val innvilgedePeriodeResultatBarna = behandlingResultat.periodeResultater.filter {
            identBarnMap.containsKey(it.personIdent) && it.hentSamletResultat() == BehandlingResultatType.INNVILGET
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
                                val person = identBarnMap[periodeResultatBarn.personIdent]!! // TODO: bør vi kaste error her eller?
                                val stønadFom =
                                        maksimum(overlappendePerioderesultatSøker.periodeFom, periodeResultatBarn.periodeFom)
                                val stønadTom =
                                        minimum(overlappendePerioderesultatSøker.periodeTom, periodeResultatBarn.periodeTom)

                                val beløpsperioder = satsService.hentGyldigSatsFor(
                                        satstype = SatsType.ORBA,
                                        stønadFraOgMed = settRiktigStønadFom(stønadFom),
                                        stønadTilOgMed = settRiktigStønadTom(periodeResultatBarn.periodeTomKommerFra18ÅrsVilkår,
                                                                             stønadTom)
                                )

                                beløpsperioder.map { beløpsperiode ->
                                    AndelTilkjentYtelse(
                                            behandlingId = behandlingResultat.behandling.id,
                                            tilkjentYtelse = tilkjentYtelse,
                                            personId = person.id,
                                            stønadFom = beløpsperiode.fraOgMed.atDay(1),
                                            stønadTom = beløpsperiode.tilOgMed.atEndOfMonth(),
                                            beløp = beløpsperiode.beløp,
                                            type = Ytelsetype.ORDINÆR_BARNETRYGD
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
            YearMonth.from(tilOgMed.plusMonths(1))
    }
}

private fun maksimum(periodeFomSoker: LocalDate?, periodeFomBarn: LocalDate?): LocalDate {
    if (periodeFomSoker == null && periodeFomBarn == null) {
        throw IllegalStateException("Både søker og barn kan ikke ha null i periodeFom dato")
    }

    return maxOf(periodeFomSoker ?: LocalDate.MIN, periodeFomBarn ?: LocalDate.MIN)
}

private fun minimum(periodeTomSoker: LocalDate?, periodeTomBarn: LocalDate?): LocalDate {
    if (periodeTomSoker == null && periodeTomBarn == null) {
        throw IllegalStateException("Både søker og barn kan ikke ha null i periodeTom dato")
    }

    return minOf(periodeTomBarn ?: LocalDate.MAX, periodeTomSoker ?: LocalDate.MAX)
}

