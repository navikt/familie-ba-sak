package no.nav.familie.ba.sak.behandling.domene.personopplysninger

import no.nav.familie.ba.sak.common.BaseEntitet
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "GR_PERSONOPPLYSNINGER")
class PersonopplysningGrunnlag(
        @Column(name = "fk_behandling_id", updatable = false, nullable = false)
        val behandlingId: Long?
) : BaseEntitet() {

    /**
     * Kun synlig for abstract test scenario
     *
     * @return id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GR_PERSONOPPLYSNINGER_SEQ")
    val id: Long? = null

    @Column(name = "aktiv", nullable = false)
    private var aktiv = true

    @OneToMany(mappedBy = "personopplysningGrunnlag", cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH])
    private var personer: MutableList<Person> = ArrayList()

    fun setAktiv(aktiv: Boolean) {
        this.aktiv = aktiv
    }

    val registrertePersoner: Optional<List<Person>>
        get() = Optional.ofNullable(personer)

    fun leggTilPerson(person: Person) {
        person.setPersonopplysningGrunnlag(this)
        personer.add(person)
    }

    val søker: Person? = personer.firstOrNull { it.type?.equals(PersonType.SØKER)?:false }

    val barna: List<Person>
        get() {
            val barna: MutableList<Person> = LinkedList()
            for (p in personer) {
                if (p.type?.equals(PersonType.BARN)?:false){
                    barna.add(p)
                }
            }
            return barna
        }

    override fun toString(): String {
        val sb = StringBuilder("PersonopplysningGrunnlagEntitet{")
        sb.append("id=").append(id)
        sb.append(", personer=").append(registrertePersoner.toString())
        sb.append(", barna=").append(barna.toString())
        sb.append(", aktiv=").append(aktiv)
        sb.append('}')
        return sb.toString()
    }
}