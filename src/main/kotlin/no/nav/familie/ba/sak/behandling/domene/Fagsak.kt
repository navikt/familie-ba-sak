package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import javax.persistence.*

@Entity(name = "Fagsak")
@Table(name = "FAGSAK")
data class Fagsak(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fagsak_seq_generator")
        @SequenceGenerator(name = "fagsak_seq_generator", sequenceName = "fagsak_seq", allocationSize = 50)
        val id: Long = 0,

        @Embedded
        @AttributeOverrides(AttributeOverride(name = "aktørId", column = Column(name = "aktoer_id", updatable = false)))
        val aktørId: AktørId,

        @Embedded
        @AttributeOverrides(AttributeOverride(name = "ident", column = Column(name = "person_ident", updatable = false)))
        val personIdent: PersonIdent,

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        var status: FagsakStatus = FagsakStatus.OPPRETTET
) : BaseEntitet() {

    override fun toString(): String {
        return "Fagsak(id=$id, aktørId=$aktørId)"
    }
}

enum class FagsakStatus {
    OPPRETTET, LØPENDE, STANSET
}