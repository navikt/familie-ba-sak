package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.iNordiskLand
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner

fun PdlRestKlient.lagErNordiskStatsborgerTidslinje(personResultat: PersonResultat): Tidslinje<Boolean> { // todo fjerne
    val statsborgerskapGruppertPåNavn =
        hentStatsborgerskap(personResultat.aktør, historikk = true)
            .groupBy { it.land }
            .mapValues { (_, perLand) ->
                val unikeStatsborgerskapInnslag = perLand.distinct()
                val innslagMedDato = unikeStatsborgerskapInnslag.filter { it.gyldigFraOgMed != null || it.gyldigTilOgMed != null }

                innslagMedDato.ifEmpty { unikeStatsborgerskapInnslag }
            }

    return statsborgerskapGruppertPåNavn.values
        .map { statsborgerskapSammeLand ->
            statsborgerskapSammeLand
                .map { Periode(it, it.gyldigFraOgMed, it.gyldigTilOgMed) }
                .tilTidslinje()
        }.kombiner { statsborgerskap -> statsborgerskap.any { it.iNordiskLand() } }
}

fun lagErNordiskStatsborgerTidslinje(personopplysningGrunnlag: PersonopplysningGrunnlag): Tidslinje<Boolean> {
    val statsborgerskapGruppertPåNavn =
        personopplysningGrunnlag.søker.statsborgerskap
            .groupBy { it.landkode }
            .mapValues { (_, perLand) ->
                val unikeStatsborgerskapInnslag = perLand.distinct()
                val innslagMedDato = unikeStatsborgerskapInnslag.filter { it.gyldigPeriode?.fom != null || it.gyldigPeriode?.tom != null }

                innslagMedDato.ifEmpty { unikeStatsborgerskapInnslag }
            }

    return statsborgerskapGruppertPåNavn.values
        .map { statsborgerskapSammeLand ->
            statsborgerskapSammeLand
                .map { Periode(it, it.gyldigPeriode?.fom, it.gyldigPeriode?.tom) }
                .tilTidslinje()
        }.kombiner { statsborgerskap -> statsborgerskap.any { it.iNordiskLand() } }
}
