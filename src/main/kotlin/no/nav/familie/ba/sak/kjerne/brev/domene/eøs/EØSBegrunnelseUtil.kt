package no.nav.familie.ba.sak.kjerne.brev.domene.eøs

import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertKompetanse

fun hentKompetanserForEØSBegrunnelse(
    eøsBegrunnelseMedTriggere: EØSBegrunnelseMedTriggere,
    minimerteKompetanser: List<MinimertKompetanse>
) =
    minimerteKompetanser.filter { minimertKompetanse ->
        eøsBegrunnelseMedTriggere.sanityEØSBegrunnelse.annenForeldersAktivitet
            .contains(minimertKompetanse.annenForeldersAktivitet) &&
            eøsBegrunnelseMedTriggere.sanityEØSBegrunnelse.barnetsBostedsland.map { it.tilLandkode() }
                .contains(minimertKompetanse.barnetsBostedsland) &&
            eøsBegrunnelseMedTriggere.sanityEØSBegrunnelse.kompetanseResultat.contains(
                minimertKompetanse.resultat
            )
    }
