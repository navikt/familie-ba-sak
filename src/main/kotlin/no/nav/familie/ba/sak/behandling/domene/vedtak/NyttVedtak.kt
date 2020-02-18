package no.nav.familie.ba.sak.behandling.domene.vedtak

import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import java.time.LocalDate

data class NyttVedtak(
        val resultat: VedtakResultat,
        val samletVilkårResultat: List<RestVilkårResultat>,
        val begrunnelse: String
)

data class NyBeregning(
        val barnasBeregning: Array<BarnBeregning>
)

data class BarnBeregning(
        val fødselsnummer: String,
        val beløp: Int,
        val stønadFom: LocalDate
)