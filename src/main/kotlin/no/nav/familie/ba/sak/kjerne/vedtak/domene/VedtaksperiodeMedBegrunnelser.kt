package no.nav.familie.ba.sak.kjerne.vedtak.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.hentAndelerForSegment
import no.nav.familie.ba.sak.kjerne.beregning.domene.hentLøpendeAndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.lagUtbetalingsperiodeDetaljer
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.utledSegmenter
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import org.hibernate.annotations.SortComparator
import java.time.LocalDate
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
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
@Entity(name = "Vedtaksperiode")
@Table(name = "VEDTAKSPERIODE")
data class VedtaksperiodeMedBegrunnelser(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtaksperiode_seq_generator")
    @SequenceGenerator(
        name = "vedtaksperiode_seq_generator",
        sequenceName = "vedtaksperiode_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @JsonIgnore
    @ManyToOne @JoinColumn(name = "fk_vedtak_id")
    val vedtak: Vedtak,

    @Column(name = "fom", updatable = false)
    val fom: LocalDate? = null,

    @Column(name = "tom", updatable = false)
    val tom: LocalDate? = null,

    @Column(name = "type", updatable = false)
    @Enumerated(EnumType.STRING)
    val type: Vedtaksperiodetype,

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vedtaksperiodeMedBegrunnelser",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @SortComparator(BegrunnelseComparator::class)
    val begrunnelser: MutableSet<Vedtaksbegrunnelse> = sortedSetOf(comparator),

    // Bruker list for å bevare rekkefølgen som settes frontend.
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vedtaksperiodeMedBegrunnelser",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf()

) : BaseEntitet() {

    fun settBegrunnelser(nyeBegrunnelser: List<Vedtaksbegrunnelse>) {
        begrunnelser.clear()
        begrunnelser.addAll(nyeBegrunnelser)
    }

    fun settFritekster(nyeFritekster: List<VedtaksbegrunnelseFritekst>) {
        fritekster.clear()
        fritekster.addAll(nyeFritekster)
    }

    fun harFriteksterUtenStandardbegrunnelser(): Boolean {
        return (type == Vedtaksperiodetype.OPPHØR || type == Vedtaksperiodetype.AVSLAG) && fritekster.isNotEmpty() && begrunnelser.isEmpty()
    }

    fun harFriteksterOgStandardbegrunnelser(): Boolean {
        return fritekster.isNotEmpty() && begrunnelser.isNotEmpty()
    }

    fun hentNullablePeriode() = NullablePeriode(
        fom = this.fom,
        tom = this.tom,
    )

    fun hentUtbetalingsperiodeDetaljer(
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ) =
        if (this.type == Vedtaksperiodetype.UTBETALING || this.type == Vedtaksperiodetype.ENDRET_UTBETALING || this.type == Vedtaksperiodetype.FORTSATT_INNVILGET) {
            val andelerForVedtaksperiodetype = andelerTilkjentYtelse.filter {
                if (this.type == Vedtaksperiodetype.ENDRET_UTBETALING) {
                    endretUtbetalingAndelSkalVæreMed(it)
                } else {
                    it.endretUtbetalingAndeler.isEmpty()
                }
            }
            val vertikaltSegmentForVedtaksperiode =
                if (this.type == Vedtaksperiodetype.FORTSATT_INNVILGET)
                    andelerForVedtaksperiodetype.hentLøpendeAndelForVedtaksperiode()
                else hentVertikaltSegmentForVedtaksperiode(andelerForVedtaksperiodetype)

            val andelerForSegment =
                andelerForVedtaksperiodetype.hentAndelerForSegment(vertikaltSegmentForVedtaksperiode)

            andelerForSegment.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag)
        } else {
            emptyList()
        }

    fun hentVertikaltSegmentForVedtaksperiode(
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>
    ) = andelerTilkjentYtelse
        .utledSegmenter()
        .find { localDateSegment ->
            localDateSegment.fom == this.fom || localDateSegment.tom == this.tom
        } ?: throw Feil("Finner ikke segment for vedtaksperiode (${this.fom}, ${this.tom})")

    fun endretUtbetalingAndelSkalVæreMed(andelTilkjentYtelse: AndelTilkjentYtelse) =
        andelTilkjentYtelse.endretUtbetalingAndeler.isNotEmpty() && andelTilkjentYtelse.endretUtbetalingAndeler.all { endretUtbetalingAndel ->
            this.begrunnelser.any { vedtaksbegrunnelse ->
                vedtaksbegrunnelse.personIdenter.contains(
                    endretUtbetalingAndel.person!!.personIdent.ident
                )
            }
        }

    companion object {
        val comparator = BegrunnelseComparator()
    }
}

class BegrunnelseComparator : Comparator<Vedtaksbegrunnelse> {

    override fun compare(o1: Vedtaksbegrunnelse, o2: Vedtaksbegrunnelse): Int {
        return if (o1.vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET) {
            -1
        } else 1
    }
}
