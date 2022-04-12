package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class StatsborgerskapService(
    private val integrasjonClient: IntegrasjonClient
) {

    fun hentLand(landkode: String): String = integrasjonClient.hentLand(landkode)

    fun hentStatsborgerskapMedMedlemskap(
        statsborgerskap: Statsborgerskap,
        person: Person
    ): List<GrStatsborgerskap> {
        if (statsborgerskap.iNordiskLand()) {
            return listOf(
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(
                        fom = statsborgerskap.hentFom(),
                        tom = statsborgerskap.gyldigTilOgMed
                    ),
                    landkode = statsborgerskap.land,
                    medlemskap = Medlemskap.NORDEN,
                    person = person
                )
            )
        }

        val historiskEØSMedlemsskapForLand =
            integrasjonClient.hentAlleEØSLand().betydninger[statsborgerskap.land] ?: emptyList()

        var datoFra = statsborgerskap.hentFom()

        return if (datoFra == null && statsborgerskap.gyldigTilOgMed == null) {
            val idag = LocalDate.now()
            listOf(
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(
                        fom = idag,
                        tom = null
                    ),
                    landkode = statsborgerskap.land,
                    medlemskap = finnMedlemskap(
                        statsborgerskap,
                        historiskEØSMedlemsskapForLand,
                        idag
                    ),
                    person = person
                )
            )
        } else {
            hentMedlemskapsDatoIntervaller(
                historiskEØSMedlemsskapForLand,
                datoFra,
                statsborgerskap.gyldigTilOgMed
            ).fold(emptyList<GrStatsborgerskap>()) { medlemskapsperioder, periode ->
                val medlemskapsperiode = GrStatsborgerskap(
                    gyldigPeriode = periode,
                    landkode = statsborgerskap.land,
                    medlemskap = finnMedlemskap(
                        statsborgerskap,
                        historiskEØSMedlemsskapForLand,
                        periode.fom
                    ),
                    person = person
                )
                medlemskapsperioder + listOf(medlemskapsperiode)
            }
        }
    }

    fun gammelHentStatsborgerskapMedMedlemskap(
        statsborgerskap: Statsborgerskap,
        person: Person
    ): List<GrStatsborgerskap> {

        if (statsborgerskap.iNordiskLand()) {
            return listOf(
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(
                        fom = statsborgerskap.hentFom(),
                        tom = statsborgerskap.gyldigTilOgMed
                    ),
                    landkode = statsborgerskap.land,
                    medlemskap = Medlemskap.NORDEN,
                    person = person
                )
            )
        }

        val alleEØSLandInkludertHistoriske =
            integrasjonClient.hentAlleEØSLand().betydninger[statsborgerskap.land] ?: emptyList()
        val grStatsborgerskap = ArrayList<GrStatsborgerskap>()
        var datoFra = statsborgerskap.hentFom()

        if (datoFra == null && statsborgerskap.gyldigTilOgMed == null) {
            val idag = LocalDate.now()
            grStatsborgerskap += GrStatsborgerskap(
                gyldigPeriode = DatoIntervallEntitet(
                    fom = idag,
                    tom = null
                ),
                landkode = statsborgerskap.land,
                medlemskap = finnMedlemskap(
                    statsborgerskap,
                    alleEØSLandInkludertHistoriske,
                    idag
                ),
                person = person
            )
            return grStatsborgerskap
        }

        hentMedlemskapsIntervaller(
            alleEØSLandInkludertHistoriske,
            statsborgerskap.hentFom(),
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

    fun hentSterkesteMedlemskap(statsborgerskap: Statsborgerskap): Medlemskap? {
        if (statsborgerskap.iNordiskLand()) {
            return Medlemskap.NORDEN
        }

        val alleEØSLandInkludertHistoriske =
            integrasjonClient.hentAlleEØSLand().betydninger[statsborgerskap.land] ?: emptyList()
        var datoFra = statsborgerskap.hentFom()

        val alleMedlemskap = hentMedlemskapsIntervaller(
            alleEØSLandInkludertHistoriske,
            statsborgerskap.hentFom(),
            statsborgerskap.gyldigTilOgMed
        ).fold(mutableSetOf<Medlemskap>()) { acc, dato ->
            acc.add(
                finnMedlemskap(
                    statsborgerskap,
                    alleEØSLandInkludertHistoriske,
                    datoFra
                )
            )
            datoFra = dato
            acc
        }

        alleMedlemskap.add(
            finnMedlemskap(
                statsborgerskap,
                alleEØSLandInkludertHistoriske,
                datoFra
            )
        )

        return finnSterkesteMedlemskap(alleMedlemskap.toList())
    }

    private fun hentMedlemskapsIntervaller(
        eøsLand: List<BetydningDto>,
        fra: LocalDate?,
        til: LocalDate?
    ): List<LocalDate> =
        eøsLand.flatMap {
            listOf(it.gyldigFra, it.gyldigTil.plusDays(1))
        }.filter { datoForEndringIMedlemskap ->
            erInnenforDatoerSomBetegnerUendelighetIKodeverk(datoForEndringIMedlemskap)
        }.filter { datoForEndringIMedlemskap ->
            (fra == null || datoForEndringIMedlemskap.isAfter(fra)) &&
                (til == null || datoForEndringIMedlemskap.isBefore(til))
        }

    private fun hentMedlemskapsDatoIntervaller(
        medlemsland: List<BetydningDto>,
        fra: LocalDate?,
        til: LocalDate?
    ): List<DatoIntervallEntitet> {
        val filtrerteEndringsdatoer = medlemsland.flatMap {
            listOf(it.gyldigFra, it.gyldigTil)
        }.filter { datoForEndringIMedlemskap ->
            erInnenforDatoerSomBetegnerUendelighetIKodeverk(datoForEndringIMedlemskap)
        }

        return listOf(
            fra,
            filtrerteEndringsdatoer.first(),
            filtrerteEndringsdatoer.lastOrNull(),
            til
        ).filterNotNull().windowed(2, 1)
            .map { DatoIntervallEntitet(it[0], it[1]) }
    }

    fun testHentMedlemskapsIntervaller(
        medlemsland: List<BetydningDto>,
        fra: LocalDate?,
        til: LocalDate?
    ): List<DatoIntervallEntitet> {
        val filtrerteEndringsdatoerMedlemskap = medlemsland.flatMap {
            listOf(it.gyldigFra, it.gyldigTil)
        }.filter { datoForEndringIMedlemskap ->
            erInnenforDatoerSomBetegnerUendelighetIKodeverk(datoForEndringIMedlemskap)
        }

        return listOf(
            fra,
            filtrerteEndringsdatoerMedlemskap.first(),
            filtrerteEndringsdatoerMedlemskap.lastOrNull(),
            til
        ).filterNotNull().windowed(
            2,
            1
        )
            .map { DatoIntervallEntitet(it[0], it[1]) }
    }

    private fun finnMedlemskap(
        statsborgerskap: Statsborgerskap,
        perioderEØSLand: List<BetydningDto>,
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
        perioderEØSLand: List<BetydningDto>,
        fraDato: LocalDate?
    ): Boolean =
        perioderEØSLand.any {
            fraDato == null || (
                it.gyldigFra <= fraDato &&
                    it.gyldigTil >= fraDato
                )
        }

    private fun erInnenforDatoerSomBetegnerUendelighetIKodeverk(dato: LocalDate) =
        dato.isAfter(TIDLIGSTE_DATO_I_KODEVERK) && dato.isBefore(SENESTE_DATO_I_KODEVERK)

    companion object {

        const val LANDKODE_UKJENT = "XUK"
        const val LANDKODE_STATSLØS = "XXX"
        val TIDLIGSTE_DATO_I_KODEVERK: LocalDate = LocalDate.parse("1900-01-02")
        val SENESTE_DATO_I_KODEVERK: LocalDate = LocalDate.parse("9990-01-01")
    }
}

fun Statsborgerskap.hentFom() = this.bekreftelsesdato ?: this.gyldigFraOgMed

fun Statsborgerskap.iNordiskLand() = Norden.values().map { it.name }.contains(this.land)

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
    ALA
}
