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
@Entity(name = "FeilutbetaltValuta")
@Table(name = "FEILUTBETALT_VALUTA")
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "feilutbetalt_valuta_seq_generator")
    @SequenceGenerator(
        name = "feilutbetalt_valuta_seq_generator",
        sequenceName = "feilutbetalt_valuta_seq",
        allocationSize = 50
    )
    val id: Long = 0
) : BaseEntitet()
