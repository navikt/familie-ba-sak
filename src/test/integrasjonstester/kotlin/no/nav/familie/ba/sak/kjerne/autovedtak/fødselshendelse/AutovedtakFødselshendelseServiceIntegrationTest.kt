package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService
import no.nav.familie.ba.sak.fake.FakeTaskRepositoryWrapper
import no.nav.familie.ba.sak.fake.tilPayload
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.FødselshendelseData
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.ba.sak.task.dto.OpprettOppgaveTaskDTO
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class AutovedtakFødselshendelseServiceIntegrationTest(
    @Autowired private val autovedtakFødselshendelseService: AutovedtakFødselshendelseService,
    @Autowired private val fakeTaskRepositoryWrapper: FakeTaskRepositoryWrapper,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
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

        @Test
        fun `skal henlegge behandling og opprette manuell vurder livshendelse oppgave med begrunnelse IKKE_BODD_I_RIKET_I_12_MND når krav om 12 mnd ikke er møtt`() {
            // Arrange
            val mor = lagPerson(fødselsdato = LocalDate.of(2004, 5, 2))
            val fnrMor = mor.aktør.aktivFødselsnummer()

            val barn = lagPerson(fødselsdato = LocalDate.now().minusMonths(4))
            val fnrBarn = barn.aktør.aktivFødselsnummer()

            val eksisterendeBarn = lagPerson(fødselsdato = LocalDate.now().minusYears(2))

            FakePersonopplysningerService.leggTilPersonInfo(
                fødselsdato = mor.fødselsdato,
                egendefinertMock =
                    PersonInfo(
                        fødselsdato = mor.fødselsdato,
                        kjønn = Kjønn.KVINNE,
                        bostedsadresser =
                            listOf(
                                lagBostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(2),
                                    gyldigTilOgMed = LocalDate.now().minusYears(1),
                                    vegadresse =
                                        lagVegadresse(
                                            matrikkelId = 2L,
                                            husnummer = "2",
                                            bruksenhetsnummer = "2",
                                            adressenavn = "Test",
                                            kommunenummer = "0002",
                                            postnummer = "0002",
                                        ),
                                ),
                                lagBostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusMonths(3),
                                    gyldigTilOgMed = null,
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
                            ),
                        opphold =
                            mutableListOf(
                                Opphold(
                                    type = OPPHOLDSTILLATELSE.MIDLERTIDIG,
                                    oppholdFra = LocalDate.now().minusMonths(3),
                                    oppholdTil = null,
                                ),
                            ),
                        forelderBarnRelasjon =
                            setOf(
                                ForelderBarnRelasjon(
                                    aktør = barn.aktør,
                                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                ),
                                ForelderBarnRelasjon(
                                    aktør = eksisterendeBarn.aktør,
                                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
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
                                    gyldigFraOgMed = LocalDate.now().minusMonths(4),
                                    gyldigTilOgMed = null,
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
                            ),
                        opphold =
                            mutableListOf(
                                Opphold(
                                    type = OPPHOLDSTILLATELSE.PERMANENT,
                                    oppholdFra = LocalDate.now().minusMonths(4),
                                    oppholdTil = null,
                                ),
                            ),
                        forelderBarnRelasjon =
                            setOf(
                                ForelderBarnRelasjon(
                                    aktør = mor.aktør,
                                    relasjonsrolle = FORELDERBARNRELASJONROLLE.MOR,
                                ),
                            ),
                    ),
                personIdent = fnrBarn,
            )
            FakePersonopplysningerService.leggTilPersonInfo(fødselsdato = eksisterendeBarn.fødselsdato)

            val fødselshendelseData =
                FødselshendelseData(
                    NyBehandlingHendelse(
                        fnrMor,
                        listOf(fnrBarn),
                    ),
                )

            // Act
            val resultat = autovedtakFødselshendelseService.kjørBehandling(fødselshendelseData)

            val fagsak = fagsakService.hentAlleFagsakerForAktør(mor.aktør).single()
            val behandling = behandlingHentOgPersisterService.hentBehandlinger(fagsak.id).single()

            val opprettOppgaveTask = fakeTaskRepositoryWrapper.hentLagredeTaskerAvType(OpprettOppgaveTask.TASK_STEP_TYPE).tilPayload<OpprettOppgaveTaskDTO>().single { it.behandlingId == behandling.id && it.oppgavetype == Oppgavetype.VurderLivshendelse }

            // Assert
            assertThat(resultat).contains("Henlegger behandling")
            assertThat(opprettOppgaveTask.beskrivelse).isEqualTo("Fødselshendelse: Mor bosatt i riket i mindre enn 12 måneder.")
        }
    }
}
