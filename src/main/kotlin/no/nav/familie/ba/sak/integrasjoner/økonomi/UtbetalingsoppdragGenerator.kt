package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.SMÅBARNSTILLEGG_SUFFIX
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.andelerTilOpphørMedDato
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.andelerTilOpprettelse
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.gjeldendeForrigeOffsetForKjede
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.sisteAndelPerKjede
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.sisteBeståendeAndelPerKjede
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.*
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class UtbetalingsoppdragGenerator(
        private val beregningService: BeregningService,
) {

    /**
     * Lager utbetalingsoppdrag med kjedede perioder av andeler.
     * Ved opphør sendes kun siste utbetalingsperiode (med opphørsdato).
     *
     * @param[saksbehandlerId] settes på oppdragsnivå
     * @param[vedtak] for å hente fagsakid, behandlingid, vedtaksdato, ident, og evt opphørsdato
     * @param[erFørsteBehandlingPåFagsak] for å sette aksjonskode på oppdragsnivå og bestemme om vi skal telle fra start
     * @param[forrigeKjeder] Et sett med kjeder som var gjeldende for forrige behandling på fagsaken
     * @param[oppdaterteKjeder] Et sett med andeler knyttet til en person (dvs en kjede), hvor andeler er helt nye,
     * @param[erSimulering] flag for om beregnet er en simulering, da skal komplett nytt betlaingsoppdrag generes
     *                      og ny tilkjentytelse skal ikke persisteres,
     * @param[endretMigreringsDato] Satt betyr at en endring skjedd fra før den eksisterende migreringsdatoen, som en konsekevens
     *                              skal hele betalingsoppdraget opphøre.
     * flag for om beregnet er en simulering, da skal komplett nytt betlaingsoppdrag generes
     *                      og ny tilkjentytelse skal ikke persisteres,
     * har endrede datoer eller må bygges opp igjen pga endringer før i kjeden
     * @return Utbetalingsoppdrag for vedtak
     */
    fun lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
            saksbehandlerId: String,
            vedtak: Vedtak,
            erFørsteBehandlingPåFagsak: Boolean,
            forrigeKjeder: Map<String, List<AndelTilkjentYtelse>> = emptyMap(),
            sisteOffsetPåFagsak: Int? = null,
            oppdaterteKjeder: Map<String, List<AndelTilkjentYtelse>> = emptyMap(),
            erSimulering: Boolean = false,
            endretMigreringsDato: YearMonth? = null,
    ): Utbetalingsoppdrag {

        // Hos økonomi skiller man på endring på oppdragsnivå 110 og på linjenivå 150 (periodenivå).
        // Da de har opplevd å motta
        // UEND på oppdrag som skulle vært ENDR anbefaler de at kun ENDR brukes når sak
        // ikke er ny, så man slipper å forholde seg til om det er endring over 150-nivå eller ikke.
        val aksjonskodePåOppdragsnivå = if (erFørsteBehandlingPåFagsak) NY else ENDR

        //endretMigreringsDato satt betyr at endring skjedd for migreringsdato og som en
        //konsekvens så skal hele betalingsoppdraget opphøre.
        val erEndretMigreringsDato = endretMigreringsDato != null

        // Generer et komplett nytt eller bare endringer på et eksisterende betalingsoppdrag.
        val sisteBeståenAndelIHverKjede = if (erSimulering || erEndretMigreringsDato) {
            // Gjennom å sette andeler til null markeres at alle perioder i kjeden skal opphøres.
            sisteAndelPerKjede(forrigeKjeder, oppdaterteKjeder)
        } else {
            // For å kunne behandling alle forlengelser/forkortelser av perioder likt har vi valgt å konsekvent opphøre og erstatte.
            // Det vil si at vi alltid gjenoppbygger kjede fra første endring, selv om vi i realiteten av og til kun endrer datoer
            // på en eksisterende linje (endring på 150 linjenivå).
            sisteBeståendeAndelPerKjede(forrigeKjeder, oppdaterteKjeder)
        }

        val andelerTilOpphør =
                andelerTilOpphørMedDato(forrigeKjeder, oppdaterteKjeder, sisteBeståenAndelIHverKjede, endretMigreringsDato)
        val andelerTilOpprettelse: List<List<AndelTilkjentYtelse>> =
                andelerTilOpprettelse(oppdaterteKjeder, sisteBeståenAndelIHverKjede)

        val opprettes: List<Utbetalingsperiode> = if (andelerTilOpprettelse.isNotEmpty())
            lagUtbetalingsperioderForOpprettelseOgOppdaterTilkjentYtelse(
                    andeler = andelerTilOpprettelse,
                    erFørsteBehandlingPåFagsak = erFørsteBehandlingPåFagsak,
                    vedtak = vedtak,
                    sisteOffsetIKjedeOversikt = gjeldendeForrigeOffsetForKjede(forrigeKjeder),
                    sisteOffsetPåFagsak = sisteOffsetPåFagsak,
                    skalOppdatereTilkjentYtelse = !erSimulering,
            ) else emptyList()

        val opphøres: List<Utbetalingsperiode> = if (andelerTilOpphør.isNotEmpty())
            lagUtbetalingsperioderForOpphør(
                    andeler = andelerTilOpphør,
                    vedtak = vedtak,
            ) else emptyList()

        return Utbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                kodeEndring = aksjonskodePåOppdragsnivå,
                fagSystem = FAGSYSTEM,
                saksnummer = vedtak.behandling.fagsak.id.toString(),
                aktoer = vedtak.behandling.fagsak.hentAktivIdent().ident,
                utbetalingsperiode = listOf(opphøres, opprettes).flatten()
        )
    }

    fun lagUtbetalingsperioderForOpphør(andeler: List<Pair<AndelTilkjentYtelse, YearMonth>>,
                                        vedtak: Vedtak): List<Utbetalingsperiode> {
        val utbetalingsperiodeMal = UtbetalingsperiodeMal(
                vedtak = vedtak,
                erEndringPåEksisterendePeriode = true
        )

        return andeler.map { (sisteAndelIKjede, opphørKjedeFom) ->
            utbetalingsperiodeMal.lagPeriodeFraAndel(andel = sisteAndelIKjede,
                                                     periodeIdOffset = sisteAndelIKjede.periodeOffset!!.toInt(),
                                                     forrigePeriodeIdOffset = sisteAndelIKjede.forrigePeriodeOffset?.toInt(),
                                                     opphørKjedeFom = opphørKjedeFom)
        }
    }

    fun lagUtbetalingsperioderForOpprettelseOgOppdaterTilkjentYtelse(
            andeler: List<List<AndelTilkjentYtelse>>,
            vedtak: Vedtak,
            erFørsteBehandlingPåFagsak: Boolean,
            sisteOffsetIKjedeOversikt: Map<String, Int>,
            sisteOffsetPåFagsak: Int? = null,
            skalOppdatereTilkjentYtelse: Boolean,
    ): List<Utbetalingsperiode> {
        var offset =
                if (!erFørsteBehandlingPåFagsak)
                    sisteOffsetPåFagsak?.plus(1)
                    ?: throw IllegalStateException("Skal finnes offset når ikke første behandling på fagsak")
                else 0

        val utbetalingsperiodeMal = UtbetalingsperiodeMal(
                vedtak = vedtak
        )

        val utbetalingsperioder = andeler.filter { kjede -> kjede.isNotEmpty() }
                .flatMap { kjede: List<AndelTilkjentYtelse> ->
                    val ident = kjede.first().personIdent
                    val ytelseType = kjede.first().type
                    var forrigeOffsetIKjede: Int? = null
                    if (!erFørsteBehandlingPåFagsak) {
                        forrigeOffsetIKjede = if (ytelseType == YtelseType.SMÅBARNSTILLEGG) {
                            sisteOffsetIKjedeOversikt[ident + SMÅBARNSTILLEGG_SUFFIX]
                        } else {
                            sisteOffsetIKjedeOversikt[ident]
                        }
                    }
                    kjede.sortedBy { it.stønadFom }.mapIndexed { index, andel ->
                        val forrigeOffset = if (index == 0) forrigeOffsetIKjede else offset - 1
                        utbetalingsperiodeMal.lagPeriodeFraAndel(andel, offset, forrigeOffset).also {
                            andel.periodeOffset = offset.toLong()
                            andel.forrigePeriodeOffset = forrigeOffset?.toLong()
                            andel.kildeBehandlingId = andel.behandlingId // Trengs for å finne tilbake ved konsistensavstemming
                            offset++
                        }
                    }
                }

        // TODO Vi bør se om vi kan flytte ut denne side effecten
        if (skalOppdatereTilkjentYtelse) {
            val oppdatertTilkjentYtelse = andeler.flatten().firstOrNull()?.tilkjentYtelse ?: throw Feil(
                    "Andeler mangler ved generering av utbetalingsperioder. Får tom liste."
            )
            beregningService.lagreTilkjentYtelseMedOppdaterteAndeler(oppdatertTilkjentYtelse)
        }


        return utbetalingsperioder
    }
}