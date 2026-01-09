package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.FødselshendelseData
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class AutovedtakFødselshendelseServiceIntegrationTest(
    @Autowired private val autovedtakFødselshendelseService: AutovedtakFødselshendelseService,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class KjørBehandling {
        @Test
        fun `skal kunne behandle en fødselshendelse hvor mor har en adresse hvor gyldigFraOgMed er satt men ikke angittFlyttedato`() {
            // Arrange
            val mor = lagPerson(fødselsdato = LocalDate.of(2004, 5, 2))
            val fnrMor = mor.aktør.aktivFødselsnummer()

            val barn = lagPerson(fødselsdato = LocalDate.of(2025, 12, 30))
            val fnrBarn = barn.aktør.aktivFødselsnummer()

            FakePersonopplysningerService.leggTilPersonInfo(
                fødselsdato = mor.fødselsdato,
                egendefinertMock =
                    PersonInfo(
                        fødselsdato = mor.fødselsdato,
                        kjønn = Kjønn.KVINNE,
                        bostedsadresser =
                            listOf(
                                lagBostedsadresse(
                                    gyldigFraOgMed = LocalDate.of(2026, 1, 5),
                                    gyldigTilOgMed = null,
                                    angittFlyttedato = LocalDate.of(2026, 1, 5),
                                    vegadresse =
                                        lagVegadresse(
                                            matrikkelId = 2L,
                                            husnummer = "2",
                                            bruksenhetsnummer = "2",
                                            adressenavn = "Adressenavn2",
                                            kommunenummer = "0002",
                                            postnummer = "0002",
                                        ),
                                ),
                                lagBostedsadresse(
                                    gyldigFraOgMed = LocalDate.of(2004, 5, 2),
                                    gyldigTilOgMed = null,
                                    angittFlyttedato = null,
                                    vegadresse =
                                        lagVegadresse(
                                            matrikkelId = 1L,
                                            husnummer = "1",
                                            bruksenhetsnummer = "1",
                                            adressenavn = "Adressenavn1",
                                            kommunenummer = "0001",
                                            postnummer = "0001",
                                        ),
                                ),
                            ),
                        forelderBarnRelasjon =
                            setOf(
                                ForelderBarnRelasjon(
                                    aktør = barn.aktør,
                                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                ),
                            ),
                        statsborgerskap =
                            listOf(
                                Statsborgerskap(
                                    gyldigFraOgMed = null,
                                    gyldigTilOgMed = null,
                                    land = "NOR",
                                    bekreftelsesdato = null,
                                ),
                            ),
                    ),
                personIdent = fnrMor,
            )

            FakePersonopplysningerService.leggTilPersonInfo(
                fødselsdato = barn.fødselsdato,
                egendefinertMock =
                    PersonInfo(
                        fødselsdato = barn.fødselsdato,
                        bostedsadresser =
                            listOf(
                                lagBostedsadresse(
                                    gyldigFraOgMed = LocalDate.of(2026, 1, 5),
                                    gyldigTilOgMed = null,
                                    angittFlyttedato = LocalDate.of(2026, 1, 5),
                                    vegadresse =
                                        lagVegadresse(
                                            matrikkelId = 1L,
                                            husnummer = "1",
                                            bruksenhetsnummer = "1",
                                            adressenavn = "Adressenavn1",
                                            kommunenummer = "0001",
                                            postnummer = "0001",
                                        ),
                                    matrikkeladresse = null,
                                    ukjentBosted = null,
                                    folkeregistermetadata = null,
                                ),
                                lagBostedsadresse(
                                    gyldigFraOgMed = LocalDate.of(2025, 12, 30),
                                    gyldigTilOgMed = null,
                                    angittFlyttedato = LocalDate.of(2025, 12, 30),
                                    vegadresse =
                                        lagVegadresse(
                                            matrikkelId = 2L,
                                            husnummer = "2",
                                            bruksenhetsnummer = "2",
                                            adressenavn = "Adressenavn2",
                                            kommunenummer = "0002",
                                            postnummer = "0002",
                                        ),
                                    matrikkeladresse = null,
                                    ukjentBosted = null,
                                    folkeregistermetadata = null,
                                ),
                            ),
                        forelderBarnRelasjon =
                            setOf(
                                ForelderBarnRelasjon(
                                    aktør = mor.aktør,
                                    relasjonsrolle = FORELDERBARNRELASJONROLLE.MOR,
                                ),
                            ),
                        statsborgerskap =
                            listOf(
                                Statsborgerskap(
                                    land = "NOR",
                                    gyldigFraOgMed = LocalDate.of(2025, 12, 30),
                                    gyldigTilOgMed = null,
                                    bekreftelsesdato = null,
                                    folkeregistermetadata = null,
                                ),
                            ),
                    ),
                personIdent = fnrBarn,
            )

            val fødselshendelseData =
                FødselshendelseData(
                    NyBehandlingHendelse(
                        fnrMor,
                        listOf(fnrBarn),
                    ),
                )

            // Act
            val resultat = autovedtakFødselshendelseService.kjørBehandling(fødselshendelseData)

            // Assert
            assertThat(resultat).isEqualTo("Behandling ferdig")
        }
    }
}
