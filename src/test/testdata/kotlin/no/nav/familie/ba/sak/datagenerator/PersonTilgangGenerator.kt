package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.sikkerhet.PersonTilgang
import no.nav.familie.tilgangsmaskin.Avvisningskode

// Begrunnelsene er hentet fra Tilgangsmaskinen (regel-messages.properties i navikt/populasjonstilgangskontroll).
const val BEGRUNNELSE_STRENGT_FORTROLIG_FRA_TILGANGSMASKINEN = "Du har ikke tilgang til brukere med strengt fortrolig adresse (kode 6)"
const val BEGRUNNELSE_STRENGT_FORTROLIG_UTLAND_FRA_TILGANGSMASKINEN = "Du har ikke tilgang til brukere med strengt fortrolig adresse i utlandet (§ 19)"
const val BEGRUNNELSE_FORTROLIG_FRA_TILGANGSMASKINEN = "Du har ikke tilgang til brukere med fortrolig adresse (kode 7)"
const val BEGRUNNELSE_SKJERMING_FRA_TILGANGSMASKINEN = "Du har ikke tilgang til Nav-ansatte og deres nærmeste familie"

fun lagPersonTilgangAvvistGrunnetStrengtFortrolig(personIdent: String) =
    PersonTilgang.avvist(
        personIdent = personIdent,
        avvisningskode = Avvisningskode.AVVIST_STRENGT_FORTROLIG_ADRESSE,
        begrunnelse = BEGRUNNELSE_STRENGT_FORTROLIG_FRA_TILGANGSMASKINEN,
    )

fun lagPersonTilgangAvvistGrunnetStrengtFortroligUtland(personIdent: String) =
    PersonTilgang.avvist(
        personIdent = personIdent,
        avvisningskode = Avvisningskode.AVVIST_STRENGT_FORTROLIG_UTLAND,
        begrunnelse = BEGRUNNELSE_STRENGT_FORTROLIG_UTLAND_FRA_TILGANGSMASKINEN,
    )

fun lagPersonTilgangAvvistGrunnetFortrolig(personIdent: String) =
    PersonTilgang.avvist(
        personIdent = personIdent,
        avvisningskode = Avvisningskode.AVVIST_FORTROLIG_ADRESSE,
        begrunnelse = BEGRUNNELSE_FORTROLIG_FRA_TILGANGSMASKINEN,
    )

fun lagPersonTilgangAvvistGrunnetSkjerming(personIdent: String) =
    PersonTilgang.avvist(
        personIdent = personIdent,
        avvisningskode = Avvisningskode.AVVIST_SKJERMING,
        begrunnelse = BEGRUNNELSE_SKJERMING_FRA_TILGANGSMASKINEN,
    )
