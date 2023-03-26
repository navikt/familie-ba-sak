package no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingIdConverter
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
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
    @Convert(converter = BehandlingIdConverter::class)
    val behandlingId: BehandlingId,

    @OneToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,

    @Column(name = "fom", nullable = false, columnDefinition = "DATE")
    val fom: LocalDate,

    @Column(name = "tom", nullable = false, columnDefinition = "DATE")
    val tom: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "datakilde", nullable = false)
    val datakilde: PeriodeOvergangsstønad.Datakilde
) : BaseEntitet() {
    fun tilInternPeriodeOvergangsstønad() = InternPeriodeOvergangsstønad(
        personIdent = this.aktør.aktivFødselsnummer(),
        fomDato = this.fom,
        tomDato = this.tom
    )
}

fun PeriodeOvergangsstønad.tilPeriodeOvergangsstønadGrunnlag(behandlingId: BehandlingId, aktør: Aktør) =
    PeriodeOvergangsstønadGrunnlag(
        behandlingId = behandlingId,
        aktør = aktør,
        fom = this.fomDato,
        tom = this.tomDato,
        datakilde = this.datakilde
    )
