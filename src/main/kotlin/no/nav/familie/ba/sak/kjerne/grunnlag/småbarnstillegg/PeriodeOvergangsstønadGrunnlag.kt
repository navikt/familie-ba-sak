package no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator
import javax.persistence.Table

/**
 * Periode vi har hentet fra ef-sak som representerer når en person
 * har hatt full overgangsstønad.
 */
@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "PeriodeOvergangsstønadGrunnlag")
@Table(name = "GR_PERIODE_OVERGANGSSTONAD")
class PeriodeOvergangsstønadGrunnlag(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gr_periode_overgangsstonad_seq_generator")
    @SequenceGenerator(
        name = "gr_periode_overgangsstonad_seq_generator",
        sequenceName = "gr_periode_overgangsstonad_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandlingId: Long,

    @Column(name = "person_ident", nullable = false, updatable = false)
    val personIdent: String,

    @Column(name = "fom", nullable = false, columnDefinition = "DATE")
    val fom: LocalDate,

    @Column(name = "tom", nullable = false, columnDefinition = "DATE")
    val tom: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "datakilde", nullable = false)
    val datakilde: PeriodeOvergangsstønad.Datakilde,
) : BaseEntitet()

fun PeriodeOvergangsstønad.tilPeriodeOvergangsstønadGrunnlag(behandlingId: Long) = PeriodeOvergangsstønadGrunnlag(
    behandlingId = behandlingId,
    personIdent = this.personIdent,
    fom = this.fomDato,
    tom = this.tomDato,
    datakilde = this.datakilde
)
