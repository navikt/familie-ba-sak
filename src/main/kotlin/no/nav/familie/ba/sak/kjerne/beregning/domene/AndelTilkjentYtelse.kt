package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.math.BigDecimal
import java.time.YearMonth
import java.util.*
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "AndelTilkjentYtelse")
@Table(name = "ANDEL_TILKJENT_YTELSE")
data class AndelTilkjentYtelse(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "andel_tilkjent_ytelse_seq_generator")
    @SequenceGenerator(
        name = "andel_tilkjent_ytelse_seq_generator",
        sequenceName = "andel_tilkjent_ytelse_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandlingId: Long,

    @ManyToOne
    @JoinColumn(name = "tilkjent_ytelse_id", nullable = false, updatable = false)
    var tilkjentYtelse: TilkjentYtelse,

    @Column(name = "person_ident", nullable = false, updatable = false)
    val personIdent: String,

    @Column(name = "kalkulert_utbetalingsbelop", nullable = false)
    val kalkulertUtbetalingsbeløp: Int,

    @Column(name = "stonad_fom", nullable = false, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val stønadFom: YearMonth,

    @Column(name = "stonad_tom", nullable = false, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val stønadTom: YearMonth,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: YtelseType,

    @Column(name = "sats", nullable = false)
    val sats: Int,

    // TODO: Bør dette hete gradering? I så fall rename og migrer i endringstabell også
    @Column(name = "prosent", nullable = false)
    val prosent: BigDecimal,

    @ManyToMany(mappedBy = "andelTilkjentYtelser")
    val endretUtbetalingAndeler: List<EndretUtbetalingAndel> = emptyList(),

    // kildeBehandlingId, periodeOffset og forrigePeriodeOffset trengs kun i forbindelse med
    // iverksetting/konsistensavstemming, og settes først ved generering av selve oppdraget mot økonomi.

    // Samme informasjon finnes i utbetalingsoppdraget på hver enkelt sak, men for å gjøre operasjonene mer forståelig
    // og enklere å jobbe med har vi valgt å trekke det ut hit.

    @Column(name = "kilde_behandling_id")
    var kildeBehandlingId: Long? = null, // Brukes til å finne hvilke behandlinger som skal konsistensavstemmes

    @Column(name = "periode_offset")
    var periodeOffset: Long? = null, // Brukes for å koble seg på tidligere kjeder sendt til økonomi

    @Column(name = "forrige_periode_offset")
    var forrigePeriodeOffset: Long? = null

) : BaseEntitet() {

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        } else if (this === other) {
            return true
        }

        val annen = other as AndelTilkjentYtelse
        return Objects.equals(behandlingId, annen.behandlingId) &&
            Objects.equals(type, annen.type) &&
            Objects.equals(kalkulertUtbetalingsbeløp, annen.kalkulertUtbetalingsbeløp) &&
            Objects.equals(stønadFom, annen.stønadFom) &&
            Objects.equals(stønadTom, annen.stønadTom) &&
            Objects.equals(personIdent, annen.personIdent)
    }

    override fun hashCode(): Int {
        return Objects.hash(id, behandlingId, type, kalkulertUtbetalingsbeløp, stønadFom, stønadTom, personIdent)
    }

    override fun toString(): String {
        return "AndelTilkjentYtelse(id = $id, behandling = $behandlingId, " +
            "beløp = $kalkulertUtbetalingsbeløp, stønadFom = $stønadFom, stønadTom = $stønadTom, periodeOffset = $periodeOffset)"
    }

    fun erTilsvarendeForUtbetaling(other: AndelTilkjentYtelse): Boolean {
        return (
            this.personIdent == other.personIdent &&
                this.stønadFom == other.stønadFom &&
                this.stønadTom == other.stønadTom &&
                this.kalkulertUtbetalingsbeløp == other.kalkulertUtbetalingsbeløp &&
                this.type == other.type
            )
    }

    fun overlapperMed(andelFraAnnenBehandling: AndelTilkjentYtelse): Boolean {
        return this.type == andelFraAnnenBehandling.type &&
            this.stønadFom <= andelFraAnnenBehandling.stønadTom &&
            this.stønadTom >= andelFraAnnenBehandling.stønadFom
    }

    fun stønadsPeriode() = MånedPeriode(this.stønadFom, this.stønadTom)

    fun erUtvidet() = this.type == YtelseType.UTVIDET_BARNETRYGD

    fun erLøpende(): Boolean = this.stønadTom > YearMonth.now()

    companion object {

        /**
         * Merk at det søkes snitt på visse attributter (erTilsvarendeForUtbetaling)
         * og man kun returnerer objekter fra receiver (ikke other)
         */
        fun Set<AndelTilkjentYtelse>.snittAndeler(other: Set<AndelTilkjentYtelse>): Set<AndelTilkjentYtelse> {
            val andelerKunIDenne = this.subtractAndeler(other)
            return this.subtractAndeler(andelerKunIDenne)
        }

        fun Set<AndelTilkjentYtelse>.disjunkteAndeler(other: Set<AndelTilkjentYtelse>): Set<AndelTilkjentYtelse> {
            val andelerKunIDenne = this.subtractAndeler(other)
            val andelerKunIAnnen = other.subtractAndeler(this)
            return andelerKunIDenne.union(andelerKunIAnnen)
        }

        private fun Set<AndelTilkjentYtelse>.subtractAndeler(other: Set<AndelTilkjentYtelse>): Set<AndelTilkjentYtelse> {
            return this.filter { a ->
                other.none { b -> a.erTilsvarendeForUtbetaling(b) }
            }.toSet()
        }
    }
}

fun List<AndelTilkjentYtelse>.slåSammenBack2BackAndelsperioderMedSammeBeløp(): List<AndelTilkjentYtelse> {
    if (this.size <= 1) return this
    val sorterteAndeler = this.sortedBy { it.stønadFom }
    val sammenslåtteAndeler = mutableListOf<AndelTilkjentYtelse>()
    var andel = sorterteAndeler.firstOrNull()
    sorterteAndeler.forEach { andelTilkjentYtelse ->
        andel = andel ?: andelTilkjentYtelse
        val back2BackAndelsperiodeMedSammeBeløp = this.singleOrNull {
            andel!!.stønadTom.plusMonths(1).equals(it.stønadFom) &&
                andel!!.personIdent == it.personIdent &&
                andel!!.kalkulertUtbetalingsbeløp == it.kalkulertUtbetalingsbeløp
        }
        andel = if (back2BackAndelsperiodeMedSammeBeløp != null) {
            andel!!.copy(stønadTom = back2BackAndelsperiodeMedSammeBeløp.stønadTom)
        } else {
            sammenslåtteAndeler.add(andel!!)
            null
        }
    }
    if (andel != null) sammenslåtteAndeler.add(andel!!)
    return sammenslåtteAndeler
}

enum class YtelseType(val klassifisering: String) {
    ORDINÆR_BARNETRYGD("BATR"),
    UTVIDET_BARNETRYGD("BATR"),
    SMÅBARNSTILLEGG("BATRSMA"),
    EØS("BATR"),
    MANUELL_VURDERING("BATR")
}
