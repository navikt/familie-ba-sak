package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
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

class FakePdlRestKlient(
    restOperations: RestOperations,
    personidentService: PersonidentService,
) : SystemOnlyPdlRestKlient(
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

    override fun hentAdresser(ident: String): PdlAdresserPerson? =
        PdlAdresserPerson(
            bostedsadresse =
                bostedsadresser[ident]
                    ?: listOf(
                        Bostedsadresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(1),
                            gyldigTilOgMed = null,
                            vegadresse = null,
                            matrikkeladresse = lagMatrikkeladresse(1234L),
                            ukjentBosted = null,
                        ),
                    ),
            deltBosted = deltBosteder[ident] ?: emptyList(),
            oppholdsadresse = oppholdsadresser[ident] ?: emptyList(),
        )

    companion object {
        private val bostedsadresser = mutableMapOf<String, MutableList<Bostedsadresse>>()
        private val deltBosteder = mutableMapOf<String, MutableList<DeltBosted>>()
        private val oppholdsadresser = mutableMapOf<String, MutableList<Oppholdsadresse>>()

        fun leggTilBostedsadresseIPDL(
            personIdenter: List<String>,
            bostedsadresse: Bostedsadresse,
        ) {
            personIdenter.forEach { personIdent ->
                bostedsadresser.getOrPut(personIdent, { mutableListOf() }).add(bostedsadresse)
            }
        }

        fun leggTilDeltBostedIPDL(
            personIdenter: List<String>,
            deltBosted: DeltBosted,
        ) {
            personIdenter.forEach { personIdent ->
                deltBosteder.getOrPut(personIdent, { mutableListOf() }).add(deltBosted)
            }
        }

        fun leggTilOppholdsadresseIPDL(
            personIdenter: List<String>,
            oppholdsadresse: Oppholdsadresse,
        ) {
            personIdenter.forEach { personIdent ->
                oppholdsadresser.getOrPut(personIdent, { mutableListOf() }).add(oppholdsadresse)
            }
        }
    }
}
