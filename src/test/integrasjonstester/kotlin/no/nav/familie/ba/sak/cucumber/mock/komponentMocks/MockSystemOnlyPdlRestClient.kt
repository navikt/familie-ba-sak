package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBostedsadresseOgDeltBostedPerson
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse

fun mockSystemOnlyPdlRestClient(
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
): SystemOnlyPdlRestClient =
    mockk<SystemOnlyPdlRestClient> {
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
                    PdlBostedsadresseOgDeltBostedPerson(
                        bostedsadresse = listOf(Bostedsadresse(gyldigFraOgMed = fødselsdato, vegadresse = vegadresseIOslo)),
                        deltBosted = eksisterendeAdresser?.deltBosted ?: emptyList(),
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
    }
