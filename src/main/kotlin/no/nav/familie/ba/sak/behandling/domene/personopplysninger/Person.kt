package no.nav.familie.ba.sak.behandling.domene.personopplysninger

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import java.time.LocalDate
import javax.persistence.*


// TODO Legg til toString som ikke logger personident
@Entity(name = "Person")
@Table(name = "PO_PERSON")
class Person(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_seq")
        @SequenceGenerator(name = "behandling_seq")
        val id: Long? = null,

        //SØKER, BARN, ANNENPART
        @Enumerated(EnumType.STRING) @Column(name = "type")
        val type: PersonType? = null,

        @Column(name = "foedselsdato", nullable = false)
        val fødselsdato: LocalDate?,

        @Embedded
        @AttributeOverrides(AttributeOverride(name = "ident", column = Column(name = "person_ident", updatable = false)))
        val personIdent: PersonIdent? = null,

        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_gr_personopplysninger_id", nullable = false, updatable = false)
        val personopplysningGrunnlag: PersonopplysningGrunnlag
) : BaseEntitet()