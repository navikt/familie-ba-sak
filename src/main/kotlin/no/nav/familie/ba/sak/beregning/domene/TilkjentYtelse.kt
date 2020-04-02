package no.nav.familie.ba.sak.beregning.domene

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelse
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.hibernate.annotations.Type
import java.time.LocalDate
import javax.persistence.*

@Entity(name = "TilkjentYtelse")
@Table(name = "TILKJENT_YTELSE")
data class TilkjentYtelse(

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tilkjent_ytelse_seq_generator")
        @SequenceGenerator(name = "tilkjent_ytelse_seq_generator", sequenceName = "tilkjent_ytelse_seq", allocationSize = 50)
        val id: Long = 0,

        @OneToOne(optional = false) @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "stonad_fom", nullable = true)
        var stønadFom: LocalDate? = null,

        @Column(name = "stonad_tom", nullable = true)
        var stønadTom: LocalDate? = null,

        @Column(name = "opphor_fom", nullable = true)
        var opphørFom: LocalDate? = null,

        @Column(name = "opprettet_dato", nullable = false)
        val opprettetDato: LocalDate,

        @Column(name = "endret_dato", nullable = false)
        var endretDato: LocalDate,

        @Column(name = "utbetalingsoppdrag", columnDefinition = "TEXT")
        var utbetalingsoppdrag: String? = null,

        @OneToMany(mappedBy = "tilkjentYtelse", cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE])
        val andelerTilkjentYtelse: Set<AndelTilkjentYtelse>
)