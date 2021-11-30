package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.Språkkode
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDate
import java.time.Period
import java.util.Objects
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
@Entity(name = "Person")
@Table(name = "PO_PERSON")
data class Person(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_person_seq_generator")
    @SequenceGenerator(name = "po_person_seq_generator", sequenceName = "po_person_seq", allocationSize = 50)
    val id: Long = 0,

    // SØKER, BARN, ANNENPART
    @Enumerated(EnumType.STRING) @Column(name = "type")
    val type: PersonType,

    @Column(name = "foedselsdato", nullable = false)
    val fødselsdato: LocalDate,

    @Column(name = "navn", nullable = false)
    val navn: String = "",

    @Enumerated(EnumType.STRING) @Column(name = "kjoenn", nullable = false)
    val kjønn: Kjønn,

    @Enumerated(EnumType.STRING) @Column(name = "maalform", nullable = false)
    val målform: Målform = Målform.NB,

    /*@Embedded
    @AttributeOverrides(
        AttributeOverride(
            name = "ident",
            column = Column(name = "person_ident", updatable = false)
        )
    )
    val XXYY: PersonIdent,*/

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_gr_personopplysninger_id", nullable = false, updatable = false)
    val personopplysningGrunnlag: PersonopplysningGrunnlag,

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,

    @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    // Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
    @Fetch(value = FetchMode.SUBSELECT)
    var bostedsadresser: MutableList<GrBostedsadresse> = mutableListOf(),

    @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    // Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
    @Fetch(value = FetchMode.SUBSELECT)
    var statsborgerskap: List<GrStatsborgerskap> = emptyList(),

    @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    // Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
    @Fetch(value = FetchMode.SUBSELECT)
    var opphold: List<GrOpphold> = emptyList(),

    @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    // Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
    @Fetch(value = FetchMode.SUBSELECT)
    var arbeidsforhold: List<GrArbeidsforhold> = emptyList(),

    @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    // Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
    @Fetch(value = FetchMode.SUBSELECT)
    var sivilstander: List<GrSivilstand> = emptyList(),
) : BaseEntitet() {

    override fun toString(): String {
        return """Person(aktørId=$aktør,
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
        return Objects.hash(aktør, fødselsdato)
    }

    fun hentAlder(): Int = Period.between(fødselsdato, LocalDate.now()).years

    fun hentSeksårsdag(): LocalDate = fødselsdato.plusYears(6)
}

enum class Kjønn {
    MANN,
    KVINNE,
    UKJENT
}

enum class Medlemskap {
    NORDEN,
    EØS,
    TREDJELANDSBORGER,
    STATSLØS,
    UKJENT
}

enum class Målform {
    NB,
    NN;

    fun tilSanityFormat() = when (this) {
        NB -> "bokmaal"
        NN -> "nynorsk"
    }

    fun tilSpråkkode() = when (this) {
        NB -> Språkkode.NB
        NN -> Språkkode.NN
    }
}
