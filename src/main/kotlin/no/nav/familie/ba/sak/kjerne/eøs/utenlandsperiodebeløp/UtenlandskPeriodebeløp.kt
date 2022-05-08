package no.nav.familie.ba.sak.kjerne.eøs.utenlandsperiodebeløp

import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.math.BigDecimal
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
@Entity(name = "UtenlandskPeriodebeløp")
@Table(name = "UTENLANDSK_PERIODEBELOEP")
data class UtenlandskPeriodebeløp(
    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val fom: YearMonth?,

    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val tom: YearMonth?,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "AKTOER_TIL_UTENLANDSK_PERIODEBELOEP",
        joinColumns = [JoinColumn(name = "fk_utenlandsk_periodebeloep_id")],
        inverseJoinColumns = [JoinColumn(name = "fk_aktoer_id")]
    )
    override val barnAktører: Set<Aktør> = emptySet(),

    @Column(name = "beloep")
    val beløp: BigDecimal?,

    @Column(name = "valutakode")
    val valutakode: String?,

    @Column(name = "intervall")
    val intervall: String?,
) : PeriodeOgBarnSkjemaEntitet<UtenlandskPeriodebeløp>() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "utenlandsk_periodebeloep_seq_generator")
    @SequenceGenerator(
        name = "utenlandsk_periodebeloep_seq_generator",
        sequenceName = "utenlandsk_periodebeloep_seq",
        allocationSize = 50
    )
    override var id: Long = 0

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    override var behandlingId: Long = 0

    override fun utenSkjema(): UtenlandskPeriodebeløp = copy(
        beløp = null,
        valutakode = null,
        intervall = null
    )

    override fun kopier(fom: YearMonth?, tom: YearMonth?, barnAktører: Set<Aktør>) = copy(
        fom = fom,
        tom = tom,
        barnAktører = barnAktører
    )
}
