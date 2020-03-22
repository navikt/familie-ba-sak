package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.BaseEntitet
import java.time.LocalDate
import javax.persistence.*

@Entity(name = "VedtakPerson")
@Table(name = "VEDTAK_PERSON")
data class VedtakPerson(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtak_person_seq_generator")
        @SequenceGenerator(name = "vedtak_person_seq_generator", sequenceName = "vedtak_person_seq", allocationSize = 50)
        val id: Long = 0,

        @Column(name = "fk_vedtak_id", nullable = false, updatable = false)
        val vedtakId: Long,

        @Column(name = "fk_person_id", nullable = false, updatable = false)
        val personId: Long,

        @Column(name = "belop", nullable = false)
        val beløp: Int,

        @Column(name = "stonad_fom", nullable = false)
        val stønadFom: LocalDate,

        @Column(name = "stonad_tom", nullable = false)
        val stønadTom: LocalDate,

        @Enumerated(EnumType.STRING)
        @Column(name = "type", nullable = false)
        val type: Ytelsetype
) : BaseEntitet()

enum class Ytelsetype(val klassifisering: String) {
        ORDINÆR_BARNETRYGD("BATR"),
        UTVIDET_BARNETRYGD("BATR"),
        SMÅBARNSTILLEGG("BATRSMA"),
        EØS("BATR"),
        MANUELL_VURDERING("BATR")
}