package no.nav.familie.ba.sak.kjerne.brev.domene.eøs

import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat

fun hentKompetanserForEØSBegrunnelse(
    eøsBegrunnelseMedTriggere: EØSBegrunnelseMedTriggere,
    minimerteKompetanser: List<MinimertKompetanse>
) =
    minimerteKompetanser.filter {
        eøsBegrunnelseMedTriggere.erGyldigForKompetanseMedData(
            annenForeldersAktivitetFraKompetanse = it.annenForeldersAktivitet,
            barnetsBostedslandFraKompetanse = it.barnetsBostedsland,
            resultatFraKompetanse = it.resultat
        )
    }

fun EØSBegrunnelseMedTriggere.erGyldigForKompetanseMedData(
    annenForeldersAktivitetFraKompetanse: AnnenForeldersAktivitet,
    barnetsBostedslandFraKompetanse: String,
    resultatFraKompetanse: KompetanseResultat,
) =
    sanityEØSBegrunnelse.annenForeldersAktivitet
        .contains(annenForeldersAktivitetFraKompetanse) &&
        sanityEØSBegrunnelse.barnetsBostedsland.map { it.tilLandkode() }
            .contains(barnetsBostedslandFraKompetanse) &&
        sanityEØSBegrunnelse.kompetanseResultat.contains(
            resultatFraKompetanse
        )
