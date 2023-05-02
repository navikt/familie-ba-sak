package no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag

import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsperiodeMal
import no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag.Bestående2.beregnNyeKjeder
import no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag.BeståendeAndelerBeregner.erLik
import no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag.OppdragBeregnerUtil.validerAndeler
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.time.YearMonth

object NyUtbetalingsoppdragGenerator2 {

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
            val nyKjede = beregnNyeKjeder(forrigeAndeler, nyeAndeler, sisteAndel, sisteOffset)
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
        nyKjede.beståendeAndeler.map { AndelMedOffset(it) } +
            nyKjede.nyeAndeler.map { AndelMedOffset(it, behandlingsinformasjon.behandlingId) }
    }

    // Hos økonomi skiller man på endring på oppdragsnivå 110 og på linjenivå 150 (periodenivå).
    // Da de har opplevd å motta
    // UEND på oppdrag som skulle vært ENDR anbefaler de at kun ENDR brukes når sak
    // ikke er ny, så man slipper å forholde seg til om det er endring over 150-nivå eller ikke.
    private fun kodeEndring(forrigeAndeler: List<AndelData>?) =
        if (forrigeAndeler == null) KodeEndring.NY else KodeEndring.ENDR

    private fun List<AndelData>.groupByIdentOgType(): Map<IdentOgType, List<AndelData>> =
        groupBy { IdentOgType(it.ident, it.type) }

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

object Bestående2 {

    fun beregnNyeKjeder(
        forrige: List<AndelData>,
        nye: List<AndelData>,
        sisteAndel: AndelData?,
        offset: Long?,
    ): ResultatForKjede {
        var gjeldendeOffset = offset
        var forrigeOffset = sisteAndel?.offset
        val (bestående, opphørsdato) = beregnNyeKjeder(forrige, nye)
        val fraIndex = bestående.size
        val nyKjede = nye.subList(fraIndex, nye.size).mapIndexed { index, andelData ->
            gjeldendeOffset = (gjeldendeOffset ?: -1) + 1
            val nyAndel = andelData.copy(offset = gjeldendeOffset, forrigeOffset = forrigeOffset)
            forrigeOffset = gjeldendeOffset
            nyAndel
        }
        return ResultatForKjede(
            bestående,
            nyKjede,
            getOpphørsandel(opphørsdato, sisteAndel),
            gjeldendeOffset ?: error("Mangler offset"),
        )
    }

    private fun getOpphørsandel(
        opphørsdato: YearMonth?,
        sisteAndel: AndelData?,
    ): Pair<AndelData, YearMonth>? {
        return opphørsdato?.let { Pair(sisteAndel ?: error("Kan ikke opphøre når det ikke finnes en siste andel"), it) }
    }

    fun beregnNyeKjeder(
        forrige: List<AndelData>,
        nye: List<AndelData>,
    ): Pair<List<AndelData>, YearMonth?> {
        val beståendeAndeler = mutableListOf<AndelData>()
        var opphørsdato: YearMonth? = null
        for (index in 0 until forrige.size) {
            val forrige = forrige[index]
            val ny = if (nye.size > index) nye[index] else null
            val nyNeste = if (nye.size > index + 1) nye[index + 1] else null

            if (ny != null && forrige.erLik(ny)) {
                beståendeAndeler.add(ny.medOffsetOgKildebehandlingFra(forrige))
                continue
            }

            if (ny == null || forrige.fom < ny.fom) {
                opphørsdato = forrige.fom
            } else if (forrige.fom > ny.fom || forrige.beløp != ny.beløp) {
                // Ny andel skriver over
            } else if (forrige.tom > ny.tom) {
                opphørsdato = utledOpphørsdatoForAvkortetAndel(nyNeste, ny)
                beståendeAndeler.add(ny.medOffsetOgKildebehandlingFra(forrige))
            }
            break
        }
        return beståendeAndeler to opphørsdato
    }

    /**
     * Utleder opphørsdato når en tidligere andel blir avkortet.
     * Hvis neste andel begynner direkt etterpå så sender man ikke et opphør
     */
    private fun utledOpphørsdatoForAvkortetAndel(
        nyNeste: AndelData?,
        ny: AndelData,
    ) = if (nyNeste == null || nyNeste.fom != ny.tom.plusMonths(1)) {
        ny.tom.plusMonths(1)
    } else {
        null
    }

    fun AndelData.medOffsetOgKildebehandlingFra(forrige: AndelData): AndelData =
        this.copy(
            offset = forrige.offset,
            forrigeOffset = forrige.forrigeOffset,
            kildeBehandlingId = forrige.kildeBehandlingId,
        )
}
