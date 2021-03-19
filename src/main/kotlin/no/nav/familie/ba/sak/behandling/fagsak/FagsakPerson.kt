package no.nav.familie.ba.sak.behandling.fagsak

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import java.time.LocalDateTime
import javax.persistence.AttributeOverride
import javax.persistence.AttributeOverrides
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "FagsakPerson")
@Table(name = "FAGSAK_PERSON")
data class FagsakPerson(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fagsak_person_seq_generator")
        @SequenceGenerator(name = "fagsak_person_seq_generator", sequenceName = "fagsak_person_seq", allocationSize = 50)
        val id: Long = 0,

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @JoinColumn(name = "fk_fagsak_id", nullable = false, updatable = false)
        @JsonIgnore
        val fagsak: Fagsak,

        @Embedded
        @AttributeOverrides(AttributeOverride(name = "ident", column = Column(name = "ident", updatable = false)))
        val personIdent: PersonIdent,

        @Column(name = "opprettet_av", nullable = false, updatable = false)
        val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),

        @Column(name = "opprettet_tid", nullable = false, updatable = false)
        val opprettetTidspunkt: LocalDateTime = LocalDateTime.now()
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FagsakPerson

        if (id != other.id) return false
        if (personIdent != other.personIdent) return false
        if (opprettetAv != other.opprettetAv) return false
        if (opprettetTidspunkt != other.opprettetTidspunkt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + personIdent.hashCode()
        result = 31 * result + opprettetAv.hashCode()
        result = 31 * result + opprettetTidspunkt.hashCode()
        return result
    }

    override fun toString(): String {
        return "FagsakPerson(id=$id, fagsak=${fagsak.id}, opprettetTidspunkt=$opprettetTidspunkt)"
    }
}