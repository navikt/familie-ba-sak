package no.nav.familie.ba.sak.kjerne.beregning.domene

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import java.time.LocalDate
import java.time.YearMonth

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "TilkjentYtelse")
@Table(name = "TILKJENT_YTELSE")
data class TilkjentYtelse(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tilkjent_ytelse_seq_generator")
    @SequenceGenerator(
        name = "tilkjent_ytelse_seq_generator",
        sequenceName = "tilkjent_ytelse_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @OneToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
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
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "tilkjentYtelse",
        cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE, CascadeType.REMOVE],
        orphanRemoval = true,
    )
    val andelerTilkjentYtelse: MutableSet<AndelTilkjentYtelse> = mutableSetOf(),
)

fun TilkjentYtelse.tilTidslinjeMedAndeler() =
    this.andelerTilkjentYtelse
        .tilTidslinjerPerAktørOgType()
        .values
        .kombiner()

fun TilkjentYtelse.utbetalingsoppdrag(): Utbetalingsoppdrag? = this.utbetalingsoppdrag?.let { jsonMapper.readValue(it, Utbetalingsoppdrag::class.java) }

fun TilkjentYtelse.utbetalingsperioder() = this.utbetalingsoppdrag()?.utbetalingsperiode ?: emptyList()
