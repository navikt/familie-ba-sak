package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hjemlerTilhørendeFritekst

fun utledOrdinæreHjemler(
    sanityBegrunnelser: List<SanityBegrunnelse>,
    sanityEøsBegrunnelser: List<SanityEØSBegrunnelse>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean,
    finnesVedtaksperiodeMedFritekst: Boolean,
): List<String> {
    val ordinæreHjemler = mutableSetOf<String>()
    ordinæreHjemler.addAll(sanityBegrunnelser.flatMap { it.hjemler })
    ordinæreHjemler.addAll(sanityEøsBegrunnelser.flatMap { it.hjemler })

    if (opplysningspliktHjemlerSkalMedIBrev) {
        val hjemlerNårOpplysningspliktIkkeOppfylt = listOf("17", "18")
        ordinæreHjemler.addAll(hjemlerNårOpplysningspliktIkkeOppfylt)
    }

    if (finnesVedtaksperiodeMedFritekst) {
        ordinæreHjemler.addAll(hjemlerTilhørendeFritekst.map { it.toString() })
    }

    return ordinæreHjemler
        .map { it.toInt() }
        .sorted()
        .map { it.toString() }
        .distinct()
}
