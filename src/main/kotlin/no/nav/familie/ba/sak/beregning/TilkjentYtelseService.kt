package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vilkår.SakType
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import java.time.LocalDate
import java.time.YearMonth

object TilkjentYtelseService {

    fun beregnTilkjentYtelse(behandlingResultat: BehandlingResultat,
                             personopplysningGrunnlag: PersonopplysningGrunnlag): TilkjentYtelse {

        val identBarnMap = personopplysningGrunnlag.barna
                .associateBy { it.personIdent.ident }

        val søkerMap = personopplysningGrunnlag.søker
                .associateBy { it.personIdent.ident }

        val innvilgetPeriodeResultatSøker = behandlingResultat.periodeResultater.filter {
            søkerMap.containsKey(it.personIdent) && it.allePåkrevdeVilkårErOppfylt(
                    PersonType.SØKER,
                    SakType.valueOfType(behandlingResultat.behandling.kategori)
            )
        }
        val innvilgedePeriodeResultatBarna = behandlingResultat.periodeResultater.filter {
            identBarnMap.containsKey(it.personIdent) && it.allePåkrevdeVilkårErOppfylt(
                    PersonType.BARN,
                    SakType.valueOfType(behandlingResultat.behandling.kategori)
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
                                val stønadTomKommerFra18ÅrsVilkår = stønadTom == periodeResultatBarn.vilkårResultater.find { it.vilkårType == Vilkår.UNDER_18_ÅR }?.periodeTom
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
                                            personId = person.id,
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
}

private fun maksimum(periodeFomSoker: LocalDate?, periodeFomBarn: LocalDate?): LocalDate {
    if (periodeFomSoker == null && periodeFomBarn == null) {
        throw error("Både søker og barn kan ikke ha null i periodeFom-dato")
    }

    return maxOf(periodeFomSoker ?: LocalDate.MIN, periodeFomBarn ?: LocalDate.MIN)
}

private fun minimum(periodeTomSoker: LocalDate?, periodeTomBarn: LocalDate?): LocalDate {
    if (periodeTomSoker == null && periodeTomBarn == null) {
        throw error("Både søker og barn kan ikke ha null i periodeTom-dato")
    }

    return minOf(periodeTomBarn ?: LocalDate.MAX, periodeTomSoker ?: LocalDate.MAX)
}
