package no.nav.familie.ba.sak.kjerne.behandling.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "BehandlingSøknadsinfo")
@Table(name = "BEHANDLING_SOKNADSINFO")
data class BehandlingSøknadsinfo(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_søknadsinfo_seq_generator")
    @SequenceGenerator(
        name = "behandling_søknadsinfo_seq_generator",
        sequenceName = "behandling_soknadsinfo_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,

    val mottattDato: LocalDateTime

) : BaseEntitet()
