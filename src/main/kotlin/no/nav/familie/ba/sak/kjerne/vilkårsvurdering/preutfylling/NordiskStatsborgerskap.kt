package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.iNordiskLand
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner

fun PdlRestClient.lagErNordiskStatsborgerTidslinje(personResultat: PersonResultat): Tidslinje<Boolean> {
    val statsborgerskapGruppertPåNavn =
        this
            .hentStatsborgerskap(personResultat.aktør, historikk = true)
            .groupBy { it.land }

    return statsborgerskapGruppertPåNavn.values
        .map { statsborgerskapSammeLand ->
            statsborgerskapSammeLand
                .map { Periode(it, it.gyldigFraOgMed, it.gyldigTilOgMed) }
                .tilTidslinje()
        }.kombiner { statsborgerskap -> statsborgerskap.any { it.iNordiskLand() } }
}
