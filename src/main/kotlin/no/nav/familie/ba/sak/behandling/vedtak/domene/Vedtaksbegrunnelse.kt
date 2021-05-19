package no.nav.familie.ba.sak.behandling.vedtak.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.restDomene.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.tilVedtaksperiodeType
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.StringListConverter
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table


@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Vedtaksbegrunnelse")
@Table(name = "VEDTAKSBEGRUNNELSE")
class Vedtaksbegrunnelse(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtaksbegrunnelse_seq_generator")
        @SequenceGenerator(name = "vedtaksbegrunnelse_seq_generator",
                           sequenceName = "vedtaksbegrunnelse_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @JsonIgnore
        @ManyToOne @JoinColumn(name = "fk_vedtaksperiode_id")
        var vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,

        @Enumerated(EnumType.STRING)
        @Column(name = "vedtak_begrunnelse_spesifikasjon", updatable = false)
        val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,

        @Column(name = "identer", columnDefinition = "TEXT")
        @Convert(converter = StringListConverter::class)
        val identer: List<String> = emptyList(),
)

fun Vedtaksbegrunnelse.tilRestVedtaksbegrunnelse() = RestVedtaksbegrunnelse(
        vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
        identer = this.identer
)

fun RestVedtaksbegrunnelse.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): Vedtaksbegrunnelse {
    if (this.vedtakBegrunnelseSpesifikasjon.erFritekstBegrunnelse()) {
        throw Feil("Kan ikke fastsette fritekstbegrunnelse på begrunnelser på vedtaksperioder. Bruk heller fritekster.")
    }

    if (this.vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType.tilVedtaksperiodeType() != vedtaksperiodeMedBegrunnelser.type) {
        throw Feil("Begrunnelsestype ${this.vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType} passer ikke med typen '${vedtaksperiodeMedBegrunnelser.type}' som er satt på perioden.")
    }

    return Vedtaksbegrunnelse(
            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
            vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
            identer = this.identer
    )
}