package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.BaseEntitet
import java.time.LocalDate
import javax.persistence.*

@Entity(name = "VedtakPerson")
@Table(name = "VEDTAK_PERSON")
data class VedtakPerson(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtak_person_seq_generator")
        @SequenceGenerator(name = "vedtak_person_seq_generator", sequenceName = "vedtak_person_seq", allocationSize = 50)
        val id: Long = 0,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_vedtak_id", nullable = false, updatable = false)
        val vedtak: Vedtak,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_person_id", nullable = false, updatable = false)
        val person: Person,

        @OneToMany(fetch = FetchType.EAGER,
                cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH])
        val ytelsePerioder: MutableList<YtelsePeriode> = mutableListOf()
) : BaseEntitet()