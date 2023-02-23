package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.erUnder6ÅrTidslinje
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.Sats
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerSenereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerTidligereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjær
import java.time.LocalDate

object SatsTidspunkt {
    val senesteSatsTidspunkt: LocalDate = LocalDate.MAX
}

object SatsService {

    private val satser = listOf(
        Sats(SatsType.ORBA, 970, LocalDate.MIN, LocalDate.of(2019, 2, 28)),
        Sats(SatsType.ORBA, 1054, LocalDate.of(2019, 3, 1), LocalDate.of(2023, 2, 28)),
        Sats(SatsType.ORBA, 1083, LocalDate.of(2023, 3, 1), LocalDate.MAX),

        Sats(SatsType.SMA, 660, LocalDate.MIN, LocalDate.of(2023, 2, 28)),
        Sats(SatsType.SMA, 678, LocalDate.of(2023, 3, 1), LocalDate.MAX),

        Sats(SatsType.TILLEGG_ORBA, 970, LocalDate.MIN, LocalDate.of(2019, 2, 28)),
        Sats(SatsType.TILLEGG_ORBA, 1054, LocalDate.of(2019, 3, 1), LocalDate.of(2020, 8, 31)),
        Sats(SatsType.TILLEGG_ORBA, 1354, LocalDate.of(2020, 9, 1), LocalDate.of(2021, 8, 31)),
        Sats(SatsType.TILLEGG_ORBA, 1654, LocalDate.of(2021, 9, 1), LocalDate.of(2021, 12, 31)),
        Sats(SatsType.TILLEGG_ORBA, 1676, LocalDate.of(2022, 1, 1), LocalDate.of(2023, 2, 28)),
        Sats(SatsType.TILLEGG_ORBA, 1723, LocalDate.of(2023, 3, 1), LocalDate.MAX),

        Sats(SatsType.FINN_SVAL, 1054, LocalDate.MIN, LocalDate.of(2014, 3, 31)),

        Sats(SatsType.UTVIDET_BARNETRYGD, 970, LocalDate.MIN, LocalDate.of(2019, 2, 28)),
        Sats(SatsType.UTVIDET_BARNETRYGD, 1054, LocalDate.of(2019, 3, 1), LocalDate.of(2023, 2, 28)),
        Sats(SatsType.UTVIDET_BARNETRYGD, 2489, LocalDate.of(2023, 3, 1), LocalDate.MAX)
    )

    fun finnSisteSatsFor(satstype: SatsType) = finnAlleSatserFor(satstype).find {
        it.gyldigTom == LocalDate.MAX
    }!!

    fun finnSatsendring(startDato: LocalDate): List<Sats> = hentAllesatser()
        .filter { it.gyldigFom == startDato }
        .filter { it.gyldigFom != LocalDate.MIN }

    /**
     * SatsService.senesteSatsTidspunkt brukes for å mocke inn et tidspunkt som ligger tidligere enn gjeldende satser
     * alle satser som er gyldige fra etter dette tidspunktet vil filtreres bort
     * gyldigTom vil settes til LocalDate.MAX for det som nå blir siste gyldige sats, dvs varer uendelig
     */
    internal fun hentAllesatser() = satser
        .filter { it.gyldigFom <= SatsTidspunkt.senesteSatsTidspunkt }
        .map {
            val overstyrtTom = if (SatsTidspunkt.senesteSatsTidspunkt < it.gyldigTom) LocalDate.MAX else it.gyldigTom
            it.copy(gyldigTom = overstyrtTom)
        }

    fun finnAlleSatserFor(type: SatsType): List<Sats> = hentAllesatser().filter { it.type == type }

    fun hentDatoForSatsendring(
        satstype: SatsType,
        oppdatertBeløp: Int
    ): LocalDate? = hentAllesatser().find { it.type == satstype && it.beløp == oppdatertBeløp }?.gyldigFom
}

fun fomErPåSatsendring(fom: LocalDate?): Boolean =
    SatsService
        .finnSatsendring(fom?.førsteDagIInneværendeMåned() ?: TIDENES_MORGEN)
        .isNotEmpty()

fun satstypeTidslinje(satsType: SatsType) =
    tidslinje {
        SatsService.finnAlleSatserFor(satsType)
            .map {
                val fom = if (it.gyldigFom == LocalDate.MIN) null else it.gyldigFom.toYearMonth()
                val tom = if (it.gyldigTom == LocalDate.MAX) null else it.gyldigTom.toYearMonth()
                Periode(
                    fraOgMed = fom.tilTidspunktEllerTidligereEnn(tom),
                    tilOgMed = tom.tilTidspunktEllerSenereEnn(fom),
                    it.beløp
                )
            }
    }

fun lagOrdinærTidslinje(barn: Person): Tidslinje<Int, Måned> {
    val orbaTidslinje = satstypeTidslinje(SatsType.ORBA)
    val tilleggOrbaTidslinje = satstypeTidslinje(SatsType.TILLEGG_ORBA).filtrerMed(erUnder6ÅrTidslinje(barn))
    return orbaTidslinje
        .kombinerMed(tilleggOrbaTidslinje) { orba, tillegg -> tillegg ?: orba }
        .klippBortPerioderFørBarnetBleFødt(fødselsdato = barn.fødselsdato)
}

private fun Tidslinje<Int, Måned>.klippBortPerioderFørBarnetBleFødt(fødselsdato: LocalDate) = this.beskjær(
    fraOgMed = fødselsdato.tilMånedTidspunkt(),
    tilOgMed = MånedTidspunkt.uendeligLengeTil(fødselsdato.toYearMonth())
)
