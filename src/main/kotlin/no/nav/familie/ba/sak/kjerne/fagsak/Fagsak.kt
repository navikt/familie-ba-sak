package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import java.util.Objects
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

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

    @Column(name = "arkivert", nullable = false)
    var arkivert: Boolean = false,

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "fagsak",
        cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE],
        orphanRemoval = false
    )
    var søkerIdenter: Set<FagsakPerson> = setOf()
) : BaseEntitet() {

    fun hentAktivIdent(): PersonIdent {
        return søkerIdenter.maxByOrNull { it.opprettetTidspunkt }?.personIdent
            ?: error("Fant ingen ident på fagsak $id")
    }

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }

    override fun toString(): String {
        return "Fagsak(id=$id)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Fagsak

        if (id != other.id) return false

        return true
    }
}

enum class FagsakStatus {
    OPPRETTET,
    LØPENDE, // Har minst én behandling gjeldende for fremtidig utbetaling
    AVSLUTTET
}
