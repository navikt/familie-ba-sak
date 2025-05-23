package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.kontrakter.ba.søknad.v1.SIVILSTANDTYPE
import no.nav.familie.kontrakter.ba.søknad.v1.SøknadAdresse
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype
import no.nav.familie.kontrakter.ba.søknad.v5.RegistrertBostedType
import no.nav.familie.kontrakter.ba.søknad.v8.Barn
import no.nav.familie.kontrakter.ba.søknad.v8.Søker
import no.nav.familie.kontrakter.ba.søknad.v9.BarnetrygdSøknad
import no.nav.familie.kontrakter.felles.søknad.Søknadsfelt

fun lagBarnetrygdSøknadV9(
    søkerFnr: String = randomFnr(),
    barnFnr: List<String> = listOf(randomFnr()),
    søknadstype: Søknadstype = Søknadstype.ORDINÆR,
    erEøs: Boolean = false,
    originalspråk: String = "nb",
): BarnetrygdSøknad =
    BarnetrygdSøknad(
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

fun lagSøkerV8(fnr: String): Søker =
    Søker(
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

fun lagBarnV8(fnr: String): Barn =
    Barn(
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
