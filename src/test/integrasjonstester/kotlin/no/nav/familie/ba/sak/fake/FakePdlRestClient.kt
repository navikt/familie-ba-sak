package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.VergemaalEllerFremtidsfullmakt
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

class FakePdlRestClient(
    restOperations: RestOperations,
    personidentService: PersonidentService,
) : SystemOnlyPdlRestClient(
        pdlBaseUrl = URI("dummy_uri"),
        restTemplate = restOperations,
        personidentService = personidentService,
    ) {
    override fun hentBostedsadresseOgDeltBostedForPersoner(identer: List<String>): Map<String, PdlAdresserPerson> =
        identer.associateWith {
            PdlAdresserPerson(
                bostedsadresse =
                    bostedsadresser[it]
                        ?: listOf(
                            Bostedsadresse(
                                gyldigFraOgMed = LocalDate.now().minusYears(1),
                                gyldigTilOgMed = null,
                                vegadresse = null,
                                matrikkeladresse = lagMatrikkeladresse(1234L),
                                ukjentBosted = null,
                            ),
                        ),
                deltBosted = deltBosteder[it] ?: emptyList(),
                oppholdsadresse = emptyList(),
            )
        }

    override fun hentAdresserForPersoner(identer: List<String>): Map<String, PdlAdresserPerson> =
        identer.associateWith {
            PdlAdresserPerson(
                bostedsadresse =
                    bostedsadresser[it]
                        ?: listOf(
                            Bostedsadresse(
                                gyldigFraOgMed = LocalDate.now().minusYears(1),
                                gyldigTilOgMed = null,
                                vegadresse = null,
                                matrikkeladresse = lagMatrikkeladresse(1234L),
                                ukjentBosted = null,
                            ),
                        ),
                deltBosted = deltBosteder[it] ?: emptyList(),
                oppholdsadresse = oppholdsadresser[it] ?: emptyList(),
            )
        }

    override fun hentStatsborgerskap(
        aktør: Aktør,
        historikk: Boolean,
    ): List<Statsborgerskap> =
        listOf(
            Statsborgerskap(
                land = "NOR",
                gyldigFraOgMed = LocalDate.now().minusYears(1),
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            ),
        )

    override fun hentOppholdstillatelse(
        aktør: Aktør,
        historikk: Boolean,
    ): List<Opphold> =
        listOf(
            Opphold(
                oppholdFra = LocalDate.now().minusYears(10),
                oppholdTil = null,
                type = OPPHOLDSTILLATELSE.PERMANENT,
            ),
        )

    override fun hentVergemaalEllerFremtidsfullmakt(aktør: Aktør): List<VergemaalEllerFremtidsfullmakt> = emptyList()

    companion object {
        private val bostedsadresser = mutableMapOf<String, List<Bostedsadresse>>()
        private val deltBosteder = mutableMapOf<String, List<DeltBosted>>()
        private val oppholdsadresser = mutableMapOf<String, List<Oppholdsadresse>>()

        fun leggTilBostedsadresseIPDL(
            personIdenter: List<String>,
            bostedsadresse: Bostedsadresse,
        ) {
            personIdenter.forEach { personIdent ->
                bostedsadresser[personIdent] = listOf(bostedsadresse)
            }
        }

        fun leggTilDeltBostedIPDL(
            personIdenter: List<String>,
            deltBosted: DeltBosted,
        ) {
            personIdenter.forEach { personIdent ->
                deltBosteder[personIdent] = listOf(deltBosted)
            }
        }

        fun leggTilOppholdsadresseIPDL(
            personIdenter: List<String>,
            oppholdsadresse: Oppholdsadresse,
        ) {
            personIdenter.forEach { personIdent ->
                oppholdsadresser[personIdent] = listOf(oppholdsadresse)
            }
        }
    }
}
