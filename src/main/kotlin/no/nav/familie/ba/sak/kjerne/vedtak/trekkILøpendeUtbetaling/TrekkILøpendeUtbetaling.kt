package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
import javax.persistence.Column
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
    var fom: LocalDate,
    @Column(name = "tom", columnDefinition = "DATE")
    var tom: LocalDate,
    @Column(name = "feilutbetalt_beloep", nullable = false)
    var feilutbetaltBeløp: Int,

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trekk_i_loepende_utbetaling_seq_generator")
    @SequenceGenerator(
        name = "trekk_i_loepende_utbetaling_seq_generator",
        sequenceName = "trekk_i_loepende_utbetaling_seq",
        allocationSize = 50
    )
    val id: Long = 0
) : BaseEntitet()
