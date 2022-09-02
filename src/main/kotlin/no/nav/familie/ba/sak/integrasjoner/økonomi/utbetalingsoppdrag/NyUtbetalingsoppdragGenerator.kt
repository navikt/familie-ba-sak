package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsperiodeMal
import no.nav.familie.ba.sak.integrasjoner.økonomi.valider
import no.nav.familie.ba.sak.integrasjoner.økonomi.validerOpphørsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.SMÅBARNSTILLEGG_SUFFIX
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.andelerTilOpphørMedDato
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.andelerTilOpprettelse
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.kjedeinndelteAndeler
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.sisteAndelPerKjede
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.sisteBeståendeAndelPerKjede
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class NyUtbetalingsoppdragGenerator {

    /**
     * Lager utbetalingsoppdrag med kjedede perioder av andeler.
     * Ved opphør sendes kun siste utbetalingsperiode (med opphørsdato).
     *
     * @param[vedtakMedTilkjentYtelse] tilpasset objekt som inneholder tilkjentytelse,og andre nødvendige felter som trenges for å lage utbetalingsoppdrag
     * @param[forrigeTilkjentYtelser] forrige tilkjentYtelser
     * @return oppdatert TilkjentYtelse som inneholder generert utbetalingsoppdrag
     */
    internal fun lagTilkjentYtelseMedUtbetalingsoppdrag(
        vedtakMedTilkjentYtelse: VedtakMedTilkjentYtelse,
        forrigeTilkjentYtelse: TilkjentYtelse? = null
    ): TilkjentYtelse {
        val tilkjentYtelse = vedtakMedTilkjentYtelse.tilkjentYtelse
        val vedtak = vedtakMedTilkjentYtelse.vedtak
        val erFørsteBehandlingPåFagsak = forrigeTilkjentYtelse == null
        // Filtrer kun andeler som kan sendes til oppdrag
        val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }

        // grupperer andeler basert på personIdent.
        // Markerte Småbarnstilegg med spesielt suffix SMÅBARNSTILLEGG_SUFFIX
        val oppdaterteKjeder: Map<String, List<AndelTilkjentYtelse>> = kjedeinndelteAndeler(andelerTilkjentYtelse)

        // grupperer forrige andeler basert på personIdent.
        // Markerte Småbarnstilegg med spesielt suffix SMÅBARNSTILLEGG_SUFFIX
        val forrigeAndeler = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.filter { it.erAndelSomSkalSendesTilOppdrag() }
            ?: emptyList()
        val forrigeKjeder: Map<String, List<AndelTilkjentYtelse>> = kjedeinndelteAndeler(forrigeAndeler)

        val erEndretMigreringsDato = vedtakMedTilkjentYtelse.endretMigreringsdato != null
        // Generer et komplett nytt eller bare endringer på et eksisterende betalingsoppdrag.
        val sisteBeståenAndelIHverKjede = if (vedtakMedTilkjentYtelse.erSimulering || erEndretMigreringsDato) {
            // Gjennom å sette andeler til null markeres at alle perioder i kjeden skal opphøres.
            sisteAndelPerKjede(forrigeKjeder, oppdaterteKjeder)
        } else {
            // For å kunne behandling alle forlengelser/forkortelser av perioder likt har vi valgt å konsekvent opphøre og erstatte.
            // Det vil si at vi alltid gjenoppbygger kjede fra første endring, selv om vi i realiteten av og til kun endrer datoer
            // på en eksisterende linje (endring på 150 linjenivå).
            sisteBeståendeAndelPerKjede(forrigeKjeder, oppdaterteKjeder)
        }

        // Finner ut andeler som er opprettet
        val andelerTilOpprettelse: List<List<AndelTilkjentYtelse>> =
            andelerTilOpprettelse(oppdaterteKjeder, sisteBeståenAndelIHverKjede)

        // Trenger denne sjekken som slipper å sette offset når det ikke finnes andelerTilOpprettelse,dvs nullutbetaling
        val opprettes: List<Utbetalingsperiode> = if (andelerTilOpprettelse.isNotEmpty()) {
            // lager utbetalingsperioder og oppdaterer andelerTilkjentYtelse
            val opprettelsePeriodeMedAndeler = lagUtbetalingsperioderForOpprettelse(
                andeler = andelerTilOpprettelse,
                erFørsteBehandlingPåFagsak = erFørsteBehandlingPåFagsak,
                vedtak = vedtak,
                sisteOffsetIKjedeOversikt = vedtakMedTilkjentYtelse.sisteOffsetPerIdent,
                sisteOffsetPåFagsak = vedtakMedTilkjentYtelse.sisteOffsetPåFagsak
            )
            // oppdater andeler i tilkjentYtelse
            tilkjentYtelse.andelerTilkjentYtelse.addAll(opprettelsePeriodeMedAndeler.first)
            opprettelsePeriodeMedAndeler.second
        } else {
            emptyList()
        }

        // Finner ut andeler som er opphørt
        val andelerTilOpphør = andelerTilOpphørMedDato(
            forrigeKjeder,
            sisteBeståenAndelIHverKjede,
            vedtakMedTilkjentYtelse.endretMigreringsdato
        )
        val opphøres: List<Utbetalingsperiode> = lagUtbetalingsperioderForOpphør(
            andeler = andelerTilOpphør,
            vedtak = vedtak
        )

        val aksjonskodePåOppdragsnivå =
            if (erFørsteBehandlingPåFagsak) Utbetalingsoppdrag.KodeEndring.NY else Utbetalingsoppdrag.KodeEndring.ENDR
        val utbetalingsoppdrag = Utbetalingsoppdrag(
            saksbehandlerId = vedtakMedTilkjentYtelse.saksbehandlerId,
            kodeEndring = aksjonskodePåOppdragsnivå,
            fagSystem = FAGSYSTEM,
            saksnummer = vedtak.behandling.fagsak.id.toString(),
            aktoer = vedtak.behandling.fagsak.aktør.aktivFødselsnummer(),
            utbetalingsperiode = listOf(opphøres, opprettes).flatten()
        )

        // valider utbetalingsoppdrag
        val erBehandlingOpphørt = vedtak.behandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT ||
            vedtak.behandling.resultat == Behandlingsresultat.OPPHØRT
        if (!vedtakMedTilkjentYtelse.erSimulering && erBehandlingOpphørt) utbetalingsoppdrag.validerOpphørsoppdrag()
        utbetalingsoppdrag.also {
            it.valider(
                behandlingsresultat = vedtak.behandling.resultat,
                behandlingskategori = vedtak.behandling.kategori,
                // her må vi sende alle andeler slik at det valideres for nullutbetalinger også
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.toList(),
                erEndreMigreringsdatoBehandling = vedtak.behandling.opprettetÅrsak == BehandlingÅrsak.ENDRE_MIGRERINGSDATO
            )
        }

        // oppdater tilkjentYtlese med andelerTilkjentYTelser og utbetalingsoppdrag
        return tilkjentYtelse.copy(
            behandling = vedtak.behandling,
            utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag)
        )
    }

    private fun lagUtbetalingsperioderForOpphør(
        andeler: List<Pair<AndelTilkjentYtelse, YearMonth>>,
        vedtak: Vedtak
    ): List<Utbetalingsperiode> =
        andeler.map { (sisteAndelIKjede, opphørKjedeFom) ->
            UtbetalingsperiodeMal(
                vedtak = vedtak,
                erEndringPåEksisterendePeriode = true
            ).lagPeriodeFraAndel(
                andel = sisteAndelIKjede,
                periodeIdOffset = sisteAndelIKjede.periodeOffset!!.toInt(),
                forrigePeriodeIdOffset = sisteAndelIKjede.forrigePeriodeOffset?.toInt(),
                opphørKjedeFom = opphørKjedeFom
            )
        }

    private fun lagUtbetalingsperioderForOpprettelse(
        andeler: List<List<AndelTilkjentYtelse>>,
        vedtak: Vedtak,
        erFørsteBehandlingPåFagsak: Boolean,
        sisteOffsetIKjedeOversikt: Map<String, Int>,
        sisteOffsetPåFagsak: Int? = null
    ): Pair<List<AndelTilkjentYtelse>, List<Utbetalingsperiode>> {
        var offset = if (!erFørsteBehandlingPåFagsak) {
            sisteOffsetPåFagsak?.plus(1)
                ?: throw IllegalStateException("Skal finnes offset når ikke første behandling på fagsak")
        } else {
            0
        }

        val utbetalingsperiode = andeler.filter { kjede -> kjede.isNotEmpty() }
            .flatMap { kjede: List<AndelTilkjentYtelse> ->
                val ident = kjede.first().aktør.aktivFødselsnummer()
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
                    UtbetalingsperiodeMal(
                        vedtak = vedtak
                    ).lagPeriodeFraAndel(andel, offset, forrigeOffset).also {
                        andel.periodeOffset = offset.toLong()
                        andel.forrigePeriodeOffset = forrigeOffset?.toLong()
                        // Trengs for å finne tilbake ved konsistensavstemming
                        andel.kildeBehandlingId = andel.behandlingId
                        offset++
                    }
                }
            }
        return andeler.flatten() to utbetalingsperiode
    }
}
