package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import java.util.*
import javax.persistence.*

@Entity(name = "Behandling") @Table(name = "BEHANDLING")
class Behandling : BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_seq")
    @SequenceGenerator(name = "behandling_seq") val id: Long? = null
    @ManyToOne(optional = false) @JoinColumn(name = "fagsak_id", nullable = false, updatable = false) var fagsak: Fagsak? = null
        private set
    @Column(name = "journalpost_id", nullable = false) var journalpostID: String? = null
        private set

    /**
     * saksnummer fra GSAK.
     */
    @Column(name = "saksnummer", nullable = true)
    var saksnummer: String? = null

    internal constructor() { // Hibernate
    }

    private constructor(fagsak: Fagsak, journalpostID: String, saksnummer: String?) {
        Objects.requireNonNull(fagsak, "behandling må tilknyttes parent Fagsak")
        this.fagsak = fagsak
        this.journalpostID = journalpostID
        this.saksnummer = saksnummer;
    }

    override fun toString(): String {
        return "behandling{" +
               "id=" + id +
               "fagsak=" + fagsak +
               "journalpostID=" + journalpostID +
               "saksnummer=" + saksnummer +
               '}'
    }
}