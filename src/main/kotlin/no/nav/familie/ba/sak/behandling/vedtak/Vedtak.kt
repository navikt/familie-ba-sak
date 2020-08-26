package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.restDomene.RestStønadBrevBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.common.BaseEntitet
import java.time.LocalDate
import javax.persistence.*


@Entity(name = "Vedtak")
@Table(name = "VEDTAK")
class Vedtak(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtak_seq_generator")
        @SequenceGenerator(name = "vedtak_seq_generator", sequenceName = "vedtak_seq", allocationSize = 50)
        val id: Long = 0,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "ansvarlig_enhet", nullable = true)
        var ansvarligEnhet: String? = null,

        @Column(name = "vedtaksdato", nullable = true)
        var vedtaksdato: LocalDate? = null,

        @Column(name = "stonad_brev_pdf", nullable = true)
        var stønadBrevPdF: ByteArray? = null,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @Column(name = "fk_forrige_vedtak_id")
        val forrigeVedtakId: Long? = null,

        @Column(name = "opphor_dato")
        val opphørsdato: LocalDate? = null,

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "vedtak",
                   cascade = [CascadeType.ALL],
                   orphanRemoval = true
        )
        val stønadBrevBegrunnelser: MutableSet<StønadBrevBegrunnelse> = mutableSetOf()

) : BaseEntitet() {

    override fun toString(): String {
        return "Vedtak(id=$id, behandling=$behandling, vedtaksdato=$vedtaksdato, aktiv=$aktiv, forrigeVedtakId=$forrigeVedtakId, opphørsdato=$opphørsdato)"
    }

    fun leggTilStønadBrevBegrunnelse(begrunnelse: StønadBrevBegrunnelse) {
        stønadBrevBegrunnelser.add(begrunnelse)
    }

    fun endreStønadBrevBegrunnelse(id: Long?, resultat: BehandlingResultatType?, begrunnelse: String?) {
        val brevBegrunnelseSomSkalEndres = stønadBrevBegrunnelser.find { it.id == id }

        if (brevBegrunnelseSomSkalEndres != null) {
            brevBegrunnelseSomSkalEndres.resultat = resultat
            brevBegrunnelseSomSkalEndres.begrunnelse = begrunnelse
        }
    }
}

/*fun hentStønadBrevMetadata(): StønadBrevMetadata? {
    return when {
        stønadBrevMetadata.isNullOrBlank() -> null
        else -> objectMapper.readValue<StønadBrevMetadata>(stønadBrevMetadata!!)
    }
}

val stønadBrevBegrunnelser: Map<String, Map<String, String>>
    get() {
        return if (stønadBrevMetadata.isNullOrBlank()) {
            emptyMap()
        } else {
            objectMapper.readValue<StønadBrevMetadata>(stønadBrevMetadata!!).begrunnelser
        }
    }

fun settStønadBrevBegrunnelse(periode: Periode, begrunnelse: String, begrunnelseId: String) {
    val metadata: StønadBrevMetadata = when (stønadBrevMetadata.isNullOrBlank()) {
        true -> {
            var begrunnelseMedId = mutableMapOf(begrunnelseId to begrunnelse)
            StønadBrevMetadata(
                    begrunnelser = mutableMapOf(periode.hash to begrunnelseMedId)
            )
        }
        false -> {
            val metadata: StønadBrevMetadata = objectMapper.readValue(stønadBrevMetadata!!)
            if(metadata.begrunnelser[periode.hash] === null) {
                var begrunnelseMedId = mutableMapOf(begrunnelseId to begrunnelse)
                metadata.begrunnelser[periode.hash] = begrunnelseMedId
                metadata
            } else {
                metadata.begrunnelser[periode.hash]!![begrunnelseId] = begrunnelse
                metadata
            }
        }
    }

    stønadBrevMetadata = objectMapper.writeValueAsString(metadata)
}
}

data class StønadBrevMetadata(
    var begrunnelser: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
)*/