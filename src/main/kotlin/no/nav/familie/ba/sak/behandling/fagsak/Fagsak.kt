package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import java.util.*
import javax.persistence.*

@Entity(name = "Fagsak")
@Table(name = "FAGSAK")
data class Fagsak(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fagsak_seq_generator")
        @SequenceGenerator(name = "fagsak_seq_generator", sequenceName = "fagsak_seq", allocationSize = 50)
        val id: Long = 0,

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        var status: FagsakStatus = FagsakStatus.OPPRETTET,

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "fagsak",
                   cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE],
                   orphanRemoval = false
        )
        var søkerIdenter: Set<FagsakPerson> = setOf()
) : BaseEntitet() {

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }

    override fun toString(): String {
        return "Fagsak(id=$id)"
    }

    fun hentAktivIdent(): PersonIdent {
        return søkerIdenter.maxBy { it.opprettetTidspunkt }?.personIdent ?: error("Fant ingen ident på fagsak $id")
    }
}

enum class FagsakStatus {
    OPPRETTET, LØPENDE, STANSET
}