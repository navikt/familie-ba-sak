package no.nav.familie.ba.sak.behandling.vedtak

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
import java.util.*
import javax.persistence.*

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

        @ManyToOne(optional = true, fetch = FetchType.LAZY) @JoinColumn(name = "fk_vilkar_resultat_id")
        val vilkårResultat: VilkårResultat? = null,
) : BaseEntitet() {

    override fun hashCode(): Int {
        return Objects.hash(fom, tom, begrunnelse, vilkårResultat)
    }

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