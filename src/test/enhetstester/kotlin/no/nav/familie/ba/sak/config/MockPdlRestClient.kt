package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBostedsadresseOgDeltBostedPerson
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Service
@Profile("mock-pdl-client")
@Primary
class MockPdlRestClient(
    restOperations: RestOperations,
    personidentService: PersonidentService,
) : SystemOnlyPdlRestClient(
        pdlBaseUrl = URI("dummy_uri"),
        restTemplate = restOperations,
        personidentService = personidentService,
    ) {
    override fun hentBostedsadresseOgDeltBostedForPersoner(identer: List<String>): Map<String, PdlBostedsadresseOgDeltBostedPerson> =
        identer.associateWith {
            PdlBostedsadresseOgDeltBostedPerson(
                bostedsadresse =
                    listOf(
                        Bostedsadresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(1),
                            gyldigTilOgMed = null,
                            vegadresse = null,
                            matrikkeladresse = lagMatrikkeladresse(1234L),
                            ukjentBosted = null,
                        ),
                    ),
                deltBosted = emptyList(),
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
}
