package no.nav.familie.ba.sak.behandling.domene.vedtak

import java.time.LocalDate

data class NyttVedtak(
        val resultat: VedtakResultat
)

data class NyBeregning(
        val barnasBeregning: Array<BarnBeregning>
)

data class BarnBeregning(
        val fødselsnummer: String,
        val beløp: Int,
        val stønadFom: LocalDate
)