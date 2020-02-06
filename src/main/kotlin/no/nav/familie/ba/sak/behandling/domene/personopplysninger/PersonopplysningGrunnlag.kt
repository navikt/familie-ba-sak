package no.nav.familie.ba.sak.behandling.domene.personopplysninger

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import java.time.LocalDate
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "GR_PERSONOPPLYSNINGER")
data class PersonopplysningGrunnlag(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GR_PERSONOPPLYSNINGER_SEQ")
        val id: Long = 0,
        @Column(name = "fk_behandling_id", updatable = false, nullable = false)
        val behandlingId: Long,
        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "personopplysningGrunnlag",
                   cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH])
        val personer: MutableList<Person> = mutableListOf(),
        @Column(name = "aktiv", nullable = false)
        val aktiv: Boolean = true

) : BaseEntitet() {

    fun leggTilPerson(type: PersonType, personIdent: PersonIdent, fødselsdato: LocalDate) :PersonopplysningGrunnlag {
        personer.add(Person(type=type,personIdent = personIdent,fødselsdato = fødselsdato,personopplysningGrunnlag =  this))
        return this;
    }

    val barna: List<Person>
        get() = personer.filter { it.type == PersonType.BARN }

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

