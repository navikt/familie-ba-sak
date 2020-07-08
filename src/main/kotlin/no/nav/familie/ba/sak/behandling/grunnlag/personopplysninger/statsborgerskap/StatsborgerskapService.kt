package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.personinfo.Ident
import no.nav.familie.kontrakter.felles.personinfo.Statsborgerskap
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class StatsborgerskapService(
        private val integrasjonClient: IntegrasjonClient
) {

    val fomComparator =
            Comparator { stb1: GrStatsborgerskap, stb2: GrStatsborgerskap
                ->
                val fom1 = stb1.gyldigPeriode?.fom ?: null
                val fom2 = stb2.gyldigPeriode?.fom ?: null

                if (fom1 == null && fom2 == null) {
                    return@Comparator 0
                } else if (fom1 == null) {
                    return@Comparator -1;
                } else if (fom2 == null) {
                    return@Comparator 1;
                }
                fom1.compareTo(fom2)
            }

    fun hentStatsborgerskapMedMedlemskapOgHistorikk(ident: Ident, person: Person): List<GrStatsborgerskap> =
            integrasjonClient.hentStatsborgerskap(ident).flatMap {
                hentStatsborgerskapMedMedlemskap(it, person)
            }.sortedWith(fomComparator)

    private fun hentStatsborgerskapMedMedlemskap(statsborgerskap: Statsborgerskap, person: Person): List<GrStatsborgerskap> {
        if (erNordisk(statsborgerskap.land)) {
            return listOf(GrStatsborgerskap(gyldigPeriode = DatoIntervallEntitet(fom = statsborgerskap.gyldigFraOgMed,
                                                                                 tom = statsborgerskap.gyldigTilOgMed),
                                            landkode = statsborgerskap.land,
                                            medlemskap = Medlemskap.NORDEN,
                                            person = person))
        }

        val perioderSomEØSLand = integrasjonClient.hentAlleEØSLand().betydninger[statsborgerskap.land]

        val grStatsborgerskap = ArrayList<GrStatsborgerskap>()
        var datoFra = statsborgerskap.gyldigFraOgMed

        while (true) {
            val datoTil = finnDatoTil(perioderSomEØSLand, datoFra, statsborgerskap)

            grStatsborgerskap += GrStatsborgerskap(gyldigPeriode = DatoIntervallEntitet(fom = datoFra, tom = datoTil),
                                                   landkode = statsborgerskap.land,
                                                   medlemskap = finnMedlemskap(statsborgerskap, perioderSomEØSLand, datoFra),
                                                   person = person)

            if (datoTil == statsborgerskap.gyldigTilOgMed) break;

            datoFra = datoTil?.plusDays(1)
        }

        return grStatsborgerskap
    }

    private fun finnDatoTil(eøsLand: List<BetydningDto>?, datoFra: LocalDate?, statsborgerskap: Statsborgerskap): LocalDate? =
            // Finn neste dato i statsborgerskaps medlemskaps-intervall,
            // det er enten start stopp på medlemsintervall eller start stop på statsborgerskaps intervall.
            eøsLand?.flatMap { listOf(it.gyldigFra.minusDays(1), it.gyldigTil) }
                    // fjern datum som betegner uendelighet i kodeverk (9999-01-01 og 1900-01-01)
                    ?.filter { it.isAfter(LocalDate.parse("1900-01-02")) &&
                                it.isBefore(LocalDate.parse("9990-01-01"))}

                    // Dato til - skal være etter dato fra men før statsborgerskapets siste gyldige dag.
                    // Statsborgerskap null er samme som uendelighet.
                    ?.firstOrNull {
                         datoFra == null ||
                         (it > datoFra &&
                        (statsborgerskap.gyldigTilOgMed == null || it < statsborgerskap.gyldigTilOgMed))
                    }
            ?: statsborgerskap.gyldigTilOgMed

    private fun finnMedlemskap(statsborgerskap: Statsborgerskap,
                               perioderEØSLand: List<BetydningDto>?,
                               gyldigFraOgMed: LocalDate?): Medlemskap =
            when {
                erNordisk(statsborgerskap.land) -> Medlemskap.NORDEN
                erEØS(perioderEØSLand, gyldigFraOgMed) -> Medlemskap.EØS
                erTredjeland(statsborgerskap.land) -> Medlemskap.TREDJELANDSBORGER
                else -> Medlemskap.UKJENT
            }

    private fun erEØS(perioderEØSLand: List<BetydningDto>?,
                      fraDato: LocalDate?): Boolean =
            perioderEØSLand?.any { fraDato == null || (it.gyldigFra <= fraDato &&
                                   it.gyldigTil >= fraDato) } ?: false

    private fun erNordisk(landkode: String): Boolean = Norden.values().map { it.name }.contains(landkode)

    private fun erTredjeland(landkode: String): Boolean = landkode != LANDKODE_UKJENT

    companion object {
        const val LANDKODE_UKJENT = "XUK"
    }
}

enum class Norden {
    NOR,
    SWE,
    FIN,
    DNK,
    ISL,
    FRO,
    GRL,
    ALA
}