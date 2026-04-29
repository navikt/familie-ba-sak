package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.tidslinje.utvidelser.verdiPåTidspunkt
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class StatsborgerskapService(
    private val kodeverkService: KodeverkService,
    private val featureToggleService: FeatureToggleService,
) {
    fun hentLand(landkode: String): String = kodeverkService.hentLand(landkode)

    fun hentStatsborgerskapMedMedlemskap(
        statsborgerskap: Statsborgerskap,
        person: Person,
    ): List<GrStatsborgerskap> =
        lagMedlemskapTIdslinjeForStatsborgerskap(statsborgerskap)
            .beskjærFraOgMed(person.fødselsdato)
            .tilPerioderIkkeNull()
            .map {
                GrStatsborgerskap(
                    gyldigPeriode =
                        DatoIntervallEntitet(
                            fom = it.fom,
                            tom = it.tom,
                        ),
                    landkode = statsborgerskap.land,
                    medlemskap = it.verdi,
                    person = person,
                )
            }

    fun lagMedlemskapTIdslinjeForStatsborgerskap(
        statsborgerskap: Statsborgerskap,
    ): Tidslinje<Medlemskap> {
        val datoFra = statsborgerskap.hentFom()

        val eøsTidslinje = kodeverkService.hentEøsMedlemskapsTidslinje(statsborgerskap.land)

        val statsborgerskapTidslinje =
            Periode(
                statsborgerskap,
                fom = datoFra,
                tom = statsborgerskap.gyldigTilOgMed,
            ).tilTidslinje()

        return statsborgerskapTidslinje
            .kombinerMed(eøsTidslinje) { statsborgerskap, erEøsland ->
                statsborgerskap?.let { finnMedlemskap(it, erEøsland ?: false) }
            }
    }

    fun hentSterkesteMedlemskapVedTidspunkt(
        statsborgerskap: Statsborgerskap,
        tidspunkt: LocalDate = statsborgerskap.gyldigTilOgMed ?: LocalDate.now(),
    ): Medlemskap? = lagMedlemskapTIdslinjeForStatsborgerskap(statsborgerskap).verdiPåTidspunkt(tidspunkt)

    private fun finnMedlemskap(
        statsborgerskap: Statsborgerskap,
        erEøsland: Boolean,
    ): Medlemskap =
        when {
            statsborgerskap.iNordiskLand() -> Medlemskap.NORDEN
            erEøsland -> Medlemskap.EØS
            statsborgerskap.iTredjeland() -> Medlemskap.TREDJELANDSBORGER
            statsborgerskap.erStatsløs() -> Medlemskap.STATSLØS
            else -> Medlemskap.UKJENT
        }

    companion object {
        const val LANDKODE_UKJENT = "XUK"
        const val LANDKODE_STATSLØS = "XXX"
    }
}

fun Statsborgerskap.hentFom() = this.gyldigFraOgMed ?: this.bekreftelsesdato

fun Statsborgerskap.iNordiskLand() = Norden.entries.map { it.name }.contains(this.land)

fun Statsborgerskap.iTredjeland() = this.land != StatsborgerskapService.LANDKODE_UKJENT

fun Statsborgerskap.erStatsløs() = this.land == StatsborgerskapService.LANDKODE_STATSLØS

/**
 * Norge, Sverige, Finland, Danmark, Island, Grønland, Færøyene og Åland
 */
enum class Norden {
    NOR,
    SWE,
    FIN,
    DNK,
    ISL,
    FRO,
    GRL,
    ALA,
}
