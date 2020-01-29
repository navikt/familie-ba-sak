package no.nav.familie.ba.sak.behandling.domene.vedtak

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.common.BaseEntitet
import java.time.LocalDate
import javax.persistence.*

// TODO endre til vedtakBarn
@Entity(name = "VedtakBarn")
@Table(name = "VEDTAK_BARN")
data class VedtakBarn(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtak_barn_seq")
        @SequenceGenerator(name = "vedtak_barn_seq")
        val id: Long? = null,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_vedtak_id", nullable = false, updatable = false)
        val vedtak: Vedtak,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_person_id", nullable = false, updatable = false)
        val barn: Person,

        @Column(name = "belop", nullable = false)
        val beløp: Int,

        @Column(name = "stonad_fom", nullable = false)
        val stønadFom: LocalDate,

        @Column(name = "stonad_tom", nullable = false)
        val stønadTom: LocalDate
) : BaseEntitet()