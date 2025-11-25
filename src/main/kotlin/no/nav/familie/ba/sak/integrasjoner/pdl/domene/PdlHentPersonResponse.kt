package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.familie.ba.sak.common.PdlPersonKanIkkeBehandlesIFagSystemÅrsak
import no.nav.familie.ba.sak.common.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ba.sak.integrasjoner.pdl.logger
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap

data class PdlHentPersonResponse(
    val person: PdlPersonData?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlPersonData(
    val folkeregisteridentifikator: List<PdlFolkeregisteridentifikator>,
    val foedselsdato: List<PdlFødselsDato>,
    val navn: List<PdlNavn> = emptyList(),
    val kjoenn: List<PdlKjoenn> = emptyList(),
    val forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
    val adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
    val sivilstand: List<Sivilstand> = emptyList(),
    val bostedsadresse: List<Bostedsadresse>,
    val oppholdsadresse: List<Oppholdsadresse> = emptyList(),
    val deltBosted: List<DeltBosted> = emptyList(),
    val opphold: List<Opphold> = emptyList(),
    val statsborgerskap: List<Statsborgerskap> = emptyList(),
    val doedsfall: List<PdlDødsfallResponse> = emptyList(),
    val doedfoedtBarn: List<PdlDødfødtBarnResponse> = emptyList(),
    val kontaktinformasjonForDoedsbo: List<PdlKontaktinformasjonForDødsbo> = emptyList(),
) {
    fun validerOmPersonKanBehandlesIFagsystem() {
        if (foedselsdato.isEmpty() && doedfoedtBarn.isEmpty()) {
            throw PdlPersonKanIkkeBehandlesIFagsystem(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        }

        if (folkeregisteridentifikator.firstOrNull()?.status == FolkeregisteridentifikatorStatus.OPPHOERT) {
            throw PdlPersonKanIkkeBehandlesIFagsystem(
                PdlPersonKanIkkeBehandlesIFagSystemÅrsak.OPPHØRT,
            )
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFolkeregisteridentifikator(
    val identifikasjonsnummer: String?,
    val status: FolkeregisteridentifikatorStatus,
    val type: FolkeregisteridentifikatorType?,
)

enum class FolkeregisteridentifikatorStatus { I_BRUK, OPPHOERT }

enum class FolkeregisteridentifikatorType { FNR, DNR }

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFødselsDato(
    val foedselsdato: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlNavn(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val metadata: PdlMetadata,
) {
    fun fulltNavn(): String =
        when (mellomnavn) {
            null -> "$fornavn $etternavn"
            else -> "$fornavn $mellomnavn $etternavn"
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlKjoenn(
    val kjoenn: Kjønn,
    val metadata: PdlMetadata,
)

data class PdlMetadata(
    val master: String,
    val historisk: Boolean,
)

// Filtrer på historisk slik at ikke-historiske alltid får prioritet
fun List<PdlNavn>.filtrerNavnPåKilde(): PdlNavn? =
    this
        .filter { it.metadata.historisk == false }
        .minByOrNull { it.metadata.master.kildeTilPrioritet() }

// Filtrer på historisk slik at ikke-historiske alltid får prioritet
fun List<PdlKjoenn>.filtrerKjønnPåKilde(): PdlKjoenn? =
    this
        .filter { it.metadata.historisk == false }
        .minByOrNull { it.metadata.master.kildeTilPrioritet() }

fun String.kildeTilPrioritet(): Int =
    when (uppercase()) {
        "PDL" -> {
            1
        }

        "FREG" -> {
            2
        }

        else -> {
            logger.warn("Ukjent kilde fra PDL: $this. Bør legges til.")
            3
        }
    }
