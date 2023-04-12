package no.nav.familie.ba.sak.kjerne.behandling.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
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
@Entity(name = "BehandlingMigreringsinfo")
@Table(name = "BEHANDLING_MIGRERINGSINFO")
data class BehandlingMigreringsinfo(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_migreringsinfo_seq_generator")
    @SequenceGenerator(
        name = "behandling_migreringsinfo_seq_generator",
        sequenceName = "behandling_migreringsinfo_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,

    val migreringsdato: LocalDate

) : BaseEntitet()
