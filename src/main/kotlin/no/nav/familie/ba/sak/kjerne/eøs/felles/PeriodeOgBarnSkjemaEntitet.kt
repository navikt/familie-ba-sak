package no.nav.familie.ba.sak.kjerne.eøs.felles

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.time.YearMonth
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class PeriodeOgBarnSkjemaEntitet<T : PeriodeOgBarnSkjema<T>> : BaseEntitet(), PeriodeOgBarnSkjema<T> {

    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val fom: YearMonth? = null

    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val tom: YearMonth? = null

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "AKTOER_TIL_KOMPETANSE",
        joinColumns = [JoinColumn(name = "fk_kompetanse_id")],
        inverseJoinColumns = [JoinColumn(name = "fk_aktoer_id")]
    )
    override val barnAktører: Set<Aktør> = emptySet()

    abstract var id: Long

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    open var behandlingId: Long = 0
}
