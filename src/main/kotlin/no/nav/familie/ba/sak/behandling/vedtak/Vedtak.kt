package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.common.BaseEntitet
import java.time.LocalDate
import javax.persistence.*

@Entity(name = "Vedtak")
@Table(name = "VEDTAK")
data class Vedtak(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtak_seq_generator")
        @SequenceGenerator(name = "vedtak_seq_generator", sequenceName = "vedtak_seq", allocationSize = 50)
        val id: Long = 0,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "ansvarlig_saksbehandler", nullable = false)
        val ansvarligSaksbehandler: String,

        @Column(name = "vedtaksdato", nullable = false)
        val vedtaksdato: LocalDate,

        @Column(name = "stonad_brev_markdown", columnDefinition = "TEXT")
        val stønadBrevMarkdown: String = "",

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @Column(name = "fk_forrige_vedtak_id")
        val forrigeVedtakId: Long? = null,

        @Column(name = "opphor_dato")
        val opphørsdato: LocalDate? = null
) : BaseEntitet() {

        override fun toString(): String {
                return "Vedtak(id=$id, behandling=$behandling, ansvarligSaksbehandler='$ansvarligSaksbehandler', vedtaksdato=$vedtaksdato, aktiv=$aktiv, forrigeVedtakId=$forrigeVedtakId, opphørsdato=$opphørsdato)"
        }
}