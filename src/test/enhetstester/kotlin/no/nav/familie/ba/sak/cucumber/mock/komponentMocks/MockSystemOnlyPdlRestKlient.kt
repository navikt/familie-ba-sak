package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.datagenerator.lagPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse

fun mockSystemOnlyPdlRestKlient(
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
): SystemOnlyPdlRestKlient =
    mockk<SystemOnlyPdlRestKlient> {
        every { hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
            val identer = firstArg<List<String>>()
            val vegadresseIOslo = Vegadresse(null, null, null, null, null, "0301", null, null)
            identer.associateWith { ident ->
                val fødselsdato =
                    dataFraCucumber.persongrunnlag
                        .flatMap { it.value.personer }
                        .first { it.aktør.aktivFødselsnummer() == ident }
                        .fødselsdato

                val eksisterendeAdresser = dataFraCucumber.adresser[ident]
                if (eksisterendeAdresser == null || eksisterendeAdresser.bostedsadresse.isEmpty()) {
                    PdlAdresserPerson(
                        bostedsadresse = listOf(Bostedsadresse(gyldigFraOgMed = fødselsdato, vegadresse = vegadresseIOslo)),
                        deltBosted = eksisterendeAdresser?.deltBosted ?: emptyList(),
                    )
                } else {
                    eksisterendeAdresser
                }
            }
        }

        every { hentAdresserForPersoner(any()) } answers {
            val identer = firstArg<List<String>>()
            val vegadresseIOslo = Vegadresse(null, null, null, null, null, "0301", null, null)
            identer.associateWith { ident ->
                val fødselsdato =
                    dataFraCucumber.persongrunnlag
                        .flatMap { it.value.personer }
                        .first { it.aktør.aktivFødselsnummer() == ident }
                        .fødselsdato

                val eksisterendeAdresser = dataFraCucumber.adresser[ident]
                if (eksisterendeAdresser == null || eksisterendeAdresser.bostedsadresse.isEmpty()) {
                    PdlAdresserPerson(
                        bostedsadresse = listOf(Bostedsadresse(gyldigFraOgMed = fødselsdato, vegadresse = vegadresseIOslo)),
                        deltBosted = eksisterendeAdresser?.deltBosted ?: emptyList(),
                        oppholdsadresse = eksisterendeAdresser?.oppholdsadresse ?: emptyList(),
                    )
                } else {
                    eksisterendeAdresser
                }
            }
        }

        every { hentStatsborgerskap(any(), any()) } answers {
            val aktør = firstArg<Aktør>()
            val fødselsdato =
                dataFraCucumber.persongrunnlag
                    .flatMap { it.value.personer }
                    .first { it.aktør == aktør }
                    .fødselsdato

            listOf(
                Statsborgerskap(
                    land = "NOR",
                    gyldigFraOgMed = fødselsdato,
                    gyldigTilOgMed = null,
                    bekreftelsesdato = fødselsdato,
                ),
            )
        }

        every {
            hentPerson(
                fødselsnummer = any(),
                personInfoQuery = any(),
            )
        } answers {
            lagPersonInfo(
                bostedsadresser = dataFraCucumber.adresser[firstArg<String>()]?.bostedsadresse ?: emptyList(),
                oppholdsadresser = dataFraCucumber.adresser[firstArg<String>()]?.oppholdsadresse ?: emptyList(),
                deltBosted = dataFraCucumber.adresser[firstArg<String>()]?.deltBosted ?: emptyList(),
            )
        }
    }
