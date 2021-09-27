package no.nav.familie.ba.sak.kjerne.endretutbetaling.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.math.BigDecimal
import java.time.YearMonth
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
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "EndretUtbetalingAndel")
@Table(name = "ENDRET_UTBETALING_ANDEL")
data class EndretUtbetalingAndel(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "endret_utbetaling_andel_seq_generator")
    @SequenceGenerator(
        name = "endret_utbetaling_andel_seq_generator",
        sequenceName = "endret_utbetaling_andel_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandlingId: Long,

    @ManyToOne @JoinColumn(name = "fk_po_person_id", nullable = false)
    val person: Person,

    @Column(name = "prosent", nullable = false)
    val prosent: BigDecimal,

    @Column(name = "fom", nullable = false, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val fom: YearMonth,

    @Column(name = "tom", nullable = false, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val tom: YearMonth,

    @Enumerated(EnumType.STRING)
    @Column(name = "aarsak", nullable = false)
    val årsak: Årsak,

    @Column(name = "begrunnelse", nullable = false)
    var begrunnelse: String,

    @ManyToMany
    @JoinTable(
            name = "ANDEL_TIL_ENDRET_ANDEL",
            joinColumns = [JoinColumn(name = "fk_endret_utbetaling_andel_id")] ,
            inverseJoinColumns = [JoinColumn(name = "fk_andel_tilkjent_ytelse_id")]
    )
    val andelTilkjentYtelser: List<AndelTilkjentYtelse> = emptyList(),

) : BaseEntitet() {
    fun overlapperMed(periode: MånedPeriode) = periode.overlapperHeltEllerDelvisMed(this.periode())

    fun periode() = MånedPeriode(this.fom, this.tom)
}

enum class Årsak(val visningsnavn: String) {
    DELT_BOSTED("Delt bosted"),
    EØS_SEKUNDÆRLAND("Eøs sekundærland");

    fun kanGiNullutbetaling() = this == EØS_SEKUNDÆRLAND
}