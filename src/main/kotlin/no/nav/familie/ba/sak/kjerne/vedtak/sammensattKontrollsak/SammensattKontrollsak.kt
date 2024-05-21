package no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak

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
import no.nav.familie.ba.sak.ekstern.restDomene.RestSammensattKontrollsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "SammensattKontrollsak")
@Table(name = "sammensatt_kontrollsak")
class SammensattKontrollsak(
    @OneToOne
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,
    @Column(name = "fritekst", nullable = false)
    var fritekst: String,
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sammensatt_kontrollsak_seq_generator")
    @SequenceGenerator(
        name = "sammensatt_kontrollsak_seq_generator",
        sequenceName = "sammensatt_kontrollsak_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
) : BaseEntitet()

fun SammensattKontrollsak.tilRestSammensattKontrollsak() = RestSammensattKontrollsak(id = id, behandlingId = behandling.id, fritekst = fritekst)
