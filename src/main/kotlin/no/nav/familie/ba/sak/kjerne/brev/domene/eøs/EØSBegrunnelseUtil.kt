package no.nav.familie.ba.sak.kjerne.brev.domene.eøs

import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hentBarnetsBostedslandFraLandkode

fun hentKompetanserForEØSBegrunnelse(
    eøsBegrunnelseMedTriggere: EØSBegrunnelseMedTriggere,
    minimerteKompetanser: List<MinimertKompetanse>
) =
    minimerteKompetanser.filter { minimertKompetanse ->
        eøsBegrunnelseMedTriggere.sanityEØSBegrunnelse.annenForeldersAktivitet
            .contains(minimertKompetanse.annenForeldersAktivitet) &&
            eøsBegrunnelseMedTriggere.sanityEØSBegrunnelse.barnetsBostedsland
                .contains(hentBarnetsBostedslandFraLandkode(minimertKompetanse.barnetsBostedsland)) &&
            eøsBegrunnelseMedTriggere.sanityEØSBegrunnelse.kompetanseResultat.contains(
                minimertKompetanse.resultat
            )
    }
