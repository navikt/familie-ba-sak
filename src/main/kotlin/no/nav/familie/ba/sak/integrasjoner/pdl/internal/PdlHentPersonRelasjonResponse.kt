package no.nav.familie.ba.sak.integrasjoner.pdl.internal

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon

data class PdlHentPersonRelasjonerResponse(
    val data: PdlPersonRelasjon,
    override val errors: List<PdlError>?
) :
    PdlBaseResponse(errors)

data class PdlPersonRelasjon(val person: PdlPersonRelasjonData?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlPersonRelasjonData(
    val forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
)
