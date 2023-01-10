package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.erUnder6ÅrTidslinje
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.Sats
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
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
import java.time.YearMonth

object SatsService {

    private val satser = listOf(
        Sats(SatsType.ORBA, 1054, LocalDate.of(2019, 3, 1), LocalDate.MAX),
        Sats(SatsType.ORBA, 970, LocalDate.MIN, LocalDate.of(2019, 2, 28)),
        Sats(SatsType.SMA, 660, LocalDate.MIN, LocalDate.MAX),
        Sats(SatsType.TILLEGG_ORBA, 970, LocalDate.MIN, LocalDate.of(2019, 2, 28)),
        Sats(SatsType.TILLEGG_ORBA, 1054, LocalDate.of(2019, 3, 1), LocalDate.of(2020, 8, 31)),
        Sats(SatsType.TILLEGG_ORBA, 1354, LocalDate.of(2020, 9, 1), LocalDate.of(2021, 8, 31)),
        Sats(SatsType.TILLEGG_ORBA, 1654, LocalDate.of(2021, 9, 1), LocalDate.of(2021, 12, 31)),
        Sats(SatsType.TILLEGG_ORBA, 1676, LocalDate.of(2022, 1, 1), LocalDate.MAX),
        Sats(SatsType.FINN_SVAL, 1054, LocalDate.MIN, LocalDate.of(2014, 3, 31))
    )

    val tilleggEndringJanuar2022 = YearMonth.of(2022, 1)

    val tilleggOrdinærSatsTilTester: Sats =
        satser.findLast {
            it.type == SatsType.TILLEGG_ORBA && it.gyldigFom <= LocalDate.now().plusDays(1)
        }!!

    val sisteUtvidetSatsTilTester: Sats =
        satser.find {
            it.type == SatsType.ORBA && it.gyldigTom == LocalDate.MAX
        }!!

    val sisteSmåbarnstilleggSatsTilTester: Sats =
        satser.find {
            it.type == SatsType.SMA && it.gyldigTom == LocalDate.MAX
        }!!

    val sisteTilleggOrdinærSats: Sats =
        satser.find {
            it.type == SatsType.TILLEGG_ORBA && it.gyldigTom == LocalDate.MAX
        }!!

    val tilleggOrdinærSatsNesteMånedTilTester: Sats =
        satser.findLast {
            it.type == SatsType.TILLEGG_ORBA && it.gyldigFom.toYearMonth() <= LocalDate.now().nesteMåned()
        }!!

    fun finnSatsendring(startDato: LocalDate): List<Sats> = satser
        .filter { it.gyldigFom == startDato }
        .filter { it.gyldigFom != LocalDate.MIN }

    fun hentGyldigSatsFor(
        satstype: SatsType,
        stønadFraOgMed: YearMonth,
        stønadTilOgMed: YearMonth,
        maxSatsGyldigFraOgMed: YearMonth = YearMonth.now()
    ): List<SatsPeriode> {
        return finnAlleSatserFor(satstype)
            .map { SatsPeriode(it.beløp, it.gyldigFom.toYearMonth(), it.gyldigTom.toYearMonth()) }
            .filter { it.fraOgMed <= maxSatsGyldigFraOgMed }
            .map { SatsPeriode(it.sats, maxOf(it.fraOgMed, stønadFraOgMed), minOf(it.tilOgMed, stønadTilOgMed)) }
            .filter { it.fraOgMed <= it.tilOgMed }
    }

    internal fun hentAllesatser() = satser

    private fun finnAlleSatserFor(type: SatsType): List<Sats> = satser.filter { it.type == type }

    data class SatsPeriode(
        val sats: Int,
        val fraOgMed: YearMonth,
        val tilOgMed: YearMonth
    )

    fun hentDatoForSatsendring(
        satstype: SatsType,
        oppdatertBeløp: Int
    ): LocalDate? = satser.find { it.type == satstype && it.beløp == oppdatertBeløp }?.gyldigFom
}

fun fomErPåSatsendring(fom: LocalDate?): Boolean =
    SatsService
        .finnSatsendring(fom?.førsteDagIInneværendeMåned() ?: TIDENES_MORGEN)
        .isNotEmpty()

fun satstypeTidslinje(satsType: SatsType, maxSatsGyldigFraOgMed: YearMonth = SatsService.tilleggEndringJanuar2022) =
    tidslinje {
        SatsService.hentAllesatser()
            .filter { it.type == satsType }
            .filter { it.gyldigFom.toYearMonth() <= maxSatsGyldigFraOgMed }
            .map {
                val fom = if (it.gyldigFom == LocalDate.MIN) null else it.gyldigFom.toYearMonth()
                val tom = if (it.gyldigTom == LocalDate.MAX) null else it.gyldigTom.toYearMonth()
                no.nav.familie.ba.sak.kjerne.tidslinje.Periode(
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
