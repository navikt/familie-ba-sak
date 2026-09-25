package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsreglerFaktaSøknad
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.time.LocalDate

fun lagFiltreringsreglerFaktaSøknad(
    søker: Person = tilfeldigSøker(fødselsdato = LocalDate.now().minusYears(18)),
    søkerMottarLøpendeUtvidet: Boolean = false,
    søkerOppfyllerVilkårForUtvidetBarnetrygd: Boolean = false,
    søkerMottarEøsBarnetrygd: Boolean = false,
    barnaSomSkalVurderes: List<Person> = listOf(tilfeldigPerson()),
    søkerLever: Boolean = true,
    barnaLever: Boolean = true,
    søkerHarVerge: Boolean = false,
    utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned: Boolean = false,
    søkerHarKryssetPåEøsSpørsmålISøknaden: Boolean = false,
    søkerHarKryssetForDeltBostedISøknaden: Boolean = false,
    søkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden: Boolean = false,
    søknadenInneholderVedlegg: Boolean = false,
    søkerHarIkkeLøpendeUtbetalingOgHarAldriHattUtbetaling: Boolean = false,
    søkerHarAdressebeskyttelseGradering6Eller19: Boolean = false,
    barnHarAdressebeskyttelseGradering6Eller19: Boolean = false,
    søkerOgBarnHarForelderBarnRelasjon: Boolean = true,
    søkerHarAktivNorskBostedsadresse: Boolean = true,
    barnHarAktivNorskBostedsadresse: Boolean = true,
    søkerHarUkrainskStatsborgerskap: Boolean = false,
    barnHarUkrainskStatsborgerskap: Boolean = false,
) = FiltreringsreglerFaktaSøknad(
    søker = søker,
    søkerMottarLøpendeUtvidet = søkerMottarLøpendeUtvidet,
    søkerOppfyllerVilkårForUtvidetBarnetrygd = søkerOppfyllerVilkårForUtvidetBarnetrygd,
    søkerMottarEøsBarnetrygd = søkerMottarEøsBarnetrygd,
    barnaSomSkalVurderes = barnaSomSkalVurderes,
    søkerLever = søkerLever,
    barnaLever = barnaLever,
    søkerHarVerge = søkerHarVerge,
    utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned =
    utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned,
    søkerHarKryssetPåEøsSpørsmålISøknaden = søkerHarKryssetPåEøsSpørsmålISøknaden,
    søkerHarKryssetForDeltBostedISøknaden = søkerHarKryssetForDeltBostedISøknaden,
    søkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden =
    søkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden,
    søknadenInneholderVedlegg = søknadenInneholderVedlegg,
    søkerHarIkkeLøpendeUtbetalingOgHarAldriHattUtbetaling = søkerHarIkkeLøpendeUtbetalingOgHarAldriHattUtbetaling,
    søkerHarAdressebeskyttelseGradering6Eller19 = søkerHarAdressebeskyttelseGradering6Eller19,
    barnHarAdressebeskyttelseGradering6Eller19 = barnHarAdressebeskyttelseGradering6Eller19,
    søkerOgBarnHarForelderBarnRelasjon = søkerOgBarnHarForelderBarnRelasjon,
    søkerHarAktivNorskBostedsadresse = søkerHarAktivNorskBostedsadresse,
    barnHarAktivNorskBostedsadresse = barnHarAktivNorskBostedsadresse,
    søkerHarUkrainskStatsborgerskap = søkerHarUkrainskStatsborgerskap,
    barnHarUkrainskStatsborgerskap = barnHarUkrainskStatsborgerskap,
)
