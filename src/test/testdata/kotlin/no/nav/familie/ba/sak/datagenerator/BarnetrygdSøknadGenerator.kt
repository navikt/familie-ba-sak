package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.kontrakter.ba.søknad.v1.SIVILSTANDTYPE
import no.nav.familie.kontrakter.ba.søknad.v1.SøknadAdresse
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype
import no.nav.familie.kontrakter.ba.søknad.v5.RegistrertBostedType
import no.nav.familie.kontrakter.ba.søknad.v7.Dokumentasjonsbehov
import no.nav.familie.kontrakter.ba.søknad.v7.Søknaddokumentasjon
import no.nav.familie.kontrakter.ba.søknad.v7.Søknadsvedlegg
import no.nav.familie.kontrakter.ba.søknad.v8.AndreForelder
import no.nav.familie.kontrakter.ba.søknad.v8.AndreForelderUtvidet
import no.nav.familie.kontrakter.felles.søknad.Søknadsfelt
import no.nav.familie.kontrakter.ba.søknad.v10.Barn as BarnV10
import no.nav.familie.kontrakter.ba.søknad.v10.BarnetrygdSøknad as BarnetrygdSøknadV10
import no.nav.familie.kontrakter.ba.søknad.v10.Søker as SøkerV10
import no.nav.familie.kontrakter.ba.søknad.v8.Barn as BarnV8
import no.nav.familie.kontrakter.ba.søknad.v8.Søker as SøkerV8
import no.nav.familie.kontrakter.ba.søknad.v9.BarnetrygdSøknad as BarnetrygdSøknadV9

fun lagBarnetrygdSøknadV10(
    søkerFnr: String = randomFnr(),
    barnFnr: List<String> = listOf(randomFnr()),
    søknadstype: Søknadstype = Søknadstype.ORDINÆR,
    erEøs: Boolean = false,
    originalspråk: String = "nb",
    inneholderVedlegg: Boolean = false,
    erFosterbarn: Boolean = false,
    erIBeredskapshjem: Boolean = false,
    harKryssetForDeltBosted: Boolean = false,
    oppholderSegIInstitusjon: Boolean = false,
    søkerErAsylsøker: Boolean = false,
    barnErAsylsøker: Boolean = false,
    erAdoptertFraUtland: Boolean = false,
    borFastMedSøker: Boolean = true,
): BarnetrygdSøknadV10 =
    BarnetrygdSøknadV10(
        kontraktVersjon = 10,
        søker = lagSøkerV10(søkerFnr, erAsylsøker = søkerErAsylsøker),
        barn =
            barnFnr.map {
                lagBarnV10(
                    it,
                    erFosterbarn = erFosterbarn,
                    erIBeredskapshjem = erIBeredskapshjem,
                    harKryssetForDeltBosted = harKryssetForDeltBosted,
                    oppholderSegIInstitusjon = oppholderSegIInstitusjon,
                    erAsylsøker = barnErAsylsøker,
                    erAdoptertFraUtland = erAdoptertFraUtland,
                    borFastMedSøker = borFastMedSøker,
                )
            },
        antallEøsSteg = if (erEøs) 1 else 0,
        dokumentasjon = lagSøknaddokumentasjon(inneholderVedlegg = inneholderVedlegg),
        originalSpråk = originalspråk,
        finnesPersonMedAdressebeskyttelse = false,
        søknadstype = søknadstype,
        spørsmål = emptyMap(),
        teksterUtenomSpørsmål = emptyMap(),
    )

fun lagBarnetrygdSøknadV9(
    søkerFnr: String = randomFnr(),
    barnFnr: List<String> = listOf(randomFnr()),
    søknadstype: Søknadstype = Søknadstype.ORDINÆR,
    erEøs: Boolean = false,
    originalspråk: String = "nb",
    inneholderVedlegg: Boolean = false,
    erFosterbarn: Boolean = false,
    erIBeredskapshjem: Boolean = false,
    harKryssetForDeltBosted: Boolean = false,
    oppholderSegIInstitusjon: Boolean = false,
    søkerErAsylsøker: Boolean = false,
    barnErAsylsøker: Boolean = false,
    erAdoptertFraUtland: Boolean = false,
    borFastMedSøker: Boolean = true,
): BarnetrygdSøknadV9 =
    BarnetrygdSøknadV9(
        kontraktVersjon = 9,
        søker = lagSøkerV8(søkerFnr, erAsylsøker = søkerErAsylsøker),
        barn =
            barnFnr.map {
                lagBarnV8(
                    it,
                    erFosterbarn = erFosterbarn,
                    erIBeredskapshjem = erIBeredskapshjem,
                    harKryssetForDeltBosted = harKryssetForDeltBosted,
                    oppholderSegIInstitusjon = oppholderSegIInstitusjon,
                    erAsylsøker = barnErAsylsøker,
                    erAdoptertFraUtland = erAdoptertFraUtland,
                    borFastMedSøker = borFastMedSøker,
                )
            },
        antallEøsSteg = if (erEøs) 1 else 0,
        dokumentasjon = lagSøknaddokumentasjon(inneholderVedlegg = inneholderVedlegg),
        originalSpråk = originalspråk,
        finnesPersonMedAdressebeskyttelse = false,
        søknadstype = søknadstype,
        spørsmål = emptyMap(),
        teksterUtenomSpørsmål = emptyMap(),
    )

fun lagSøkerV10(
    fnr: String,
    erAsylsøker: Boolean = false,
): SøkerV10 =
    SøkerV10(
        harEøsSteg = false,
        ident = lagStringSøknadsfelt(fnr),
        navn = lagStringSøknadsfelt("Navn"),
        statsborgerskap = lagStringSøknadsfelt(listOf("Norge")),
        adresse =
            lagStringSøknadsfelt(
                SøknadAdresse(
                    adressenavn = "Gate",
                    postnummer = null,
                    husbokstav = null,
                    bruksenhetsnummer = null,
                    husnummer = null,
                    poststed = null,
                ),
            ),
        adressebeskyttelse = false,
        sivilstand = lagStringSøknadsfelt(SIVILSTANDTYPE.UOPPGITT),
        utenlandsperioder = emptyList(),
        arbeidsperioderUtland = emptyList(),
        pensjonsperioderUtland = emptyList(),
        arbeidsperioderNorge = emptyList(),
        pensjonsperioderNorge = emptyList(),
        andreUtbetalingsperioder = emptyList(),
        idNummer = emptyList(),
        spørsmål = lagSøkerSpørsmål(erAsylsøker = erAsylsøker),
        nåværendeSamboer = null,
        tidligereSamboere = emptyList(),
    )

fun lagBarnV10(
    fnr: String,
    erFosterbarn: Boolean = false,
    erIBeredskapshjem: Boolean = false,
    harKryssetForDeltBosted: Boolean = false,
    oppholderSegIInstitusjon: Boolean = false,
    erAsylsøker: Boolean = false,
    erAdoptertFraUtland: Boolean = false,
    borFastMedSøker: Boolean = true,
): BarnV10 =
    BarnV10(
        harEøsSteg = false,
        ident = lagStringSøknadsfelt(fnr),
        navn = lagStringSøknadsfelt(""),
        registrertBostedType = lagStringSøknadsfelt(RegistrertBostedType.REGISTRERT_SOKERS_ADRESSE),
        alder = null,
        andreForelder = if (harKryssetForDeltBosted) lagAndreForelder() else null,
        utenlandsperioder = emptyList(),
        omsorgsperson = null,
        idNummer = emptyList(),
        spørsmål =
            lagBarnSpørsmål(
                erFosterbarn = erFosterbarn,
                erIBeredskapshjem = erIBeredskapshjem,
                oppholderSegIInstitusjon = oppholderSegIInstitusjon,
                erAsylsøker = erAsylsøker,
                erAdoptertFraUtland = erAdoptertFraUtland,
                borFastMedSøker = borFastMedSøker,
            ),
        eøsBarnetrygdsperioder = emptyList(),
    )

fun lagSøkerV8(
    fnr: String,
    erAsylsøker: Boolean = false,
): SøkerV8 =
    SøkerV8(
        harEøsSteg = false,
        ident = lagStringSøknadsfelt(fnr),
        navn = lagStringSøknadsfelt("Navn"),
        statsborgerskap = lagStringSøknadsfelt(listOf("Norge")),
        adresse =
            lagStringSøknadsfelt(
                SøknadAdresse(
                    adressenavn = "Gate",
                    postnummer = null,
                    husbokstav = null,
                    bruksenhetsnummer = null,
                    husnummer = null,
                    poststed = null,
                ),
            ),
        adressebeskyttelse = false,
        sivilstand = lagStringSøknadsfelt(SIVILSTANDTYPE.UOPPGITT),
        utenlandsperioder = emptyList(),
        arbeidsperioderUtland = emptyList(),
        pensjonsperioderUtland = emptyList(),
        arbeidsperioderNorge = emptyList(),
        pensjonsperioderNorge = emptyList(),
        andreUtbetalingsperioder = emptyList(),
        idNummer = emptyList(),
        spørsmål = lagSøkerSpørsmål(erAsylsøker = erAsylsøker),
        nåværendeSamboer = null,
        tidligereSamboere = emptyList(),
    )

fun lagBarnV8(
    fnr: String,
    erFosterbarn: Boolean = false,
    erIBeredskapshjem: Boolean = false,
    harKryssetForDeltBosted: Boolean = false,
    oppholderSegIInstitusjon: Boolean = false,
    erAsylsøker: Boolean = false,
    erAdoptertFraUtland: Boolean = false,
    borFastMedSøker: Boolean = true,
): BarnV8 =
    BarnV8(
        harEøsSteg = false,
        ident = lagStringSøknadsfelt(fnr),
        navn = lagStringSøknadsfelt(""),
        registrertBostedType = lagStringSøknadsfelt(RegistrertBostedType.REGISTRERT_SOKERS_ADRESSE),
        alder = null,
        andreForelder = if (harKryssetForDeltBosted) lagAndreForelder() else null,
        utenlandsperioder = emptyList(),
        omsorgsperson = null,
        idNummer = emptyList(),
        spørsmål =
            lagBarnSpørsmål(
                erFosterbarn = erFosterbarn,
                erIBeredskapshjem = erIBeredskapshjem,
                oppholderSegIInstitusjon = oppholderSegIInstitusjon,
                erAsylsøker = erAsylsøker,
                erAdoptertFraUtland = erAdoptertFraUtland,
                borFastMedSøker = borFastMedSøker,
            ),
        eøsBarnetrygdsperioder = emptyList(),
    )

private fun lagSøkerSpørsmål(erAsylsøker: Boolean): Map<String, Søknadsfelt<Any>> =
    mapOf(
        "erAsylsøker" to lagStringSøknadsfelt<Any>(erAsylsøker.tilSøknadssvar()),
    )

private fun lagBarnSpørsmål(
    erFosterbarn: Boolean,
    erIBeredskapshjem: Boolean,
    oppholderSegIInstitusjon: Boolean,
    erAsylsøker: Boolean,
    erAdoptertFraUtland: Boolean,
    borFastMedSøker: Boolean,
): Map<String, Søknadsfelt<Any>> =
    mapOf(
        "erFosterbarn" to lagStringSøknadsfelt<Any>(erFosterbarn.tilSøknadssvar()),
        "erIBeredskapshjem" to lagStringSøknadsfelt<Any>(erIBeredskapshjem.tilSøknadssvar()),
        "oppholderSegIInstitusjon" to lagStringSøknadsfelt<Any>(oppholderSegIInstitusjon.tilSøknadssvar()),
        "erAsylsøker" to lagStringSøknadsfelt<Any>(erAsylsøker.tilSøknadssvar()),
        "erAdoptertFraUtland" to lagStringSøknadsfelt<Any>(erAdoptertFraUtland.tilSøknadssvar()),
        "borFastMedSøker" to lagStringSøknadsfelt<Any>(borFastMedSøker.tilSøknadssvar()),
    )

private fun lagAndreForelder(): AndreForelder =
    AndreForelder(
        kanIkkeGiOpplysninger = lagStringSøknadsfelt("NEI"),
        skriftligAvtaleOmDeltBosted = lagStringSøknadsfelt("JA"),
        utvidet = AndreForelderUtvidet(),
    )

private fun lagSøknaddokumentasjon(inneholderVedlegg: Boolean): List<Søknaddokumentasjon> =
    if (inneholderVedlegg) {
        listOf(
            Søknaddokumentasjon(
                dokumentasjonsbehov = Dokumentasjonsbehov.ANNEN_DOKUMENTASJON,
                harSendtInn = true,
                opplastedeVedlegg =
                    listOf(
                        Søknadsvedlegg(
                            dokumentId = "1",
                            navn = "vedlegg.pdf",
                            tittel = Dokumentasjonsbehov.ANNEN_DOKUMENTASJON,
                        ),
                    ),
                dokumentasjonSpråkTittel = emptyMap(),
            ),
        )
    } else {
        emptyList()
    }

private fun Boolean.tilSøknadssvar(): String = if (this) "JA" else "NEI"

fun <T> lagStringSøknadsfelt(verdi: T): Søknadsfelt<T> =
    Søknadsfelt(
        label = mapOf("nb" to ""),
        verdi = mapOf("nb" to verdi),
    )
