package no.nav.familie.ba.sak.kjerne.skjermetbarnsøker

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "SkjermetBarnSøker")
@Table(name = "SKJERMET_BARN_SOKER")
data class SkjermetBarnSøker(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "skjermet_barn_soker_seq_generator")
    @SequenceGenerator(name = "skjermet_barn_soker_seq_generator", sequenceName = "skjermet_barn_soker_seq", allocationSize = 50)
    val id: Long = 0,
    @Column(name = "fk_aktor_id", updatable = false, length = 50)
    val aktørId: String,
) : BaseEntitet() {
    override fun toString(): String = "SkjermetBarnSøker(id=$id)"
}
