package no.nav.familie.ba.sak.kjerne.vedtak.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.StringListConverter
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hentMånedOgÅrForBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilBrevTekst
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
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

    @Column(name = "person_identer", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter::class)
    val personIdenter: List<String> = emptyList(),
) {

    fun kopier(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): Vedtaksbegrunnelse = Vedtaksbegrunnelse(
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
        personIdenter = this.personIdenter
    )
}

fun Vedtaksbegrunnelse.tilRestVedtaksbegrunnelse() = RestVedtaksbegrunnelse(
    vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
    vedtakBegrunnelseType = this.vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType,
    personIdenter = this.personIdenter
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

fun RestVedtaksbegrunnelse.tilBrevBegrunnelse(
    vedtaksperiode: NullablePeriode,
    personerIPersongrunnlag: List<Person>,
    målform: Målform,
    uregistrerteBarn: List<BarnMedOpplysninger>,
    beløp: String,
): Begrunnelse {
    val personerPåBegrunnelse =
        personerIPersongrunnlag.filter { person -> this.personIdenter.contains(person.personIdent.ident) }

    val gjelderSøker = personerPåBegrunnelse.any { it.type == PersonType.SØKER }

    val erAvslagPåKunSøker = gjelderSøker &&
        personerPåBegrunnelse.size == 1 &&
        this.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG

    val barnasFødselsdatoer = hentBarnasFødselsdagerForBegrunnelse(
        uregistrerteBarn = uregistrerteBarn,
        erAvslagPåKunSøker = erAvslagPåKunSøker,
        personerIPersongrunnlag = personerIPersongrunnlag,
        personerPåBegrunnelse = personerPåBegrunnelse
    )

    val antallBarn = hentAntallBarnForBegrunnelse(uregistrerteBarn, erAvslagPåKunSøker, barnasFødselsdatoer)

    val månedOgÅrBegrunnelsenGjelderFor =
        if (vedtaksperiode.fom == null) null
        else this.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
            periode = Periode(
                fom = vedtaksperiode.fom,
                tom = vedtaksperiode.tom ?: TIDENES_ENDE
            )
        )

    return BegrunnelseData(
        gjelderSoker = gjelderSøker,
        barnasFodselsdatoer = barnasFødselsdatoer.tilBrevTekst(),
        antallBarn = antallBarn,
        maanedOgAarBegrunnelsenGjelderFor = månedOgÅrBegrunnelsenGjelderFor,
        maalform = målform.tilSanityFormat(),
        apiNavn = this.vedtakBegrunnelseSpesifikasjon.sanityApiNavn,
        belop = beløp
    )
}

private fun RestVedtaksbegrunnelse.hentAntallBarnForBegrunnelse(
    uregistrerteBarn: List<BarnMedOpplysninger>,
    erAvslagPåKunSøker: Boolean,
    barnasFødselsdatoer: List<LocalDate>
) = if (this.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN)
    uregistrerteBarn.size
else if (erAvslagPåKunSøker)
    0
else
    barnasFødselsdatoer.size

private fun RestVedtaksbegrunnelse.hentBarnasFødselsdagerForBegrunnelse(
    uregistrerteBarn: List<BarnMedOpplysninger>,
    erAvslagPåKunSøker: Boolean,
    personerIPersongrunnlag: List<Person>,
    personerPåBegrunnelse: List<Person>
) = if (this.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN)
    uregistrerteBarn.mapNotNull { it.fødselsdato }
else if (erAvslagPåKunSøker) {
    personerIPersongrunnlag.filter { it.type == PersonType.BARN }.map { it.fødselsdato }
} else
    personerPåBegrunnelse.filter { it.type == PersonType.BARN }.map { it.fødselsdato }
