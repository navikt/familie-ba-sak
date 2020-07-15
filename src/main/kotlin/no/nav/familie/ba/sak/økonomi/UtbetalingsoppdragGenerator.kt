package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.*
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class UtbetalingsoppdragGenerator(
        val persongrunnlagService: PersongrunnlagService,
        val beregningService: BeregningService) {

    /**
     * Lager utbetalingsoppdrag med kjedede perioder av andeler.
     * Ved opphør sendes kun siste utbetalingsperiode (med opphørsdato).
     *
     * @param[saksbehandlerId] settes på oppdragsnivå
     * @param[vedtak] for å hente fagsakid, behandlingid, vedtaksdato, ident, og evt opphørsdato
     * @param[behandlingResultatType] for å sjekke om fullstendig opphør
     * @param[erFørsteBehandlingPåFagsak] for å sette aksjonskode på oppdragsnivå og bestemme om vi skal telle fra start
     * @param[nyeAndeler] én liste per kjede, hvor andeler er helt nye, har endrede datoer eller må bygges opp igjen pga endringer før i kjeden
     * @param[opphørteAndeler] et par per kjede, hvor par består av siste andel i kjeden og
     * @return Utbetalingsoppdrag for vedtak
     */
    fun lagUtbetalingsoppdrag(saksbehandlerId: String,
                              vedtak: Vedtak,
                              behandlingResultatType: BehandlingResultatType,
                              erFørsteBehandlingPåFagsak: Boolean,
                              andelerTilOpprettelse: List<List<AndelTilkjentYtelse>> = emptyList(),
                              andelerTilOpphør: List<Pair<AndelTilkjentYtelse, LocalDate>> = emptyList()): Utbetalingsoppdrag {

        val erFullstendigOpphør = behandlingResultatType == BehandlingResultatType.OPPHØRT
        if (erFullstendigOpphør && andelerTilOpprettelse.isNotEmpty()) {
            throw IllegalStateException("Finnes nye andeler når behandling skal opphøres")
        }

        // Hos økonomi skiller man på endring på oppdragsnivå og på linjenivå (periodenivå).
        // For å kunne behandling alle forlengelser/forkortelser av perioder likt har vi valgt å konsekvent opphøre og erstatte.
        // På grunn av dette vil vi på oppdragsnivå kun ha aksjonskode UEND ved fullstendig opphør, selv om vi i realiteten av
        // og til kun endrer datoer på en eksisterende linje (endring på linjenivå).
        val aksjonskodePåOppdragsnivå =
                if (erFørsteBehandlingPåFagsak) NY
                else if (erFullstendigOpphør) UEND
                else ENDR

        val opprettes: List<Utbetalingsperiode> = if (andelerTilOpprettelse.isNotEmpty())
            lagUtbetalingsperioderForOpprettelse(
                    andeler = andelerTilOpprettelse,
                    erFørsteBehandlingPåFagsak = erFørsteBehandlingPåFagsak,
                    vedtak = vedtak) else emptyList()

        val opphøres: List<Utbetalingsperiode> = if (andelerTilOpphør.isNotEmpty())
            lagUtbetalingsperioderForOpphør(
                    andeler = andelerTilOpphør,
                    vedtak = vedtak) else emptyList()

        return Utbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                kodeEndring = aksjonskodePåOppdragsnivå,
                fagSystem = FAGSYSTEM,
                saksnummer = vedtak.behandling.fagsak.id.toString(),
                aktoer = vedtak.behandling.fagsak.hentAktivIdent().ident,
                utbetalingsperiode = listOf(opprettes, opphøres).flatten()
        )
    }

    fun lagUtbetalingsperioderForOpphør(andeler: List<Pair<AndelTilkjentYtelse, LocalDate>>,
                                                vedtak: Vedtak): List<Utbetalingsperiode> {
        val utbetalingsperiodeMal = UtbetalingsperiodeMal(vedtak, true)
        return andeler.map { (sisteAndelIKjede, opphørKjedeFom) ->
            utbetalingsperiodeMal.lagPeriodeFraAndel(andel = sisteAndelIKjede,
                                                     periodeIdOffset = sisteAndelIKjede.periodeOffset!!.toInt(),
                                                     forrigePeriodeIdOffset = null,
                                                     opphørKjedeFom = opphørKjedeFom)
        }
    }

    fun lagUtbetalingsperioderForOpprettelse(andeler: List<List<AndelTilkjentYtelse>>,
                                                     vedtak: Vedtak,
                                                     erFørsteBehandlingPåFagsak: Boolean): List<Utbetalingsperiode> {
        val fagsakId = vedtak.behandling.fagsak.id
        var offset =
                if (!erFørsteBehandlingPåFagsak)
                    hentSisteOffsetForFagsak(fagsakId)
                    ?: throw IllegalStateException("Skal finnes offset når ikke første behandling på fagsak")
                else 0

        val utbetalingsperiodeMal = UtbetalingsperiodeMal(vedtak)

        val utbetalingsperioder =
                andeler.flatMap { kjede: List<AndelTilkjentYtelse> ->
                    val ident = kjede.first().personIdent
                    val ytelseType = kjede.first().type
                    val erSøker = personErSøkerPåBehandling(ident, vedtak.behandling)
                    var forrigeOffsetIKjede: Int? = null
                    if (!erFørsteBehandlingPåFagsak) {
                        forrigeOffsetIKjede = if (erSøker) {
                            hentSisteOffsetForPerson(fagsakId = fagsakId, personIdent = ident, ytelseType = ytelseType)
                        } else {
                            hentSisteOffsetForPerson(fagsakId = fagsakId, personIdent = ident)
                        }
                    }
                    kjede.sortedBy { it.stønadFom }.mapIndexed { index, andel ->
                        val forrigeOffset = if (index == 0) forrigeOffsetIKjede else offset - 1
                        utbetalingsperiodeMal.lagPeriodeFraAndel(andel, offset, forrigeOffset).also {
                            andel.periodeOffset = offset.toLong()
                            offset++
                        }
                    }
                }
        beregningService.lagreOppdaterteAndelerTilkjentYtelse(andeler.flatten())
        return utbetalingsperioder
    }

    fun personErSøkerPåBehandling(ident: String, behandling: Behandling): Boolean =
            persongrunnlagService.hentSøker(behandling)!!.personIdent.ident == ident

    fun hentSisteOffsetForPerson(fagsakId: Long, personIdent: String, ytelseType: YtelseType? = null): Int? {
        val andelerPåFagsak = beregningService.hentAndelerTilkjentYtelseForFagsak(fagsakId)
        val sorterteAndeler = andelerPåFagsak
                .filter { it.personIdent == personIdent }
                .sortedBy { it.periodeOffset }
        return if (ytelseType != null) {
            sorterteAndeler.filter { it.type == ytelseType }.lastOrNull()?.periodeOffset?.toInt()
        } else {
            sorterteAndeler.lastOrNull()?.periodeOffset?.toInt()
        }
    }

    fun hentSisteOffsetForFagsak(fagsakId: Long): Int? {
        val andelerPåFagsak = beregningService.hentAndelerTilkjentYtelseForFagsak(fagsakId)
        val sorterteAndeler = andelerPåFagsak.sortedBy { it.periodeOffset }
        return sorterteAndeler.lastOrNull()?.periodeOffset?.toInt()
    }
}