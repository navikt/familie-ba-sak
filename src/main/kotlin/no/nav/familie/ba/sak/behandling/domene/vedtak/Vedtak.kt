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
        @SequenceGenerator(name = "vedtak_seq", allocationSize = 1)
        val id: Long? = null,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "ansvarlig_saksbehandler", nullable = false)
        val ansvarligSaksbehandler: String,

        @Column(name = "vedtaksdato", nullable = false)
        val vedtaksdato: LocalDate,

        @Column(name = "stonad_brev_markdown", columnDefinition = "TEXT")
        var stønadBrevMarkdown: String = "",

        @Enumerated(EnumType.STRING)
        @Column(name = "resultat", nullable = false)
        val resultat: VedtakResultat,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,


        @Column(name = "fk_forrige_vedtak_id")
        var forrigeVedtakId: Long? = null,

        @Column(name = "opphor_dato")
        var opphørsdato: LocalDate? = null,

        @Column(name = "begrunnelse", columnDefinition = "TEXT")
        var begrunnelse: String

) : BaseEntitet()

enum class VedtakResultat {
    INNVILGET, AVSLÅTT, OPPHØRT, HENLAGT
}

fun VedtakResultat.toDokGenTemplate(): String {
    return when (this) {
        VedtakResultat.INNVILGET -> "Innvilget"
        VedtakResultat.AVSLÅTT -> "Avslag"
        else -> throw RuntimeException("Invalid/Unsupported vedtak result")
    }
}