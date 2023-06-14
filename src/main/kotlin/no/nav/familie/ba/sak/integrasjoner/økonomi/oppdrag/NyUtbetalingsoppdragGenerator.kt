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
    ): UtbetalingsoppdragOgAndelerMedOffset {/*
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
            val opphørsdato = finnOpphørsdato(forrigeAndeler, nyeAndeler)

            // TODO må nog sende med endretMigreringsDato/erSimulering? her og (eller opphørsdato?)
            val nyKjede = beregnNyKjede(
                forrigeAndeler.uten0beløp(), nyeAndeler.uten0beløp(), sisteAndel, sisteOffset, opphørsdato
            )
            sisteOffset = nyKjede.sisteOffset
            nyKjede
        }
    }

    /**
     * Må håndtere det at man simulererer / endrer migreringsdato / første andel begynner med 0-beløp
     * Hva skjer når det er kombinasjon av disse?
     * Hvordan håndterer man eks migrering av
     */
    private fun finnOpphørsdato(forrigeAndeler: List<AndelData>, nyeAndeler: List<AndelData>): YearMonth? {
        // TODO erSImulering / endretMigreringsdato
        // valider at endretMigreringsdato < forrigeAndeler/nyeAndeler ?
        return finnOpphørsdatoPga0Beløp(forrigeAndeler, nyeAndeler)
    }

    private fun finnOpphørsdatoPga0Beløp(forrigeAndeler: List<AndelData>, nyeAndeler: List<AndelData>): YearMonth? {
        val forrigeFørsteAndel = forrigeAndeler.firstOrNull()
        val nyFørsteAndel = nyeAndeler.firstOrNull()
        if (
            forrigeFørsteAndel != null && nyFørsteAndel != null &&
            nyFørsteAndel.beløp == 0 && nyFørsteAndel.fom < forrigeFørsteAndel.fom
        ) {
            return nyFørsteAndel.fom
        }
        return null
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
        nyKjede.beståendeAndeler.map { AndelMedOffset(it) } + nyKjede.nyeAndeler.map {
            AndelMedOffset(
                it,
                behandlingsinformasjon.behandlingId
            )
        }
    }

    // Hos økonomi skiller man på endring på oppdragsnivå 110 og på linjenivå 150 (periodenivå).
    // Da de har opplevd å motta
    // UEND på oppdrag som skulle vært ENDR anbefaler de at kun ENDR brukes når sak
    // ikke er ny, så man slipper å forholde seg til om det er endring over 150-nivå eller ikke.
    private fun kodeEndring(forrigeAndeler: List<AndelData>?) =
        if (forrigeAndeler == null) KodeEndring.NY else KodeEndring.ENDR

    /**
     * Hvis vi har et opphørsdato så
     */
    private fun beregnNyKjede(
        forrige: List<AndelData>,
        nye: List<AndelData>,
        sisteAndel: AndelData?,
        offset: Long?,
        opphørHeleKjedenFra: YearMonth?,
    ): ResultatForKjede {
        return if (opphørHeleKjedenFra != null) {
            beregnNyKjedeMedOpphørsdato(forrige, nye, offset, sisteAndel, opphørHeleKjedenFra)
        } else {
            beregnNyKjede(forrige, nye, offset, sisteAndel)
        }
    }

    private fun beregnNyKjedeMedOpphørsdato(
        forrige: List<AndelData>, nye: List<AndelData>, offset: Long?, sisteAndel: AndelData?, opphørsdato: YearMonth
    ): ResultatForKjede {
        val (nyeAndelerMedOffset, gjeldendeOffset) = nyeAndelerMedOffset(nye, offset, sisteAndel)
        val opphørsandel = sisteAndel?.let {
            forrige.firstOrNull()?.let {
                if (opphørsdato > it.fom) error("Opphørsdato=$opphørsdato må være før første andelen sitt fom=${it.fom}")
            }
            Pair(it, opphørsdato)
        }
        return ResultatForKjede(
            beståendeAndeler = emptyList(),
            nyeAndeler = nyeAndelerMedOffset,
            opphørsandel = opphørsandel,
            sisteOffset = gjeldendeOffset
        )
    }

    private fun beregnNyKjede(
        forrige: List<AndelData>, nye: List<AndelData>, offset: Long?, sisteAndel: AndelData?
    ): ResultatForKjede {
        val beståendeAndeler = finnBeståendeAndeler(forrige, nye)
        val nyeAndeler = nye.subList(beståendeAndeler.andeler.size, nye.size)

        val (nyeAndelerMedOffset, gjeldendeOffset) = nyeAndelerMedOffset(nyeAndeler, offset, sisteAndel)
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

    private fun nyeAndelerMedOffset(
        nyeAndeler: List<AndelData>, offset: Long?, sisteAndel: AndelData?
    ): Pair<List<AndelData>, Long> {
        var gjeldendeOffset = offset ?: -1
        var forrigeOffset = sisteAndel?.offset
        val nyeAndelerMedOffset = nyeAndeler.mapIndexed { index, andelData ->
            gjeldendeOffset += 1
            val nyAndel = andelData.copy(offset = gjeldendeOffset, forrigeOffset = forrigeOffset)
            forrigeOffset = gjeldendeOffset
            nyAndel
        }
        return Pair(nyeAndelerMedOffset, gjeldendeOffset)
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
