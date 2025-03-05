package no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingvedtak

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.ekstern.restDomene.RestForenkletTilbakekrevingVedtak
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "ForenkletTilbakekrevingVedtak")
@Table(name = "forenklet_tilbakekreving_vedtak")
data class ForenkletTilbakekrevingVedtak(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "forenklet_tilbakekreving_vedtak_seq_generator")
    @SequenceGenerator(
        name = "forenklet_tilbakekreving_vedtak_seq_generator",
        sequenceName = "forenklet_tilbakekreving_vedtak_seq",
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

fun ForenkletTilbakekrevingVedtak.tilRestForenkletTilbakekrevingVedtak() =
    RestForenkletTilbakekrevingVedtak(
        id = this.id,
        behandlingId = this.behandlingId,
        samtykke = this.samtykke,
        fritekst = this.fritekst,
    )
