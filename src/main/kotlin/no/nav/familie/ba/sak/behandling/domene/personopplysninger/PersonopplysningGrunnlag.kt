package no.nav.familie.ba.sak.behandling.domene.personopplysninger

import no.nav.familie.ba.sak.common.BaseEntitet
import java.util.*
import javax.persistence.*

@Entity
@Table  (name = "GR_PERSONOPPLYSNINGER")
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

    @OneToMany(fetch= FetchType.EAGER, mappedBy = "personopplysningGrunnlag", cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH])
    var personer: MutableList<Person> = LinkedList()

    fun setAktiv(aktiv: Boolean) {
        this.aktiv = aktiv
    }

    fun leggTilPerson(person: Person) {
        personer.add(person)
    }

    val søker: Person?
        get() {
            return personer.firstOrNull { it.type?.equals(PersonType.SØKER) ?: false }
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

