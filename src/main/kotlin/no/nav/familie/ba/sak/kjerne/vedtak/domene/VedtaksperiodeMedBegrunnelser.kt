package no.nav.familie.ba.sak.kjerne.vedtak.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.hentAndelerForSegment
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.lagUtbetalingsperiodeDetaljer
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.utledSegmenter
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.time.LocalDate
import java.time.YearMonth

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
    @ManyToOne
    @JoinColumn(name = "fk_vedtak_id")
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
    val begrunnelser: MutableSet<Vedtaksbegrunnelse> = mutableSetOf(),

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vedtaksperiodeMedBegrunnelser",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val eøsBegrunnelser: MutableSet<EØSBegrunnelse> = mutableSetOf(),

    // Bruker list for å bevare rekkefølgen som settes frontend.
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vedtaksperiodeMedBegrunnelser",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf()

) : BaseEntitet() {

    override fun toString(): String {
        return "VedtaksperiodeMedBegrunnelser(id=$id, fom=$fom, tom=$tom, type=$type, begrunnelser=$begrunnelser, eøsBegrunnelser=$eøsBegrunnelser, fritekster=$fritekster)"
    }

    fun settBegrunnelser(nyeBegrunnelser: List<Vedtaksbegrunnelse>) {
        begrunnelser.clear()
        begrunnelser.addAll(nyeBegrunnelser)
    }

    fun settEØSBegrunnelser(nyeEØSBegrunnelser: List<EØSBegrunnelse>) {
        eøsBegrunnelser.clear()
        eøsBegrunnelser.addAll(nyeEØSBegrunnelser)
    }

    fun settFritekster(nyeFritekster: List<VedtaksbegrunnelseFritekst>) {
        fritekster.clear()
        fritekster.addAll(nyeFritekster)
    }

    fun harFriteksterUtenStandardbegrunnelser(): Boolean {
        return (type == Vedtaksperiodetype.OPPHØR || type == Vedtaksperiodetype.AVSLAG) && fritekster.isNotEmpty() && begrunnelser.isEmpty() && eøsBegrunnelser.isEmpty()
    }

    fun harFriteksterOgStandardbegrunnelser(): Boolean {
        return fritekster.isNotEmpty() && begrunnelser.isNotEmpty()
    }

    fun hentUtbetalingsperiodeDetaljer(
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ): List<UtbetalingsperiodeDetalj> =
        if (andelerTilkjentYtelse.isEmpty()) {
            emptyList()
        } else if (this.type == Vedtaksperiodetype.UTBETALING ||
            this.type == Vedtaksperiodetype.FORTSATT_INNVILGET ||
            this.type == Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING
        ) {
            val vertikaltSegmentForVedtaksperiode =
                if (this.type == Vedtaksperiodetype.FORTSATT_INNVILGET) {
                    hentLøpendeAndelForVedtaksperiode(andelerTilkjentYtelse)
                } else {
                    hentVertikaltSegmentForVedtaksperiode(
                        andelerTilkjentYtelse = andelerTilkjentYtelse
                    )
                }

            val andelerForSegment =
                andelerTilkjentYtelse.hentAndelerForSegment(vertikaltSegmentForVedtaksperiode)

            andelerForSegment.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag)
        } else {
            emptyList()
        }

    private fun hentVertikaltSegmentForVedtaksperiode(
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
    ) = andelerTilkjentYtelse
        .utledSegmenter()
        .find { localDateSegment ->
            localDateSegment.fom.isSameOrBefore(this.fom ?: TIDENES_MORGEN) &&
                localDateSegment.tom.isSameOrAfter(this.tom ?: TIDENES_ENDE)
        }
        ?: throw Feil("Finner ikke segment for vedtaksperiode (${this.fom}, ${this.tom}) blant segmenter ${andelerTilkjentYtelse.utledSegmenter()}")
}

fun List<VedtaksperiodeMedBegrunnelser>.erAlleredeBegrunnetMedBegrunnelse(
    standardbegrunnelser: List<Standardbegrunnelse>,
    måned: YearMonth
): Boolean {
    return this.any {
        it.fom?.toYearMonth() == måned && it.begrunnelser.any { standardbegrunnelse -> standardbegrunnelse.standardbegrunnelse in standardbegrunnelser }
    }
}

private fun hentLøpendeAndelForVedtaksperiode(andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>): LocalDateSegment<Int> {
    val sorterteSegmenter = andelerTilkjentYtelse.utledSegmenter().sortedBy { it.fom }
    return sorterteSegmenter.lastOrNull { it.fom.toYearMonth() <= inneværendeMåned() }
        ?: sorterteSegmenter.firstOrNull()
        ?: throw Feil("Finner ikke gjeldende segment ved fortsatt innvilget")
}
