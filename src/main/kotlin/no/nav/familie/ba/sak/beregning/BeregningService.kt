package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelse
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.beregning.domene.PeriodeResultat
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

@Service
class BeregningService(
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
        private val fagsakService: FagsakService,
        private val tilkjentYtelseRepository: TilkjentYtelseRepository,
        private val behandlingResultatRepository: BehandlingResultatRepository,
        private val satsService: SatsService
) {
    fun hentAndelerTilkjentYtelseForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> {
        return andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
    }

    fun hentTilkjentYtelseForBehandling(behandlingId: Long): TilkjentYtelse {
        return tilkjentYtelseRepository.findByBehandling(behandlingId)
    }

    @Transactional
    fun oppdaterBehandlingMedBeregning(behandling: Behandling,
                                       personopplysningGrunnlag: PersonopplysningGrunnlag): Ressurs<RestFagsak> {

        andelTilkjentYtelseRepository.slettAlleAndelerTilkjentYtelseForBehandling(behandling.id)
        tilkjentYtelseRepository.slettTilkjentYtelseFor(behandling)
        val behandlingResultat = behandlingResultatRepository.findByBehandlingAndAktiv(behandling.id)
                ?: throw IllegalStateException("Kunne ikke hente behandlingsresultat for behandling med id ${behandling.id}")

        val tilkjentYtelse = TilkjentYtelse(
                behandling = behandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now()
        )

        val andelerTilkjentYtelse = mapBehandlingResultatTilAndelerTilkjentYtelse(
                behandling.id,
                behandlingResultat,
                tilkjentYtelse,
                personopplysningGrunnlag
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)
        tilkjentYtelseRepository.save(tilkjentYtelse)

        return fagsakService.hentRestFagsak(behandling.fagsak.id)
    }

    fun oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling: Behandling,
                                                    utbetalingsoppdrag: Utbetalingsoppdrag): TilkjentYtelse {

        val nyTilkjentYtelse = populerTilkjentYtelse(behandling, utbetalingsoppdrag)
        return tilkjentYtelseRepository.save(nyTilkjentYtelse)
    }

    private fun populerTilkjentYtelse(behandling: Behandling,
                                      utbetalingsoppdrag: Utbetalingsoppdrag): TilkjentYtelse {
        val erRentOpphør = utbetalingsoppdrag.utbetalingsperiode.size == 1 && utbetalingsoppdrag.utbetalingsperiode[0].opphør != null
        var opphørsdato: LocalDate? = null
        if (utbetalingsoppdrag.utbetalingsperiode[0].opphør != null) {
            opphørsdato = utbetalingsoppdrag.utbetalingsperiode[0].opphør!!.opphørDatoFom
        }

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandling.id)

        return tilkjentYtelse.apply {
            this.utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag)
            this.stønadTom = utbetalingsoppdrag.utbetalingsperiode.maxBy { it.vedtakdatoTom }!!.vedtakdatoTom
            this.stønadFom = if (erRentOpphør) null else utbetalingsoppdrag.utbetalingsperiode
                    .filter { !it.erEndringPåEksisterendePeriode }
                    .minBy { it.vedtakdatoFom }!!.vedtakdatoFom
            this.endretDato = LocalDate.now()
            this.opphørFom = opphørsdato
        }
    }

    private fun mapBehandlingResultatTilAndelerTilkjentYtelse(behandlingId: Long,
                                                              behandlingResultat: BehandlingResultat,
                                                              tilkjentYtelse: TilkjentYtelse,
                                                              personopplysningGrunnlag: PersonopplysningGrunnlag)
            : List<AndelTilkjentYtelse> {

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

        return innvilgedePeriodeResultatBarna.flatMap { periodeResultatBarn ->
            innvilgetPeriodeResultatSøker.filter { overlapper(it, periodeResultatBarn) }.flatMap { fellesPerioderesultat ->
                val person = identBarnMap[periodeResultatBarn.personIdent]!!
                val stønadFom = maks(fellesPerioderesultat.periodeFom, periodeResultatBarn.periodeFom)
                val stønadTom = minimum(fellesPerioderesultat.periodeTom, periodeResultatBarn.periodeTom)

                // TODO: Denne sjekken kan gjøres mer robust når VilkårResultatene får perioder knyttet til seg. Da kan vi sjekke om det var 18-års vilkåret som var utslagsgivende for Tom-datoen
                val erOrdinærBarnetrygdTil18ÅrsDag = YearMonth.from(person.fødselsdato.plusYears(18)) == YearMonth.from(stønadTom)
                val beløpsperioder = satsService.hentGyldigSatsFor(
                        satstype = SatsType.ORBA,
                        stønadFraOgMed = YearMonth.from(stønadFom),
                        stønadTilOgMed = YearMonth.from(stønadTom)
                )

                beløpsperioder.map { beløpsperiode ->
                    AndelTilkjentYtelse(
                            behandlingId = behandlingId,
                            tilkjentYtelse = tilkjentYtelse,
                            personId = person.id,
                            stønadFom = settRiktigStønadFom(beløpsperiode),
                            stønadTom = settRiktigStønadTom(erOrdinærBarnetrygdTil18ÅrsDag, beløpsperiode),
                            beløp = beløpsperiode.beløp,
                            type = Ytelsetype.ORDINÆR_BARNETRYGD
                    )
                }
            }
        }
    }

    private fun settRiktigStønadFom(beløpsperiode: SatsService.BeløpPeriode) =
            beløpsperiode.fraOgMed.plusMonths(1).atDay(1)

    private fun settRiktigStønadTom(erOrdinaerBarnetrygdTil18ÅrsDag: Boolean, satsPeriode: SatsService.BeløpPeriode): LocalDate {
        return if (erOrdinaerBarnetrygdTil18ÅrsDag)
            satsPeriode.tilOgMed.minusMonths(1).atEndOfMonth()
        else
            satsPeriode.tilOgMed.plusMonths(1).atEndOfMonth()
    }
}

private fun maks(periodeFomSoker: LocalDate?, periodeFomBarn: LocalDate?): LocalDate {
    if (periodeFomSoker == null && periodeFomBarn == null) {
        throw IllegalStateException("Både søker og barn kan ikke ha null i periodeFom dato")
    }
    return when {
        periodeFomSoker == null -> periodeFomBarn!!
        periodeFomBarn == null -> periodeFomSoker
        periodeFomBarn > periodeFomSoker -> periodeFomBarn
        else -> periodeFomSoker
    }
}

private fun minimum(periodeTomSoker: LocalDate?, periodeTomBarn: LocalDate?): LocalDate {
    if (periodeTomSoker == null && periodeTomBarn == null) {
        throw IllegalStateException("Både søker og barn kan ikke ha null i periodeTom dato")
    }
    return when {
        periodeTomSoker == null -> periodeTomBarn!!
        periodeTomBarn == null -> periodeTomSoker
        periodeTomBarn > periodeTomSoker -> periodeTomSoker
        else -> periodeTomBarn
    }
}

private fun overlapper(søker: PeriodeResultat, barn: PeriodeResultat): Boolean =
        (søker.periodeFom == null || barn.periodeTom == null || søker.periodeFom <= barn.periodeTom)
                && (søker.periodeTom == null || barn.periodeFom == null || søker.periodeTom >= barn.periodeFom)