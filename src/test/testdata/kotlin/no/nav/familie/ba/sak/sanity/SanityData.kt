package no.nav.familie.ba.sak.sanity

import no.nav.familie.ba.sak.kjerne.brev.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.RestSanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.testfiler.Testfil.SANITY_BEGRUNNELSER
import no.nav.familie.ba.sak.testfiler.Testfil.SANITY_EØS_BEGRUNNELSER
import no.nav.familie.kontrakter.felles.objectMapper

// For å laste ned begrunnelsene kjør scriptet "src/test/resources/oppdater-sanity-mock.sh" eller
// se https://familie-brev.sanity.studio/ba-brev/vision med query fra SanityQueries.kt.
object SanityData {
    fun hentSanityBegrunnelser(): List<SanityBegrunnelse> {
        val restSanityBegrunnelser =
            objectMapper
                .readValue(SANITY_BEGRUNNELSER, Array<RestSanityBegrunnelse>::class.java)
                .toList()

        return restSanityBegrunnelser.mapNotNull { it.tilSanityBegrunnelse() }
    }

    fun hentSanityBegrunnelserMap(): Map<Standardbegrunnelse, SanityBegrunnelse> {
        val sanityBegrunnelser = hentSanityBegrunnelser()
        val enumPåApiNavn = Standardbegrunnelse.entries.associateBy { it.sanityApiNavn }

        return sanityBegrunnelser
            .mapNotNull {
                val begrunnelseEnum = enumPåApiNavn[it.apiNavn]
                if (begrunnelseEnum == null) {
                    null
                } else {
                    begrunnelseEnum to it
                }
            }.toMap()
    }

    fun hentSanityEØSBegrunnelser(): List<SanityEØSBegrunnelse> {
        val restSanityEØSBegrunnelser =
            objectMapper
                .readValue(
                    SANITY_EØS_BEGRUNNELSER,
                    Array<RestSanityEØSBegrunnelse>::class.java,
                ).toList()

        return restSanityEØSBegrunnelser.mapNotNull { it.tilSanityEØSBegrunnelse() }
    }

    fun hentSanityEØSBegrunnelserMap(): Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse> {
        val sanityEØSBegrunnelser = hentSanityEØSBegrunnelser()
        val enumPåApiNavn = EØSStandardbegrunnelse.entries.associateBy { it.sanityApiNavn }

        return sanityEØSBegrunnelser
            .mapNotNull {
                val begrunnelseEnum = enumPåApiNavn[it.apiNavn]
                if (begrunnelseEnum == null) {
                    null
                } else {
                    begrunnelseEnum to it
                }
            }.toMap()
    }
}
