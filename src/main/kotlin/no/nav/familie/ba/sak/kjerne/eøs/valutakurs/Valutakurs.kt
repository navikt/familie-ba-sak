package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

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
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.tilSisteVirkedag
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Valutakurs")
@Table(name = "VALUTAKURS")
data class Valutakurs(
    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val fom: YearMonth?,
    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val tom: YearMonth?,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "AKTOER_TIL_VALUTAKURS",
        joinColumns = [JoinColumn(name = "fk_valutakurs_id")],
        inverseJoinColumns = [JoinColumn(name = "fk_aktoer_id")],
    )
    override val barnAktører: Set<Aktør> = emptySet(),
    @Column(name = "valutakursdato", columnDefinition = "DATE")
    val valutakursdato: LocalDate? = null,
    @Column(name = "valutakode")
    val valutakode: String? = null,
    @Column(name = "kurs", nullable = false)
    val kurs: BigDecimal? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "vurderingsform")
    val vurderingsform: Vurderingsform,
) : PeriodeOgBarnSkjemaEntitet<Valutakurs>() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "valutakurs_seq_generator")
    @SequenceGenerator(
        name = "valutakurs_seq_generator",
        sequenceName = "valutakurs_seq",
        allocationSize = 50,
    )
    override var id: Long = 0

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    override var behandlingId: Long = 0

    // Valutakode skal alltid være satt (muligens til null), så den slettes ikke
    override fun utenInnhold() =
        copy(
            valutakursdato = null,
            kurs = null,
            vurderingsform = Vurderingsform.IKKE_VURDERT,
        )

    override fun kopier(
        fom: YearMonth?,
        tom: YearMonth?,
        barnAktører: Set<Aktør>,
    ) = copy(
        fom = fom,
        tom = tom,
        barnAktører = barnAktører.toSet(),
    )

    fun erObligatoriskeFelterSatt() =
        fom != null &&
            erObligatoriskeFelterUtenomTidsperioderSatt()

    fun erObligatoriskeFelterUtenomTidsperioderSatt() =
        this.valutakode != null &&
            this.kurs != null &&
            this.valutakursdato != null &&
            this.barnAktører.isNotEmpty() &&
            this.vurderingsform != Vurderingsform.IKKE_VURDERT

    companion object {
        val NULL = Valutakurs(fom = null, tom = null, barnAktører = emptySet(), vurderingsform = Vurderingsform.IKKE_VURDERT)
    }
}

enum class Vurderingsform {
    MANUELL,
    AUTOMATISK,
    IKKE_VURDERT,
}

sealed interface IValutakurs {
    val id: Long
    val behandlingId: Long
}

data class TomValutakurs(
    override val id: Long,
    override val behandlingId: Long,
) : IValutakurs

data class UtfyltValutakurs(
    override val id: Long,
    override val behandlingId: Long,
    val fom: YearMonth,
    val tom: YearMonth?,
    val barnAktører: Set<Aktør>,
    val valutakursdato: LocalDate,
    val valutakode: String,
    val kurs: BigDecimal,
    val vurderingsform: Vurderingsform,
) : IValutakurs

fun Valutakurs.tilIValutakurs(): IValutakurs =
    if (this.erObligatoriskeFelterSatt()) {
        UtfyltValutakurs(
            id = this.id,
            behandlingId = this.behandlingId,
            fom = this.fom!!,
            tom = this.tom,
            barnAktører = this.barnAktører,
            valutakursdato = this.valutakursdato!!,
            valutakode = this.valutakode!!,
            kurs = this.kurs!!,
            vurderingsform = this.vurderingsform,
        )
    } else {
        TomValutakurs(
            id = this.id,
            behandlingId = this.behandlingId,
        )
    }

fun List<UtfyltValutakurs>.tilTidslinje() =
    this
        .map {
            Periode(
                verdi = it,
                fom = it.fom.førsteDagIInneværendeMåned(),
                tom = it.tom?.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()

fun Collection<Valutakurs>.måValutakurserOppdateresForMåned(
    måned: YearMonth,
) = none {
    val fom = it.fom ?: TIDENES_MORGEN.toYearMonth()
    val forventetValutakursdato = fom.minusMonths(1).tilSisteVirkedag()

    fom == måned && it.valutakursdato == forventetValutakursdato
}

fun Collection<Valutakurs>.filtrerUtfylteValutakurser() = this.map { it.tilIValutakurs() }.filterIsInstance<UtfyltValutakurs>()
