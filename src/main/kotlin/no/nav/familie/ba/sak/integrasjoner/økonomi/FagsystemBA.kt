package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.felles.utbetalingsgenerator.domain.Fagsystem

enum class FagsystemBA(
    override val kode: String,
    override val gyldigeSatstyper: Set<YtelsetypeBA>,
) : Fagsystem {
    BARNETRYGD(
        "BA",
        setOf(YtelsetypeBA.ORDINÆR_BARNETRYGD, YtelsetypeBA.UTVIDET_BARNETRYGD, YtelsetypeBA.SMÅBARNSTILLEGG),
    ),
}
