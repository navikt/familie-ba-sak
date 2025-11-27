package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService.IdentMedAdressebeskyttelse
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING

fun finnPersonMedStrengesteAdressebeskyttelse(personer: List<IdentMedAdressebeskyttelse>): String? =
    personer
        .fold(
            null,
            @Suppress("ktlint:standard:blank-line-before-declaration")
            fun(
                person: IdentMedAdressebeskyttelse?,
                neste: IdentMedAdressebeskyttelse,
            ): IdentMedAdressebeskyttelse? =
                when {
                    person?.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG -> {
                        person
                    }

                    neste.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG -> {
                        neste
                    }

                    person?.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND -> {
                        person
                    }

                    neste.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND -> {
                        neste
                    }

                    person?.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.FORTROLIG -> {
                        person
                    }

                    neste.adressebeskyttelsegradering == ADRESSEBESKYTTELSEGRADERING.FORTROLIG -> {
                        neste
                    }

                    else -> {
                        null
                    }
                },
        )?.ident
