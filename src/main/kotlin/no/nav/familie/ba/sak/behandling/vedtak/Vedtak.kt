package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
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

    fun settStønadBrevBegrunnelser(nyeBegrunnelser: Set<StønadBrevBegrunnelse>) {
        stønadBrevBegrunnelser.clear()
        stønadBrevBegrunnelser.addAll(nyeBegrunnelser)
    }

    fun hentStønadBrevBegrunnelse(begrunnelseId: Long): StønadBrevBegrunnelse? {
        return stønadBrevBegrunnelser.find { it.id == begrunnelseId }
    }

    fun leggTilStønadBrevBegrunnelse(begrunnelse: StønadBrevBegrunnelse) {
        stønadBrevBegrunnelser.add(begrunnelse)
    }

    fun slettStønadBrevBegrunnelse(begrunnelseId: Long) {
        hentStønadBrevBegrunnelse(begrunnelseId) ?: throw Feil(message = "Prøver å slette en begrunnelse som ikke finnes",
                                                               frontendFeilmelding = "Begrunnelsen du prøver å slette finnes ikke i systemet.")

        settStønadBrevBegrunnelser(stønadBrevBegrunnelser.filter { begrunnelseId != it.id }.toSet())
    }

    fun endreStønadBrevBegrunnelse(id: Long?, resultat: BehandlingResultatType?, begrunnelse: String?) {
        val brevBegrunnelseSomSkalEndres = stønadBrevBegrunnelser.find { it.id == id }

        if (brevBegrunnelseSomSkalEndres != null) {
            brevBegrunnelseSomSkalEndres.resultat = resultat
            brevBegrunnelseSomSkalEndres.begrunnelse = begrunnelse
        } else {
            throw Feil(message = "Prøver å endre på en begrunnelse som ikke finnes")
        }
    }
}