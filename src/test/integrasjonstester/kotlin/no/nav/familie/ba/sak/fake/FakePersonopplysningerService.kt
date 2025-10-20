package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjonMaskert
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockBarnAutomatiskBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockBarnAutomatiskBehandling2
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockBarnAutomatiskBehandling2Fnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockBarnAutomatiskBehandlingFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockBarnAutomatiskBehandlingSkalFeile
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockBarnAutomatiskBehandlingSkalFeileFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockSøkerAutomatiskBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockSøkerAutomatiskBehandlingFnr
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE.BARN
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE.FAR
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE.MEDMOR
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate

class FakePersonopplysningerService(
    pdlRestKlient: PdlRestKlient,
    systemOnlyPdlRestKlient: SystemOnlyPdlRestKlient,
    familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
    integrasjonKlient: IntegrasjonKlient,
) : PersonopplysningerService(
        pdlRestKlient,
        systemOnlyPdlRestKlient,
        familieIntegrasjonerTilgangskontrollService,
        integrasjonKlient,
    ) {
    init {
        settPersoninfoMedRelasjonerForPredefinerteTestpersoner()
    }

    override fun hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør: Aktør): PersonInfo {
        validerFødselsnummer(aktør.aktivFødselsnummer())
        sjekkPersonIkkeFunnet(aktør.aktivFødselsnummer())

        return personInfo[aktør.aktivFødselsnummer()] ?: personInfo.getValue(INTEGRASJONER_FNR)
    }

    override fun hentPersoninfoEnkel(aktør: Aktør): PersonInfo =
        personInfo[aktør.aktivFødselsnummer()]
            ?: personInfo.getValue(INTEGRASJONER_FNR)

    override fun hentPersoninfoNavnOgAdresse(aktør: Aktør): PersonInfo = hentPersoninfoEnkel(aktør)

    override fun hentAdressebeskyttelseSomSystembruker(aktør: Aktør): ADRESSEBESKYTTELSEGRADERING =
        if (aktør.aktivFødselsnummer() == BARN_DET_IKKE_GIS_TILGANG_TIL_FNR) {
            ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
        } else {
            personInfo[aktør.aktivFødselsnummer()]?.adressebeskyttelseGradering ?: ADRESSEBESKYTTELSEGRADERING.UGRADERT
        }

    override fun hentGjeldendeStatsborgerskap(aktør: Aktør): Statsborgerskap =
        personInfo[aktør.aktivFødselsnummer()]?.statsborgerskap?.firstOrNull()
            ?: Statsborgerskap(
                "NOR",
                LocalDate.of(1990, 1, 25),
                LocalDate.of(1990, 1, 25),
                null,
            )

    override fun hentLandkodeAlpha2UtenlandskBostedsadresse(aktør: Aktør): String = personerMedLandkode[aktør.aktivFødselsnummer()] ?: "NO"

    companion object {
        val personInfo: MutableMap<String, PersonInfo> =
            mutableMapOf(
                mockBarnAutomatiskBehandlingFnr to mockBarnAutomatiskBehandling,
                mockBarnAutomatiskBehandling2Fnr to mockBarnAutomatiskBehandling2,
                mockSøkerAutomatiskBehandlingFnr to mockSøkerAutomatiskBehandling,
                mockBarnAutomatiskBehandlingSkalFeileFnr to mockBarnAutomatiskBehandlingSkalFeile,
            )

        val personInfoIkkeFunnet: MutableSet<String> = mutableSetOf()

        val personerMedLandkode: MutableMap<String, String> = mutableMapOf()

        fun leggTilLandkodeForPerson(
            personIdent: String,
            Landkode: String,
        ) {
            personerMedLandkode[personIdent] = Landkode
        }

        fun leggTilPersonIkkeFunnet(
            personIdent: String,
        ) {
            personInfoIkkeFunnet.add(personIdent)
        }

        fun sjekkPersonIkkeFunnet(personIdent: String) {
            if (personInfoIkkeFunnet.contains(personIdent)) {
                throw notFoundException()
            }
        }

        fun validerFødselsnummer(fødselsnummer: String) {
            try {
                Fødselsnummer(fødselsnummer)
            } catch (e: IllegalStateException) {
                throw notFoundException()
            }
        }

        private fun notFoundException() =
            HttpClientErrorException(
                HttpStatus.NOT_FOUND,
                "Fant ikke forespurte data på person.",
            )

        fun leggTilPersonInfo(
            fødselsdato: LocalDate,
            egendefinertMock: PersonInfo? = null,
            personIdent: String? = null,
        ): String {
            val personIdent = personIdent ?: randomFnr(fødselsdato)
            personInfo[personIdent] = egendefinertMock ?: PersonInfo(
                fødselsdato = fødselsdato,
                bostedsadresser = mutableListOf(bostedsadresse),
                kjønn = Kjønn.entries.random(),
                navn = "$personIdent sitt navn",
                statsborgerskap =
                    listOf(
                        Statsborgerskap(
                            land = "NOR",
                            bekreftelsesdato = fødselsdato,
                            gyldigFraOgMed = fødselsdato,
                            gyldigTilOgMed = null,
                        ),
                    ),
            )
            return personIdent
        }

        fun leggTilRelasjonIPersonInfo(
            personIdent: String,
            relatertPersonsIdent: String,
            relatertPersonsRelasjonsrolle: FORELDERBARNRELASJONROLLE,
        ) {
            personInfo[personIdent] =
                personInfo[personIdent]!!.copy(
                    forelderBarnRelasjon =
                        personInfo[personIdent]!!.forelderBarnRelasjon +
                            ForelderBarnRelasjon(
                                aktør = lagAktør(relatertPersonsIdent),
                                relasjonsrolle = relatertPersonsRelasjonsrolle,
                                navn = personInfo.getValue(relatertPersonsIdent).navn,
                                fødselsdato = personInfo.getValue(relatertPersonsIdent).fødselsdato,
                                adressebeskyttelseGradering = personInfo.getValue(relatertPersonsIdent).adressebeskyttelseGradering,
                            ),
                )
        }

        fun leggTilRelasjonIPersonInfoMaskert(
            personIdent: String,
            maskertPersonsRelasjonsrolle: FORELDERBARNRELASJONROLLE,
        ) {
            personInfo[personIdent] =
                personInfo[personIdent]!!.copy(
                    forelderBarnRelasjonMaskert =
                        personInfo[personIdent]!!.forelderBarnRelasjonMaskert +
                            ForelderBarnRelasjonMaskert(
                                relasjonsrolle = maskertPersonsRelasjonsrolle,
                                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
                            ),
                )
        }

        fun leggTilBostedsadresserIPersonInfo(
            personIdenter: List<String>,
            bostedsadresser: List<Bostedsadresse>,
        ) {
            personIdenter.forEach {
                personInfo[it] = personInfo[it]!!.copy(bostedsadresser = bostedsadresser)
            }
        }

        fun settPersonInfoStatsborgerskap(
            personIdent: String,
            statsborgerskap: Statsborgerskap,
        ) {
            personInfo[personIdent] =
                personInfo[personIdent]!!.copy(
                    statsborgerskap = listOf(statsborgerskap),
                )
        }

        fun settPersoninfoMedRelasjonerForPredefinerteTestpersoner() {
            val (søker1, søker2, søker3) = søkerFnr
            val (barn1, barn2) = barnFnr

            personInfo[søker1] = personInfoSøker1
            personInfo[søker2] = personInfoSøker2
            personInfo[søker3] = personInfoSøker3
            personInfo[barn1] = personInfoBarn1
            personInfo[barn2] = personInfoBarn2
            personInfo[INTEGRASJONER_FNR] = personInfoIntegrasjonerFnr

            leggTilRelasjonIPersonInfo(søker1, barn1, BARN)
            leggTilRelasjonIPersonInfo(søker1, barn2, BARN)
            leggTilRelasjonIPersonInfo(søker1, søker2, MEDMOR)

            leggTilRelasjonIPersonInfo(søker2, barn1, BARN)
            leggTilRelasjonIPersonInfo(søker2, barn2, BARN)
            leggTilRelasjonIPersonInfo(søker2, søker1, FAR)

            leggTilRelasjonIPersonInfo(søker3, barn1, BARN)
            leggTilRelasjonIPersonInfo(søker3, barn2, BARN)
            leggTilRelasjonIPersonInfo(søker3, søker1, FAR)
            leggTilRelasjonIPersonInfoMaskert(søker3, BARN)

            leggTilRelasjonIPersonInfo(INTEGRASJONER_FNR, barn1, BARN)
            leggTilRelasjonIPersonInfo(INTEGRASJONER_FNR, barn2, BARN)
            leggTilRelasjonIPersonInfo(INTEGRASJONER_FNR, søker2, MEDMOR)
        }
    }
}

const val BARN_DET_IKKE_GIS_TILGANG_TIL_FNR = "12345678912"
const val INTEGRASJONER_FNR = "10000111111"

private val søkerFnr = arrayOf("12345678910", "11223344556", "12345678911")
private val barnFnr = arrayOf(randomFnr(), randomFnr())

private val bostedsadresse =
    Bostedsadresse(
        matrikkeladresse =
            Matrikkeladresse(
                matrikkelId = 123L,
                bruksenhetsnummer = "H301",
                tilleggsnavn = "navn",
                postnummer = "0202",
                kommunenummer = "2231",
            ),
    )

private val bostedsadresseHistorikk =
    mutableListOf(
        Bostedsadresse(
            angittFlyttedato = LocalDate.now().minusDays(15),
            gyldigTilOgMed = null,
            matrikkeladresse =
                Matrikkeladresse(
                    matrikkelId = 123L,
                    bruksenhetsnummer = "H301",
                    tilleggsnavn = "navn",
                    postnummer = "0202",
                    kommunenummer = "2231",
                ),
        ),
        Bostedsadresse(
            angittFlyttedato = LocalDate.now().minusYears(1),
            gyldigTilOgMed = LocalDate.now().minusDays(16),
            matrikkeladresse =
                Matrikkeladresse(
                    matrikkelId = 123L,
                    bruksenhetsnummer = "H301",
                    tilleggsnavn = "navn",
                    postnummer = "0202",
                    kommunenummer = "2231",
                ),
        ),
    )

private val sivilstandHistorisk =
    listOf(
        Sivilstand(type = SIVILSTANDTYPE.GIFT, gyldigFraOgMed = LocalDate.now().minusMonths(8)),
        Sivilstand(type = SIVILSTANDTYPE.SKILT, gyldigFraOgMed = LocalDate.now().minusMonths(4)),
    )

private val personInfoSøker1 =
    PersonInfo(
        fødselsdato = LocalDate.of(1990, 2, 19),
        kjønn = Kjønn.KVINNE,
        navn = "Mor Moresen",
        bostedsadresser = bostedsadresseHistorikk,
        sivilstander = sivilstandHistorisk,
        statsborgerskap =
            listOf(
                Statsborgerskap(
                    land = "DNK",
                    bekreftelsesdato = LocalDate.now().minusYears(1),
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = null,
                ),
            ),
    )

private val personInfoBarn1 =
    PersonInfo(
        fødselsdato = LocalDate.now().withDayOfMonth(10).minusYears(6),
        bostedsadresser = mutableListOf(bostedsadresse),
        sivilstander =
            listOf(
                Sivilstand(
                    type = SIVILSTANDTYPE.UOPPGITT,
                    gyldigFraOgMed = LocalDate.now().minusMonths(8),
                ),
            ),
        kjønn = Kjønn.MANN,
        navn = "Gutten Barnesen",
    )

private val personInfoBarn2 =
    PersonInfo(
        fødselsdato = LocalDate.now().withDayOfMonth(18).minusYears(2),
        bostedsadresser = mutableListOf(bostedsadresse),
        sivilstander =
            listOf(
                Sivilstand(
                    type = SIVILSTANDTYPE.GIFT,
                    gyldigFraOgMed = LocalDate.now().minusMonths(8),
                ),
            ),
        kjønn = Kjønn.KVINNE,
        navn = "Jenta Barnesen",
        adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.FORTROLIG,
    )

private val personInfoSøker2 =
    PersonInfo(
        fødselsdato = LocalDate.of(1995, 2, 19),
        bostedsadresser = mutableListOf(),
        sivilstander =
            listOf(
                Sivilstand(
                    type = SIVILSTANDTYPE.GIFT,
                    gyldigFraOgMed = LocalDate.now().minusMonths(8),
                ),
            ),
        kjønn = Kjønn.MANN,
        navn = "Far Faresen",
    )

private val personInfoSøker3 =
    PersonInfo(
        fødselsdato = LocalDate.of(1985, 7, 10),
        bostedsadresser = mutableListOf(),
        sivilstander =
            listOf(
                Sivilstand(
                    type = SIVILSTANDTYPE.GIFT,
                    gyldigFraOgMed = LocalDate.now().minusMonths(8),
                ),
            ),
        kjønn = Kjønn.KVINNE,
        navn = "Moder Jord",
        adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
    )

private val personInfoIntegrasjonerFnr =
    PersonInfo(
        fødselsdato = LocalDate.of(1965, 2, 19),
        bostedsadresser = mutableListOf(bostedsadresse),
        kjønn = Kjønn.KVINNE,
        navn = "Mor Integrasjon person",
        sivilstander = sivilstandHistorisk,
    )
