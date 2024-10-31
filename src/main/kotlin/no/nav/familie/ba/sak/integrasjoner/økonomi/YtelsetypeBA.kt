package no.nav.familie.ba.sak.integrasjoner.økonomi

enum class YtelsetypeBA(
    override val klassifisering: String,
    override val satsType: no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType = no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType.MND,
) : no.nav.familie.felles.utbetalingsgenerator.domain.Ytelsestype {
    ORDINÆR_BARNETRYGD("BATR"),
    UTVIDET_BARNETRYGD("BAUTV-OP"),
    UTVIDET_BARNETRYGD_GAMMEL("BATR"),
    SMÅBARNSTILLEGG("BATRSMA"),
}
