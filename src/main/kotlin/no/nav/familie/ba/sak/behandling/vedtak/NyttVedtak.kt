package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import java.time.LocalDate

data class NyttVedtak(
        val resultat: VedtakResultat,
        val samletVilkårResultat: List<RestVilkårResultat>,
        val begrunnelse: String
)

data class Opphørsvedtak(
        val opphørsdato: LocalDate
)