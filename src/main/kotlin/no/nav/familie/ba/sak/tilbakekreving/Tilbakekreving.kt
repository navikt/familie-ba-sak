package no.nav.familie.ba.sak.tilbakekreving

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Tilbakekreving")
@Table(name = "tilbakekreving")
data class Tilbakekreving(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tilbakekreving_seq_generator")
        @SequenceGenerator(name = "tilbakekreving_seq_generator",
                           sequenceName = "tilbakekreving_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @OneToOne(optional = false) @JoinColumn(name = "fk_vedtak_id", nullable = false, updatable = false)
        val vedtak: Vedtak,

        @Enumerated(EnumType.STRING)
        @Column(name = "valg")
        var valg: Tilbakekrevingsvalg,

        @Column(name = "varsel")
        var varsel: String? = null,

        @Column(name = "begrunnelse")
        var begrunnelse: String,

        @Column(name = "tilbakekrevingsbehandling_id")
        var tilbakekrevingsbehandlingId: String?,
) : BaseEntitet() {

    fun tilRestTilbakekreving() = RestTilbakekreving(
            vedtakId = this.vedtak.id,
            valg = valg,
            varsel = varsel,
            begrunnelse = begrunnelse,
            tilbakekrevingsbehandlingId = tilbakekrevingsbehandlingId,
    )

}
