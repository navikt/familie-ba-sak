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
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjær
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.LocalDate
import java.time.YearMonth
import no.nav.familie.tidslinje.Periode as TidslinjePeriode

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Vedtaksperiode")
@Table(name = "VEDTAKSPERIODE")
data class VedtaksperiodeMedBegrunnelser(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtaksperiode_seq_generator")
    @SequenceGenerator(
        name = "vedtaksperiode_seq_generator",
        sequenceName = "vedtaksperiode_seq",
        allocationSize = 50,
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
        orphanRemoval = true,
    )
    val begrunnelser: MutableSet<Vedtaksbegrunnelse> = mutableSetOf(),
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vedtaksperiodeMedBegrunnelser",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val eøsBegrunnelser: MutableSet<EØSBegrunnelse> = mutableSetOf(),
    // Bruker list for å bevare rekkefølgen som settes frontend.
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vedtaksperiodeMedBegrunnelser",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf(),
) : BaseEntitet() {
    override fun toString(): String = "VedtaksperiodeMedBegrunnelser(id=$id, fom=$fom, tom=$tom, type=$type, begrunnelser=$begrunnelser, eøsBegrunnelser=$eøsBegrunnelser, fritekster=$fritekster)"

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

    fun harFriteksterUtenStandardbegrunnelser(): Boolean = fritekster.isNotEmpty() && begrunnelser.isEmpty() && eøsBegrunnelser.isEmpty()

    fun erBegrunnet() = !(begrunnelser.isEmpty() && fritekster.isEmpty() && eøsBegrunnelser.isEmpty())
}

fun List<VedtaksperiodeMedBegrunnelser>.erAlleredeBegrunnetMedBegrunnelse(
    standardbegrunnelser: List<Standardbegrunnelse>,
    måned: YearMonth,
): Boolean =
    this.any {
        it.fom?.toYearMonth() == måned && it.begrunnelser.any { standardbegrunnelse -> standardbegrunnelse.standardbegrunnelse in standardbegrunnelser }
    }

@Suppress("DEPRECATION")
fun VedtaksperiodeMedBegrunnelser.hentUtbetalingsperiodeDetaljer(
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
): List<UtbetalingsperiodeDetalj> {
    val utbetalingsperiodeDetaljer = andelerTilkjentYtelse.tilUtbetalingerTidslinje(personopplysningGrunnlag)

    return when (this.type) {
        Vedtaksperiodetype.AVSLAG,
        -> {
            emptyList()
        }

        Vedtaksperiodetype.FORTSATT_INNVILGET -> {
            val løpendeUtbetalingsperiode =
                utbetalingsperiodeDetaljer
                    .tilPerioder()
                    .lastOrNull { (it.fom?.toYearMonth() ?: MIN_MÅNED) <= inneværendeMåned() }
                    ?: utbetalingsperiodeDetaljer.tilPerioder().firstOrNull()

            løpendeUtbetalingsperiode?.verdi?.toList()
                ?: throw Feil("Finner ikke gjeldende segment ved fortsatt innvilget")
        }

        Vedtaksperiodetype.UTBETALING,
        Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
        Vedtaksperiodetype.ENDRET_UTBETALING,
        -> {
            finnUtbetalingsperioderRelevantForVedtaksperiode(utbetalingsperiodeDetaljer)?.toList() ?: throw Feil(
                "Finner ikke segment for vedtaksperiode (${this.fom}, ${this.tom})}",
            )
        }

        Vedtaksperiodetype.OPPHØR -> {
            finnUtbetalingsperioderRelevantForOpphørVedtaksperiode(utbetalingsperiodeDetaljer)?.toList()
                ?: emptyList()
        }
    }
}

private fun VedtaksperiodeMedBegrunnelser.finnUtbetalingsperioderRelevantForVedtaksperiode(
    utbetalingsperiodeDetaljer: Tidslinje<Iterable<UtbetalingsperiodeDetalj>>,
): Iterable<UtbetalingsperiodeDetalj>? =
    utbetalingsperiodeDetaljer
        .beskjær(this.fom ?: TIDENES_MORGEN, this.tom ?: TIDENES_ENDE)
        .tilPerioder()
        .firstNotNullOfOrNull { it.verdi }

private fun VedtaksperiodeMedBegrunnelser.finnUtbetalingsperioderRelevantForOpphørVedtaksperiode(
    utbetalingsperiodeDetaljer: Tidslinje<Iterable<UtbetalingsperiodeDetalj>>,
): Iterable<UtbetalingsperiodeDetalj>? {
    val verdi =
        utbetalingsperiodeDetaljer
            .tilPerioder()
            .find { andelerVertikal ->
                andelerVertikal.fom?.førsteDagIInneværendeMåned() == this.fom
            }?.verdi

    return verdi
}

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilUtbetalingerTidslinje(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
) = groupBy { Pair(it.aktør, it.type) }
    .map { (_, andelerForAktørOgType) ->
        andelerForAktørOgType
            .map {
                TidslinjePeriode(
                    fom = it.stønadFom.førsteDagIInneværendeMåned(),
                    tom = it.stønadTom.sisteDagIInneværendeMåned(),
                    verdi =
                        UtbetalingsperiodeDetalj(
                            andel = it,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                        ),
                )
            }.tilTidslinje()
    }.kombiner { it.takeIf { it.toList().isNotEmpty() } }
    .slåSammenLikePerioder()

fun hentBrevPeriodeType(
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
    erUtbetalingEllerDeltBostedIPeriode: Boolean,
): BrevPeriodeType =
    hentBrevPeriodeType(
        vedtaksperiodetype = vedtaksperiodeMedBegrunnelser.type,
        fom = vedtaksperiodeMedBegrunnelser.fom,
        erUtbetalingEllerDeltBostedIPeriode = erUtbetalingEllerDeltBostedIPeriode,
    )

@Suppress("DEPRECATION")
fun hentBrevPeriodeType(
    vedtaksperiodetype: Vedtaksperiodetype,
    fom: LocalDate?,
    erUtbetalingEllerDeltBostedIPeriode: Boolean,
): BrevPeriodeType =
    when (vedtaksperiodetype) {
        Vedtaksperiodetype.FORTSATT_INNVILGET -> {
            BrevPeriodeType.FORTSATT_INNVILGET
        }

        Vedtaksperiodetype.UTBETALING -> {
            when {
                erUtbetalingEllerDeltBostedIPeriode -> BrevPeriodeType.UTBETALING
                else -> BrevPeriodeType.INGEN_UTBETALING
            }
        }

        Vedtaksperiodetype.AVSLAG -> {
            if (fom != null) BrevPeriodeType.INGEN_UTBETALING else BrevPeriodeType.INGEN_UTBETALING_UTEN_PERIODE
        }

        Vedtaksperiodetype.OPPHØR -> {
            BrevPeriodeType.INGEN_UTBETALING
        }

        Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> {
            BrevPeriodeType.UTBETALING
        }

        Vedtaksperiodetype.ENDRET_UTBETALING,
        -> {
            throw Feil("Endret utbetaling skal ikke benyttes lenger.")
        }
    }
