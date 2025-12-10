package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class PdlFalskIdentitetResponse(
    val person: PdlFalskIdentitetPerson,
)

data class PdlFalskIdentitetPerson(
    val falskIdentitet: PdlFalskIdentitet?,
)

data class PdlFalskIdentitet(
    val erFalsk: Boolean,
    val folkeregistermetadata: PdlFolkeregistermetadata?,
    val metadata: PdlFalskIdentitetMetadata?,
    val rettIdentitetErUkjent: Boolean?,
    val rettIdentitetVedIdentifikasjonsnummer: String?,
    val rettIdentitetVedOpplysninger: PdlFalskIdentitetIdentifiserendeInformasjon?,
)

data class PdlFolkeregistermetadata(
    val aarsak: String?,
    val ajourholdstidspunkt: LocalDateTime?,
    val gyldighetstidspunkt: LocalDateTime?,
    val kilde: String?,
    val opphoerstidspunkt: LocalDateTime?,
    val sekvens: Int?,
)

data class PdlFalskIdentitetMetadata(
    val master: String?,
    val historisk: Boolean,
)

data class PdlEndring(
    val hendelseId: String,
    val kilde: String,
    val registrert: LocalDateTime,
    val registrertAv: String,
    val systemkilde: String,
    val type: PdlEndringstype,
)

enum class PdlEndringstype {
    KORRIGER,
    OPPHOER,
    OPPRETT,
}

data class PdlFalskIdentitetIdentifiserendeInformasjon(
    val foedselsdato: LocalDate?,
    val kjoenn: PdlKjoennType?,
    val personnavn: PdlPersonnavn,
    val statsborgerskap: List<String>,
)

enum class PdlKjoennType {
    KVINNE,
    MANN,
    UKJENT,
}

data class PdlPersonnavn(
    val etternavn: String,
    val fornavn: String,
    val mellomnavn: String?,
)
