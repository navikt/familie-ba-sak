package no.nav.familie.ba.sak.behandling.domene.personopplysninger

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import java.time.LocalDate
import javax.persistence.*


@Entity(name = "Person")
@Table(name = "PO_PERSON")
class Person(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_person_seq")
        @SequenceGenerator(name = "po_person_seq")
        val id: Long? = null,

        //SØKER, BARN, ANNENPART
        @Enumerated(EnumType.STRING) @Column(name = "type")
        val type: PersonType,

        @Column(name = "foedselsdato", nullable = false)
        val fødselsdato: LocalDate?,

        @Embedded
        @AttributeOverrides(AttributeOverride(name = "ident", column = Column(name = "person_ident", updatable = false)))
        val personIdent: PersonIdent,

        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_gr_personopplysninger_id", nullable = false, updatable = false)
        val personopplysningGrunnlag: PersonopplysningGrunnlag
) : BaseEntitet() {

    override fun toString(): String {
        return """Person(id=$id,
                        |type=$type
                        |fødselsdato=$fødselsdato)""".trimMargin()
    }
}
