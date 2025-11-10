package no.nav.familie.ba.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepAdresse.ADRESSETYPE
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepAdresse.KOMMUNENUMMER
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.AKTØR_ID
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.parseAktørId
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.parseAktørIdListe
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse

fun parseAdresser(
    dataTable: DataTable,
    persongrunnlag: Map<Long, PersonopplysningGrunnlag>,
): Map<String, PdlAdresserPerson> =
    dataTable
        .asMaps()
        .flatMap {
            parseAktørIdListe(it).map { aktørId ->
                it.toMutableMap().apply { put(AKTØR_ID.nøkkel, aktørId) }
            }
        }.groupBy { parseAktørId(it) }
        .map { (aktørId, rader) ->

            val adresserPerType = rader.groupBy { parseValgfriString(ADRESSETYPE, it) ?: "Bostedsadresse" }

            if (adresserPerType.keys.any { key -> key !in setOf("Bostedsadresse", "Delt bosted", "Oppholdsadresse") }) {
                throw Feil("Adressetype må være 'Bostedsadresse', 'Oppholdsadresse' eller 'Delt bosted'")
            }

            val bostedsadresseRad = adresserPerType["Bostedsadresse"].orEmpty()
            val oppholdsadresseRad = adresserPerType["Oppholdsadresse"].orEmpty()
            val deltBostedRad = adresserPerType["Delt bosted"].orEmpty()

            val bostedsadresser =
                bostedsadresseRad.map { rad ->
                    val fraDato = parseDato(Domenebegrep.FRA_DATO, rad)
                    val tilDato = parseValgfriDato(Domenebegrep.TIL_DATO, rad)
                    val kommunenummer = parseString(KOMMUNENUMMER, rad)

                    Bostedsadresse(
                        gyldigFraOgMed = fraDato,
                        gyldigTilOgMed = tilDato,
                        vegadresse =
                            Vegadresse(
                                matrikkelId = null,
                                husnummer = null,
                                husbokstav = null,
                                bruksenhetsnummer = null,
                                adressenavn = null,
                                kommunenummer = kommunenummer,
                                tilleggsnavn = null,
                                postnummer = null,
                            ),
                    )
                }

            val oppholdsadresser =
                oppholdsadresseRad.map { rad ->
                    val fraDato = parseDato(Domenebegrep.FRA_DATO, rad)
                    val tilDato = parseValgfriDato(Domenebegrep.TIL_DATO, rad)
                    val kommunenummer = parseString(KOMMUNENUMMER, rad)

                    Oppholdsadresse(
                        gyldigFraOgMed = fraDato,
                        gyldigTilOgMed = tilDato,
                        vegadresse =
                            Vegadresse(
                                matrikkelId = null,
                                husnummer = null,
                                husbokstav = null,
                                bruksenhetsnummer = null,
                                adressenavn = null,
                                kommunenummer = kommunenummer,
                                tilleggsnavn = null,
                                postnummer = null,
                            ),
                    )
                }

            val deltBosted =
                deltBostedRad.map { rad ->
                    val fraDato = parseDato(Domenebegrep.FRA_DATO, rad)
                    val tilDato = parseValgfriDato(Domenebegrep.TIL_DATO, rad)
                    val kommunenummer = parseString(KOMMUNENUMMER, rad)

                    DeltBosted(
                        startdatoForKontrakt = fraDato,
                        sluttdatoForKontrakt = tilDato,
                        vegadresse =
                            Vegadresse(
                                matrikkelId = null,
                                husnummer = null,
                                husbokstav = null,
                                bruksenhetsnummer = null,
                                adressenavn = null,
                                kommunenummer = kommunenummer,
                                tilleggsnavn = null,
                                postnummer = null,
                            ),
                    )
                }

            val ident =
                persongrunnlag
                    .values
                    .flatMap { it.personer }
                    .first { it.aktør.aktørId == aktørId }
                    .aktør
                    .aktivFødselsnummer()

            ident to
                PdlAdresserPerson(
                    bostedsadresser = bostedsadresser,
                    deltBosted = deltBosted,
                    oppholdsadresser = oppholdsadresser,
                )
        }.toMap()
