package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse

fun hentFolketrygdlovenHjemler(
    sanityStandardbegrunnelser: List<SanityBegrunnelse>,
    sanityEøsBegrunnelser: List<SanityEØSBegrunnelse>,
) =
    (sanityStandardbegrunnelser.flatMap { it.hjemlerFolketrygdloven } + sanityEøsBegrunnelser.flatMap { it.hjemlerFolketrygdloven })
        .distinct()
