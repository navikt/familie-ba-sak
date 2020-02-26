package no.nav.familie.ba.sak.behandling.domene.vedtak

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.common.BaseEntitet
import java.time.LocalDate
import javax.persistence.*

@Entity(name = "VedtakPerson")
@Table(name = "VEDTAK_PERSON")
data class VedtakPerson(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtak_person_seq_generator")
        @SequenceGenerator(name = "vedtak_person_seq_generator", sequenceName = "vedtak_person_seq", allocationSize = 50)
        val id: Long? = null,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_vedtak_id", nullable = false, updatable = false)
        val vedtak: Vedtak,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_person_id", nullable = false, updatable = false)
        val person: Person,

        @Column(name = "belop", nullable = false)
        val beløp: Int,

        @Column(name = "stonad_fom", nullable = false)
        val stønadFom: LocalDate,

        @Column(name = "stonad_tom", nullable = false)
        val stønadTom: LocalDate,

        @Enumerated(EnumType.STRING)
        @Column(name = "type", nullable = false)
        val type: Ytelsetype

) : BaseEntitet()