package no.nav.familie.ba.sak.behandling.domene.personopplysninger

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import java.time.LocalDate
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "GR_PERSONOPPLYSNINGER")
class PersonopplysningGrunnlag(
        @Column(name = "fk_behandling_id", updatable = false, nullable = false)
        val behandlingId: Long?,
        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true

) : BaseEntitet() {

    /**
     * Kun synlig for abstract test scenario
     *
     * @return id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GR_PERSONOPPLYSNINGER_SEQ")
    val id: Long? = null


    @OneToMany(fetch = FetchType.EAGER,
               mappedBy = "personopplysningGrunnlag",
               cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH])
    val personer: MutableList<Person> = LinkedList()

    fun leggTilPerson(person: Person) {
        if(personer.none { p->p.personIdent.ident.equals(person.personIdent.ident) }) {
            personer.add(person)
        }
    }

    fun leggTilPerson(type: PersonType, personIdent: PersonIdent, fødselsdato: LocalDate) :PersonopplysningGrunnlag {
        leggTilPerson(Person(type=type,personIdent = personIdent,fødselsdato = fødselsdato,personopplysningGrunnlag =  this))
        return this;
    }

    val søker: Person?
        get() {
            for (p in personer) {
                if (p.type?.equals(PersonType.SØKER) == true) {
                    return p
                }
            }
            return null
        }

    val barna: List<Person>
        get() {
            val barna: MutableList<Person> = LinkedList()
            for (p in personer) {
                if (p.type?.equals(PersonType.BARN) == true) {
                    barna.add(p)
                }
            }
            return barna
        }

    override fun toString(): String {
        val sb = StringBuilder("PersonopplysningGrunnlagEntitet{")
        sb.append("id=").append(id)
        sb.append(", personer=").append(personer.toString())
        sb.append(", barna=").append(barna.toString())
        sb.append(", aktiv=").append(aktiv)
        sb.append('}')
        return sb.toString()
    }
}

