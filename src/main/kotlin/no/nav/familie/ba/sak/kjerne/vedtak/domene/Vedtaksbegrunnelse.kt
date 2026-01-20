package no.nav.familie.ba.sak.kjerne.vedtak.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.støtterFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.VedtaksbegrunnelseDto
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Vedtaksbegrunnelse")
@Table(name = "VEDTAKSBEGRUNNELSE")
class Vedtaksbegrunnelse(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtaksbegrunnelse_seq_generator")
    @SequenceGenerator(
        name = "vedtaksbegrunnelse_seq_generator",
        sequenceName = "vedtaksbegrunnelse_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_vedtaksperiode_id", nullable = false, updatable = false)
    val vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
    @Enumerated(EnumType.STRING)
    @Column(name = "vedtak_begrunnelse_spesifikasjon", updatable = false)
    val standardbegrunnelse: Standardbegrunnelse,
) {
    fun kopier(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): Vedtaksbegrunnelse =
        Vedtaksbegrunnelse(
            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
            standardbegrunnelse = this.standardbegrunnelse,
        )

    override fun toString(): String = "Vedtaksbegrunnelse(id=$id, standardbegrunnelse=$standardbegrunnelse)"
}

fun Vedtaksbegrunnelse.tilVedtaksbegrunnelseDto(
    sanityBegrunnelser: List<SanityBegrunnelse>,
    alleBegrunnelserSkalStøtteFritekst: Boolean,
) = VedtaksbegrunnelseDto(
    standardbegrunnelse = this.standardbegrunnelse.enumnavnTilString(),
    vedtakBegrunnelseType = this.standardbegrunnelse.vedtakBegrunnelseType,
    vedtakBegrunnelseSpesifikasjon = this.standardbegrunnelse.enumnavnTilString(),
    støtterFritekst = if (alleBegrunnelserSkalStøtteFritekst) true else this.standardbegrunnelse.støtterFritekst(sanityBegrunnelser),
)
