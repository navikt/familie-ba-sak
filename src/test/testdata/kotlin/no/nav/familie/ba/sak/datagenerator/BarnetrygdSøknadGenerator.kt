package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.kontrakter.ba.søknad.v1.SIVILSTANDTYPE
import no.nav.familie.kontrakter.ba.søknad.v1.SøknadAdresse
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype
import no.nav.familie.kontrakter.ba.søknad.v5.RegistrertBostedType
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
): BarnetrygdSøknadV10 =
    BarnetrygdSøknadV10(
        kontraktVersjon = 10,
        søker = lagSøkerV10(søkerFnr),
        barn = barnFnr.map { lagBarnV10(it) },
        antallEøsSteg = if (erEøs) 1 else 0,
        dokumentasjon = emptyList(),
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
): BarnetrygdSøknadV9 =
    BarnetrygdSøknadV9(
        kontraktVersjon = 9,
        søker = lagSøkerV8(søkerFnr),
        barn = barnFnr.map { lagBarnV8(it) },
        antallEøsSteg = if (erEøs) 1 else 0,
        dokumentasjon = emptyList(),
        originalSpråk = originalspråk,
        finnesPersonMedAdressebeskyttelse = false,
        søknadstype = søknadstype,
        spørsmål = emptyMap(),
        teksterUtenomSpørsmål = emptyMap(),
    )

fun lagSøkerV10(fnr: String): SøkerV10 =
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
        spørsmål = emptyMap(),
        nåværendeSamboer = null,
        tidligereSamboere = emptyList(),
    )

fun lagBarnV10(fnr: String): BarnV10 =
    BarnV10(
        harEøsSteg = false,
        ident = lagStringSøknadsfelt(fnr),
        navn = lagStringSøknadsfelt(""),
        registrertBostedType = lagStringSøknadsfelt(RegistrertBostedType.REGISTRERT_SOKERS_ADRESSE),
        alder = null,
        andreForelder = null,
        utenlandsperioder = emptyList(),
        omsorgsperson = null,
        idNummer = emptyList(),
        spørsmål = emptyMap(),
        eøsBarnetrygdsperioder = emptyList(),
    )

fun lagSøkerV8(fnr: String): SøkerV8 =
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
        spørsmål = emptyMap(),
        nåværendeSamboer = null,
        tidligereSamboere = emptyList(),
    )

fun lagBarnV8(fnr: String): BarnV8 =
    BarnV8(
        harEøsSteg = false,
        ident = lagStringSøknadsfelt(fnr),
        navn = lagStringSøknadsfelt(""),
        registrertBostedType = lagStringSøknadsfelt(RegistrertBostedType.REGISTRERT_SOKERS_ADRESSE),
        alder = null,
        andreForelder = null,
        utenlandsperioder = emptyList(),
        omsorgsperson = null,
        idNummer = emptyList(),
        spørsmål = emptyMap(),
        eøsBarnetrygdsperioder = emptyList(),
    )

fun <T> lagStringSøknadsfelt(verdi: T): Søknadsfelt<T> =
    Søknadsfelt(
        label = mapOf("no" to ""),
        verdi = mapOf("no" to verdi),
    )
