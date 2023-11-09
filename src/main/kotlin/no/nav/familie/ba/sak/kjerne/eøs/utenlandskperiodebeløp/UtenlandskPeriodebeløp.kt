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
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
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
    ) =
        copy(
            fom = fom,
            tom = tom,
            barnAktører = barnAktører.toSet(), // .toSet() brukes for at det skal bli et nytt sett (to objekter kan ikke ha referanse til samme sett)
        )

    fun erObligatoriskeFelterSatt() =
        fom != null &&
            erObligatoriskeFelterUtenomTidsperioderSatt()

    fun erObligatoriskeFelterUtenomTidsperioderSatt() =
        this.valutakode != null &&
            this.beløp != null &&
            this.intervall != null &&
            this.utbetalingsland != null &&
            this.barnAktører.isNotEmpty()

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
) : IUtenlandskPeriodebeløp

fun UtenlandskPeriodebeløp.tilIUtenlandskPeriodebeløp(): IUtenlandskPeriodebeløp {
    return if (this.erObligatoriskeFelterSatt()) {
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
        )
    } else {
        TomUtenlandskPeriodebeløp(
            id = this.id,
            behandlingId = this.behandlingId,
        )
    }
}

fun List<UtfyltUtenlandskPeriodebeløp>.tilTidslinje() =
    this.map {
        Periode(
            fraOgMed = it.fom.tilTidspunkt(),
            tilOgMed = it.tom?.tilTidspunkt() ?: MånedTidspunkt.uendeligLengeTil(),
            innhold = it,
        )
    }.tilTidslinje()
