package no.nav.familie.ba.sak.kjerne.vedtak.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.brev.domene.beløpUtbetaltFor
import no.nav.familie.ba.sak.kjerne.brev.domene.totaltUtbetalt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hentMånedOgÅrForBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilBrevTekst
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Vedtaksbegrunnelse")
@Table(name = "VEDTAKSBEGRUNNELSE")
class Vedtaksbegrunnelse(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtaksbegrunnelse_seq_generator")
    @SequenceGenerator(
        name = "vedtaksbegrunnelse_seq_generator",
        sequenceName = "vedtaksbegrunnelse_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @JsonIgnore
    @ManyToOne @JoinColumn(name = "fk_vedtaksperiode_id", nullable = false, updatable = false)
    val vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,

    @Enumerated(EnumType.STRING)
    @Column(name = "vedtak_begrunnelse_spesifikasjon", updatable = false)
    val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
) {

    fun kopier(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): Vedtaksbegrunnelse = Vedtaksbegrunnelse(
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
    )

    override fun toString(): String {
        return "Vedtaksbegrunnelse(id=$id, standardbegrunnelse=$vedtakBegrunnelseSpesifikasjon)"
    }
}

fun Vedtaksbegrunnelse.tilRestVedtaksbegrunnelse() = RestVedtaksbegrunnelse(
    vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
    vedtakBegrunnelseType = this.vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType,
)

interface Begrunnelse

data class BegrunnelseData(
    val gjelderSoker: Boolean,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maanedOgAarBegrunnelsenGjelderFor: String?,
    val maalform: String,
    val apiNavn: String,
    val belop: String,
) : Begrunnelse

data class FritekstBegrunnelse(val fritekst: String) : Begrunnelse

fun BrevBegrunnelseGrunnlagMedPersoner.tilBrevBegrunnelse(
    vedtaksperiode: NullablePeriode,
    personerIPersongrunnlag: List<MinimertRestPerson>,
    brevMålform: Målform,
    uregistrerteBarn: List<MinimertUregistrertBarn>,
    minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>
): Begrunnelse {
    val personerPåBegrunnelse =
        personerIPersongrunnlag.filter { person -> this.personIdenter.contains(person.personIdent) }

    val gjelderSøker = personerPåBegrunnelse.any { it.type == PersonType.SØKER }

    val barnasFødselsdatoer = this.hentBarnasFødselsdagerForBegrunnelse(
        uregistrerteBarn = uregistrerteBarn,
        personerIBehandling = personerIPersongrunnlag,
        personerPåBegrunnelse = personerPåBegrunnelse,
        personerMedUtbetaling = minimerteUtbetalingsperiodeDetaljer.map { it.person },
        gjelderSøker = gjelderSøker
    )

    val antallBarn = this.hentAntallBarnForBegrunnelse(
        uregistrerteBarn = uregistrerteBarn,
        gjelderSøker = gjelderSøker,
        barnasFødselsdatoer = barnasFødselsdatoer,
    )

    val månedOgÅrBegrunnelsenGjelderFor =
        if (vedtaksperiode.fom == null) null
        else this.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
            periode = Periode(
                fom = vedtaksperiode.fom,
                tom = vedtaksperiode.tom ?: TIDENES_ENDE
            )
        )

    val beløp = this.hentBeløp(gjelderSøker, minimerteUtbetalingsperiodeDetaljer)

    this.validerBrevbegrunnelse(
        gjelderSøker = gjelderSøker,
        barnasFødselsdatoer = barnasFødselsdatoer,
    )

    return BegrunnelseData(
        gjelderSoker = gjelderSøker,
        barnasFodselsdatoer = barnasFødselsdatoer.tilBrevTekst(),
        antallBarn = antallBarn,
        maanedOgAarBegrunnelsenGjelderFor = månedOgÅrBegrunnelsenGjelderFor,
        maalform = brevMålform.tilSanityFormat(),
        apiNavn = this.vedtakBegrunnelseSpesifikasjon.sanityApiNavn,
        belop = Utils.formaterBeløp(beløp)
    )
}

private fun BrevBegrunnelseGrunnlagMedPersoner.validerBrevbegrunnelse(
    gjelderSøker: Boolean,
    barnasFødselsdatoer: List<LocalDate>,
) {
    if (!gjelderSøker && barnasFødselsdatoer.isEmpty() && !this.triggesAv.satsendring) {
        throw IllegalStateException("Ingen personer på brevbegrunnelse")
    }
}

private fun BrevBegrunnelseGrunnlagMedPersoner.hentBeløp(
    gjelderSøker: Boolean,
    minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>
) = if (gjelderSøker) {
    if (this.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG ||
        this.vedtakBegrunnelseType == VedtakBegrunnelseType.OPPHØR
    ) {
        0
    } else {
        minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()
    }
} else {
    minimerteUtbetalingsperiodeDetaljer.beløpUtbetaltFor(this.personIdenter)
}
