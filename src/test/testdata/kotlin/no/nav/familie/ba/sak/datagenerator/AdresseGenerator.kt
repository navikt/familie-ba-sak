package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrVegadresseBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse.GrMatrikkeladresseOppholdsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse.GrUkjentAdresseOppholdsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse.GrUtenlandskAdresseOppholdsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse.GrVegadresseOppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.UtenlandskAdresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import java.time.LocalDate

fun lagAdresser(
    bostedsadresser: List<Adresse> = emptyList(),
    delteBosteder: List<Adresse> = emptyList(),
    oppholdsadresse: List<Adresse> = emptyList(),
): Adresser =
    Adresser(
        bostedsadresser = bostedsadresser,
        delteBosteder = delteBosteder,
        oppholdsadresse = oppholdsadresse,
    )

fun lagAdresse(
    gyldigFraOgMed: LocalDate? = LocalDate.of(2025, 1, 1),
    gyldigTilOgMed: LocalDate? = LocalDate.of(2025, 1, 1),
    vegadresse: Vegadresse? = null,
    matrikkeladresse: Matrikkeladresse? = null,
    ukjentBosted: UkjentBosted? = null,
    oppholdAnnetSted: String? = null,
): Adresse =
    Adresse(
        gyldigFraOgMed = gyldigFraOgMed,
        gyldigTilOgMed = gyldigTilOgMed,
        vegadresse = vegadresse,
        matrikkeladresse = matrikkeladresse,
        ukjentBosted = ukjentBosted,
        oppholdAnnetSted = oppholdAnnetSted,
    )

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

fun lagBostedsadresse(
    gyldigFraOgMed: LocalDate? = LocalDate.now().minusYears(1),
    gyldigTilOgMed: LocalDate? = LocalDate.now().plusYears(1),
    angittFlyttedato: LocalDate? = gyldigFraOgMed,
    vegadresse: Vegadresse? = null,
    matrikkeladresse: Matrikkeladresse? = null,
    ukjentBosted: UkjentBosted? = null,
) = Bostedsadresse(
    gyldigFraOgMed = gyldigFraOgMed,
    gyldigTilOgMed = gyldigTilOgMed,
    angittFlyttedato = angittFlyttedato,
    vegadresse = vegadresse,
    matrikkeladresse = matrikkeladresse,
    ukjentBosted = ukjentBosted,
)

fun lagDeltBosted(
    startdatoForKontrakt: LocalDate? = LocalDate.now().minusYears(1),
    sluttdatoForKontrakt: LocalDate? = LocalDate.now().plusYears(1),
    vegadresse: Vegadresse? = null,
    matrikkeladresse: Matrikkeladresse? = null,
    ukjentBosted: UkjentBosted? = null,
): DeltBosted =
    DeltBosted(
        startdatoForKontrakt = startdatoForKontrakt,
        sluttdatoForKontrakt = sluttdatoForKontrakt,
        vegadresse = vegadresse,
        matrikkeladresse = matrikkeladresse,
        ukjentBosted = ukjentBosted,
    )

fun lagOppholdsadresse(
    gyldigFraOgMed: LocalDate? = LocalDate.now().minusYears(1),
    gyldigTilOgMed: LocalDate? = LocalDate.now().plusYears(1),
    oppholdAnnetSted: String? = null,
    vegadresse: Vegadresse? = null,
    matrikkeladresse: Matrikkeladresse? = null,
    utenlandskAdresse: UtenlandskAdresse? = null,
) = Oppholdsadresse(
    gyldigFraOgMed = gyldigFraOgMed,
    gyldigTilOgMed = gyldigTilOgMed,
    oppholdAnnetSted = oppholdAnnetSted,
    vegadresse = vegadresse,
    matrikkeladresse = matrikkeladresse,
    utenlandskAdresse = utenlandskAdresse,
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

fun lagUkjentBosted(
    bostedskommune: String,
) = UkjentBosted(
    bostedskommune = bostedskommune,
)

fun lagGrVegadresseOppholdsadresse(
    matrikkelId: Long? = null,
    husnummer: String? = null,
    husbokstav: String? = null,
    bruksenhetsnummer: String? = null,
    adressenavn: String? = null,
    kommunenummer: String? = null,
    tilleggsnavn: String? = null,
    postnummer: String? = null,
    periode: DatoIntervallEntitet? = null,
    poststed: String? = null,
) = GrVegadresseOppholdsadresse(
    matrikkelId = matrikkelId,
    husnummer = husnummer,
    husbokstav = husbokstav,
    bruksenhetsnummer = bruksenhetsnummer,
    adressenavn = adressenavn,
    kommunenummer = kommunenummer,
    tilleggsnavn = tilleggsnavn,
    postnummer = postnummer,
    poststed = poststed,
).also { it.periode = periode }

fun lagGrMatrikkelOppholdsadresse(
    matrikkelId: Long? = null,
    bruksenhetsnummer: String? = null,
    kommunenummer: String? = null,
    tilleggsnavn: String? = null,
    postnummer: String? = null,
    periode: DatoIntervallEntitet? = null,
    poststed: String? = null,
) = GrMatrikkeladresseOppholdsadresse(
    matrikkelId = matrikkelId,
    bruksenhetsnummer = bruksenhetsnummer,
    kommunenummer = kommunenummer,
    tilleggsnavn = tilleggsnavn,
    postnummer = postnummer,
    poststed = poststed,
).also { it.periode = periode }

fun lagGrUtenlandskOppholdsadresse(
    adressenavnNummer: String? = null,
    bygningEtasjeLeilighet: String? = null,
    postboksNummerNavn: String? = null,
    postkode: String? = null,
    bySted: String? = null,
    regionDistriktOmraade: String? = null,
    landkode: String? = null,
    periode: DatoIntervallEntitet? = null,
    oppholdAnnetSted: OppholdAnnetSted? = null,
) = GrUtenlandskAdresseOppholdsadresse(
    adressenavnNummer = adressenavnNummer,
    bygningEtasjeLeilighet = bygningEtasjeLeilighet,
    postboksNummerNavn = postboksNummerNavn,
    postkode = postkode,
    bySted = bySted,
    regionDistriktOmraade = regionDistriktOmraade,
    landkode = landkode,
).also {
    it.periode = periode
    it.oppholdAnnetSted = oppholdAnnetSted
}

fun lagGrUkjentAdresseOppholdsadresse(
    periode: DatoIntervallEntitet? = null,
    oppholdAnnetSted: OppholdAnnetSted? = null,
) = GrUkjentAdresseOppholdsadresse().also {
    it.periode = periode
    it.oppholdAnnetSted = oppholdAnnetSted
}
