package no.nav.familie.ba.sak.kjerne.endretutbetaling.domene

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
data class AndelTilEndretAndelPk(
        @Column(name = "fk_andel_tilkjent_ytelse_id")
        var andelTilkjentYtelseId: Long,

        @Column(name = "fk_endret_utbetaling_andel_id")
        var endretUtbetalingAndelId: Long,
): Serializable