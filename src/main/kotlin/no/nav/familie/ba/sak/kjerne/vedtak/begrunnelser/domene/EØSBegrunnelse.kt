package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import javax.persistence.Column
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
@Entity(name = "eøsBegrunnelse")
@Table(name = "EOS_BEGRUNNELSE")
class EØSBegrunnelse(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eos_periodebegrunnelse_seq_generator")
    @SequenceGenerator(
        name = "eos_periodebegrunnelse_seq_generator",
        sequenceName = "eos_periodebegrunnelse_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @JsonIgnore
    @ManyToOne @JoinColumn(name = "fk_vedtaksperiode_id", nullable = false, updatable = false)
    val vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,

    @Enumerated(EnumType.STRING)
    @Column(name = "begrunnelse", updatable = false)
    val begrunnelse: EØSStandardbegrunnelse,
)
