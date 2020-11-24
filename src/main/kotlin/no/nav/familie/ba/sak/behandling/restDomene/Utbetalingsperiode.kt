package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import java.time.LocalDate

/**
 * Dataklasser som brukes til frontend og backend når man jobber med vertikale utbetalingsperioder
 */
data class Utbetalingsperiode(
        val periodeFom: LocalDate,
        val periodeTom: LocalDate,
        val sakstype: BehandlingKategori,
        val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj>,
        val ytelseTyper: List<YtelseType>,
        val antallBarn: Int,
        val utbetaltPerMnd: Int,
)

data class UtbetalingsperiodeDetalj(
        val person: RestPerson,
        val ytelseType: YtelseType,
        val utbetaltPerMnd: Int,
)