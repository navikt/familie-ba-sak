package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
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
        val vedtakBegrunnelser: MutableSet<VedtakBegrunnelse> = mutableSetOf()

) : BaseEntitet() {

    override fun toString(): String {
        return "Vedtak(id=$id, behandling=$behandling, vedtaksdato=$vedtaksdato, aktiv=$aktiv, opphørsdato=$opphørsdato)"
    }

    private fun settBegrunnelser(nyeBegrunnelser: Set<VedtakBegrunnelse>) {
        vedtakBegrunnelser.clear()
        vedtakBegrunnelser.addAll(nyeBegrunnelser)
    }

    fun hentBegrunnelse(begrunnelseId: Long): VedtakBegrunnelse? {
        return vedtakBegrunnelser.find { it.id == begrunnelseId }
    }

    fun leggTilBegrunnelse(begrunnelse: VedtakBegrunnelse) {
        vedtakBegrunnelser.add(begrunnelse)
    }

    fun slettBegrunnelse(begrunnelseId: Long) {
        hentBegrunnelse(begrunnelseId)
        ?: throw FunksjonellFeil(melding = "Prøver å slette en begrunnelse som ikke finnes",
                                 frontendFeilmelding = "Begrunnelsen du prøver å slette finnes ikke i systemet.")

        settBegrunnelser(vedtakBegrunnelser.filter { begrunnelseId != it.id }.toSet())
    }

    fun slettBegrunnelserForPeriode(periode: Periode) {
        settBegrunnelser(vedtakBegrunnelser.filterNot { it.fom == periode.fom && it.tom == periode.tom }.toSet())
    }

    fun slettAlleBegrunnelser() {
        settBegrunnelser(mutableSetOf())
    }

    fun hentHjemler(): SortedSet<Int> {
        val hjemler = mutableSetOf<Int>()
        this.vedtakBegrunnelser.forEach {
            hjemler.addAll(it.begrunnelse?.hentHjemler()?.toSet() ?: emptySet())
        }
        return hjemler.toSortedSet()
    }

    fun hentHjemmelTekst(): String {
        val hjemler = this.hentHjemler().toIntArray().map { it.toString() }
        return when (hjemler.size) {
            0 -> throw Feil("Fikk ikke med noen hjemler for vedtak")
            1 -> "§ ${hjemler[0]}"
            else -> "§§ ${Utils.slåSammen(hjemler)}"
        }
    }
}