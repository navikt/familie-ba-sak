package no.nav.familie.ba.sak.kjerne.endretutbetaling.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjonListConverter
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "EndretUtbetalingAndel")
@Table(name = "ENDRET_UTBETALING_ANDEL")
data class EndretUtbetalingAndel(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "endret_utbetaling_andel_seq_generator")
    @SequenceGenerator(
            name = "endret_utbetaling_andel_seq_generator",
            sequenceName = "endret_utbetaling_andel_seq",
            allocationSize = 50
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,

    @ManyToOne @JoinColumn(name = "fk_po_person_id")
    var person: Person? = null,

    @Column(name = "prosent")
    var prosent: BigDecimal? = null,

    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var fom: YearMonth? = null,

    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var tom: YearMonth? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "aarsak")
    var årsak: Årsak? = null,

    @Column(name = "avtaletidspunkt")
    var avtaletidspunkt: LocalDate? = null,

    @Column(name = "soknadstidspunkt")
    var søknadstidspunkt: LocalDate? = null,

    @Column(name = "begrunnelse")
    var begrunnelse: String? = null,

    @ManyToMany
    @JoinTable(
        name = "ANDEL_TIL_ENDRET_ANDEL",
        joinColumns = [JoinColumn(name = "fk_endret_utbetaling_andel_id")],
        inverseJoinColumns = [JoinColumn(name = "fk_andel_tilkjent_ytelse_id")]
    )
    val andelTilkjentYtelser: List<AndelTilkjentYtelse> = emptyList(),

    @Column(name = "vedtak_begrunnelse_spesifikasjoner")
    @Convert(converter = VedtakBegrunnelseSpesifikasjonListConverter::class)
    var vedtakBegrunnelseSpesifikasjoner: List<VedtakBegrunnelseSpesifikasjon> = emptyList(),

    ) : BaseEntitet() {

    fun overlapperMed(periode: MånedPeriode) = periode.overlapperHeltEllerDelvisMed(this.periode())

    fun periode():MånedPeriode {
        validerUtfyltEndring()
        return MånedPeriode(this.fom!!, this.tom!!)
    }

    fun validerUtfyltEndring() {
        if (person == null ||
            prosent == null ||
            fom == null ||
            tom == null ||
            årsak == null)
                throw Feil("Person, prosent, fom, tom, årsak skal være utfylt: $this.tostring()")
    }
}


enum class Årsak(val visningsnavn: String) {
    DELT_BOSTED("Delt bosted"),
    EØS_SEKUNDÆRLAND("Eøs sekundærland");

    fun kanGiNullutbetaling() = this == EØS_SEKUNDÆRLAND
}

fun EndretUtbetalingAndel.tilRestEndretUtbetalingAndel() = RestEndretUtbetalingAndel(
    id = this.id,
    personIdent = this.person?.personIdent?.ident,
    prosent = this.prosent,
    fom = this.fom,
    tom = this.tom,
    årsak = this.årsak,
    avtaletidspunkt = this.avtaletidspunkt,
    søknadstidspunkt = this.søknadstidspunkt,
    begrunnelse = this.begrunnelse
)

fun EndretUtbetalingAndel.fraRestEndretUtbetalingAndel(restEndretUtbetalingAndel: RestEndretUtbetalingAndel, person: Person) {
    this.fom = restEndretUtbetalingAndel.fom
    this.tom = restEndretUtbetalingAndel.tom
    this.prosent = restEndretUtbetalingAndel.prosent
    this.årsak = restEndretUtbetalingAndel.årsak
    this.avtaletidspunkt = restEndretUtbetalingAndel.avtaletidspunkt
    this.søknadstidspunkt = restEndretUtbetalingAndel.søknadstidspunkt
    this.begrunnelse = restEndretUtbetalingAndel.begrunnelse
    this.person = person

}