package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse

fun utledSeprasjonsavtaleStorbritanniaHjemler(sanityEøsBegrunnelser: List<SanityEØSBegrunnelse>) =
    sanityEøsBegrunnelser
        .flatMap { it.hjemlerSeperasjonsavtalenStorbritannina }
        .distinct()
