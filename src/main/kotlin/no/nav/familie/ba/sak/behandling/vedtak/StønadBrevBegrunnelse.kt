package no.nav.familie.ba.sak.behandling.vedtak

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelse
import no.nav.familie.ba.sak.dokument.domene.maler.Innvilget
import java.time.LocalDate
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
        val vedtak: Vedtak,

        @Column(name = "fom", updatable = false, nullable = false)
        val fom: LocalDate,

        @Column(name = "tom", updatable = false, nullable = false)
        val tom: LocalDate,

        @Column(name = "resultat")
        @Enumerated(EnumType.STRING)
        var resultat: BehandlingResultatType?,

        @Column(name = "begrunnelse")
        var begrunnelse: String?
)