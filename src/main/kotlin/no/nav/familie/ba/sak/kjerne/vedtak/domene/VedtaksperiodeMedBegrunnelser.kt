package no.nav.familie.ba.sak.behandling.vedtak.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.restDomene.RestVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.hentUtbetalingsperiodeForVedtaksperiode
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.brev.domene.maler.AvslagBrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.AvslagUtenPeriodeBrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.FortsattInnvilgetBrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.InnvilgelseBrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.OpphørBrevPeriode
import no.nav.familie.ba.sak.brev.finnAlleBarnsFødselsDatoerIUtbetalingsperiode
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
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
        @SequenceGenerator(name = "vedtaksperiode_seq_generator",
                           sequenceName = "vedtaksperiode_seq",
                           allocationSize = 50)
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

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "vedtaksperiodeMedBegrunnelser",
                   cascade = [CascadeType.ALL],
                   orphanRemoval = true
        )
        val begrunnelser: MutableSet<Vedtaksbegrunnelse> = mutableSetOf(),

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "vedtaksperiodeMedBegrunnelser",
                   cascade = [CascadeType.ALL],
                   orphanRemoval = true
        )
        val fritekster: MutableSet<VedtaksbegrunnelseFritekst> = mutableSetOf()

) : BaseEntitet() {

    fun settBegrunnelser(nyeBegrunnelser: List<Vedtaksbegrunnelse>) {
        begrunnelser.clear()
        begrunnelser.addAll(nyeBegrunnelser)
    }

    fun settFritekster(nyeFritekster: List<VedtaksbegrunnelseFritekst>) {
        fritekster.clear()
        fritekster.addAll(nyeFritekster)
    }
}

fun VedtaksperiodeMedBegrunnelser.tilRestVedtaksperiodeMedBegrunnelser() = RestVedtaksperiodeMedBegrunnelser(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        begrunnelser = this.begrunnelser.map { it.tilRestVedtaksbegrunnelse() },
        fritekster = this.fritekster.map { it.fritekst }
)

fun VedtaksperiodeMedBegrunnelser.tilBrevPeriode(
        søker: Person,
        personerIPersongrunnlag: List<Person>,
        utbetalingsperioder: List<Utbetalingsperiode>,
        målform: Målform,
): BrevPeriode? {
    val begrunnelserOgFritekster = byggBegrunnelserOgFriteksterForVedtaksperiode(
            vedtaksperiode = this,
            søker = søker,
            personerIPersongrunnlag = personerIPersongrunnlag,
    )

    val tomDato =
            if (this.tom?.erSenereEnnInneværendeMåned() == true) this.tom.tilDagMånedÅr()
            else null

    if (begrunnelserOgFritekster.isEmpty()) return null

    return when (this.type) {
        Vedtaksperiodetype.FORTSATT_INNVILGET -> {
            val erAutobrev = this.begrunnelser.any { vedtaksbegrunnelse ->
                vedtaksbegrunnelse.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR ||
                vedtaksbegrunnelse.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR}
            val fom = if (erAutobrev && this.fom != null) {
                val fra = if (målform == Målform.NB) "Fra" else "Frå"
                "$fra ${this.fom.tilDagMånedÅr()} får du:"
            } else null
            val utbetalingsperiode = hentUtbetalingsperiodeForVedtaksperiode(utbetalingsperioder, this.fom)
            FortsattInnvilgetBrevPeriode(
                    fom = fom ?: "Du får:",
                    belop = Utils.formaterBeløp(utbetalingsperiode.utbetaltPerMnd),
                    antallBarn = utbetalingsperiode.antallBarn.toString(),
                    barnasFodselsdager = finnAlleBarnsFødselsDatoerIUtbetalingsperiode(utbetalingsperiode),
                    begrunnelser = begrunnelserOgFritekster)
        }

        Vedtaksperiodetype.UTBETALING -> {
            val utbetalingsperiode = hentUtbetalingsperiodeForVedtaksperiode(utbetalingsperioder, this.fom)
            InnvilgelseBrevPeriode(
                    fom = this.fom!!.tilDagMånedÅr(),
                    tom = tomDato,
                    belop = Utils.formaterBeløp(utbetalingsperiode.utbetaltPerMnd),
                    antallBarn = utbetalingsperiode.antallBarn.toString(),
                    barnasFodselsdager = finnAlleBarnsFødselsDatoerIUtbetalingsperiode(utbetalingsperiode),
                    begrunnelser = begrunnelserOgFritekster)
        }

        Vedtaksperiodetype.AVSLAG ->
            if (this.fom != null)
                AvslagBrevPeriode(
                        fom = this.fom.tilDagMånedÅr(),
                        tom = tomDato,
                        begrunnelser = begrunnelserOgFritekster)
            else AvslagUtenPeriodeBrevPeriode(begrunnelser = begrunnelserOgFritekster)

        Vedtaksperiodetype.OPPHØR -> OpphørBrevPeriode(
                fom = this.fom!!.tilDagMånedÅr(),
                tom = tomDato,
                begrunnelser = begrunnelserOgFritekster)

    }
}

fun byggBegrunnelserOgFriteksterForVedtaksperiode(
        vedtaksperiode: VedtaksperiodeMedBegrunnelser,
        søker: Person,
        personerIPersongrunnlag: List<Person>,
): List<String> {
    val fritekster = vedtaksperiode.fritekster.sortedBy { it.id }.map { it.fritekst }
    val begrunnelser =
            vedtaksperiode.begrunnelser.map {
                it.tilBrevBegrunnelse(søker = søker,
                                      personerIPersongrunnlag = personerIPersongrunnlag,
                                      fom = vedtaksperiode.fom)
            }

    if (fritekster.isNotEmpty() && begrunnelser.isNotEmpty()) {
        throw FunksjonellFeil("Det ble sendt med både fritekst og begrunnelse. " +
                              "Vedtaket skal enten ha fritekst eller bregrunnelse, men ikke begge deler.")
    }

    return begrunnelser + fritekster
}