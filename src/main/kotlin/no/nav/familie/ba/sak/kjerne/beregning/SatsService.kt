package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isBetween
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.Sats
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.erUnder6ÅrTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.filtrerMed
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.time.LocalDate
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode as TidslinjePeriode
import no.nav.familie.tidslinje.Periode as FamilieFellesTidslinjePeriode
import no.nav.familie.tidslinje.Tidslinje as FamilieFellesTidslinje

object SatsTidspunkt {
    val senesteSatsTidspunkt: LocalDate = LocalDate.MAX
}

object SatsService {
    private val satser =
        listOf(
            Sats(SatsType.ORBA, 970, LocalDate.MIN, LocalDate.of(2019, 2, 28)),
            Sats(SatsType.ORBA, 1054, LocalDate.of(2019, 3, 1), LocalDate.of(2023, 2, 28)),
            Sats(SatsType.ORBA, 1083, LocalDate.of(2023, 3, 1), LocalDate.of(2023, 6, 30)),
            Sats(SatsType.ORBA, 1310, LocalDate.of(2023, 7, 1), LocalDate.of(2023, 12, 31)),
            Sats(SatsType.ORBA, 1510, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 8, 31)),
            Sats(SatsType.ORBA, 1766, LocalDate.of(2024, 9, 1), LocalDate.MAX),
            Sats(SatsType.SMA, 660, LocalDate.MIN, LocalDate.of(2023, 2, 28)),
            Sats(SatsType.SMA, 678, LocalDate.of(2023, 3, 1), LocalDate.of(2023, 6, 30)),
            Sats(SatsType.SMA, 696, LocalDate.of(2023, 7, 1), LocalDate.MAX),
            Sats(SatsType.TILLEGG_ORBA, 970, LocalDate.MIN, LocalDate.of(2019, 2, 28)),
            Sats(SatsType.TILLEGG_ORBA, 1054, LocalDate.of(2019, 3, 1), LocalDate.of(2020, 8, 31)),
            Sats(SatsType.TILLEGG_ORBA, 1354, LocalDate.of(2020, 9, 1), LocalDate.of(2021, 8, 31)),
            Sats(SatsType.TILLEGG_ORBA, 1654, LocalDate.of(2021, 9, 1), LocalDate.of(2021, 12, 31)),
            Sats(SatsType.TILLEGG_ORBA, 1676, LocalDate.of(2022, 1, 1), LocalDate.of(2023, 2, 28)),
            Sats(SatsType.TILLEGG_ORBA, 1723, LocalDate.of(2023, 3, 1), LocalDate.of(2023, 6, 30)),
            Sats(SatsType.TILLEGG_ORBA, 1766, LocalDate.of(2023, 7, 1), LocalDate.MAX), // Egentlig er tom-dato 2024-08-31, men omskriving skjer senere
            Sats(SatsType.FINN_SVAL, 1054, LocalDate.MIN, LocalDate.of(2014, 3, 31)),
            Sats(SatsType.UTVIDET_BARNETRYGD, 970, LocalDate.MIN, LocalDate.of(2019, 2, 28)),
            Sats(SatsType.UTVIDET_BARNETRYGD, 1054, LocalDate.of(2019, 3, 1), LocalDate.of(2023, 2, 28)),
            Sats(SatsType.UTVIDET_BARNETRYGD, 2489, LocalDate.of(2023, 3, 1), LocalDate.of(2023, 6, 30)),
            Sats(SatsType.UTVIDET_BARNETRYGD, 2516, LocalDate.of(2023, 7, 1), LocalDate.MAX),
        )

    fun finnSisteSatsFor(satstype: SatsType) = finnAlleSatserFor(satstype).maxBy { it.gyldigTom }

    fun finnGjeldendeSatsForDato(
        satstype: SatsType,
        dato: LocalDate,
    ): Int {
        val gjeldendeSatsForPeriode =
            satser.find { it.type == satstype && dato.isBetween(Periode(it.gyldigFom, it.gyldigTom)) }
                ?: throw Feil("Finnes ingen sats for SatsType: $satstype for dato: $dato")
        return gjeldendeSatsForPeriode.beløp
    }

    fun finnSatsendring(startDato: LocalDate): List<Sats> =
        hentAllesatser()
            .filter { it.gyldigFom == startDato }
            .filter { it.gyldigFom != LocalDate.MIN }

    /**
     * SatsService.senesteSatsTidspunkt brukes for å mocke inn et tidspunkt som ligger tidligere enn gjeldende satser
     * alle satser som er gyldige fra etter dette tidspunktet vil filtreres bort
     * gyldigTom vil settes til LocalDate.MAX for det som nå blir siste gyldige sats, dvs varer uendelig
     */
    internal fun hentAllesatser() =
        satser
            .filter { it.gyldigFom <= SatsTidspunkt.senesteSatsTidspunkt }
            .map {
                val overstyrtTom = if (SatsTidspunkt.senesteSatsTidspunkt < it.gyldigTom) LocalDate.MAX else it.gyldigTom
                it.copy(gyldigTom = overstyrtTom)
            }

    fun finnAlleSatserFor(type: SatsType): List<Sats> = hentAllesatser().filter { it.type == type }

    fun hentDatoForSatsendring(
        satstype: SatsType,
        oppdatertBeløp: Int,
    ): LocalDate? = hentAllesatser().find { it.type == satstype && it.beløp == oppdatertBeløp }?.gyldigFom
}

fun satstypeTidslinje(satsType: SatsType) =
    tidslinje {
        SatsService
            .finnAlleSatserFor(satsType)
            .map {
                val fom = if (it.gyldigFom == LocalDate.MIN) null else it.gyldigFom.toYearMonth()
                val tom = if (it.gyldigTom == LocalDate.MAX) null else it.gyldigTom.toYearMonth()
                TidslinjePeriode(
                    fraOgMed = fom.tilTidspunktEllerUendeligTidlig(tom),
                    tilOgMed = tom.tilTidspunktEllerUendeligSent(fom),
                    it.beløp,
                )
            }
    }

fun satstypeFamilieFellesTidslinje(satsType: SatsType) =
    SatsService
        .finnAlleSatserFor(satsType)
        .map {
            val fom = if (it.gyldigFom == LocalDate.MIN) null else it.gyldigFom.førsteDagIInneværendeMåned()
            val tom = if (it.gyldigTom == LocalDate.MAX) null else it.gyldigTom.sisteDagIMåned()
            FamilieFellesTidslinjePeriode(
                verdi = it.beløp,
                fom = fom,
                tom = tom,
            )
        }.tilTidslinje()

fun lagOrdinærTidslinje(barn: Person): FamilieFellesTidslinje<Int> {
    val orbaTidslinje = satstypeFamilieFellesTidslinje(SatsType.ORBA)
    val tilleggOrbaTidslinje = satstypeFamilieFellesTidslinje(SatsType.TILLEGG_ORBA).filtrerMed(erUnder6ÅrTidslinje(barn))
    return orbaTidslinje
        .kombinerMed(tilleggOrbaTidslinje) { orba, tillegg -> tillegg ?: orba }
        .beskjærFraOgMed(fraOgMed = barn.fødselsdato.førsteDagIInneværendeMåned())
}
