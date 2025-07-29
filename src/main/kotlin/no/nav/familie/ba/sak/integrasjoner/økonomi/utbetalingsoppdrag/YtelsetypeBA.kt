package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import no.nav.familie.felles.utbetalingsgenerator.domain.Ytelsestype

enum class YtelsetypeBA(
    override val klassifisering: String,
    override val satsType: Utbetalingsperiode.SatsType = Utbetalingsperiode.SatsType.MND,
) : Ytelsestype {
    ORDINÆR_BARNETRYGD("BATR"),
    UTVIDET_BARNETRYGD("BAUTV-OP"),
    FINNMARKSTILLEGG("BATRFIN"),

    // UTVIDET_BARNETRYGD_GAMMEL kan ikke slettes før vi er sikre på at alle løpende saker er over på ny klassekode
    UTVIDET_BARNETRYGD_GAMMEL("BATR"),
    SMÅBARNSTILLEGG("BATRSMA"),
}
