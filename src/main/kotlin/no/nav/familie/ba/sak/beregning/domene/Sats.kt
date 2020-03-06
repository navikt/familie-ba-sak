package no.nav.familie.ba.sak.beregning.domene

import java.time.LocalDate
import javax.persistence.*

@Entity(name = "Sats")
@Table(name = "SATS")
data class Sats(

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sats_seq_generator")
        @SequenceGenerator(name = "sats_seq_generator", sequenceName = "sats_seq", allocationSize = 50)
        val id: Long = 0,

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, updatable = false)
        val type: SatsType,

        @Column(name = "belop", nullable = false, updatable = false)
        val beløp: Int,

        @Column(name = "gyldig_fom", updatable = false)
        val gyldigFom: LocalDate?,

        @Column(name = "gyldig_tom", updatable = false)
        val gyldigTom: LocalDate?
)

enum class SatsType(val beskrivelse: String) {
    ORBA("Ordinær barnetrygd"),
    SMA("Småbarnstillegg"),
    TILLEGG_ORBA("Tillegg til barnetrygd for barn 0-6 år"),
    FINN_SVAL("Finnmark- og Svalbardtillegg")
}