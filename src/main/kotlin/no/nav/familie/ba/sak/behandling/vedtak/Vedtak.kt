package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.restDomene.RestPutUtbetalingBegrunnelse
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import java.time.LocalDate
import java.time.LocalDateTime
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

        @Column(name = "vedtaksdato", nullable = true)
        var vedtaksdato: LocalDateTime? = null,

        @Column(name = "stonad_brev_pdf", nullable = true)
        var stønadBrevPdF: ByteArray? = null,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @Column(name = "opphor_dato")
        val opphørsdato: LocalDate? = null,

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "vedtak",
                   cascade = [CascadeType.ALL],
                   orphanRemoval = true
        )
        val utbetalingBegrunnelser: MutableSet<UtbetalingBegrunnelse> = mutableSetOf()

) : BaseEntitet() {

    override fun toString(): String {
        return "Vedtak(id=$id, behandling=$behandling, vedtaksdato=$vedtaksdato, aktiv=$aktiv, opphørsdato=$opphørsdato)"
    }

    private fun settUtbetalingBegrunnelser(nyeBegrunnelser: Set<UtbetalingBegrunnelse>) {
        utbetalingBegrunnelser.clear()
        utbetalingBegrunnelser.addAll(nyeBegrunnelser)
    }

    fun hentUtbetalingBegrunnelse(begrunnelseId: Long): UtbetalingBegrunnelse? {
        return utbetalingBegrunnelser.find { it.id == begrunnelseId }
    }

    fun leggTilUtbetalingBegrunnelse(begrunnelse: UtbetalingBegrunnelse) {
        utbetalingBegrunnelser.add(begrunnelse)
    }

    fun slettUtbetalingBegrunnelse(begrunnelseId: Long) {
        hentUtbetalingBegrunnelse(begrunnelseId)
        ?: throw FunksjonellFeil(melding = "Prøver å slette en begrunnelse som ikke finnes",
                                 frontendFeilmelding = "Begrunnelsen du prøver å slette finnes ikke i systemet.")

        settUtbetalingBegrunnelser(utbetalingBegrunnelser.filter { begrunnelseId != it.id }.toSet())
    }

    fun slettUtbetalingBegrunnelser() {
        settUtbetalingBegrunnelser(mutableSetOf())
    }

    fun endreUtbetalingBegrunnelse(id: Long?,
                                   putUtbetalingBegrunnelse: RestPutUtbetalingBegrunnelse,
                                   brevBegrunnelse: String?) {
        val utbetalingBegrunnelseSomSkalEndres = utbetalingBegrunnelser.find { it.id == id }

        if (utbetalingBegrunnelseSomSkalEndres != null) {
            utbetalingBegrunnelseSomSkalEndres.begrunnelseType = putUtbetalingBegrunnelse.vedtakBegrunnelse?.vedtakBegrunnelseType
                                                                 ?: putUtbetalingBegrunnelse.vedtakBegrunnelseType
            utbetalingBegrunnelseSomSkalEndres.vedtakBegrunnelse = putUtbetalingBegrunnelse.vedtakBegrunnelse
            utbetalingBegrunnelseSomSkalEndres.brevBegrunnelse = brevBegrunnelse
        } else {
            throw Feil(message = "Prøver å endre på en begrunnelse som ikke finnes")
        }
    }
}