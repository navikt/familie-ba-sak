package no.nav.familie.ba.sak.behandling.domene.vedtak

import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import java.time.LocalDate

data class NyttVedtak(
        val resultat: VedtakResultat,
        val samletVilkårResultat: List<RestVilkårResultat>
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
        val ytelsetype : Ytelsetype
)

enum class Ytelsetype {
    ORDINÆR_BARNETRYGD,
    UTVIDET_BARNETRYGD,
    SMÅBARNSTILLEGG
}