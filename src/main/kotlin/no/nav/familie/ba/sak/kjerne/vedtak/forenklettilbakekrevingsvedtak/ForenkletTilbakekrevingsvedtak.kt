package no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingsvedtak

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.ekstern.restDomene.RestForenkletTilbakekrevingsvedtak
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "ForenkletTilbakekrevingsvedtak")
@Table(name = "forenklet_tilbakekrevingsvedtak")
data class ForenkletTilbakekrevingsvedtak(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "forenklet_tilbakekrevingsvedtak_seq_generator")
    @SequenceGenerator(
        name = "forenklet_tilbakekrevingsvedtak_seq_generator",
        sequenceName = "forenklet_tilbakekrevingsvedtak_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,
    @Column(name = "samtykke", nullable = false, updatable = true)
    var samtykke: Boolean,
    @Column(name = "fritekst", nullable = false, updatable = true)
    var fritekst: String,
) : BaseEntitet()

fun ForenkletTilbakekrevingsvedtak.tilRestForenkletTilbakekrevingsvedtak() =
    RestForenkletTilbakekrevingsvedtak(
        id = this.id,
        behandlingId = this.behandlingId,
        samtykke = this.samtykke,
        fritekst = this.fritekst,
    )
