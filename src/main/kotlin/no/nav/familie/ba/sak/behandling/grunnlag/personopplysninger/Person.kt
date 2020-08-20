package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDate
import java.util.*
import javax.persistence.*


@Entity(name = "Person")
@Table(name = "PO_PERSON")
data class Person(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_person_seq_generator")
        @SequenceGenerator(name = "po_person_seq_generator", sequenceName = "po_person_seq", allocationSize = 50)
        val id: Long = 0,

        //SØKER, BARN, ANNENPART
        @Enumerated(EnumType.STRING) @Column(name = "type")
        val type: PersonType,

        @Column(name = "foedselsdato", nullable = false)
        val fødselsdato: LocalDate,

        @Column(name = "navn", nullable = false)
        val navn: String = "",

        @Enumerated(EnumType.STRING) @Column(name = "kjoenn", nullable = false)
        val kjønn: Kjønn,

        @Enumerated(EnumType.STRING) @Column(name = "sivilstand", nullable = false)
        val sivilstand: SIVILSTAND,

        @Embedded
        @AttributeOverrides(AttributeOverride(name = "ident",
                                              column = Column(name = "person_ident", updatable = false)))
        val personIdent: PersonIdent,

        @JsonIgnore
        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_gr_personopplysninger_id", nullable = false, updatable = false)
        val personopplysningGrunnlag: PersonopplysningGrunnlag,

        @Embedded
        @AttributeOverrides(AttributeOverride(name = "aktørId", column = Column(name = "aktoer_id", updatable = false)))
        val aktørId: AktørId? = null,

        @OneToOne(cascade = [CascadeType.ALL])
        @JoinColumn
        val bostedsadresse: GrBostedsadresse? = null,

        @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
        //Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
        @Fetch(value = FetchMode.SUBSELECT)
        var statsborgerskap: List<GrStatsborgerskap>? = null,

        @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
        //Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
        @Fetch(value = FetchMode.SUBSELECT)
        var opphold: List<GrOpphold>? = null,

        @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
        //Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
        @Fetch(value = FetchMode.SUBSELECT)
        var arbeidsforhold: List<GrArbeidsforhold>? = null,

        @OneToMany(cascade = [CascadeType.ALL], fetch=FetchType.EAGER)
        //Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
        @Fetch(value = FetchMode.SUBSELECT)
        @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
        var bostedsadresseperiode: List<GrBostedsadresseperiode>? = null
) : BaseEntitet() {

    override fun toString(): String {
        return """Person(aktørId=$aktørId,
                        |type=$type
                        |fødselsdato=$fødselsdato)""".trimMargin()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val entitet: Person = other as Person
        return Objects.equals(hashCode(), entitet.hashCode())
    }

    override fun hashCode(): Int {
        return Objects.hash(personIdent, fødselsdato)
    }
}

enum class Kjønn {
    MANN, KVINNE, UKJENT
}

enum class Medlemskap {
    NORDEN, EØS, TREDJELANDSBORGER, STATSLØS, UKJENT
}



