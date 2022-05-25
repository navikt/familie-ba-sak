package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Valutakurs")
@Table(name = "VALUTAKURS")
data class Valutakurs(
    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val fom: YearMonth?,

    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val tom: YearMonth?,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "AKTOER_TIL_VALUTAKURS",
        joinColumns = [JoinColumn(name = "fk_valutakurs_id")],
        inverseJoinColumns = [JoinColumn(name = "fk_aktoer_id")]
    )
    override val barnAktører: Set<Aktør> = emptySet(),

    @Column(name = "valutakursdato", columnDefinition = "DATE")
    val valutakursdato: LocalDate? = null,

    @Column(name = "valutakode")
    val valutakode: String? = null,

    @Column(name = "kurs", nullable = false)
    val kurs: BigDecimal? = null,
) : PeriodeOgBarnSkjemaEntitet<Valutakurs>() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "valutakurs_seq_generator")
    @SequenceGenerator(
        name = "valutakurs_seq_generator",
        sequenceName = "valutakurs_seq",
        allocationSize = 50
    )
    override var id: Long = 0

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    override var behandlingId: Long = 0

    override fun utenSkjema() = copy(
        valutakode = null,
        valutakursdato = null,
        kurs = null
    )

    override fun kopier(fom: YearMonth?, tom: YearMonth?, barnAktører: Set<Aktør>) =
        copy(
            fom = fom,
            tom = tom,
            barnAktører = barnAktører
        )

    companion object {
        val NULL = Valutakurs(null, null, emptySet())
    }
}
