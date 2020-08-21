package no.nav.familie.ba.sak.behandling.vedtak

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import javax.persistence.*


@Entity(name = "StønadBrevBegrunnelse")
@Table(name = "STONAD_BREV_BEGRUNNELSE")
data class StønadBrevBegrunnelse(

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stonad_brev_begrunnelse_seq_generator")
        @SequenceGenerator(name = "stonad_brev_begrunnelse_seq_generator",
                           sequenceName = "stonad_brev_begrunnelse_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @JsonIgnore
        @ManyToOne @JoinColumn(name = "fk_vedtak_id")
        val vedtak: Vedtak?,

        @Column(name = "periode", nullable = false)
        val periode: String,

        @Column(name = "begrunnelse")
        var begrunnelse: String,

        @Column(name = "arsak")
        var årsak: String


)