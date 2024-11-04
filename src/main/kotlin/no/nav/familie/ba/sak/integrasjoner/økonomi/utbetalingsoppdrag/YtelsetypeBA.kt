package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import no.nav.familie.felles.utbetalingsgenerator.domain.Ytelsestype

enum class YtelsetypeBA(
    override val klassifisering: String,
    override val satsType: Utbetalingsperiode.SatsType = Utbetalingsperiode.SatsType.MND,
) : Ytelsestype {
    ORDINÆR_BARNETRYGD("BATR"),
    UTVIDET_BARNETRYGD("BATR"),
    SMÅBARNSTILLEGG("BATRSMA"),
}
