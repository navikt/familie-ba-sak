package no.nav.familie.ba.sak.behandling.vedtak.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table


@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "VedtaksbegrunnelseFritekst")
@Table(name = "VEDTAKSBEGRUNNELSE_FRITEKST")
class VedtaksbegrunnelseFritekst(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtaksbegrunnelse_fritekst_seq_generator")
        @SequenceGenerator(name = "vedtaksbegrunnelse_fritekst_seq_generator",
                           sequenceName = "vedtaksbegrunnelse_fritekst_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @JsonIgnore
        @ManyToOne @JoinColumn(name = "fk_vedtaksperiode_id")
        val vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,

        @Column(name = "fritekst", updatable = false)
        val fritekst: String,
)

fun tilVedtaksbegrunnelseFritekst(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
                                  fritekst: String) = VedtaksbegrunnelseFritekst(
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        fritekst = fritekst
)