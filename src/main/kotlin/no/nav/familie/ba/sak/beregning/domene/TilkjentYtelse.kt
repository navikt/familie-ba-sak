package no.nav.familie.ba.sak.beregning.domene

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.time.LocalDate
import java.time.YearMonth
import javax.persistence.*

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "TilkjentYtelse")
@Table(name = "TILKJENT_YTELSE")
data class TilkjentYtelse(

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tilkjent_ytelse_seq_generator")
        @SequenceGenerator(name = "tilkjent_ytelse_seq_generator", sequenceName = "tilkjent_ytelse_seq", allocationSize = 50)
        val id: Long = 0,

        @OneToOne(optional = false) @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "stonad_fom", nullable = true, columnDefinition = "DATE")
        @Convert(converter = YearMonthConverter::class)
        var stønadFom: YearMonth? = null,

        @Column(name = "stonad_tom", nullable = true, columnDefinition = "DATE")
        @Convert(converter = YearMonthConverter::class)
        var stønadTom: YearMonth? = null,

        @Column(name = "opphor_fom", nullable = true, columnDefinition = "DATE")
        @Convert(converter = YearMonthConverter::class)
        var opphørFom: YearMonth? = null,

        @Column(name = "opprettet_dato", nullable = false)
        val opprettetDato: LocalDate,

        @Column(name = "endret_dato", nullable = false)
        var endretDato: LocalDate,

        @Column(name = "utbetalingsoppdrag", columnDefinition = "TEXT")
        var utbetalingsoppdrag: String? = null,

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "tilkjentYtelse",
                   cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE])
        val andelerTilkjentYtelse: MutableSet<AndelTilkjentYtelse> = mutableSetOf()
) {

    fun erSendtTilIverksetting(): Boolean = utbetalingsoppdrag != null


}

private fun kombinerAndeler(lhs: LocalDateTimeline<List<AndelTilkjentYtelse>>,
                            rhs: LocalDateTimeline<AndelTilkjentYtelse>): LocalDateTimeline<List<AndelTilkjentYtelse>> {
    return lhs.combine(rhs,
                       { datoIntervall, sammenlagt, neste ->
                           StandardCombinators.allValues(datoIntervall,
                                                         sammenlagt,
                                                         neste)
                       },
                       LocalDateTimeline.JoinStyle.CROSS_JOIN)
}

fun lagTidslinjeMedOverlappendePerioderForAndeler(tidslinjer: List<LocalDateTimeline<AndelTilkjentYtelse>>): LocalDateTimeline<List<AndelTilkjentYtelse>> {
    if (tidslinjer.isEmpty()) return LocalDateTimeline(emptyList())

    val førsteSegment = tidslinjer.first().toSegments().first()
    val initiellSammenlagt =
            LocalDateTimeline(listOf(LocalDateSegment(førsteSegment.fom, førsteSegment.tom, listOf(førsteSegment.value))))
    val resterende = tidslinjer.drop(1)

    return resterende.fold(initiellSammenlagt) { sammenlagt, neste ->
        kombinerAndeler(sammenlagt, neste)
    }
}

fun TilkjentYtelse.tilTidslinjeMedAndeler(): LocalDateTimeline<List<AndelTilkjentYtelse>> {
    val tidslinjer = this.andelerTilkjentYtelse.map { andelTilkjentYtelse ->
        LocalDateTimeline(listOf(LocalDateSegment(andelTilkjentYtelse.stønadFom.førsteDagIInneværendeMåned(),
                                                  andelTilkjentYtelse.stønadTom.sisteDagIInneværendeMåned(),
                                                  andelTilkjentYtelse)))
    }

    return lagTidslinjeMedOverlappendePerioderForAndeler(tidslinjer)
}