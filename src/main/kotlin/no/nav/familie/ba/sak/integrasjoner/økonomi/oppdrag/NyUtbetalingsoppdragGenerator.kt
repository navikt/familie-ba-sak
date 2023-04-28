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

        val identerMedType = nyeKjeder.keys + forrigeKjeder.keys
        var sisteOffset = sisteAndelPerKjede.values.mapNotNull { it.offset }.maxOrNull()
        val resultat = mutableListOf<AndelMedOffset>()
        val opphørsandeler = mutableListOf<Pair<AndelData, YearMonth>>()
        val nyeAndeler2 = mutableListOf<AndelData>()
        /*
        TODO validering av endretSimuleringsdato, burde ikke kunne være etter min fom på nye andeler
        TODO erSimulering
         */
        identerMedType.forEach { identOgType ->
            val forrigeAndeler = forrigeKjeder[identOgType] ?: emptyList()
            val nyeAndeler = nyeKjeder[identOgType] ?: emptyList()
            val sisteAndel = sisteAndelPerKjede[identOgType]
            // TODO må nog sende med endretMigreringsDato/erSimulering? her og
            val nyKjede = beregnNyKjede(forrigeAndeler, nyeAndeler, sisteAndel, sisteOffset)
            sisteOffset = nyKjede.sisteOffset
            resultat.addAll(
                nyKjede.beståendeAndeler.map { AndelMedOffset(it) } +
                    nyKjede.nyeAndeler.map { AndelMedOffset(it, behandlingsinformasjon.behandlingId) },
            )
            nyeAndeler2.addAll(nyKjede.nyeAndeler)

            if (nyKjede.opphør != null && sisteAndel != null) {
                opphørsandeler.add(Pair(sisteAndel, nyKjede.opphør))
            }
        }

        val utbetalingsoppdrag = Utbetalingsoppdrag(
            saksbehandlerId = behandlingsinformasjon.saksbehandlerId,
            kodeEndring = kodeEndring(forrigeAndeler),
            fagSystem = FAGSYSTEM,
            saksnummer = behandlingsinformasjon.fagsakId.toString(),
            aktoer = behandlingsinformasjon.aktør.aktivFødselsnummer(),
            utbetalingsperiode = listOf(
                lagUtbetalingsperioderForOpphør(behandlingsinformasjon, opphørsandeler),
                lagUtbetalingsperioderForOpprettelseOgOppdaterTilkjentYtelse(behandlingsinformasjon, nyeAndeler2),
            ).flatten(),
        )

        return UtbetalingsoppdragOgAndelerMedOffset(utbetalingsoppdrag, resultat)
    }

    // Hos økonomi skiller man på endring på oppdragsnivå 110 og på linjenivå 150 (periodenivå).
    // Da de har opplevd å motta
    // UEND på oppdrag som skulle vært ENDR anbefaler de at kun ENDR brukes når sak
    // ikke er ny, så man slipper å forholde seg til om det er endring over 150-nivå eller ikke.
    private fun kodeEndring(forrigeAndeler: List<AndelData>?) =
        if (forrigeAndeler == null) KodeEndring.NY else KodeEndring.ENDR

    data class ResultatForKjede(
        val beståendeAndeler: List<AndelData>,
        val nyeAndeler: List<AndelData>,
        val opphør: YearMonth?,
        val sisteOffset: Long?, // nullable? burde ikke kunne være nullable?
    )

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
        val nyeAndelerMedOffset = nyeAndeler.mapIndexed { index, andelData ->
            gjeldendeOffset += 1
            val nyAndel = andelData.copy(offset = gjeldendeOffset, forrigeOffset = forrigeOffset)
            forrigeOffset = gjeldendeOffset
            nyAndel
        }
        return ResultatForKjede(
            beståendeAndeler = beståendeAndeler.andeler,
            nyeAndeler = nyeAndelerMedOffset,
            opphør = beståendeAndeler.opphørFra,
            sisteOffset = gjeldendeOffset,
        )
    }

    private fun List<AndelData>.groupByIdentOgType(): Map<IdentOgType, List<AndelData>> =
        groupBy { IdentOgType(it.ident, it.type) }

    private fun lagUtbetalingsperioderForOpphør(
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

    private fun lagUtbetalingsperioderForOpprettelseOgOppdaterTilkjentYtelse(
        behandlingsinformasjon: Behandlingsinformasjon,
        andeler: List<AndelData>,
    ): List<Utbetalingsperiode> {
        val utbetalingsperiodeMal = UtbetalingsperiodeMal(
            vedtak = behandlingsinformasjon.vedtak,
        )
        return andeler.map { utbetalingsperiodeMal.lagPeriodeFraAndel(it) }
    }
}
