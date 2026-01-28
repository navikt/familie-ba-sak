package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.ekstern.restDomene.tilKalkulertMånedligBeløp
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetaltFraAnnetLand
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.tilKronerPerValutaenhet
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.tilMånedligValutabeløp
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.times
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.math.BigDecimal
import java.time.YearMonth

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "UtenlandskPeriodebeløp")
@Table(name = "UTENLANDSK_PERIODEBELOEP")
data class UtenlandskPeriodebeløp(
    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val fom: YearMonth?,
    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val tom: YearMonth?,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "AKTOER_TIL_UTENLANDSK_PERIODEBELOEP",
        joinColumns = [JoinColumn(name = "fk_utenlandsk_periodebeloep_id")],
        inverseJoinColumns = [JoinColumn(name = "fk_aktoer_id")],
    )
    override val barnAktører: Set<Aktør> = emptySet(),
    @Column(name = "beloep")
    val beløp: BigDecimal? = null,
    @Column(name = "valutakode")
    val valutakode: String? = null,
    @Column(name = "intervall")
    @Enumerated(EnumType.STRING)
    val intervall: Intervall? = null,
    @Column(name = "utbetalingsland")
    val utbetalingsland: String? = null,
    @Column(name = "kalkulert_maanedlig_beloep")
    val kalkulertMånedligBeløp: BigDecimal? = null,
) : PeriodeOgBarnSkjemaEntitet<UtenlandskPeriodebeløp>() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "utenlandsk_periodebeloep_seq_generator")
    @SequenceGenerator(
        name = "utenlandsk_periodebeloep_seq_generator",
        sequenceName = "utenlandsk_periodebeloep_seq",
        allocationSize = 50,
    )
    override var id: Long = 0

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    override var behandlingId: Long = 0

    override fun utenInnhold(): UtenlandskPeriodebeløp =
        copy(
            beløp = null,
            valutakode = null,
            intervall = null,
            kalkulertMånedligBeløp = null,
        )

    override fun kopier(
        fom: YearMonth?,
        tom: YearMonth?,
        barnAktører: Set<Aktør>,
    ) = copy(
        fom = fom,
        tom = tom,
        // .toSet() brukes for at det skal bli et nytt sett (to objekter kan ikke ha referanse til samme sett)
        barnAktører = barnAktører.toSet(),
    )

    fun erObligatoriskeFelterSatt() =
        fom != null &&
            erObligatoriskeFelterUtenomTidsperioderSatt()

    fun erObligatoriskeFelterUtenomTidsperioderSatt() =
        this.valutakode != null &&
            this.beløp != null &&
            this.intervall != null &&
            this.utbetalingsland != null &&
            this.barnAktører.isNotEmpty() &&
            this.kalkulertMånedligBeløp != null

    companion object {
        val NULL = UtenlandskPeriodebeløp(null, null, emptySet())
    }
}

sealed interface IUtenlandskPeriodebeløp {
    val id: Long
    val behandlingId: Long
}

data class TomUtenlandskPeriodebeløp(
    override val id: Long,
    override val behandlingId: Long,
) : IUtenlandskPeriodebeløp

data class UtfyltUtenlandskPeriodebeløp(
    override val id: Long,
    override val behandlingId: Long,
    val fom: YearMonth,
    val tom: YearMonth?,
    val barnAktører: Set<Aktør>,
    val beløp: BigDecimal,
    val valutakode: String,
    val intervall: Intervall,
    val utbetalingsland: String,
    val kalkulertMånedligBeløp: BigDecimal,
) : IUtenlandskPeriodebeløp

fun UtenlandskPeriodebeløp.tilIUtenlandskPeriodebeløp(): IUtenlandskPeriodebeløp =
    if (this.erObligatoriskeFelterSatt()) {
        UtfyltUtenlandskPeriodebeløp(
            id = this.id,
            behandlingId = this.behandlingId,
            fom = this.fom!!,
            tom = this.tom,
            barnAktører = this.barnAktører,
            beløp = this.beløp!!,
            valutakode = this.valutakode!!,
            intervall = this.intervall!!,
            utbetalingsland = this.utbetalingsland!!,
            kalkulertMånedligBeløp = this.kalkulertMånedligBeløp!!,
        )
    } else {
        TomUtenlandskPeriodebeløp(
            id = this.id,
            behandlingId = this.behandlingId,
        )
    }

fun List<UtfyltUtenlandskPeriodebeløp>.tilTidslinje() =
    this
        .map {
            Periode(
                verdi = it,
                fom = it.fom.førsteDagIInneværendeMåned(),
                tom = it.tom?.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()

fun Collection<UtenlandskPeriodebeløp>.filtrerErUtfylt() = this.map { it.tilIUtenlandskPeriodebeløp() }.filterIsInstance<UtfyltUtenlandskPeriodebeløp>()

fun Collection<UtenlandskPeriodebeløp>.tilUtfylteUtenlandskPeriodebeløpEtterEndringstidpunktPerAktør(endringstidspunkt: YearMonth): Map<Aktør, List<UtfyltUtenlandskPeriodebeløp>> {
    val alleBarnAktørIder = this.map { it.barnAktører }.reduce { akk, neste -> akk + neste }

    val utfylteUtenlandskPeriodebeløp =
        this
            .map { it.tilIUtenlandskPeriodebeløp() }
            .filterIsInstance<UtfyltUtenlandskPeriodebeløp>()

    return alleBarnAktørIder.associateWith { aktør ->
        utfylteUtenlandskPeriodebeløp
            .filter { it.barnAktører.contains(aktør) }
            .tilTidslinje()
            .beskjærFraOgMed(endringstidspunkt.førsteDagIInneværendeMåned())
            .tilPerioder()
            .mapNotNull { it.verdi }
    }
}

fun UtenlandskPeriodebeløp.tilUtbetaltFraAnnetLand(valutakurs: Valutakurs?): UtbetaltFraAnnetLand =
    try {
        UtbetaltFraAnnetLand(
            beløp = kalkulertMånedligBeløp!!.toBigInteger().intValueExact(),
            valutakode = valutakode!!,
            beløpINok = (tilMånedligValutabeløp()!! * valutakurs.tilKronerPerValutaenhet())!!.toBigInteger().intValueExact(),
        )
    } catch (exception: NullPointerException) {
        throw Feil("Kan ikke opprette UtbetaltFraAnnetLand for periode med utenlandsk periodebeløp da ett eller flere av de påkrevde feltene er null: kalkulertMånedligBeløp = ${tilKalkulertMånedligBeløp()}, valutkode = $valutakode valutakurs = ${valutakurs.tilKronerPerValutaenhet()}")
    }
