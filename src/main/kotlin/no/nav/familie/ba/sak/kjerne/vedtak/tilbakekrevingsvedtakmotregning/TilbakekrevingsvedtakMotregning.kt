package no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning

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
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekrevingsvedtakMotregning
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "TilbakekrevingsvedtakMotregning")
@Table(name = "tilbakekrevingsvedtak_motregning")
data class TilbakekrevingsvedtakMotregning(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tilbakekrevingsvedtak_motregning_seq_generator")
    @SequenceGenerator(
        name = "tilbakekrevingsvedtak_motregning_seq_generator",
        sequenceName = "tilbakekrevingsvedtak_motregning_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @OneToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id")
    val behandling: Behandling,
    @Column(name = "samtykke", nullable = false, updatable = true)
    var samtykke: Boolean,
    @Column(name = "aarsak_til_feilutbetaling", nullable = true, updatable = true)
    var årsakTilFeilutbetaling: String? = null,
    @Column(name = "vurdering_av_skyld", nullable = true, updatable = true)
    var vurderingAvSkyld: String? = null,
    @Column(name = "varsel_dato", nullable = false, updatable = true)
    var varselDato: LocalDate = LocalDate.now(),
    @Column(name = "vedtak_pdf", nullable = true)
    var vedtakPdf: ByteArray? = null,
) : BaseEntitet()

fun TilbakekrevingsvedtakMotregning.tilRestTilbakekrevingsvedtakMotregning() =
    RestTilbakekrevingsvedtakMotregning(
        id = this.id,
        behandlingId = this.behandling.id,
        årsakTilFeilutbetaling = this.årsakTilFeilutbetaling,
        vurderingAvSkyld = this.vurderingAvSkyld,
        varselDato = this.varselDato,
        samtykke = this.samtykke,
    )
