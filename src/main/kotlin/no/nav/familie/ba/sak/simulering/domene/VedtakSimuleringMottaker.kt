package no.nav.familie.ba.sak.simulering.domene

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "VedtakSimuleringMottaker")
@Table(name = "VEDTAK_SIMULERING_MOTTAKER")
data class VedtakSimuleringMottaker(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtak_simulering_mottaker_seq_generator")
        @SequenceGenerator(name = "vedtak_simulering_mottaker_seq_generator",
                           sequenceName = "vedtak_simulering_mottaker_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @Column(name = "mottaker_nummer", nullable = false)
        val mottakerNummer: String?,

        @Enumerated(EnumType.STRING)
        @Column(name = "mottaker_type", nullable = false)
        val mottakerType: MottakerType,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_vedtak_id", nullable = false, updatable = false)
        val vedtak: Vedtak,
)