package no.nav.familie.ba.sak.kjerne.endretutbetaling.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.dokument.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjonListConverter
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.triggesAvSkalUtbetales
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
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

    @Column(name = "avtaletidspunkt_delt_bosted")
    var avtaletidspunktDeltBosted: LocalDate? = null,

    @Column(name = "soknadstidspunkt")
    var søknadstidspunkt: LocalDate? = null,

    @Column(name = "begrunnelse")
    var begrunnelse: String? = null,

    @ManyToMany(mappedBy = "endretUtbetalingAndeler")
    val andelTilkjentYtelser: List<AndelTilkjentYtelse> = emptyList(),

    @Column(name = "vedtak_begrunnelse_spesifikasjoner")
    @Convert(converter = VedtakBegrunnelseSpesifikasjonListConverter::class)
    var vedtakBegrunnelseSpesifikasjoner: List<VedtakBegrunnelseSpesifikasjon> = emptyList(),

    ) : BaseEntitet() {

    fun overlapperMed(periode: MånedPeriode) = periode.overlapperHeltEllerDelvisMed(this.periode())

    fun periode(): MånedPeriode {
        validerUtfyltEndring()
        return MånedPeriode(this.fom!!, this.tom!!)
    }

    fun validerUtfyltEndring(): Boolean {
        if (person == null ||
            prosent == null ||
            fom == null ||
            tom == null ||
            årsak == null ||
            søknadstidspunkt == null ||
            (begrunnelse == null || begrunnelse!!.isEmpty())
        ) {
            throw Feil("Person, prosent, fom, tom, årsak, begrunnese og søknadstidspunkt skal være utfylt: $this.tostring()")
        }

        if (fom!! > tom!!)
            throw Feil("fom må være lik eller komme før tom")

        if (årsak == Årsak.DELT_BOSTED && avtaletidspunktDeltBosted == null)
            throw Feil("Avtaletidspunkt skal være utfylt når årsak er delt bosted: $this.tostring()")

        return true
    }

    fun harVedtakBegrunnelseSpesifikasjon(vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon) =
        this.vedtakBegrunnelseSpesifikasjoner.contains(
            vedtakBegrunnelseSpesifikasjon
        )
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
    avtaletidspunktDeltBosted = this.avtaletidspunktDeltBosted,
    søknadstidspunkt = this.søknadstidspunkt,
    begrunnelse = this.begrunnelse
)

fun EndretUtbetalingAndel.fraRestEndretUtbetalingAndel(
    restEndretUtbetalingAndel: RestEndretUtbetalingAndel,
    person: Person
): EndretUtbetalingAndel {
    this.fom = restEndretUtbetalingAndel.fom
    this.tom = restEndretUtbetalingAndel.tom
    this.prosent = restEndretUtbetalingAndel.prosent ?: BigDecimal(0)
    this.årsak = restEndretUtbetalingAndel.årsak
    this.avtaletidspunktDeltBosted = restEndretUtbetalingAndel.avtaletidspunktDeltBosted
    this.søknadstidspunkt = restEndretUtbetalingAndel.søknadstidspunkt
    this.begrunnelse = restEndretUtbetalingAndel.begrunnelse
    this.person = person
    return this
}

fun List<EndretUtbetalingAndel>.tilVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak,
    fom: LocalDate?,
    tom: LocalDate?,
): VedtaksperiodeMedBegrunnelser {

    return VedtaksperiodeMedBegrunnelser(
        fom = fom,
        tom = tom,
        vedtak = vedtak,
        type = Vedtaksperiodetype.ENDRET_UTBETALING
    ).also { vedtakperiodeMedbegrunnelse ->
        vedtakperiodeMedbegrunnelse.begrunnelser.addAll(
            this.flatMap { it.vedtakBegrunnelseSpesifikasjoner }.toSet()
                .map { vedtakBegrunnelseSpesifikasjon ->
                    Vedtaksbegrunnelse(
                        vedtaksperiodeMedBegrunnelser = vedtakperiodeMedbegrunnelse,
                        vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
                        personIdenter = this.filter {
                            it.harVedtakBegrunnelseSpesifikasjon(vedtakBegrunnelseSpesifikasjon)
                        }.mapNotNull { it.person?.personIdent?.ident }
                    )
                }
        )
    }
}

fun hentPersonerForEtterEndretUtbetalingsperiode(
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
    triggesAv: TriggesAv
) = endretUtbetalingAndeler.filter { endretUtbetalingAndel ->
    endretUtbetalingAndel.tom!!.sisteDagIInneværendeMåned()
        .erDagenFør(vedtaksperiodeMedBegrunnelser.fom) &&
        triggesAv.endringsaarsaker.contains(endretUtbetalingAndel.årsak)
}.mapNotNull { it.person?.personIdent?.ident }

fun EndretUtbetalingAndel.hentGyldigEndretBegrunnelser(sanityBegrunnelser: List<SanityBegrunnelse>): List<VedtakBegrunnelseSpesifikasjon> {

    return VedtakBegrunnelseSpesifikasjon.values()
        .filter { vedtakBegrunnelseSpesifikasjon ->
            vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType == VedtakBegrunnelseType.ENDRET_UTBETALING
        }
        .filter { vedtakBegrunnelseSpesifikasjon ->
            val triggesAv = vedtakBegrunnelseSpesifikasjon.tilSanityBegrunnelse(sanityBegrunnelser).tilTriggesAv()
            triggesAvSkalUtbetales(listOf(this), triggesAv)
        }
}
