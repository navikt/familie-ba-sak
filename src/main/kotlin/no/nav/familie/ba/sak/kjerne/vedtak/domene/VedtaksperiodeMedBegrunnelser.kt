package no.nav.familie.ba.sak.kjerne.vedtak.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
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
    @SequenceGenerator(
        name = "vedtaksperiode_seq_generator",
        sequenceName = "vedtaksperiode_seq",
        allocationSize = 50
    )
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

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vedtaksperiodeMedBegrunnelser",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val begrunnelser: MutableSet<Vedtaksbegrunnelse> = mutableSetOf(),

    // Bruker list for å bevare rekkefølgen som settes frontend.
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vedtaksperiodeMedBegrunnelser",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf()

) : BaseEntitet() {

    fun settBegrunnelser(nyeBegrunnelser: List<Vedtaksbegrunnelse>) {
        begrunnelser.clear()
        begrunnelser.addAll(nyeBegrunnelser)
    }

    fun settFritekster(nyeFritekster: List<VedtaksbegrunnelseFritekst>) {
        fritekster.clear()
        fritekster.addAll(nyeFritekster)
    }

    fun harFriteksterUtenStandardbegrunnelser(): Boolean {
        return (type == Vedtaksperiodetype.OPPHØR || type == Vedtaksperiodetype.AVSLAG) && fritekster.isNotEmpty() && begrunnelser.isEmpty()
    }

    fun harFriteksterOgStandardbegrunnelser(): Boolean {
        return fritekster.isNotEmpty() && begrunnelser.isNotEmpty()
    }
}

fun byggBegrunnelserOgFriteksterForVedtaksperiode(
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    personerIPersongrunnlag: List<Person>,
    målform: Målform,
    uregistrerteBarn: List<BarnMedOpplysninger> = emptyList(),
): List<Begrunnelse> {
    val fritekster =
        utvidetVedtaksperiodeMedBegrunnelser.fritekster.map { FritekstBegrunnelse(it) }
    val begrunnelser =
        utvidetVedtaksperiodeMedBegrunnelser.begrunnelser.map {
            it.tilBrevBegrunnelse(
                utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
                personerPåBegrunnelse = personerIPersongrunnlag.filter { person -> it.personIdenter.contains(person.personIdent.ident) },
                målform = målform,
                uregistrerteBarn = uregistrerteBarn
            )
        }

    return begrunnelser + fritekster
}
