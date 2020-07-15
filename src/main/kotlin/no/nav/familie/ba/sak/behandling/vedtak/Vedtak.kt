package no.nav.familie.ba.sak.behandling.vedtak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.kontrakter.felles.objectMapper
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

        @Column(name = "ansvarlig_enhet", nullable = true)
        var ansvarligEnhet: String? = null,

        @Column(name = "vedtaksdato", nullable = false)
        var vedtaksdato: LocalDate,

        @Column(name = "stonad_brev_markdown", columnDefinition = "TEXT")
        var stønadBrevMarkdown: String = "",

        @Column(name = "stonad_brev_pdf", nullable = true)
        var stønadBrevPdF: ByteArray? = null,

        @Column(name = "stonad_brev_metadata", columnDefinition = "TEXT")
        var stønadBrevMetadata: String = objectMapper.writeValueAsString(StønadBrevMetadata()),

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @Column(name = "fk_forrige_vedtak_id")
        val forrigeVedtakId: Long? = null,

        @Column(name = "opphor_dato")
        val opphørsdato: LocalDate? = null
) : BaseEntitet() {

    override fun toString(): String {
        return "Vedtak(id=$id, behandling=$behandling, vedtaksdato=$vedtaksdato, aktiv=$aktiv, forrigeVedtakId=$forrigeVedtakId, opphørsdato=$opphørsdato)"
    }

    fun hentStønadBrevMetadata(): StønadBrevMetadata? {
        return when {
            stønadBrevMetadata.isBlank() -> null
            else -> objectMapper.readValue<StønadBrevMetadata>(stønadBrevMetadata)
        }
    }

    val stønadBrevBegrunnelser: Map<String, String>
        get() {
            return if (stønadBrevMetadata.isBlank()) {
                emptyMap()
            } else {
                objectMapper.readValue<StønadBrevMetadata>(stønadBrevMetadata).begrunnelser
            }
        }

    fun settStønadBrevBegrunnelse(periode: Periode, begrunnelse: String) {
        val metadata: StønadBrevMetadata = when (stønadBrevMetadata.isBlank()) {
            true -> {
                StønadBrevMetadata(
                        begrunnelser = mutableMapOf(periode.hash to begrunnelse)
                )
            }
            false -> {
                val metadata: StønadBrevMetadata = objectMapper.readValue(stønadBrevMetadata)
                metadata.begrunnelser[periode.hash] = begrunnelse
                metadata
            }
        }

        stønadBrevMetadata = objectMapper.writeValueAsString(metadata)
    }
}

data class StønadBrevMetadata(
        var begrunnelser: MutableMap<String, String> = mutableMapOf()
)
