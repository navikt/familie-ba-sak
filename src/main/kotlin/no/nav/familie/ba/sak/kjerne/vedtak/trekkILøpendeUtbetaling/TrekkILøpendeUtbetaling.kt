package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.YearMonth
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "TrekkILøpendeUtbetaling")
@Table(name = "TREKK_I_LOEPENDE_UTBETALING")
data class TrekkILøpendeUtbetaling(
    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,
    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val fom: YearMonth?,
    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val tom: YearMonth?,
    @Column(name = "feilutbetalt_beloep", nullable = false)
    val feilutbetaltBeløp: Int
) {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trekk_i_loepende_utbetaling_seq_generator")
    @SequenceGenerator(
        name = "trekk_i_loepende_utbetaling_seq_generator",
        sequenceName = "trekk_i_loepende_utbetaling_seq",
        allocationSize = 50
    )
    val id: Long = 0
}
