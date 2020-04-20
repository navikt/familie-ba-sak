package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.common.BaseEntitet
import java.time.LocalDate
import java.util.*
import javax.persistence.*

@Entity(name = "AndelTilkjentYtelse")
@Table(name = "ANDEL_TILKJENT_YTELSE")
data class AndelTilkjentYtelse(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "andel_tilkjent_ytelse_seq_generator")
        @SequenceGenerator(name = "andel_tilkjent_ytelse_seq_generator", sequenceName = "andel_tilkjent_ytelse_seq", allocationSize = 50)
        val id: Long = 0,

        @Column(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandlingId: Long,

        @ManyToOne
        @JoinColumn(name = "tilkjent_ytelse_id", nullable = false, updatable = false)
        var tilkjentYtelse: TilkjentYtelse,

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
        val type: YtelseType
) : BaseEntitet() {

        override fun equals(other: Any?): Boolean {
                if (other == null || javaClass != other.javaClass) {
                        return false
                } else if (this === other) {
                        return true
                }

                val annen = other as AndelTilkjentYtelse
                return Objects.equals(behandlingId, annen.behandlingId)
                        && Objects.equals(type, annen.type)
                        && Objects.equals(beløp, annen.beløp)
                        && Objects.equals(stønadFom, annen.stønadFom)
                        && Objects.equals(stønadTom, annen.stønadTom)
                        && Objects.equals(personId, annen.personId)
        }

        override fun hashCode(): Int {
                return Objects.hash(id, behandlingId, type, beløp, stønadFom, stønadTom, personId)
        }

        override fun toString(): String {
                return "AndelTilkjentYtelse(id = $id, behandling = $behandlingId, person = $personId, " +
                       "beløp = $beløp, stønadFom = $stønadFom, stønadTom = $stønadTom)"
        }
}


enum class YtelseType(val klassifisering: String) {
        ORDINÆR_BARNETRYGD("BATR"),
        UTVIDET_BARNETRYGD("BATR"),
        SMÅBARNSTILLEGG("BATRSMA"),
        EØS("BATR"),
        MANUELL_VURDERING("BATR")
}