package no.nav.familie.ba.sak.behandling.domene.vedtak

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.common.BaseEntitet
import java.time.LocalDate
import javax.persistence.*

@Entity(name = "Vedtak")
@Table(name = "VEDTAK")
data class Vedtak(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtak_seq")
        @SequenceGenerator(name = "vedtak_seq")
        val id: Long? = null,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "ansvarlig_saksbehandler", nullable = false)
        val ansvarligSaksbehandler: String,

        @Column(name = "vedtaksdato", nullable = false)
        val vedtaksdato: LocalDate,

        @Column(name = "stonad_brev_markdown", columnDefinition = "TEXT")
        var stønadBrevMarkdown: String = "",

        // TODO Endre til resultat (INNVILGET, AVSLÅTT, OPPHØRT, HENLAGT)
        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        var resultat: VedtakResultat = VedtakResultat.OPPRETTET,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true
) : BaseEntitet()

enum class VedtakResultat {
        OPPRETTET, INNVILGET, AVSLÅTT, OPPHØRT, HENLAGT
}