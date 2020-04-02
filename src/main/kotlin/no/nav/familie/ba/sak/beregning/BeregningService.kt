package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelse
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BeregningService(
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
        private val fagsakService: FagsakService,
        private val tilkjentYtelseRepository: TilkjentYtelseRepository
) {
    fun hentAndelerTilkjentYtelseForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> {
        return andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
    }

    fun hentTilkjentYtelseForBehandling(behandlingId: Long): TilkjentYtelse {
        return tilkjentYtelseRepository.findByBehandling(behandlingId)
    }

    @Transactional
    fun oppdaterBehandlingMedBeregning(behandling: Behandling,
                                       personopplysningGrunnlag: PersonopplysningGrunnlag,
                                       nyBeregning: NyBeregning): Ressurs<RestFagsak> {

        val andelerTilkjentYtelse = mapNyBeregningTilAndelerTilkjentYtelse(behandling.id, nyBeregning, personopplysningGrunnlag)
        andelTilkjentYtelseRepository.slettAlleAndelerTilkjentYtelseForBehandling(behandling.id)

        val tilkjentYtelse = TilkjentYtelse(
                behandling = behandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
                andelerTilkjentYtelse = andelerTilkjentYtelse.toMutableSet()
        )
        tilkjentYtelseRepository.save(tilkjentYtelse)

        return fagsakService.hentRestFagsak(behandling.fagsak.id)
    }

    fun oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling: Behandling,
                                                    utbetalingsoppdrag: Utbetalingsoppdrag) {

        val nyTilkjentYtelse = populerTilkjentYtelse(behandling, utbetalingsoppdrag)
        tilkjentYtelseRepository.save(nyTilkjentYtelse)
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
}

fun mapNyBeregningTilAndelerTilkjentYtelse(behandlingId: Long, nyBeregning: NyBeregning, personopplysningGrunnlag: PersonopplysningGrunnlag)
        : List<AndelTilkjentYtelse> {

    val identBarnMap = personopplysningGrunnlag.barna
            .associateBy { it.personIdent.ident }

    return nyBeregning.personBeregninger
            .filter { identBarnMap.containsKey(it.ident) }
            .map {

                val person = identBarnMap[it.ident]!!

                if (it.stønadFom.isBefore(person.fødselsdato)) {
                    error("Ugyldig fra og med dato for barn med fødselsdato ${person.fødselsdato}")
                }

                val sikkerStønadFom = it.stønadFom.withDayOfMonth(1)
                val sikkerStønadTom = person.fødselsdato.plusYears(18).sisteDagIForrigeMåned()

                if (sikkerStønadTom.isBefore(sikkerStønadFom)) {
                    error("Stønadens fra-og-med-dato (${sikkerStønadFom}) er etter til-og-med-dato (${sikkerStønadTom}). ")
                }

                AndelTilkjentYtelse(personId = person.id,
                        behandlingId = behandlingId,
                        beløp = it.beløp,
                        stønadFom = sikkerStønadFom,
                        stønadTom = sikkerStønadTom,
                        type = it.ytelsetype)
            }
}