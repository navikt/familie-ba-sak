package no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag

import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsperiodeMal
import no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag.BeståendeAndelerBeregner.finnBeståendeAndeler
import no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag.OppdragBeregnerUtil.validerAndeler
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.time.YearMonth

object NyUtbetalingsoppdragGenerator {

    fun lagUtbetalingsoppdrag(
        behandlingsinformasjon: Behandlingsinformasjon,
        nyeAndeler: List<AndelData>,
        forrigeAndeler: List<AndelData>?,
        sisteAndelPerKjede: Map<IdentOgType, AndelData> = emptyMap(),
        // erSimulering: Boolean = false,
        // endretMigreringsDato: YearMonth? = null
    ): UtbetalingsoppdragOgAndelerMedOffset {
        validerAndeler(forrigeAndeler, nyeAndeler)
        val nyeKjeder = nyeAndeler.groupByIdentOgType()
        val forrigeKjeder = (forrigeAndeler ?: emptyList()).groupByIdentOgType()

        return lagUtbetalingsoppdrag(
            nyeKjeder = nyeKjeder,
            forrigeKjeder = forrigeKjeder,
            sisteAndelPerKjede = sisteAndelPerKjede,
            behandlingsinformasjon = behandlingsinformasjon,
            forrigeAndeler = forrigeAndeler,
        )
    }

    private fun lagUtbetalingsoppdrag(
        nyeKjeder: Map<IdentOgType, List<AndelData>>,
        forrigeKjeder: Map<IdentOgType, List<AndelData>>,
        sisteAndelPerKjede: Map<IdentOgType, AndelData>,
        behandlingsinformasjon: Behandlingsinformasjon,
        forrigeAndeler: List<AndelData>?,
    ): UtbetalingsoppdragOgAndelerMedOffset {
        /*
        TODO validering av endretSimuleringsdato, burde ikke kunne være etter min fom på nye andeler
        TODO erSimulering
         */
        val nyeKjeder = lagNyeKjeder(nyeKjeder, forrigeKjeder, sisteAndelPerKjede)

        val utbetalingsoppdrag = Utbetalingsoppdrag(
            saksbehandlerId = behandlingsinformasjon.saksbehandlerId,
            kodeEndring = kodeEndring(forrigeAndeler),
            fagSystem = FAGSYSTEM,
            saksnummer = behandlingsinformasjon.fagsakId.toString(),
            aktoer = behandlingsinformasjon.aktør.aktivFødselsnummer(),
            utbetalingsperiode = utbetalingsperioder(behandlingsinformasjon, nyeKjeder),
        )

        return UtbetalingsoppdragOgAndelerMedOffset(
            utbetalingsoppdrag,
            andelerMedOffset(behandlingsinformasjon, nyeKjeder),
        )
    }

    private fun lagNyeKjeder(
        nyeKjeder: Map<IdentOgType, List<AndelData>>,
        forrigeKjeder: Map<IdentOgType, List<AndelData>>,
        sisteAndelPerKjede: Map<IdentOgType, AndelData>,
    ): List<ResultatForKjede> {
        val alleIdentOgTyper = nyeKjeder.keys + forrigeKjeder.keys
        var sisteOffset = sisteAndelPerKjede.values.mapNotNull { it.offset }.maxOrNull()
        return alleIdentOgTyper.map { identOgType ->
            val forrigeAndeler = forrigeKjeder[identOgType] ?: emptyList()
            val nyeAndeler = nyeKjeder[identOgType] ?: emptyList()
            val sisteAndel = sisteAndelPerKjede[identOgType]
            // TODO må nog sende med endretMigreringsDato/erSimulering? her og
            val nyKjede = beregnNyKjede(forrigeAndeler, nyeAndeler, sisteAndel, sisteOffset)
            sisteOffset = nyKjede.sisteOffset
            nyKjede
        }
    }

    private fun utbetalingsperioder(
        behandlingsinformasjon: Behandlingsinformasjon,
        nyeKjeder: List<ResultatForKjede>,
    ): List<Utbetalingsperiode> {
        val opphørsperioder = lagOpphørsperioder(behandlingsinformasjon, nyeKjeder.mapNotNull { it.opphørsandel })
        val nyePerioder = lagNyePerioder(behandlingsinformasjon, nyeKjeder.flatMap { it.nyeAndeler })
        return opphørsperioder + nyePerioder
    }

    private fun andelerMedOffset(
        behandlingsinformasjon: Behandlingsinformasjon,
        nyeKjeder: List<ResultatForKjede>,
    ): List<AndelMedOffset> = nyeKjeder.flatMap { nyKjede ->
        nyKjede.beståendeAndeler.filter { it.beløp != 0 }.map { AndelMedOffset(it) } +
            nyKjede.nyeAndeler.filter { it.beløp != 0 }.map { AndelMedOffset(it, behandlingsinformasjon.behandlingId) }
    }

    // Hos økonomi skiller man på endring på oppdragsnivå 110 og på linjenivå 150 (periodenivå).
    // Da de har opplevd å motta
    // UEND på oppdrag som skulle vært ENDR anbefaler de at kun ENDR brukes når sak
    // ikke er ny, så man slipper å forholde seg til om det er endring over 150-nivå eller ikke.
    private fun kodeEndring(forrigeAndeler: List<AndelData>?) =
        if (forrigeAndeler == null) KodeEndring.NY else KodeEndring.ENDR

    private fun beregnNyKjede(
        forrige: List<AndelData>,
        nye: List<AndelData>,
        sisteAndel: AndelData?,
        offset: Long?,
    ): ResultatForKjede {
        val beståendeAndeler = finnBeståendeAndeler(forrige, nye)
        val nyeAndeler = nye.subList(beståendeAndeler.andeler.size, nye.size)
        var gjeldendeOffset = offset ?: -1
        var forrigeOffset = sisteAndel?.offset
        val nyeAndelerMedOffset = nyeAndeler.filter { it.beløp != 0 }.mapIndexed { index, andelData ->
            gjeldendeOffset += 1
            val nyAndel = andelData.copy(offset = gjeldendeOffset, forrigeOffset = forrigeOffset)
            forrigeOffset = gjeldendeOffset
            nyAndel
        }
        // TODO på en eller annen måte verifisere at offset har blitt satt?
        if (gjeldendeOffset < 0) {
            // gjeldendeOffset =
        }
        return ResultatForKjede(
            beståendeAndeler = beståendeAndeler.andeler,
            nyeAndeler = nyeAndelerMedOffset,
            opphørsandel = beståendeAndeler.opphørFra?.let {
                Pair(sisteAndel ?: error("Må ha siste andel for å kunne opphøre"), it)
            },
            sisteOffset = gjeldendeOffset,
        )
    }

    private fun List<AndelData>.groupByIdentOgType(): Map<IdentOgType, List<AndelData>> =
        groupBy { IdentOgType(it.ident, it.type) }.mapValues { it.value.sortedBy { it.fom } }

    private fun lagOpphørsperioder(
        behandlingsinformasjon: Behandlingsinformasjon,
        andeler: List<Pair<AndelData, YearMonth>>,
    ): List<Utbetalingsperiode> {
        val utbetalingsperiodeMal = UtbetalingsperiodeMal(
            vedtak = behandlingsinformasjon.vedtak,
            erEndringPåEksisterendePeriode = true,
        )

        return andeler.map {
            utbetalingsperiodeMal.lagPeriodeFraAndel(it.first, opphørKjedeFom = it.second)
        }
    }

    private fun lagNyePerioder(
        behandlingsinformasjon: Behandlingsinformasjon,
        andeler: List<AndelData>,
    ): List<Utbetalingsperiode> {
        val utbetalingsperiodeMal = UtbetalingsperiodeMal(
            vedtak = behandlingsinformasjon.vedtak,
        )
        return andeler.map { utbetalingsperiodeMal.lagPeriodeFraAndel(it) }
    }
}
