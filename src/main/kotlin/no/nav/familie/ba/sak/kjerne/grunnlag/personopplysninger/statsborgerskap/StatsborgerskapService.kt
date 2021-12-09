package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class StatsborgerskapService(
    private val integrasjonClient: IntegrasjonClient,
    private val personopplysningerService: PersonopplysningerService
) {

    fun hentLand(landkode: String): String = integrasjonClient.hentLand(landkode)

    val fomComparator =
        Comparator { stb1: GrStatsborgerskap, stb2: GrStatsborgerskap
            ->
            val fom1 = stb1.gyldigPeriode?.fom
            val fom2 = stb2.gyldigPeriode?.fom

            if (fom1 == null && fom2 == null) {
                return@Comparator 0
            } else if (fom1 == null) {
                return@Comparator -1
            } else if (fom2 == null) {
                return@Comparator 1
            }
            fom1.compareTo(fom2)
        }

    /**
     * Denne brukes kun i tester, men kan være relevant dersom man skal
     * implementere automatisk vurdering av lovlig opphold vilkår.
     *
     * I tillegg har vi erfart at datakvaliteten på periode på statsborgerskap er
     * ganske dårlig og koden under som ser på perioder kan derfor være utdatert
     * og bør gås opp igjen om man skal bruke det.
     */
    fun hentStatsborgerskapMedMedlemskapOgHistorikk(aktør: Aktør, person: Person): List<GrStatsborgerskap> =
        listOf(personopplysningerService.hentGjeldendeStatsborgerskap(aktør)).flatMap { statsborgerskap: Statsborgerskap ->
            hentStatsborgerskapMedMedlemskap(statsborgerskap, person)
        }.sortedWith(fomComparator)

    private fun hentStatsborgerskapMedMedlemskap(
        statsborgerskap: Statsborgerskap,
        person: Person
    ): List<GrStatsborgerskap> {
        if (statsborgerskap.iNordiskLand()) {
            return listOf(
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(
                        fom = statsborgerskap.gyldigFraOgMed,
                        tom = statsborgerskap.gyldigTilOgMed
                    ),
                    landkode = statsborgerskap.land,
                    medlemskap = Medlemskap.NORDEN,
                    person = person
                )
            )
        }

        val alleEØSLandInkludertHistoriske = integrasjonClient.hentAlleEØSLand().betydninger[statsborgerskap.land]
        val grStatsborgerskap = ArrayList<GrStatsborgerskap>()
        var datoFra = statsborgerskap.gyldigFraOgMed

        hentMedlemskapsIntervaller(
            alleEØSLandInkludertHistoriske,
            statsborgerskap.gyldigFraOgMed,
            statsborgerskap.gyldigTilOgMed
        )
            .forEach {
                grStatsborgerskap += GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(
                        fom = datoFra,
                        tom = it.minusDays(1)
                    ),
                    landkode = statsborgerskap.land,
                    medlemskap = finnMedlemskap(
                        statsborgerskap,
                        alleEØSLandInkludertHistoriske,
                        datoFra
                    ),
                    person = person
                )
                datoFra = it
            }

        grStatsborgerskap += GrStatsborgerskap(
            gyldigPeriode = DatoIntervallEntitet(
                fom = datoFra,
                tom = statsborgerskap.gyldigTilOgMed
            ),
            landkode = statsborgerskap.land,
            medlemskap = finnMedlemskap(
                statsborgerskap,
                alleEØSLandInkludertHistoriske,
                datoFra
            ),
            person = person
        )

        return grStatsborgerskap
    }

    private fun hentMedlemskapsIntervaller(
        eøsLand: List<BetydningDto>?,
        fra: LocalDate?,
        til: LocalDate?
    ): List<LocalDate> =
        eøsLand?.flatMap {
            listOf(it.gyldigFra, it.gyldigTil.plusDays(1))
        }?.filter { datoForEndringIMedlemskap ->
            erInnenforDatoerSomBetegnerUendelighetIKodeverk(datoForEndringIMedlemskap)
        }?.filter { datoForEndringIMedlemskap ->
            (fra == null || datoForEndringIMedlemskap.isAfter(fra)) &&
                (til == null || datoForEndringIMedlemskap.isBefore(til))
        } ?: emptyList()

    private fun finnMedlemskap(
        statsborgerskap: Statsborgerskap,
        perioderEØSLand: List<BetydningDto>?,
        gyldigFraOgMed: LocalDate?
    ): Medlemskap =
        when {
            statsborgerskap.iNordiskLand() -> Medlemskap.NORDEN
            erEØS(perioderEØSLand, gyldigFraOgMed) -> Medlemskap.EØS
            statsborgerskap.iTredjeland() -> Medlemskap.TREDJELANDSBORGER
            statsborgerskap.erStatsløs() -> Medlemskap.STATSLØS
            else -> Medlemskap.UKJENT
        }

    private fun erEØS(
        perioderEØSLand: List<BetydningDto>?,
        fraDato: LocalDate?
    ): Boolean =
        perioderEØSLand?.any {
            fraDato == null || (
                it.gyldigFra <= fraDato &&
                    it.gyldigTil >= fraDato
                )
        } ?: false

    private fun erInnenforDatoerSomBetegnerUendelighetIKodeverk(dato: LocalDate) =
        dato.isAfter(TIDLIGSTE_DATO_I_KODEVERK) && dato.isBefore(SENESTE_DATO_I_KODEVERK)

    companion object {

        const val LANDKODE_UKJENT = "XUK"
        const val LANDKODE_STATSLØS = "XXX"
        val TIDLIGSTE_DATO_I_KODEVERK: LocalDate = LocalDate.parse("1900-01-02")
        val SENESTE_DATO_I_KODEVERK: LocalDate = LocalDate.parse("9990-01-01")
    }
}

fun Statsborgerskap.iNordiskLand() = Norden.values().map { it.name }.contains(this.land)

fun Statsborgerskap.iTredjeland() = this.land != StatsborgerskapService.LANDKODE_UKJENT

fun Statsborgerskap.erStatsløs() = this.land == StatsborgerskapService.LANDKODE_STATSLØS

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
