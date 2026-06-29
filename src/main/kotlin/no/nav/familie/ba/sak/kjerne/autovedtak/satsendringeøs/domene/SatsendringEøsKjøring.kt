package no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.YearMonthConverter
import org.hibernate.Hibernate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Objects

@Entity(name = "SatsendringEøsKjøring")
@Table(name = "satsendring_eos_kjoering")
class SatsendringEøsKjøring(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "satsendring_eos_kjoering_seq_generator")
    @SequenceGenerator(
        name = "satsendring_eos_kjoering_seq_generator",
        sequenceName = "satsendring_eos_kjoering_seq",
        allocationSize = 50,
    )
    var id: Long = 0,
    @Column(name = "fk_fagsak_id", nullable = false, updatable = false)
    var fagsakId: Long,
    @Column(name = "fk_behandling_id")
    var behandlingId: Long? = null,
    @Column(name = "utbetalingsland", nullable = false, updatable = false)
    var utbetalingsland: String,
    @Column(name = "sats_tid", nullable = false, updatable = false, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var satsTidspunkt: YearMonth,
    @Column(name = "feiltype")
    var feiltype: String? = null,
    @Column(name = "start_tid", nullable = false, updatable = false)
    var startTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "ferdig_tid")
    var ferdigTidspunkt: LocalDateTime? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as SatsendringEøsKjøring
        return id == other.id
    }

    override fun hashCode(): Int = Objects.hashCode(id)

    override fun toString(): String = this::class.simpleName + "(id = $id , fagsakId = $fagsakId )"
}
