package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.restDomene.RestDeleteVedtakBegrunnelser
import no.nav.familie.ba.sak.behandling.restDomene.RestPostFritekstVedtakBegrunnelser
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.toVedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
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
        var opphørsdato: LocalDate? = null,

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

    fun settFritekstbegrunnelser(restPostFritekstVedtakBegrunnelser: RestPostFritekstVedtakBegrunnelser) {
        settBegrunnelser(
                (vedtakBegrunnelser.filterNot {
                    it.fom == restPostFritekstVedtakBegrunnelser.fom &&
                    it.tom == restPostFritekstVedtakBegrunnelser.tom &&
                    it.begrunnelse == restPostFritekstVedtakBegrunnelser.vedtaksperiodetype.toVedtakBegrunnelseSpesifikasjon()
                } +
                 restPostFritekstVedtakBegrunnelser.fritekster.map {
                     VedtakBegrunnelse(
                             vedtak = this,
                             fom = restPostFritekstVedtakBegrunnelser.fom,
                             tom = restPostFritekstVedtakBegrunnelser.tom,
                             begrunnelse = restPostFritekstVedtakBegrunnelser.vedtaksperiodetype.toVedtakBegrunnelseSpesifikasjon(),
                             brevBegrunnelse = it
                     )
                 }).toSet())
    }

    fun slettBegrunnelse(begrunnelseId: Long) {
        hentBegrunnelse(begrunnelseId)
        ?: throw FunksjonellFeil(melding = "Prøver å slette en begrunnelse som ikke finnes",
                                 frontendFeilmelding = "Begrunnelsen du prøver å slette finnes ikke i systemet.")

        settBegrunnelser(vedtakBegrunnelser.filter { begrunnelseId != it.id }.toSet())
    }

    fun slettUtbetalingOgOpphørBegrunnelserBegrunnelserForPeriode(periode: Periode) {
        settBegrunnelser(vedtakBegrunnelser.filterNot {
            it.begrunnelse.vedtakBegrunnelseType != VedtakBegrunnelseType.AVSLAG ||
            (it.fom == periode.fom && it.tom == periode.tom)
        }.toSet())
    }

    fun slettBegrunnelserForPeriodeOgVedtaksbegrunnelseTyper(restDeleteVedtakBegrunnelser: RestDeleteVedtakBegrunnelser) {
        settBegrunnelser(vedtakBegrunnelser.filterNot {
            (it.fom == restDeleteVedtakBegrunnelser.fom &&
             it.tom == restDeleteVedtakBegrunnelser.tom &&
             restDeleteVedtakBegrunnelser.vedtakbegrunnelseTyper.contains(it.begrunnelse.vedtakBegrunnelseType))
        }.toSet())
    }

    fun slettAlleUtbetalingOpphørOgAvslagFritekstBegrunnelser() = settBegrunnelser(
            vedtakBegrunnelser.filter { it.begrunnelse.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG && it.begrunnelse != VedtakBegrunnelseSpesifikasjon.AVSLAG_FRITEKST }.toSet())

    fun slettAvslagBegrunnelse(vilkårResultatId: Long,
                               begrunnelse: VedtakBegrunnelseSpesifikasjon) {
        settBegrunnelser(vedtakBegrunnelser.filterNot {
            it.vilkårResultat?.id == vilkårResultatId &&
            it.begrunnelse == begrunnelse
        }.toSet())
    }

    fun slettAlleAvslagBegrunnelserForVilkår(vilkårResultatId: Long) = settBegrunnelser(vedtakBegrunnelser.filterNot { it.vilkårResultat?.id == vilkårResultatId }
                                                                                                .toSet())

    fun hentHjemler(): SortedSet<Int> {
        val hjemler = mutableSetOf<Int>()
        this.vedtakBegrunnelser.forEach {
            hjemler.addAll(it.begrunnelse.hentHjemler()?.toSet() ?: emptySet())
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