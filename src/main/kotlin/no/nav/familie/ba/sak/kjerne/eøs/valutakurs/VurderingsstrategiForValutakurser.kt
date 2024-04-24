package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

// Kan sannsynligvis slettes på sikt om vi ikke lenger trenger å ha mulighet til å overstyre valutakursene
@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "VurderingsstrategiForValutakurserDB")
@Table(name = "vurderingsstrategi_for_valutakurser")
data class VurderingsstrategiForValutakurserDB(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vurderingsstrategi_for_valutakurser_seq_generator")
    @SequenceGenerator(name = "vurderingsstrategi_for_valutakurser_seq_generator", sequenceName = "vurderingsstrategi_for_valutakurser_seq", allocationSize = 50)
    val id: Long = 0,
    @Column(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandlingId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "vurderingsstrategi_for_valutakurser", nullable = false)
    val vurderingsstrategiForValutakurser: VurderingsstrategiForValutakurser = VurderingsstrategiForValutakurser.AUTOMATISK,
)

enum class VurderingsstrategiForValutakurser {
    MANUELL,
    AUTOMATISK,
}
