package no.nav.familie.ba.sak.kjerne.minside

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "MinsideAktivering")
@Table(name = "MINSIDE_AKTIVERING")
data class MinsideAktivering(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "minside_aktivering_seq_generator")
    @SequenceGenerator(name = "minside_aktivering_seq_generator", sequenceName = "minside_aktivering_seq", allocationSize = 50)
    val id: Long = 0,
    @OneToOne(optional = false)
    @JoinColumn(name = "fk_aktor_id", nullable = false, updatable = false)
    val aktør: Aktør,
    @Column(name = "aktivert", nullable = false)
    val aktivert: Boolean = false,
) : BaseEntitet()
