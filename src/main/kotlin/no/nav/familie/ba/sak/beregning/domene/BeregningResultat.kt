package no.nav.familie.ba.sak.beregning.domene

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.hibernate.annotations.Type
import java.time.LocalDate
import javax.persistence.*

@Entity(name = "BeregningResultat")
@Table(name = "BEREGNINGRESULTAT")
data class BeregningResultat(

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "beregningresultat_seq_generator")
        @SequenceGenerator(name = "beregningresultat_seq_generator", sequenceName = "beregningresultat_seq", allocationSize = 50)
        val id: Long = 0,

        @OneToOne(optional = false) @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "stonad_fom", nullable = true)
        val stønadFom: LocalDate?,

        @Column(name = "stonad_tom", nullable = false)
        val stønadTom: LocalDate,

        @Column(name = "opphor_fom", nullable = true)
        val opphørFom: LocalDate?,

        @Column(name = "opprettet_dato", nullable = false)
        val opprettetDato: LocalDate,

        @Column(name = "utbetalingsoppdrag", columnDefinition = "TEXT")
        val utbetalingsoppdrag: String
)