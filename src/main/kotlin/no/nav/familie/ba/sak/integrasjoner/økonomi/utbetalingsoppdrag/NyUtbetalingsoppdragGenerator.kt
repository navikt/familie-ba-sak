package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsperiodeMal
import no.nav.familie.ba.sak.integrasjoner.økonomi.valider
import no.nav.familie.ba.sak.integrasjoner.økonomi.validerOpphørsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.SMÅBARNSTILLEGG_SUFFIX
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.andelerTilOpphørMedDato
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.andelerTilOpprettelse
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.kjedeinndelteAndeler
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.sisteAndelPerKjede
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.sisteBeståendeAndelPerKjede
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
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
import java.time.LocalDate
import java.time.YearMonth

@Component
class NyUtbetalingsoppdragGenerator {

    /**
     * Lager utbetalingsoppdrag med kjedede perioder av andeler.
     * Ved opphør sendes kun siste utbetalingsperiode (med opphørsdato).
     *
     * @param[saksbehandlerId] settes på oppdragsnivå
     * @param[vedtak] for å hente fagsakid, behandlingid, vedtaksdato, ident, og evt opphørsdato
     * @param[erFørsteBehandlingPåFagsak] for å sette aksjonskode på oppdragsnivå og bestemme om vi skal telle fra start
     * @param[forrigeKjeder] Et sett med kjeder som var gjeldende for forrige behandling på fagsaken
     * @param[sisteOffsetPerIdent] Siste iverksatte offset mot økonomi per ident.
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
    internal fun lagUtbetalingsoppdragMedTilkjentYtelse(
        saksbehandlerId: String,
        vedtak: Vedtak,
        erFørsteBehandlingPåFagsak: Boolean,
        forrigeKjeder: Map<String, List<AndelTilkjentYtelse>> = emptyMap(),
        sisteOffsetPerIdent: Map<String, Int> = emptyMap(),
        sisteOffsetPåFagsak: Int? = null,
        oppdaterteKjeder: Map<String, List<AndelTilkjentYtelse>> = emptyMap(),
        erSimulering: Boolean = false,
        endretMigreringsDato: YearMonth? = null
    ): Utbetalingsoppdrag {
        // Hos økonomi skiller man på endring på oppdragsnivå 110 og på linjenivå 150 (periodenivå).
        // Da de har opplevd å motta
        // UEND på oppdrag som skulle vært ENDR anbefaler de at kun ENDR brukes når sak
        // ikke er ny, så man slipper å forholde seg til om det er endring over 150-nivå eller ikke.
        val aksjonskodePåOppdragsnivå =
            if (erFørsteBehandlingPåFagsak) Utbetalingsoppdrag.KodeEndring.NY else Utbetalingsoppdrag.KodeEndring.ENDR

        // endretMigreringsDato satt betyr at endring skjedd for migreringsdato og som en
        // konsekvens så skal hele betalingsoppdraget opphøre.
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

        val andelerTilOpprettelse: List<List<AndelTilkjentYtelse>> =
            andelerTilOpprettelse(oppdaterteKjeder, sisteBeståenAndelIHverKjede)

        // Trenger denne sjekken som slipper å sette offset når det ikke finnes andelerTilOpprettelse,dvs nullutbetaling
        val opprettes: List<Utbetalingsperiode> = if (andelerTilOpprettelse.isNotEmpty()) {
            lagUtbetalingsperioderForOpprettelse(
                andeler = andelerTilOpprettelse,
                erFørsteBehandlingPåFagsak = erFørsteBehandlingPåFagsak,
                vedtak = vedtak,
                sisteOffsetIKjedeOversikt = sisteOffsetPerIdent,
                sisteOffsetPåFagsak = sisteOffsetPåFagsak
            ).second
        } else {
            emptyList()
        }
        val andelerTilOpphør = andelerTilOpphørMedDato(forrigeKjeder, sisteBeståenAndelIHverKjede, endretMigreringsDato)
        val opphøres: List<Utbetalingsperiode> = lagUtbetalingsperioderForOpphør(
            andeler = andelerTilOpphør,
            vedtak = vedtak
        )

        return Utbetalingsoppdrag(
            saksbehandlerId = saksbehandlerId,
            kodeEndring = aksjonskodePåOppdragsnivå,
            fagSystem = FAGSYSTEM,
            saksnummer = vedtak.behandling.fagsak.id.toString(),
            aktoer = vedtak.behandling.fagsak.aktør.aktivFødselsnummer(),
            utbetalingsperiode = listOf(opphøres, opprettes).flatten()
        )
    }

    /**
     * Lager utbetalingsoppdrag med kjedede perioder av andeler.
     * Ved opphør sendes kun siste utbetalingsperiode (med opphørsdato).
     *
     * @param[tilkjentYtelseMetaData] tilpasset objekt som inneholder tilkjentytelse,og andre nødvendige felter som trenges for å lage utbetalingsoppdrag
     * @param[forrigeTilkjentYtelser] forrige tilkjentYtelser
     * @return oppdatert TilkjentYtelse som inneholder generert utbetalingsoppdrag
     */
    internal fun lagTilkjentYtelseMedUtbetalingsoppdrag(
        tilkjentYtelseMetaData: TilkjentYtelseMetaData,
        forrigeTilkjentYtelse: TilkjentYtelse? = null
    ): TilkjentYtelse {
        val tilkjentYtelse = tilkjentYtelseMetaData.tilkjentYtelse
        val vedtak = tilkjentYtelseMetaData.vedtak
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

        val erEndretMigreringsDato = tilkjentYtelseMetaData.endretMigreringsdato != null
        // Generer et komplett nytt eller bare endringer på et eksisterende betalingsoppdrag.
        val sisteBeståenAndelIHverKjede = if (tilkjentYtelseMetaData.erSimulering || erEndretMigreringsDato) {
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
            val opprettelsePeriodeMedAndeler = lagUtbetalingsperioderForOpprettelse(
                andeler = andelerTilOpprettelse,
                erFørsteBehandlingPåFagsak = erFørsteBehandlingPåFagsak,
                vedtak = vedtak,
                sisteOffsetIKjedeOversikt = tilkjentYtelseMetaData.sisteOffsetPerIdent,
                sisteOffsetPåFagsak = tilkjentYtelseMetaData.sisteOffsetPåFagsak
            )
            // oppdater andeler i tilkjentYtelse
            tilkjentYtelse.andelerTilkjentYtelse.clear()
            tilkjentYtelse.andelerTilkjentYtelse.addAll(opprettelsePeriodeMedAndeler.first)
            opprettelsePeriodeMedAndeler.second
        } else {
            emptyList()
        }

        // Finner ut andeler som er opphørt
        val andelerTilOpphør = andelerTilOpphørMedDato(
            forrigeKjeder,
            sisteBeståenAndelIHverKjede,
            tilkjentYtelseMetaData.endretMigreringsdato
        )
        val opphøres: List<Utbetalingsperiode> = lagUtbetalingsperioderForOpphør(
            andeler = andelerTilOpphør,
            vedtak = vedtak
        )

        val aksjonskodePåOppdragsnivå =
            if (erFørsteBehandlingPåFagsak) Utbetalingsoppdrag.KodeEndring.NY else Utbetalingsoppdrag.KodeEndring.ENDR
        val utbetalingsoppdrag = Utbetalingsoppdrag(
            saksbehandlerId = tilkjentYtelseMetaData.saksbehandlerId,
            kodeEndring = aksjonskodePåOppdragsnivå,
            fagSystem = FAGSYSTEM,
            saksnummer = vedtak.behandling.fagsak.id.toString(),
            aktoer = vedtak.behandling.fagsak.aktør.aktivFødselsnummer(),
            utbetalingsperiode = listOf(opphøres, opprettes).flatten()
        )

        // valider utbetalingsoppdrag
        val erBehandlingOpphørt = vedtak.behandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT ||
            vedtak.behandling.resultat == Behandlingsresultat.OPPHØRT
        if (!tilkjentYtelseMetaData.erSimulering && erBehandlingOpphørt) utbetalingsoppdrag.validerOpphørsoppdrag()
        utbetalingsoppdrag.also {
            it.valider(
                behandlingsresultat = vedtak.behandling.resultat,
                behandlingskategori = vedtak.behandling.kategori,
                kompetanser = tilkjentYtelseMetaData.kompetanser.toList(),
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                erEndreMigreringsdatoBehandling = vedtak.behandling.opprettetÅrsak == BehandlingÅrsak.ENDRE_MIGRERINGSDATO
            )
        }

        // oppdater tilkjentYtlese med andelerTilkjentYTelser og utbetalingsoppdrag
        return oppdaterTilkjentYtelse(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            utbetalingsoppdrag = utbetalingsoppdrag
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
        var offset =
            if (!erFørsteBehandlingPåFagsak) {
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
                        andel.kildeBehandlingId =
                            andel.behandlingId // Trengs for å finne tilbake ved konsistensavstemming
                        offset++
                    }
                }
            }
        return andeler.flatten() to utbetalingsperiode
    }

    internal fun oppdaterTilkjentYtelse(
        behandling: Behandling,
        tilkjentYtelse: TilkjentYtelse,
        utbetalingsoppdrag: Utbetalingsoppdrag
    ): TilkjentYtelse {
        val erRentOpphør =
            utbetalingsoppdrag.utbetalingsperiode.isNotEmpty() && utbetalingsoppdrag.utbetalingsperiode.all { it.opphør != null }
        var opphørsdato: LocalDate? = null
        if (erRentOpphør) {
            opphørsdato = utbetalingsoppdrag.utbetalingsperiode.minOf { it.opphør!!.opphørDatoFom }
        }

        if (behandling.type == BehandlingType.REVURDERING) {
            val opphørPåRevurdering = utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør != null }
            if (opphørPåRevurdering.isNotEmpty()) {
                opphørsdato = opphørPåRevurdering.maxByOrNull { it.opphør!!.opphørDatoFom }!!.opphør!!.opphørDatoFom
            }
        }

        return tilkjentYtelse.apply {
            this.utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag)
            this.stønadTom = tilkjentYtelse.andelerTilkjentYtelse.maxOfOrNull { it.stønadTom }
            this.stønadFom =
                if (erRentOpphør) null else tilkjentYtelse.andelerTilkjentYtelse.minOfOrNull { it.stønadFom }
            this.endretDato = LocalDate.now()
            this.opphørFom = opphørsdato?.toYearMonth()
        }
    }
}
