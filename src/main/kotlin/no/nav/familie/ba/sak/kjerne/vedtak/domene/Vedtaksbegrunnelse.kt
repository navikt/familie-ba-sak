package no.nav.familie.ba.sak.kjerne.vedtak.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksbegrunnelse
import no.nav.familie.ba.sak.ekstern.restDomene.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilVedtaksperiodeType
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.StringListConverter
import no.nav.familie.ba.sak.common.tilMånedÅr
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
        @SequenceGenerator(name = "vedtaksbegrunnelse_seq_generator",
                           sequenceName = "vedtaksbegrunnelse_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @JsonIgnore
        @ManyToOne @JoinColumn(name = "fk_vedtaksperiode_id")
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

fun RestPutVedtaksbegrunnelse.tilVedtaksbegrunnelse(
        vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser
): Vedtaksbegrunnelse {
    if (this.vedtakBegrunnelseSpesifikasjon.erFritekstBegrunnelse()) {
        throw Feil("Kan ikke fastsette fritekstbegrunnelse på begrunnelser på vedtaksperioder. Bruk heller fritekster.")
    }

    if (this.vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType.tilVedtaksperiodeType() != vedtaksperiodeMedBegrunnelser.type) {
        throw Feil("Begrunnelsestype ${this.vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType} passer ikke med " +
                   "typen '${vedtaksperiodeMedBegrunnelser.type}' som er satt på perioden.")
    }

    return Vedtaksbegrunnelse(
            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
            vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
            personIdenter = this.personIdenter
    )
}

fun Vedtaksbegrunnelse.tilBrevBegrunnelse(
        søker: Person,
        personerIPersongrunnlag: List<Person>,
        fom: LocalDate?,
) = this.vedtakBegrunnelseSpesifikasjon.hentBeskrivelse(
        gjelderSøker = this.personIdenter.contains(søker.personIdent.ident),
        barnasFødselsdatoer = this.personIdenter.map { ident ->
            hentFødselsdatodatoFraPersonopplysningsgrunnlag(personerIPersongrunnlag, ident)
        },
        månedOgÅrBegrunnelsenGjelderFor = fom?.tilMånedÅr() ?: "",
        målform = søker.målform
)


private fun hentFødselsdatodatoFraPersonopplysningsgrunnlag(personer: List<Person>, ident: String) =
        personer.find { person -> person.personIdent.ident == ident }?.fødselsdato
        ?: throw Feil("Fant ikke person i personopplysningsgrunnlag")