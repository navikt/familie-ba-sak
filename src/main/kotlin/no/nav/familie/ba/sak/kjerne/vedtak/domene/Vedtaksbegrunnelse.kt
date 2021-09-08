package no.nav.familie.ba.sak.kjerne.vedtak.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.StringListConverter
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.ekstern.restDomene.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon.Companion.tilBrevTekst
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hentMånedOgÅrForBegrunnelse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
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
) : Begrunnelse

data class BegrunnelseFraBaSak(val begrunnelse: String) : Begrunnelse

fun Vedtaksbegrunnelse.tilBrevBegrunnelse(
        personerPåBegrunnelse: List<Person>,
        målform: Målform,
        brukBegrunnelserFraSanity: Boolean,
): Begrunnelse {
    val barna = personerPåBegrunnelse.filter { it.type == PersonType.BARN }

    val relevanteBarnsFødselsDatoer =
            if (this.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR) {
                // Denne må behandles spesielt da begrunnelse for autobrev ved 18 år på barn innebærer at barn som ikke lenger inngår
                // i vedtaket skal inkluderes i begrunnelsen. Alle kan inkluderes da det i VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR
                // vil filtreres basert på person som er 18 år.
                personerPåBegrunnelse.map { it.fødselsdato }
            } else {
                barna.map { barn -> barn.fødselsdato }
            }
    val gjelderSøker = personerPåBegrunnelse.any { it.type == PersonType.SØKER }
    val månedOgÅrBegrunnelsenGjelderFor =
            if (this.vedtaksperiodeMedBegrunnelser.fom == null) null
            else this.vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                    periode = Periode(fom = this.vedtaksperiodeMedBegrunnelser.fom,
                                      tom = this.vedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE))

    return if (brukBegrunnelserFraSanity)
        BegrunnelseData(
                gjelderSoker = gjelderSøker,
                barnasFodselsdatoer = relevanteBarnsFødselsDatoer.tilBrevTekst(),
                antallBarn = relevanteBarnsFødselsDatoer.size,
                maanedOgAarBegrunnelsenGjelderFor = månedOgÅrBegrunnelsenGjelderFor,
                maalform = målform.tilSanityFormat(),
                apiNavn = this.vedtakBegrunnelseSpesifikasjon.sanityApiNavn,
        )
    else
        BegrunnelseFraBaSak(this.vedtakBegrunnelseSpesifikasjon.hentBeskrivelse(
                gjelderSøker = gjelderSøker,
                barnasFødselsdatoer = relevanteBarnsFødselsDatoer,
                månedOgÅrBegrunnelsenGjelderFor = månedOgÅrBegrunnelsenGjelderFor ?: "",
                målform = målform
        ))
}
