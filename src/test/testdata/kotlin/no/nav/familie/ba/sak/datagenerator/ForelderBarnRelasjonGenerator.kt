package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import java.time.LocalDate

fun lagForelderBarnRelasjon(
    aktør: Aktør = lagAktør(),
    relasjonsrolle: FORELDERBARNRELASJONROLLE = FORELDERBARNRELASJONROLLE.BARN,
    navn: String? = null,
    fødselsdato: LocalDate? = null,
    adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    kjønn: Kjønn = Kjønn.UKJENT,
    erEgenAnsatt: Boolean? = null,
): ForelderBarnRelasjon =
    ForelderBarnRelasjon(
        aktør = aktør,
        relasjonsrolle = relasjonsrolle,
        navn = navn,
        fødselsdato = fødselsdato,
        adressebeskyttelseGradering = adressebeskyttelseGradering,
        kjønn = kjønn,
        erEgenAnsatt = erEgenAnsatt,
    )
