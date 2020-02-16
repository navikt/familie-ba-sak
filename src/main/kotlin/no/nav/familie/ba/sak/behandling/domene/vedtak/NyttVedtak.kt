package no.nav.familie.ba.sak.behandling.domene.vedtak

import java.time.LocalDate

data class NyttVedtak(
        val resultat: VedtakResultat
)

data class OpphørVedtak(
        val opphørsdato: LocalDate
)

data class NyBeregning(
        val personberegninger: Array<PersonBeregning>
)

data class PersonBeregning(
        val fødselsnummer: String,
        val beløp: Int,
        val stønadFom: LocalDate,
        val personberegningType : PersonBeregningType
)

enum class PersonBeregningType {
    ORDINÆR_BARNETRYGD,
    UTVIDET_BARNETRYGD,
    SMÅBARNSTILLEGG
}