package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.kjerne.dokument.BrevKlient
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelseType
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
@Profile("mock-brev-klient")
@Primary
class BrevKlientMock : BrevKlient(
        familieBrevUri = "brev_uri_mock",
        restTemplate = RestTemplate()
) {

        override fun genererBrev(målform: String, brev: Brev): ByteArray {
                return TEST_PDF
        }

        override fun hentSanityBegrunnelse(): List<SanityBegrunnelse> {
                return navnTilNedtrekksmenyMock
        }
}

val navnTilNedtrekksmenyMock: List<SanityBegrunnelse> =
        VedtakBegrunnelseSpesifikasjon.values()
                .map {
                        SanityBegrunnelse(apiNavn = it.sanityApiNavn,
                                          navnISystem = it.name,
                                          begrunnelsetype = it.vedtakBegrunnelseType.tilSanityBegrunnelseType())
                }

fun VedtakBegrunnelseType.tilSanityBegrunnelseType() = when (this) {
        VedtakBegrunnelseType.INNVILGELSE -> SanityBegrunnelseType.INNVILGELSE
        VedtakBegrunnelseType.REDUKSJON -> SanityBegrunnelseType.REDUKSJON
        VedtakBegrunnelseType.AVSLAG -> SanityBegrunnelseType.AVSLAG
        VedtakBegrunnelseType.OPPHØR -> SanityBegrunnelseType.OPPHØR
        VedtakBegrunnelseType.FORTSATT_INNVILGET -> SanityBegrunnelseType.FORTSATT_INNVILGET
}