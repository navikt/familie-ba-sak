package no.nav.familie.ba.sak.internal

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlFolkeregisteridentifikator
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlKontaktinformasjonForDødsbo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import java.time.LocalDate

data class HentPersonFraPdlRequest(
    val ident: String,
    val begrunnelse: String,
    @Schema(example = "false") val visFødselsdato: Boolean = false,
    @Schema(example = "false") val visNavn: Boolean = false,
    @Schema(example = "false") val visKjønn: Boolean = false,
    @Schema(example = "false") val visAdressebeskyttelse: Boolean = false,
    @Schema(example = "false") val visBostedsadresse: Boolean = false,
    @Schema(example = "false") val visOppholdsadresse: Boolean = false,
    @Schema(example = "false") val visFolkeregister: Boolean = false,
    @Schema(example = "false") val visDødsfall: Boolean = false,
    @Schema(example = "false") val visStatsborgerskap: Boolean = false,
    @Schema(example = "false") val visOpphold: Boolean = false,
    @Schema(example = "false") val visSivilstand: Boolean = false,
    @Schema(example = "false") val visDeltBosted: Boolean = false,
    @Schema(example = "false") val visKontaktinformasjonForDødsbo: Boolean = false,
)

data class ForvalterPersonInfoDto(
    val fødselsdato: LocalDate? = null,
    val navn: String? = null,
    val kjønn: Kjønn? = null,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val bostedsadresser: List<Bostedsadresse>? = null,
    val oppholdsadresser: List<Oppholdsadresse>? = null,
    val deltBosted: List<DeltBosted>? = null,
    val sivilstander: List<Sivilstand>? = null,
    val opphold: List<Opphold>? = null,
    val statsborgerskap: List<Statsborgerskap>? = null,
    val dødsfall: DødsfallData? = null,
    val kontaktinformasjonForDoedsbo: PdlKontaktinformasjonForDødsbo? = null,
    val historiskeIdenter: List<PdlFolkeregisteridentifikator>? = null,
)

fun PersonInfo.tilForvalterPersonInfoDto(hentPersonFraPdlRequest: HentPersonFraPdlRequest) =
    ForvalterPersonInfoDto(
        fødselsdato = fødselsdato.takeIf { hentPersonFraPdlRequest.visFødselsdato },
        navn = navn.takeIf { hentPersonFraPdlRequest.visNavn },
        kjønn = kjønn.takeIf { hentPersonFraPdlRequest.visKjønn },
        adressebeskyttelseGradering = adressebeskyttelseGradering.takeIf { hentPersonFraPdlRequest.visAdressebeskyttelse },
        bostedsadresser = bostedsadresser.takeIf { hentPersonFraPdlRequest.visBostedsadresse },
        oppholdsadresser = oppholdsadresser.takeIf { hentPersonFraPdlRequest.visOppholdsadresse },
        deltBosted = deltBosted.takeIf { hentPersonFraPdlRequest.visDeltBosted },
        sivilstander = sivilstander.takeIf { hentPersonFraPdlRequest.visSivilstand },
        opphold = opphold.takeIf { hentPersonFraPdlRequest.visOpphold },
        statsborgerskap = statsborgerskap.takeIf { hentPersonFraPdlRequest.visStatsborgerskap },
        dødsfall = dødsfall.takeIf { hentPersonFraPdlRequest.visDødsfall },
        kontaktinformasjonForDoedsbo = kontaktinformasjonForDoedsbo.takeIf { hentPersonFraPdlRequest.visKontaktinformasjonForDødsbo },
        historiskeIdenter = historiskeIdenter.takeIf { hentPersonFraPdlRequest.visFolkeregister },
    )
