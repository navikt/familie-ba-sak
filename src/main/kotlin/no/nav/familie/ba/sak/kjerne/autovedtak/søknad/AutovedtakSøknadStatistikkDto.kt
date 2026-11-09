package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import jakarta.validation.constraints.NotEmpty

data class AutovedtakSøknadStatistikkDto(
    val journalpostId: Long,
    @field:NotEmpty
    val feilårsaker: List<AutovedtakSøknadFeilårsak>,
)

enum class AutovedtakSøknadFeilårsak {
    SØKER_HAR_LØPENDE_BARNETRYGD,
    SØKER_HAR_HATT_BARNETRYGD_TIDLIGERE,
    SØKNAD_MANGLER_VEDLEGG,
    SØKNAD_HAR_TEMA_EØS,
    SØKER_ELLER_BARN_HAR_KODE_6_ELLER_19,
}
