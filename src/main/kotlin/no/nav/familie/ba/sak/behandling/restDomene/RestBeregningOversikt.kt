package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import java.time.LocalDate

data class RestBeregningOversikt(
        val periodeFom: LocalDate,
        val periodeTom: LocalDate,
        val sakstype: BehandlingKategori,
        val beregningDetaljer: List<RestBeregningDetalj>,
        val ytelseTyper: List<YtelseType>,
        val antallBarn: Int,
        val utbetaltPerMnd: Int,
        val endring: BeregningEndring,
)

data class RestBeregningDetalj(
        val person: RestPerson,
        val ytelseType: YtelseType,
        val utbetaltPerMnd: Int,
)

data class BeregningEndring(
        val type: BeregningEndringType,
        val trengerBegrunnelse: Boolean = true
)

enum class BeregningEndringType {
    ENDRET,
    ENDRET_SATS, // Skal trigges som vanlig endring, men med satt begrunnelse
    UENDRET,
    UENDRET_SATS, // Skal ikke anses som endring, men visning av begrunnelse må spesialhåndteres
}