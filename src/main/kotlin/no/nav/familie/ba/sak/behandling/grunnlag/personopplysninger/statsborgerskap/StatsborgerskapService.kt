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
            integrasjonClient.hentStatsborgerskap(ident).flatMap { statsborgerskap ->
                hentStatsborgerskapMedMedlemskap(statsborgerskap, person)
            }.sortedWith(fomComparator)

    private fun hentStatsborgerskapMedMedlemskap(statsborgerskap: Statsborgerskap, person: Person): List<GrStatsborgerskap> {
        if (statsborgerskap.iNordiskLand()) {
            return listOf(GrStatsborgerskap(gyldigPeriode = DatoIntervallEntitet(fom = statsborgerskap.gyldigFraOgMed,
                                                                                 tom = statsborgerskap.gyldigTilOgMed),
                                            landkode = statsborgerskap.land,
                                            medlemskap = Medlemskap.NORDEN,
                                            person = person))
        }

        val alleEØSLandInkludertHistoriske = integrasjonClient.hentAlleEØSLand().betydninger[statsborgerskap.land]
        val grStatsborgerskap = ArrayList<GrStatsborgerskap>()
        var datoFra = statsborgerskap.gyldigFraOgMed

        hentMedlemskapsIntervaller(alleEØSLandInkludertHistoriske, statsborgerskap.gyldigFraOgMed, statsborgerskap.gyldigTilOgMed)
                .forEach {
                    grStatsborgerskap += GrStatsborgerskap(gyldigPeriode = DatoIntervallEntitet(fom = datoFra,
                                                                                                tom = it.minusDays(1)),
                                                           landkode = statsborgerskap.land,
                                                           medlemskap = finnMedlemskap(statsborgerskap,
                                                                                       alleEØSLandInkludertHistoriske,
                                                                                       datoFra),
                                                           person = person)
                    datoFra = it
                }

        grStatsborgerskap += GrStatsborgerskap(gyldigPeriode = DatoIntervallEntitet(fom = datoFra,
                                                                                    tom = statsborgerskap.gyldigTilOgMed),
                                               landkode = statsborgerskap.land,
                                               medlemskap = finnMedlemskap(statsborgerskap,
                                                                           alleEØSLandInkludertHistoriske,
                                                                           datoFra),
                                               person = person)

        return grStatsborgerskap
    }

    private fun hentMedlemskapsIntervaller(eøsLand: List<BetydningDto>?, fra: LocalDate?, til: LocalDate?): List<LocalDate> =
            eøsLand?.flatMap {
                listOf(it.gyldigFra, it.gyldigTil.plusDays(1))
            }?.filter {datoForEndringIMedlemskap ->
                erInnenforDatoerSomBetegnerUendelighetIKodeverk(datoForEndringIMedlemskap)
            }?.filter { datoForEndringIMedlemskap ->
                (fra == null || datoForEndringIMedlemskap.isAfter(fra)) &&
                (til == null || datoForEndringIMedlemskap.isBefore(til))
            } ?: emptyList()


    private fun finnMedlemskap(statsborgerskap: Statsborgerskap,
                               perioderEØSLand: List<BetydningDto>?,
                               gyldigFraOgMed: LocalDate?): Medlemskap =
            when {
                statsborgerskap.iNordiskLand() -> Medlemskap.NORDEN
                erEØS(perioderEØSLand, gyldigFraOgMed) -> Medlemskap.EØS
                statsborgerskap.iTredjeland() -> Medlemskap.TREDJELANDSBORGER
                else -> Medlemskap.UKJENT
            }

    private fun erEØS(perioderEØSLand: List<BetydningDto>?,
                      fraDato: LocalDate?): Boolean =
            perioderEØSLand?.any {
                fraDato == null || (it.gyldigFra <= fraDato &&
                                    it.gyldigTil >= fraDato)
            } ?: false

    private fun erInnenforDatoerSomBetegnerUendelighetIKodeverk(dato: LocalDate) =
            dato.isAfter(TIDLIGSTE_DATO_I_KODEVERK) && dato.isBefore(SENESTE_DATO_I_KODEVERK)

    companion object {
        const val LANDKODE_UKJENT = "XUK"
        val TIDLIGSTE_DATO_I_KODEVERK: LocalDate = LocalDate.parse("1900-01-02")
        val SENESTE_DATO_I_KODEVERK: LocalDate = LocalDate.parse("9990-01-01")
    }
}

fun Statsborgerskap.iNordiskLand() = Norden.values().map { it.name }.contains(this.land)

fun Statsborgerskap.iTredjeland() = this.land != StatsborgerskapService.LANDKODE_UKJENT

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