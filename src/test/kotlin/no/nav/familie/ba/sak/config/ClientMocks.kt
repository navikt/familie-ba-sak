package no.nav.familie.ba.sak.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.guttenBarnesenFødselsdato
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilddMMyy
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggResponse
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.integrasjoner.lagTestJournalpost
import no.nav.familie.ba.sak.integrasjoner.lagTestOppgaveDTO
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.VergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjonMaskert
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.Personident
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.VergeData
import no.nav.familie.ba.sak.kjerne.fødselshendelse.mockBarnAutomatiskBehandling
import no.nav.familie.ba.sak.kjerne.fødselshendelse.mockBarnAutomatiskBehandling2
import no.nav.familie.ba.sak.kjerne.fødselshendelse.mockBarnAutomatiskBehandling2Fnr
import no.nav.familie.ba.sak.kjerne.fødselshendelse.mockBarnAutomatiskBehandlingFnr
import no.nav.familie.ba.sak.kjerne.fødselshendelse.mockBarnAutomatiskBehandlingSkalFeile
import no.nav.familie.ba.sak.kjerne.fødselshendelse.mockBarnAutomatiskBehandlingSkalFeileFnr
import no.nav.familie.ba.sak.kjerne.fødselshendelse.mockSøkerAutomatiskBehandling
import no.nav.familie.ba.sak.kjerne.fødselshendelse.mockSøkerAutomatiskBehandlingFnr
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate
import java.time.Month
import java.util.UUID

@TestConfiguration
class ClientMocks {

    @Bean
    @Profile("mock-pdl")
    @Primary
    fun mockPersonopplysningerService(): PersonopplysningerService {
        val mockPersonopplysningerService = mockk<PersonopplysningerService>(relaxed = false)

        clearPdlMocks(mockPersonopplysningerService)

        return mockPersonopplysningerService
    }

    @Bean
    @Primary
    fun mockIntegrasjonClient(): IntegrasjonClient {
        val mockIntegrasjonClient = mockk<IntegrasjonClient>(relaxed = false)

        clearIntegrasjonMocks(mockIntegrasjonClient)

        return mockIntegrasjonClient
    }

    @Bean
    @Primary
    @Profile("mock-pdl-test-søk")
    fun mockPDL(): PersonopplysningerService {
        val mockPersonopplysningerService = mockk<PersonopplysningerService>()

        val farId = "12345678910"
        val morId = "21345678910"
        val barnId = "31245678910"

        every {
            mockPersonopplysningerService.hentPersoninfoEnkel(any())
        } returns personInfo.getValue(INTEGRASJONER_FNR)

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(farId)
        } returns PersonInfo(fødselsdato = LocalDate.of(1969, 5, 1), kjønn = Kjønn.MANN, navn = "Far Mocksen")

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(morId)
        } returns PersonInfo(fødselsdato = LocalDate.of(1979, 5, 1), kjønn = Kjønn.KVINNE, navn = "Mor Mocksen")

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barnId)
        } returns PersonInfo(
            fødselsdato = LocalDate.of(2009, 5, 1), kjønn = Kjønn.MANN, navn = "Barn Mocksen",
            forelderBarnRelasjon = setOf(
                ForelderBarnRelasjon(
                    Personident(farId),
                    FORELDERBARNRELASJONROLLE.FAR,
                    "Far Mocksen",
                    LocalDate.of(1969, 5, 1)
                ),
                ForelderBarnRelasjon(
                    Personident(morId),
                    FORELDERBARNRELASJONROLLE.MOR,
                    "Mor Mocksen",
                    LocalDate.of(1979, 5, 1)
                )
            )
        )

        every {
            mockPersonopplysningerService.hentIdenter(any())
        } answers {
            listOf(IdentInformasjon("123", false, "FOLKEREGISTERIDENT"))
        }

        every {
            mockPersonopplysningerService.hentAktivAktørId(any())
        } answers {
            randomAktørId()
        }

        every {
            mockPersonopplysningerService.hentGjeldendeStatsborgerskap(any())
        } answers {
            Statsborgerskap(
                "NOR",
                LocalDate.of(1990, 1, 25),
                LocalDate.of(1990, 1, 25),
                null
            )
        }

        every {
            mockPersonopplysningerService.hentGjeldendeOpphold(any())
        } answers {
            Opphold(
                type = OPPHOLDSTILLATELSE.PERMANENT,
                oppholdFra = LocalDate.of(1990, 1, 25),
                oppholdTil = LocalDate.of(2499, 1, 1)
            )
        }

        every {
            mockPersonopplysningerService.hentLandkodeUtenlandskBostedsadresse(any())
        } returns "NO"

        val ukjentId = "43125678910"
        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(ukjentId)
        } throws HttpClientErrorException(HttpStatus.NOT_FOUND, "ikke funnet")

        val feilId = "41235678910"
        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(feilId)
        } throws IntegrasjonException("feil id")

        return mockPersonopplysningerService
    }

    @Bean
    @Primary
    fun mockFeatureToggleService(): FeatureToggleService {
        val mockFeatureToggleService = mockk<FeatureToggleService>(relaxed = true)

        val featureSlot = slot<String>()
        every {
            mockFeatureToggleService.isEnabled(capture(featureSlot))
        } answers {
            when (featureSlot.captured) {
                FeatureToggleConfig.KAN_BEHANDLE_UTVIDET -> true
                else -> true
            }
        }

        return mockFeatureToggleService
    }

    @Bean
    @Primary
    fun mockEnvService(): EnvService {
        val mockEnvService = mockk<EnvService>(relaxed = true)

        every {
            mockEnvService.erProd()
        } answers {
            true
        }

        every {
            mockEnvService.erPreprod()
        } answers {
            true
        }

        every {
            mockEnvService.erDev()
        } answers {
            true
        }

        every {
            mockEnvService.skalIverksetteBehandling()
        } answers {
            false
        }

        return mockEnvService
    }

    companion object {

        fun clearPdlMocks(mockPersonopplysningerService: PersonopplysningerService) {
            clearMocks(mockPersonopplysningerService)

            every {
                mockPersonopplysningerService.hentAktivAktørId(any())
            } answers {
                randomAktørId()
            }

            every {
                mockPersonopplysningerService.hentAktivPersonIdent(any())
            } answers {
                PersonIdent(randomFnr())
            }

            every {
                mockPersonopplysningerService.hentGjeldendeStatsborgerskap(any())
            } answers {
                Statsborgerskap(
                    "NOR",
                    LocalDate.of(1990, 1, 25),
                    LocalDate.of(1990, 1, 25),
                    null
                )
            }

            every {
                mockPersonopplysningerService.hentGjeldendeOpphold(any())
            } answers {
                Opphold(
                    type = OPPHOLDSTILLATELSE.PERMANENT,
                    oppholdFra = LocalDate.of(1990, 1, 25),
                    oppholdTil = LocalDate.of(2499, 1, 1)
                )
            }

            val identSlot = slot<Ident>()
            every {
                mockPersonopplysningerService.hentIdenter(capture(identSlot))
            } answers {
                listOf(
                    IdentInformasjon(identSlot.captured.ident, false, "FOLKEREGISTERIDENT"),
                    IdentInformasjon(randomFnr(), true, "FOLKEREGISTERIDENT")
                )
            }

            every {
                mockPersonopplysningerService.hentDødsfall(any())
            } returns DødsfallData(false, null)

            every {
                mockPersonopplysningerService.hentVergeData(any())
            } returns VergeData(false)

            every {
                mockPersonopplysningerService.harVerge(any())
            } returns VergeResponse(false)

            every {
                mockPersonopplysningerService.hentLandkodeUtenlandskBostedsadresse(any())
            } returns "NO"

            val idSlotForHentPersoninfo = slot<String>()
            every {
                mockPersonopplysningerService.hentPersoninfoEnkel(capture(idSlotForHentPersoninfo))
            } answers {
                when (val id = idSlotForHentPersoninfo.captured) {
                    barnFnr[0], barnFnr[1] -> personInfo.getValue(id)
                    søkerFnr[0], søkerFnr[1] -> personInfo.getValue(id)
                    else -> personInfo.getValue(INTEGRASJONER_FNR)
                }
            }

            val idSlot = slot<String>()
            every {
                mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(capture(idSlot))
            } answers {
                when (val id = idSlot.captured) {
                    "00000000000" -> throw HttpClientErrorException(
                        HttpStatus.NOT_FOUND,
                        "Fant ikke forespurte data på person."
                    )
                    barnFnr[0], barnFnr[1] -> personInfo.getValue(id)

                    søkerFnr[0] -> personInfo.getValue(id).copy(
                        forelderBarnRelasjon = setOf(
                            ForelderBarnRelasjon(
                                personIdent = Personident(id = barnFnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[0]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[0]).fødselsdato
                            ),
                            ForelderBarnRelasjon(
                                personIdent = Personident(id = barnFnr[1]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[1]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[1]).fødselsdato
                            ),
                            ForelderBarnRelasjon(
                                personIdent = Personident(id = søkerFnr[1]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.MEDMOR
                            )
                        )
                    )

                    søkerFnr[1] -> personInfo.getValue(id).copy(
                        forelderBarnRelasjon = setOf(
                            ForelderBarnRelasjon(
                                personIdent = Personident(id = barnFnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[0]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[0]).fødselsdato
                            ),
                            ForelderBarnRelasjon(
                                personIdent = Personident(id = barnFnr[1]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[1]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[1]).fødselsdato
                            ),
                            ForelderBarnRelasjon(
                                personIdent = Personident(id = søkerFnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.FAR
                            )
                        )
                    )

                    søkerFnr[2] -> personInfo.getValue(id).copy(
                        forelderBarnRelasjon = setOf(
                            ForelderBarnRelasjon(
                                personIdent = Personident(id = barnFnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[0]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[0]).fødselsdato
                            ),
                            ForelderBarnRelasjon(
                                personIdent = Personident(id = barnFnr[1]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[1]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[1]).fødselsdato,
                                adressebeskyttelseGradering = personInfo.getValue(barnFnr[1]).adressebeskyttelseGradering
                            ),
                            ForelderBarnRelasjon(
                                personIdent = Personident(id = søkerFnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.FAR
                            )
                        ),
                        forelderBarnRelasjonMaskert = setOf(
                            ForelderBarnRelasjonMaskert(
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                adressebeskyttelseGradering = personInfo.getValue(
                                    BARN_DET_IKKE_GIS_TILGANG_TIL_FNR
                                ).adressebeskyttelseGradering!!
                            )
                        )
                    )

                    INTEGRASJONER_FNR -> personInfo.getValue(id).copy(
                        forelderBarnRelasjon = setOf(
                            ForelderBarnRelasjon(
                                personIdent = Personident(id = barnFnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[0]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[0]).fødselsdato
                            ),
                            ForelderBarnRelasjon(
                                personIdent = Personident(id = barnFnr[1]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[1]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[1]).fødselsdato
                            ),
                            ForelderBarnRelasjon(
                                personIdent = Personident(id = søkerFnr[1]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.MEDMOR
                            )
                        )
                    )
                    mockBarnAutomatiskBehandlingFnr -> personInfo.getValue(id)
                    mockBarnAutomatiskBehandling2Fnr -> personInfo.getValue(id)
                    mockSøkerAutomatiskBehandlingFnr -> personInfo.getValue(id)
                    mockBarnAutomatiskBehandlingSkalFeileFnr -> personInfo.getValue(id)
                    else -> personInfo.getValue(INTEGRASJONER_FNR)
                }
            }

            every {
                mockPersonopplysningerService.hentAdressebeskyttelseSomSystembruker(capture(idSlot))
            } answers {
                if (BARN_DET_IKKE_GIS_TILGANG_TIL_FNR == idSlot.captured)
                    ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
                else
                    ADRESSEBESKYTTELSEGRADERING.UGRADERT
            }

            every { mockPersonopplysningerService.harVerge(mockSøkerAutomatiskBehandlingFnr) } returns VergeResponse(
                harVerge = false
            )
        }

        fun clearIntegrasjonMocks(mockIntegrasjonClient: IntegrasjonClient) {
            every {
                mockIntegrasjonClient.hentMaskertPersonInfoVedManglendeTilgang(any())
            } returns null

            every { mockIntegrasjonClient.hentJournalpost(any()) } answers {
                success(
                    lagTestJournalpost(
                        søkerFnr[0],
                        UUID.randomUUID().toString()
                    )
                )
            }

            every { mockIntegrasjonClient.hentJournalpost(any()) } answers {
                success(
                    lagTestJournalpost(
                        søkerFnr[0],
                        UUID.randomUUID().toString()
                    )
                )
            }

            every { mockIntegrasjonClient.hentJournalposterForBruker(any()) } answers {
                success(
                    listOf(
                        lagTestJournalpost(
                            søkerFnr[0],
                            UUID.randomUUID().toString()
                        ),
                        lagTestJournalpost(
                            søkerFnr[0],
                            UUID.randomUUID().toString()
                        )
                    )
                )
            }

            every { mockIntegrasjonClient.finnOppgaveMedId(any()) } returns
                lagTestOppgaveDTO(1L)

            every { mockIntegrasjonClient.hentOppgaver(any()) } returns
                FinnOppgaveResponseDto(
                    2,
                    listOf(lagTestOppgaveDTO(1L), lagTestOppgaveDTO(2L, Oppgavetype.BehandleSak, "Z999999"))
                )

            every { mockIntegrasjonClient.opprettOppgave(any()) } returns
                "12345678"

            every { mockIntegrasjonClient.patchOppgave(any()) } returns
                OppgaveResponse(12345678L)

            every { mockIntegrasjonClient.fordelOppgave(any(), any()) } returns
                "12345678"

            every { mockIntegrasjonClient.oppdaterJournalpost(any(), any()) } returns
                OppdaterJournalpostResponse("1234567")

            every { mockIntegrasjonClient.journalførVedtaksbrev(any(), any(), any(), any()) } returns "journalpostId"

            every {
                mockIntegrasjonClient.journalførManueltBrev(any(), any(), any(), any(), any(), any())
            } returns "journalpostId"

            every {
                mockIntegrasjonClient.leggTilLogiskVedlegg(any(), any())
            } returns LogiskVedleggResponse(12345678)

            every { mockIntegrasjonClient.distribuerBrev(any()) } returns success("bestillingsId")

            every { mockIntegrasjonClient.ferdigstillJournalpost(any(), any()) } just runs

            every { mockIntegrasjonClient.ferdigstillOppgave(any()) } just runs

            every { mockIntegrasjonClient.hentBehandlendeEnhet(any()) } returns
                listOf(Arbeidsfordelingsenhet("4833", "NAV Familie- og pensjonsytelser Oslo 1"))

            every { mockIntegrasjonClient.hentDokument(any(), any()) } returns
                success("mock data".toByteArray())

            val idSlot = slot<List<String>>()
            every {
                mockIntegrasjonClient.sjekkTilgangTilPersoner(capture(idSlot))
            } answers {
                if (idSlot.captured.isNotEmpty() && idSlot.captured.contains(BARN_DET_IKKE_GIS_TILGANG_TIL_FNR))
                    listOf(Tilgang(false, null))
                else
                    listOf(Tilgang(true, null))
            }

            every { mockIntegrasjonClient.hentPersonIdent(any()) } returns PersonIdent(søkerFnr[0])

            every { mockIntegrasjonClient.hentArbeidsforhold(any(), any()) } returns emptyList()

            every { mockIntegrasjonClient.hentBehandlendeEnhet(any()) } returns listOf(
                Arbeidsfordelingsenhet(
                    "100",
                    "NAV Familie- og pensjonsytelser Oslo 1"
                )
            )

            every { mockIntegrasjonClient.opprettSkyggesak(any(), any()) } returns Unit

            every { mockIntegrasjonClient.hentLand(any()) } returns "Testland"

            initEuKodeverk(mockIntegrasjonClient)
        }

        val FOM_1900 = LocalDate.of(1900, Month.JANUARY, 1)
        val FOM_1990 = LocalDate.of(1990, Month.JANUARY, 1)
        val FOM_2004 = LocalDate.of(2004, Month.JANUARY, 1)
        val FOM_2008 = LocalDate.of(2008, Month.JANUARY, 1)
        val TOM_2010 = LocalDate.of(2009, Month.DECEMBER, 31)
        val TOM_9999 = LocalDate.of(9999, Month.DECEMBER, 31)

        fun initEuKodeverk(integrasjonClient: IntegrasjonClient) {
            val beskrivelsePolen = BeskrivelseDto("POL", "")
            val betydningPolen = BetydningDto(FOM_2004, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelsePolen))
            val beskrivelseTyskland = BeskrivelseDto("DEU", "")
            val betydningTyskland =
                BetydningDto(FOM_1900, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseTyskland))
            val beskrivelseDanmark = BeskrivelseDto("DEN", "")
            val betydningDanmark =
                BetydningDto(FOM_1990, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseDanmark))
            val beskrivelseUK = BeskrivelseDto("GBR", "")
            val betydningUK = BetydningDto(FOM_1900, TOM_2010, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseUK))

            val kodeverkLand = KodeverkDto(
                mapOf(
                    "POL" to listOf(betydningPolen),
                    "DEU" to listOf(betydningTyskland),
                    "DEN" to listOf(betydningDanmark),
                    "GBR" to listOf(betydningUK)
                )
            )

            every { integrasjonClient.hentAlleEØSLand() }
                .returns(kodeverkLand)
        }

        val søkerFnr = arrayOf("12345678910", "11223344556", "12345678911")
        private val barnFødselsdatoer = arrayOf(
            guttenBarnesenFødselsdato,
            LocalDate.now().withDayOfMonth(18).minusYears(1)
        )
        val barnFnr = arrayOf(barnFødselsdatoer[0].tilddMMyy() + "50033", barnFødselsdatoer[1].tilddMMyy() + "50033")
        private const val BARN_DET_IKKE_GIS_TILGANG_TIL_FNR = "12345678912"
        const val INTEGRASJONER_FNR = "10000111111"
        val bostedsadresse = Bostedsadresse(
            matrikkeladresse = Matrikkeladresse(
                matrikkelId = 123L, bruksenhetsnummer = "H301", tilleggsnavn = "navn",
                postnummer = "0202", kommunenummer = "2231"
            )
        )
        private val bostedsadresseHistorikk = mutableListOf(
            Bostedsadresse(
                angittFlyttedato = LocalDate.now().minusDays(15),
                gyldigTilOgMed = null,
                matrikkeladresse = Matrikkeladresse(
                    matrikkelId = 123L,
                    bruksenhetsnummer = "H301",
                    tilleggsnavn = "navn",
                    postnummer = "0202",
                    kommunenummer = "2231"
                )
            ),
            Bostedsadresse(
                angittFlyttedato = LocalDate.now().minusYears(1),
                gyldigTilOgMed = LocalDate.now().minusDays(16),
                matrikkeladresse = Matrikkeladresse(
                    matrikkelId = 123L,
                    bruksenhetsnummer = "H301",
                    tilleggsnavn = "navn",
                    postnummer = "0202",
                    kommunenummer = "2231"
                )
            )
        )

        private val sivilstandHistorisk = listOf(
            Sivilstand(type = SIVILSTAND.GIFT, gyldigFraOgMed = LocalDate.now().minusMonths(8)),
            Sivilstand(type = SIVILSTAND.SKILT, gyldigFraOgMed = LocalDate.now().minusMonths(4)),
        )

        val personInfo = mapOf(
            søkerFnr[0] to PersonInfo(
                fødselsdato = LocalDate.of(1990, 2, 19),
                kjønn = Kjønn.KVINNE,
                navn = "Mor Moresen",
                bostedsadresser = bostedsadresseHistorikk,
                sivilstander = sivilstandHistorisk,
                statsborgerskap = listOf(
                    Statsborgerskap(
                        land = "DEN",
                        bekreftelsesdato = null,
                        gyldigFraOgMed = null,
                        gyldigTilOgMed = null
                    )
                )
            ),
            søkerFnr[1] to PersonInfo(
                fødselsdato = LocalDate.of(1995, 2, 19),
                bostedsadresser = mutableListOf(),
                sivilstander = listOf(
                    Sivilstand(
                        type = SIVILSTAND.GIFT,
                        gyldigFraOgMed = LocalDate.now().minusMonths(8)
                    )
                ),
                kjønn = Kjønn.MANN,
                navn = "Far Faresen"
            ),
            søkerFnr[2] to PersonInfo(
                fødselsdato = LocalDate.of(1985, 7, 10),
                bostedsadresser = mutableListOf(),
                sivilstander = listOf(
                    Sivilstand(
                        type = SIVILSTAND.GIFT,
                        gyldigFraOgMed = LocalDate.now().minusMonths(8)
                    )
                ),
                kjønn = Kjønn.KVINNE,
                navn = "Moder Jord",
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT
            ),
            barnFnr[0] to PersonInfo(
                fødselsdato = barnFødselsdatoer[0],
                bostedsadresser = mutableListOf(bostedsadresse),
                sivilstander = listOf(
                    Sivilstand(
                        type = SIVILSTAND.UOPPGITT,
                        gyldigFraOgMed = LocalDate.now().minusMonths(8)
                    )
                ),
                kjønn = Kjønn.MANN,
                navn = "Gutten Barnesen"
            ),
            barnFnr[1] to PersonInfo(
                fødselsdato = barnFødselsdatoer[1],
                bostedsadresser = mutableListOf(bostedsadresse),
                sivilstander = listOf(
                    Sivilstand(
                        type = SIVILSTAND.GIFT,
                        gyldigFraOgMed = LocalDate.now().minusMonths(8)
                    )
                ),
                kjønn = Kjønn.KVINNE,
                navn = "Jenta Barnesen",
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.FORTROLIG
            ),
            mockBarnAutomatiskBehandlingFnr to mockBarnAutomatiskBehandling,
            mockBarnAutomatiskBehandling2Fnr to mockBarnAutomatiskBehandling2,
            mockSøkerAutomatiskBehandlingFnr to mockSøkerAutomatiskBehandling,
            mockBarnAutomatiskBehandlingSkalFeileFnr to mockBarnAutomatiskBehandlingSkalFeile,
            INTEGRASJONER_FNR to PersonInfo(
                fødselsdato = LocalDate.of(1965, 2, 19),
                bostedsadresser = mutableListOf(bostedsadresse),
                kjønn = Kjønn.KVINNE,
                navn = "Mor Integrasjon person",
                sivilstander = sivilstandHistorisk,
            ),
            BARN_DET_IKKE_GIS_TILGANG_TIL_FNR to PersonInfo(
                fødselsdato = LocalDate.of(2019, 6, 22),
                bostedsadresser = mutableListOf(bostedsadresse),
                sivilstander = listOf(
                    Sivilstand(
                        type = SIVILSTAND.UGIFT,
                        gyldigFraOgMed = LocalDate.now().minusMonths(8)
                    )
                ),
                kjønn = Kjønn.KVINNE,
                navn = "Maskert Banditt",
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
            )
        )
    }
}

fun mockHentPersoninfoForMedIdenter(
    mockPersonopplysningerService: PersonopplysningerService,
    søkerFnr: String,
    barnFnr: String
) {
    every {
        mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(eq(barnFnr))
    } returns PersonInfo(
        fødselsdato = LocalDate.of(2018, 5, 1),
        kjønn = Kjønn.KVINNE,
        navn = "Barn Barnesen",
        sivilstander = listOf(Sivilstand(type = SIVILSTAND.GIFT, gyldigFraOgMed = LocalDate.now().minusMonths(8)))
    )

    every {
        mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(eq(søkerFnr))
    } returns PersonInfo(fødselsdato = LocalDate.of(1990, 2, 19), kjønn = Kjønn.KVINNE, navn = "Mor Moresen")

    every {
        mockPersonopplysningerService.hentAktivAktørId(any())
    } returns AktørId("1")
}

val TEST_PDF = ClientMocks::class.java.getResource("/dokument/mockvedtak.pdf").readBytes()
