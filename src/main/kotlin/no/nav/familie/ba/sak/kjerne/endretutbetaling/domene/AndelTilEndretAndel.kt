package no.nav.familie.ba.sak.kjerne.endretutbetaling.domene

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MapsId
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "AndelTilEndretAndel")
@Table(name = "ANDEL_TIL_ENDRET_ANDEL")
data class AndelTilEndretAndel(
        @EmbeddedId
        val id: AndelTilEndretAndelPk,

        @ManyToOne
        @MapsId("andelTilkjentYtelseId")
        @JoinColumn(name = "fk_andel_tilkjent_ytelse_id")
        val andelTilkjentYtelse: AndelTilkjentYtelse,

        @ManyToOne
        @MapsId("endretUtbetalingAndelId")
        @JoinColumn(name = "fk_endret_utbetaling_andel_id")
        val endretUtbetalingAndel: EndretUtbetalingAndel
)