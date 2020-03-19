package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import java.time.LocalDate
import javax.persistence.*

@Entity(name = "YtelsePeriode")
@Table(name = "YTELSE_PERIODE")
data class YtelsePeriode (

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ytelse_periode_seq_generator")
        @SequenceGenerator(name = "ytelse_periode_seq_generator", sequenceName = "ytelse_periode_seq", allocationSize = 50)
        val id: Long = 0,

        @ManyToOne
        @JoinColumn(name="fk_vedtak_person_id", nullable=false)
        val vedtakPerson: VedtakPerson,

        @Column(name = "belop", nullable = false)
        val beløp: Int,

        @Column(name = "stonad_fom", nullable = false)
        val stønadFom: LocalDate,

        @Column(name = "stonad_tom", nullable = false)
        val stønadTom: LocalDate,

        @Enumerated(EnumType.STRING)
        @Column(name = "type", nullable = false)
        val type: Ytelsetype
)

enum class Ytelsetype(val klassifisering: String) {
    ORDINÆR_BARNETRYGD("BATR"),
    UTVIDET_BARNETRYGD("BATR"),
    SMÅBARNSTILLEGG("BATRSMA"),
    EØS("BATR"),
    MANUELL_VURDERING("BATR")
}