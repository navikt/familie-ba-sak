package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import java.time.LocalDate

fun lagGrStatsborgerskap(
    person: Person = tilfeldigPerson(),
    landkode: String = "NO",
    medlemskap: Medlemskap = Medlemskap.NORDEN,
    gyldigFraOgMed: LocalDate? = LocalDate.now().minusYears(10),
    gyldigTilOgMed: LocalDate? = null,
): GrStatsborgerskap =
    GrStatsborgerskap(
        landkode = landkode,
        gyldigPeriode = DatoIntervallEntitet(gyldigFraOgMed, gyldigTilOgMed),
        medlemskap = medlemskap,
        person = person,
    )
