package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import java.time.LocalDate

data class RestBeregningOversikt(
        val periodeFom: LocalDate?,
        val periodeTom: LocalDate?,
        val sakstype: BehandlingKategori,
        val beregningDetaljer: List<RestBeregningDetalj>,
        val ytelseTyper: List<YtelseType>,
        val antallBarn: Int,
        val utbetaltPerMnd: Int)

data class RestBeregningDetalj(
        val person: RestPerson,
        val ytelseType: YtelseType,
        val utbetaltPerMnd: Int,
        val endring: Boolean,
)
