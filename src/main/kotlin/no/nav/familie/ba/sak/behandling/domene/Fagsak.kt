package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import java.util.*
import javax.persistence.*

@Entity(name = "Fagsak") @Table(name = "FAGSAK")
data class Fagsak(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fagsak_seq")
        @SequenceGenerator(name = "fagsak_seq")
        val id: Long? = null,

        @Embedded
        @AttributeOverrides(AttributeOverride(name = "aktørId", column = Column(name = "aktoer_id", updatable = false)))
        var aktørId: AktørId? = null,

        @Embedded
        @AttributeOverrides(AttributeOverride(name = "ident", column = Column(name = "person_ident", updatable = false)))
        var personIdent: PersonIdent? = null
) : BaseEntitet() {

    override fun toString(): String {
        return "Fagsak(id=$id, aktørId=$aktørId)"
    }
}