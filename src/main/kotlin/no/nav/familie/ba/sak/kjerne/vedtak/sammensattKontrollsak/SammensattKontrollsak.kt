package no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.ekstern.restDomene.SammensattKontrollsakDto
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "SammensattKontrollsak")
@Table(name = "sammensatt_kontrollsak")
data class SammensattKontrollsak(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sammensatt_kontrollsak_seq_generator")
    @SequenceGenerator(
        name = "sammensatt_kontrollsak_seq_generator",
        sequenceName = "sammensatt_kontrollsak_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,
    @Column(name = "fritekst", nullable = false)
    var fritekst: String,
) : BaseEntitet()

fun SammensattKontrollsak.tilRestSammensattKontrollsak() = SammensattKontrollsakDto(id = id, behandlingId = behandlingId, fritekst = fritekst)
