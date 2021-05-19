package no.nav.familie.ba.sak.behandling.vedtak.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Vedtaksperiode")
@Table(name = "VEDTAKSPERIODE")
data class VedtaksperiodeMedBegrunnelser(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtaksperiode_seq_generator")
        @SequenceGenerator(name = "vedtaksperiode_seq_generator",
                           sequenceName = "vedtaksperiode_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @JsonIgnore
        @ManyToOne @JoinColumn(name = "fk_behandling_id")
        val behandling: Behandling,

        @Column(name = "fom", updatable = false)
        val fom: LocalDate? = null,

        @Column(name = "tom", updatable = false)
        val tom: LocalDate? = null,

        @Column(name = "type", updatable = false)
        @Enumerated(EnumType.STRING)
        val type: Vedtaksperiodetype,

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "vedtaksperiodeMedBegrunnelser",
                   cascade = [CascadeType.ALL],
                   orphanRemoval = true
        )
        val begrunnelser: Set<Vedtaksbegrunnelse> = emptySet(),

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "vedtaksperiodeMedBegrunnelser",
                   cascade = [CascadeType.ALL],
                   orphanRemoval = true
        )
        val fritekster: Set<VedtaksbegrunnelseFritekst> = emptySet(),
) : BaseEntitet()