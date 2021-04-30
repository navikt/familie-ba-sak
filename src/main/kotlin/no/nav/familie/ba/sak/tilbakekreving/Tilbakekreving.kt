package no.nav.familie.ba.sak.tilbakekreving

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import javax.persistence.*

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

        @OneToOne(optional = false) @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

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
            valg = valg,
            varsel = varsel,
            begrunnelse = begrunnelse,
            tilbakekrevingsbehandlingId = tilbakekrevingsbehandlingId,
    )
}
