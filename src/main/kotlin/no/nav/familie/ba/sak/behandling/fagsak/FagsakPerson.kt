package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import java.time.LocalDateTime
import javax.persistence.*

@Entity(name = "FagsakPerson")
@Table(name = "FAGSAK_PERSON")
data class FagsakPerson (
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fagsak_person_seq_generator")
    @SequenceGenerator(name = "fagsak_person_seq_generator", sequenceName = "fagsak_person_seq", allocationSize = 50)
    val id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_fagsak_id", nullable = false, updatable = false)
    val fagsak: Fagsak,

    @Embedded
    @AttributeOverrides(AttributeOverride(name = "ident", column = Column(name = "ident", updatable = false)))
    val personIdent: PersonIdent,

    @Column(name = "opprettet_av", nullable = false, updatable = false)
    val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now()
)