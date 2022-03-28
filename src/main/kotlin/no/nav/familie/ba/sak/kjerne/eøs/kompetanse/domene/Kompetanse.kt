package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
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
import javax.persistence.Transient

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Kompetanse")
@Table(name = "KOMPETANSE")
data class Kompetanse(
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

    @Transient
    val søkersAktivitet: String? = null,
    @Transient
    val annenForeldersAktivitet: String? = null,
    @Transient
    val barnetsBostedsland: String? = null,
    @Transient
    val primærland: String? = null,
    @Transient
    val sekundærland: String? = null,
) : BaseEntitet() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kompetanse_seq_generator")
    @SequenceGenerator(
        name = "kompetanse_seq_generator",
        sequenceName = "kompetanse_seq",
        allocationSize = 50
    )
    var id: Long = 0

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    var behandlingId: Long = 0

    @Transient
    var status: KompetanseStatus? = KompetanseStatus.IKKE_UTFYLT
}

enum class KompetanseStatus {
    IKKE_UTFYLT,
    UFULLSTENDIG,
    OK
}

fun Kompetanse.blankUt() = this.copy(
    søkersAktivitet = null,
    annenForeldersAktivitet = null,
    barnetsBostedsland = null,
    primærland = null,
    sekundærland = null,
)

fun Kompetanse.inneholder(kompetanse: Kompetanse): Boolean {
    return this.bareSkjema() == kompetanse.bareSkjema() &&
        (this.fom == null || this.fom <= kompetanse.fom) &&
        (this.tom == null || this.tom >= kompetanse.tom) &&
        this.barnAktører.containsAll(kompetanse.barnAktører)
}

fun Kompetanse.bareSkjema() =
    this.copy(fom = null, tom = null, barnAktører = emptySet())
