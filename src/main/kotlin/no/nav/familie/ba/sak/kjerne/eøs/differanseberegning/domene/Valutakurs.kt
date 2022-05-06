package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.SequenceGenerator

data class Valutakurs(
    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val fom: YearMonth?,

    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val tom: YearMonth?,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "AKTOER_TIL_KOMPETANSE",
        joinColumns = [JoinColumn(name = "fk_kompetanse_id")],
        inverseJoinColumns = [JoinColumn(name = "fk_aktoer_id")]
    )
    val barnAktører: Set<Aktør> = emptySet(),
    @Column(name = "valutakursdato", columnDefinition = "DATE")
    val valutakursdato: LocalDate,
    @Column(name = "valutakode")
    val valutakode: String,
    @Column(name = "kurs", nullable = false)
    val kurs: BigDecimal,
) : BaseEntitet() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "valutakurs_seq_generator")
    @SequenceGenerator(
        name = "valutakurs_seq_generator",
        sequenceName = "valutakurs_seq",
        allocationSize = 50
    )
    var id: Long = 0

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    var behandlingId: Long = 0
}
