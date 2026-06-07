package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "RegistrertSøknadstidspunktPåPerson")
@Table(name = "REGISTRERT_SOKNADSTIDSPUNKT_PAA_PERSON")
data class RegistrertSøknadstidspunktPåPerson(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "registrert_soknadstidspunkt_paa_person_seq_generator")
    @SequenceGenerator(
        name = "registrert_soknadstidspunkt_paa_person_seq_generator",
        sequenceName = "registrert_soknadstidspunkt_paa_person_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,
    @Column(name = "soknadstidspunkt", nullable = false)
    val søknadstidspunkt: LocalDate,
) : BaseEntitet()
