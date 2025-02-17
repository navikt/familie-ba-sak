package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.pdl.logger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.objectMapper

object SisteUtvidetAndelOverstyrer {
    fun overstyrSisteUtvidetBarnetrygdAndel(
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag: List<TilkjentYtelse>,
        skalBrukeNyKlassekodeForUtvidetBarnetrygd: Boolean,
    ): Map<IdentOgType, AndelDataLongId> {
        return sisteAndelPerKjede.mapValues { (identOgType, sisteAndelIKjede) ->
            if (identOgType.type != YtelsetypeBA.UTVIDET_BARNETRYGD) {
                return@mapValues sisteAndelIKjede.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
            }

            if (tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag.isEmpty()) {
                return@mapValues sisteAndelIKjede.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
            }

            // Finner siste utbetalingsoppdraget som innehold kjedelementer med oppdatert utvidet klassekode
            val sisteTilkjenteYtelseMedOppdatertUtvidetBarnetrygdKlassekodeIUtbetalingsoppdrag = tilkjenteYtelserMedOppdatertUtvidetKlassekodeIUtbetalingsoppdrag.maxBy { tilkjentYtelse -> tilkjentYtelse.behandling.aktivertTidspunkt }

            // Finner det siste kjedelementet med oppdatert utvidet klassekode
            val sistOversendteUtvidetBarnetrygdKjedeelement =
                objectMapper
                    .readValue(sisteTilkjenteYtelseMedOppdatertUtvidetBarnetrygdKlassekodeIUtbetalingsoppdrag.utbetalingsoppdrag, Utbetalingsoppdrag::class.java)
                    .utbetalingsperiode
                    .single { utbetalingsperiode -> utbetalingsperiode.periodeId == sisteAndelIKjede.periodeOffset && utbetalingsperiode.klassifisering == YtelsetypeBA.UTVIDET_BARNETRYGD.klassifisering }

            if (sisteAndelIKjede.stønadFom != sistOversendteUtvidetBarnetrygdKjedeelement.vedtakdatoFom.toYearMonth()) {
                logger.warn("Overstyrer vedtakFom i andelDataLongId da fom til siste andel per kjede ikke stemmer overens med siste kjedelement oversendt til Oppdrag")
                // Oppdaterer fom i AndelDataLongId til samme fom som sist oversendte, da det ikke er 1-1 mellom fom på siste andel og fom på siste kjedelement oversendt til Oppdrag.
                return@mapValues sisteAndelIKjede.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd).copy(fom = sistOversendteUtvidetBarnetrygdKjedeelement.vedtakdatoFom.toYearMonth(), tom = sistOversendteUtvidetBarnetrygdKjedeelement.vedtakdatoTom.toYearMonth())
            }
            return@mapValues sisteAndelIKjede.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
        }
    }
}
