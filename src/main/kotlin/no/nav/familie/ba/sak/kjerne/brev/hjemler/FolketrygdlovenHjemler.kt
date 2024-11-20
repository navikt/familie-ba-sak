package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse

fun hentFolketrygdlovenHjemler(
    sanitybegrunnelser: List<SanityBegrunnelse>,
    sanityEøsBegrunnelser: List<SanityEØSBegrunnelse>,
): List<String> {
    val hjemlerFolketrygdloven = sanitybegrunnelser.flatMap { it.hjemlerFolketrygdloven }
    val hjemlerFolketrygdlovenEøs = sanityEøsBegrunnelser.flatMap { it.hjemlerFolketrygdloven }
    return (hjemlerFolketrygdloven + hjemlerFolketrygdlovenEøs).distinct()
}
