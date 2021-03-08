package no.nav.familie.ba.sak.simulering.domene

import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import java.math.BigDecimal
import java.time.LocalDate
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
@Entity(name = "SimuleringPostering")
@Table(name = "VEDTAK_SIMULERING_POSTERING")
data class VedtakSimuleringPostering(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtak_simulering_postering_seq_generator")
        @SequenceGenerator(name = "vedtak_simulering_postering_seq_generator",
                           sequenceName = "vedtak_simulering_postering_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_vedtak_simulering_mottaker_id", nullable = false, updatable = false)
        val vedtakSimuleringMottaker: VedtakSimuleringMottaker,

        @Enumerated(EnumType.STRING)
        @Column(name = "fag_omraade_kode", nullable = false)
        val fagOmrådeKode: FagOmrådeKode,

        @Column(name = "fom", updatable = false, nullable = false)
        val fom: LocalDate,

        @Column(name = "tom", updatable = false, nullable = false)
        val tom: LocalDate,

        @Enumerated(EnumType.STRING)
        @Column(name = "betaling_type", nullable = false)
        val betalingType: BetalingType,

        @Column(name = "belop", nullable = false)
        val beløp: BigDecimal,

        @Enumerated(EnumType.STRING)
        @Column(name = "postering_type", nullable = false)
        val posteringType: PosteringType,

        @Column(name = "forfallsdato", updatable = false, nullable = false)
        val forfallsdato: LocalDate,

        @Column(name = "uten_inntrekk", nullable = false)
        val utenInntrekk: Boolean,
)

