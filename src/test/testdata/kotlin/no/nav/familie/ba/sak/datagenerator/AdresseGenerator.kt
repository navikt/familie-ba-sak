package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrVegadresseBostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import java.time.LocalDate

fun lagGrVegadresse(
    matrikkelId: Long? = null,
    bruksenhetsnummer: String? = null,
    adressenavn: String? = null,
    husnummer: String? = null,
    husbokstav: String? = null,
    postnummer: String? = null,
) = GrVegadresseBostedsadresse(
    matrikkelId = matrikkelId,
    husnummer = husnummer,
    husbokstav = husbokstav,
    bruksenhetsnummer = bruksenhetsnummer,
    adressenavn = adressenavn,
    kommunenummer = null,
    tilleggsnavn = null,
    postnummer = postnummer,
    poststed = null,
)

fun lagEnkelBostedsadresse(fom: LocalDate = LocalDate.now().minusYears(1)) =
    listOf(
        Bostedsadresse(
            gyldigFraOgMed = fom,
            gyldigTilOgMed = null,
            vegadresse = null,
            matrikkeladresse = lagMatrikkeladresse(1234L),
            ukjentBosted = null,
        ),
    )

fun lagVegadresse(
    matrikkelId: Long? = null,
    husnummer: String? = null,
    husbokstav: String? = null,
    bruksenhetsnummer: String? = null,
    adressenavn: String? = null,
    kommunenummer: String? = null,
    tilleggsnavn: String? = null,
    postnummer: String? = null,
) = Vegadresse(
    matrikkelId,
    husnummer,
    husbokstav,
    bruksenhetsnummer,
    adressenavn,
    kommunenummer,
    tilleggsnavn,
    postnummer,
)

fun lagMatrikkeladresse(
    matrikkelId: Long? = null,
    bruksenhetsnummer: String? = null,
    tilleggsnavn: String? = null,
    postnummer: String? = null,
    kommunenummer: String? = null,
) = Matrikkeladresse(
    matrikkelId,
    bruksenhetsnummer = bruksenhetsnummer,
    tilleggsnavn = tilleggsnavn,
    postnummer = postnummer,
    kommunenummer = kommunenummer,
)
