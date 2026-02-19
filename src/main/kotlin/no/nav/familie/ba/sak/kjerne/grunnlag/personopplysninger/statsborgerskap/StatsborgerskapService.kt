package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
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
        eldsteBarnFødselsdato: LocalDate,
    ): List<GrStatsborgerskap> {
        if (featureToggleService.isEnabled(FeatureToggle.HARDKODET_EEAFREG_STATSBORGERSKAP)) {
            return lagMedlemskapTIdslinjeForStatsborgerskap(statsborgerskap)
                .beskjærFraOgMed(eldsteBarnFødselsdato)
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
        }

        if (statsborgerskap.iNordiskLand()) {
            return listOf(
                GrStatsborgerskap(
                    gyldigPeriode =
                        DatoIntervallEntitet(
                            fom = statsborgerskap.hentFom(),
                            tom = statsborgerskap.gyldigTilOgMed,
                        ),
                    landkode = statsborgerskap.land,
                    medlemskap = Medlemskap.NORDEN,
                    person = person,
                ),
            )
        }

        val eøsMedlemskapsPerioderForValgtLand = kodeverkService.henteEøsMedlemskapsPerioderForValgtLand(statsborgerskap.land)

        val datoFra = statsborgerskap.hentFom()

        return if (datoFra == null && statsborgerskap.gyldigTilOgMed == null) {
            val idag = LocalDate.now()
            listOf(
                GrStatsborgerskap(
                    gyldigPeriode =
                        DatoIntervallEntitet(
                            fom = idag,
                            tom = null,
                        ),
                    landkode = statsborgerskap.land,
                    medlemskap =
                        finnMedlemskap(
                            statsborgerskap = statsborgerskap,
                            eøsMedlemskapsperioderForValgtLand = eøsMedlemskapsPerioderForValgtLand,
                            gyldigFraOgMed = idag,
                        ),
                    person = person,
                ),
            )
        } else {
            hentMedlemskapsperioderUnderStatsborgerskapsperioden(
                medlemskapsperioderForValgtLand = eøsMedlemskapsPerioderForValgtLand,
                statsborgerFra = datoFra,
                statsborgerTil = statsborgerskap.gyldigTilOgMed,
            ).fold(emptyList()) { medlemskapsperioder, periode ->
                val medlemskapsperiode =
                    GrStatsborgerskap(
                        gyldigPeriode = periode,
                        landkode = statsborgerskap.land,
                        medlemskap =
                            finnMedlemskap(
                                statsborgerskap = statsborgerskap,
                                eøsMedlemskapsperioderForValgtLand = eøsMedlemskapsPerioderForValgtLand,
                                gyldigFraOgMed = periode.fom,
                            ),
                        person = person,
                    )
                medlemskapsperioder + listOf(medlemskapsperiode)
            }
        }
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
    ): Medlemskap? {
        if (featureToggleService.isEnabled(FeatureToggle.HARDKODET_EEAFREG_STATSBORGERSKAP)) {
            val medlemskapsPerioder = lagMedlemskapTIdslinjeForStatsborgerskap(statsborgerskap).verdiPåTidspunkt(tidspunkt)

            return medlemskapsPerioder
        }

        if (statsborgerskap.iNordiskLand()) {
            return Medlemskap.NORDEN
        }

        val eøsMedlemskapsPerioderForValgtLand = kodeverkService.henteEøsMedlemskapsPerioderForValgtLand(statsborgerskap.land)
        val datoFra = statsborgerskap.hentFom()

        return if (datoFra == null && statsborgerskap.gyldigTilOgMed == null) {
            val idag = LocalDate.now()
            finnMedlemskap(
                statsborgerskap = statsborgerskap,
                eøsMedlemskapsperioderForValgtLand = eøsMedlemskapsPerioderForValgtLand,
                gyldigFraOgMed = idag,
            )
        } else {
            val alleMedlemskap =
                hentMedlemskapsperioderUnderStatsborgerskapsperioden(
                    eøsMedlemskapsPerioderForValgtLand,
                    datoFra,
                    statsborgerskap.gyldigTilOgMed,
                ).fold(emptyList<Medlemskap>()) { acc, periode ->
                    acc +
                        listOf(
                            finnMedlemskap(
                                statsborgerskap = statsborgerskap,
                                eøsMedlemskapsperioderForValgtLand = eøsMedlemskapsPerioderForValgtLand,
                                gyldigFraOgMed = periode.fom,
                            ),
                        )
                }

            alleMedlemskap.finnSterkesteMedlemskap()
        }
    }

    private fun hentMedlemskapsperioderUnderStatsborgerskapsperioden(
        // Kan fjernes når vi går over til hardkodet EØS
        medlemskapsperioderForValgtLand: List<BetydningDto>,
        statsborgerFra: LocalDate?,
        statsborgerTil: LocalDate?,
    ): List<DatoIntervallEntitet> {
        val datoerMedlemskapEndrerSeg =
            medlemskapsperioderForValgtLand
                .flatMap {
                    listOf(
                        it.gyldigFra,
                        it.gyldigTil.plusDays(1),
                    )
                }
        val endringsdatoerUnderStatsborgerskapsperioden =
            datoerMedlemskapEndrerSeg
                .filter { datoForEndringIMedlemskap ->
                    erInnenforDatoerSomBetegnerUendelighetIKodeverk(datoForEndringIMedlemskap)
                }.filter { datoForEndringIMedlemskap ->
                    erInnenforDatoerForStatsborgerskapet(datoForEndringIMedlemskap, statsborgerFra, statsborgerTil)
                }

        val datoerMedlemskapEllerStatsborgerskapEndrerSeg =
            listOf(statsborgerFra) + endringsdatoerUnderStatsborgerskapsperioden + listOf(statsborgerTil)
        val naivePerioder = datoerMedlemskapEllerStatsborgerskapEndrerSeg.windowed(2, 1)
        return hentDatointervallerMedSluttdatoFørNesteStarter(naivePerioder)
    }

    private fun finnMedlemskap(
        statsborgerskap: Statsborgerskap,
        erEøsland: Boolean,
    ): Medlemskap =
        when {
            statsborgerskap.iNordiskLand() -> Medlemskap.NORDEN
            erEøsland -> Medlemskap.EØS
            statsborgerskap.erStatsløs() -> Medlemskap.STATSLØS
            statsborgerskap.erUkjent() -> Medlemskap.UKJENT
            else -> Medlemskap.TREDJELANDSBORGER
        }

    private fun finnMedlemskap(
        // Kan fjernes når vi går over til hardkodet EØS
        statsborgerskap: Statsborgerskap,
        eøsMedlemskapsperioderForValgtLand: List<BetydningDto>,
        gyldigFraOgMed: LocalDate?,
    ): Medlemskap =
        when {
            statsborgerskap.iNordiskLand() -> Medlemskap.NORDEN
            erEØSMedlemPåGittDato(eøsMedlemskapsperioderForValgtLand, gyldigFraOgMed) -> Medlemskap.EØS
            statsborgerskap.iTredjeland() -> Medlemskap.TREDJELANDSBORGER
            statsborgerskap.erStatsløs() -> Medlemskap.STATSLØS
            else -> Medlemskap.UKJENT
        }

    private fun erEØSMedlemPåGittDato(
        // Kan fjernes når vi går over til hardkodet EØS
        eøsMedlemskapsperioderForValgtLand: List<BetydningDto>,
        gjeldendeDato: LocalDate?,
    ): Boolean =
        eøsMedlemskapsperioderForValgtLand.any {
            gjeldendeDato == null ||
                (
                    it.gyldigFra <= gjeldendeDato &&
                        it.gyldigTil >= gjeldendeDato
                )
        }

    private fun erInnenforDatoerSomBetegnerUendelighetIKodeverk(dato: LocalDate) = dato.isAfter(TIDLIGSTE_DATO_I_KODEVERK) && dato.isBefore(SENESTE_DATO_I_KODEVERK) // Kan fjernes når vi går over til hardkodet EØS

    private fun erInnenforDatoerForStatsborgerskapet(
        // Kan fjernes når vi går over til hardkodet EØS
        dato: LocalDate,
        statsborgerFra: LocalDate?,
        statsborgerTil: LocalDate?,
    ) = (statsborgerFra == null || dato.isAfter(statsborgerFra)) &&
        (statsborgerTil == null || dato.isBefore(statsborgerTil))

    private fun hentDatointervallerMedSluttdatoFørNesteStarter(intervaller: List<List<LocalDate?>>): List<DatoIntervallEntitet> =
        // Kan fjernes når vi går over til hardkodet EØS
        intervaller.mapIndexed { index, endringsdatoPar ->
            val fra = endringsdatoPar[0]
            val nesteEndringsdato = endringsdatoPar[1]
            if (index != (intervaller.size - 1)) {
                if (nesteEndringsdato == null) {
                    throw Feil("EØS-medlemskap skal ikke kunne ha null som fra/til-dato")
                }
                DatoIntervallEntitet(fra, nesteEndringsdato.minusDays(1))
            } else {
                DatoIntervallEntitet(fra, nesteEndringsdato)
            }
        }

    companion object {
        const val LANDKODE_UKJENT = "XUK"
        const val LANDKODE_STATSLØS = "XXX"
        val TIDLIGSTE_DATO_I_KODEVERK: LocalDate = LocalDate.parse("1900-01-02")
        val SENESTE_DATO_I_KODEVERK: LocalDate = LocalDate.parse("9990-01-01")
    }
}

fun Statsborgerskap.hentFom() = this.gyldigFraOgMed ?: this.bekreftelsesdato

fun Statsborgerskap.iNordiskLand() = Norden.entries.map { it.name }.contains(this.land)

fun Statsborgerskap.iTredjeland() = this.land != StatsborgerskapService.LANDKODE_UKJENT

fun Statsborgerskap.erStatsløs() = this.land == StatsborgerskapService.LANDKODE_STATSLØS

fun Statsborgerskap.erUkjent() = this.land == StatsborgerskapService.LANDKODE_UKJENT

fun Statsborgerskap.erUkraina() = this.land == "UKR"

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
