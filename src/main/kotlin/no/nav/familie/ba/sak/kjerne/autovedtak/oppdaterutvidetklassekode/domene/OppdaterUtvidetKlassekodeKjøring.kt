package no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity(name = "OppdaterUtvidetKlassekodeKjøring")
@Table(name = "oppdater_utvidet_klassekode_kjoring")
data class OppdaterUtvidetKlassekodeKjøring(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "oppdater_utvidet_klassekode_kjoring_seq_generator")
    @SequenceGenerator(
        name = "oppdater_utvidet_klassekode_kjoring_seq_generator",
        sequenceName = "oppdater_utvidet_klassekode_kjoring_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "fk_fagsak_id", nullable = false, updatable = false, unique = true)
    val fagsakId: Long,
    @Column(name = "bruker_ny_klassekode", nullable = false)
    val brukerNyKlassekode: Boolean = false,
)
