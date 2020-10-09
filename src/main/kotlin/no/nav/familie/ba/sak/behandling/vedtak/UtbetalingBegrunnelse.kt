package no.nav.familie.ba.sak.behandling.vedtak

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.common.BaseEntitet
import java.time.LocalDate
import javax.persistence.*


@Entity(name = "UtbetalingBegrunnelse")
@Table(name = "UTBETALING_BEGRUNNELSE")
data class UtbetalingBegrunnelse(

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "utbetaling_begrunnelse_seq_generator")
        @SequenceGenerator(name = "utbetaling_begrunnelse_seq_generator",
                           sequenceName = "utbetaling_begrunnelse_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @JsonIgnore
        @ManyToOne @JoinColumn(name = "fk_vedtak_id")
        val vedtak: Vedtak,

        @Column(name = "fom", updatable = false, nullable = false)
        val fom: LocalDate,

        @Column(name = "tom", updatable = false, nullable = false)
        val tom: LocalDate,

        @Column(name = "begrunnelse_type")
        @Enumerated(EnumType.STRING)
        var begrunnelseType: VedtakBegrunnelseType? = null,

        @Column(name = "vedtak_begrunnelse")
        @Enumerated(EnumType.STRING)
        var vedtakBegrunnelse: VedtakBegrunnelse? = null,

        @Column(name = "brev_begrunnelse")
        var brevBegrunnelse: String? = ""
) : BaseEntitet() {
    fun erLik(annen: UtbetalingBegrunnelse) = this.fom == annen.fom
                                              && this.tom == annen.tom
                                              && this.begrunnelseType == annen.begrunnelseType
                                              && this.vedtakBegrunnelse == annen.vedtakBegrunnelse
                                              && this.brevBegrunnelse == annen.brevBegrunnelse
}