package no.nav.familie.ba.sak.kjerne.behandling.søknadreferanse

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "SøknadReferanse")
@Table(name = "SOKNAD_REFERANSE")
data class SøknadReferanse(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "soknad_referanse_seq_generator")
    @SequenceGenerator(name = "soknad_referanse_seq_generator", sequenceName = "soknad_referanse_seq", allocationSize = 50)
    val id: Long = 0,
    @Column(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandlingId: Long,
    @Column(name = "journalpost_id", nullable = false, updatable = false)
    val journalpostId: String,
) : BaseEntitet()
