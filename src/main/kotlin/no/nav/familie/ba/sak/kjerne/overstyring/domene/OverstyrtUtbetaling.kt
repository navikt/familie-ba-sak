package no.nav.familie.ba.sak.kjerne.overstyring.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
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
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "OverstyrtUtbetaling")
@Table(name = "OVERSTYRT_UTBETALING")
data class OverstyrtUtbetaling(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "overstyrt_utbetaling_seq_generator")
        @SequenceGenerator(name = "overstyrt_utbetaling_seq_generator",
                           sequenceName = "overstyrt_utbetaling_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @Column(name = "fk_vedtak_id", nullable = false, updatable = false)
        val vedtakId: Long,

        @Column(name = "pk_po_person_id", nullable = false, updatable = false)
        var personId: Long,

        @Column(name = "prosent", nullable = false)
        val prosent: Int,

        @Column(name = "fom", nullable = false, columnDefinition = "DATE")
        @Convert(converter = YearMonthConverter::class)
        val fom: YearMonth,

        @Column(name = "tom", nullable = false, columnDefinition = "DATE")
        @Convert(converter = YearMonthConverter::class)
        val tom: YearMonth,

        @Enumerated(EnumType.STRING)
        @Column(name = "aarak", nullable = false)
        val aarsak: Årsak,

        @Column(name = "begrunnelse", nullable = false)
        var begrunnelse: String

) : BaseEntitet() {}


enum class Årsak(val klassifisering: String) {
    DELT_BOSTED("Delt bosted"),
}