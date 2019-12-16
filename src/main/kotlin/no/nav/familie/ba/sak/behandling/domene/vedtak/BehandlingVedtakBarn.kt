package no.nav.familie.ba.sak.behandling.domene.vedtak

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.common.BaseEntitet
import java.time.LocalDate
import javax.persistence.*

@Entity(name = "BehandlingVedtakBarn")
@Table(name = "BEHANDLING_VEDTAK_BARN")
data class BehandlingVedtakBarn (
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_vedtak_barn_seq")
        @SequenceGenerator(name = "behandling_vedtak_barn_seq")
        val id: Long? = null,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_behandling_vedtak_id", nullable = false, updatable = false)
        val behandlingVedtak: BehandlingVedtak,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_person_id", nullable = false, updatable = false)
        val barn: Person,

        @Column(name = "belop", nullable = false)
        val beløp: Int,

        @Column(name = "stonad_fom", nullable = false)
        val stønadFom: LocalDate
): BaseEntitet()