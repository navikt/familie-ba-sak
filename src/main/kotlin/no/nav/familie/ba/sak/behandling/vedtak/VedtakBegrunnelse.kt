package no.nav.familie.ba.sak.behandling.vedtak

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
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
@Entity(name = "VedtakBegrunnelse")
@Table(name = "VEDTAK_BEGRUNNELSE")
data class VedtakBegrunnelse(

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtak_begrunnelse_seq_generator")
        @SequenceGenerator(name = "vedtak_begrunnelse_seq_generator",
                           sequenceName = "vedtak_begrunnelse_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @JsonIgnore
        @ManyToOne @JoinColumn(name = "fk_vedtak_id")
        val vedtak: Vedtak,

        @Column(name = "fom", updatable = false)
        val fom: LocalDate?,

        @Column(name = "tom", updatable = false)
        val tom: LocalDate?,

        @Column(name = "begrunnelse")
        @Enumerated(EnumType.STRING)
        var begrunnelse: VedtakBegrunnelseSpesifikasjon,

        @Column(name = "brev_begrunnelse", columnDefinition = "TEXT")
        var brevBegrunnelse: String? = "",

        @ManyToOne @JoinColumn(name = "fk_vilkar_resultat_id")
        val vilkårResultat: VilkårResultat? = null,
) : BaseEntitet() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VedtakBegrunnelse
        return vedtak == other.vedtak &&
               fom == other.fom &&
               tom == other.tom &&
               begrunnelse == other.begrunnelse &&
               vilkårResultat == other.vilkårResultat
    }
}