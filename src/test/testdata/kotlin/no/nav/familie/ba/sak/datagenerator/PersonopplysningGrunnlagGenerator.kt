package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrMatrikkeladresseBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import java.time.LocalDate

fun lagPersonopplysningGrunnlag(
    id: Long = 0L,
    behandlingId: Long = 0L,
    aktiv: Boolean = true,
    lagPersoner: (grunnlag: PersonopplysningGrunnlag) -> Set<Person> = { emptySet() },
): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(id = id, behandlingId = behandlingId, aktiv = aktiv)
    val personer = lagPersoner(personopplysningGrunnlag)
    personopplysningGrunnlag.personer.addAll(personer)
    return personopplysningGrunnlag
}

/**
 * Bruk for integrasjonstest. Bruk lagPersonopplysningGrunnlag for enhetstest
 */
fun lagPersonopplysningGrunnlagUtenId(
    behandlingId: Long,
    aktiv: Boolean = true,
): PersonopplysningGrunnlag = lagPersonopplysningGrunnlag(id = 0L, behandlingId = behandlingId, aktiv = aktiv)

fun lagTestPersonopplysningGrunnlag(
    behandlingId: Long,
    vararg personer: Person,
): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)

    personopplysningGrunnlag.personer.addAll(
        personer.map { it.copy(personopplysningGrunnlag = personopplysningGrunnlag) },
    )
    return personopplysningGrunnlag
}

fun lagTestPersonopplysningGrunnlag(
    behandlingId: Long,
    søkerPersonIdent: String,
    barnasIdenter: List<String>,
    barnasFødselsdatoer: List<LocalDate> = barnasIdenter.map { LocalDate.of(2019, 1, 1) },
    søkerFødselsdato: LocalDate = LocalDate.of(1987, 1, 1),
    søkerAktør: Aktør =
        lagAktør(søkerPersonIdent).also {
            it.personidenter.add(
                Personident(
                    fødselsnummer = søkerPersonIdent,
                    aktør = it,
                    aktiv = søkerPersonIdent == it.personidenter.first().fødselsnummer,
                ),
            )
        },
    barnAktør: List<Aktør> =
        barnasIdenter.map { fødselsnummer ->
            lagAktør(fødselsnummer).also {
                it.personidenter.add(
                    Personident(
                        fødselsnummer = fødselsnummer,
                        aktør = it,
                        aktiv = fødselsnummer == it.personidenter.first().fødselsnummer,
                    ),
                )
            }
        },
): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)
    val bostedsadresse =
        GrMatrikkeladresseBostedsadresse(
            matrikkelId = null,
            bruksenhetsnummer = "H301",
            tilleggsnavn = "navn",
            postnummer = "0202",
            kommunenummer = "2231",
            poststed = "Oslo",
        )

    val søker =
        Person(
            aktør = søkerAktør,
            type = PersonType.SØKER,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = søkerFødselsdato,
            navn = "",
            kjønn = Kjønn.KVINNE,
        ).also { søker ->
            søker.statsborgerskap =
                mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = søker))
            søker.bostedsadresser = mutableListOf(bostedsadresse.apply { person = søker })
            søker.sivilstander =
                mutableListOf(
                    GrSivilstand(
                        type = SIVILSTANDTYPE.GIFT,
                        person = søker,
                    ),
                )
        }
    personopplysningGrunnlag.personer.add(søker)

    barnAktør.mapIndexed { index, aktør ->
        personopplysningGrunnlag.personer.add(
            Person(
                aktør = aktør,
                type = PersonType.BARN,
                personopplysningGrunnlag = personopplysningGrunnlag,
                fødselsdato = barnasFødselsdatoer.get(index),
                navn = "",
                kjønn = Kjønn.MANN,
            ).also { barn ->
                barn.statsborgerskap =
                    mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = barn))
                barn.bostedsadresser = mutableListOf(bostedsadresse.apply { person = barn })
                barn.sivilstander =
                    mutableListOf(
                        GrSivilstand(
                            type = SIVILSTANDTYPE.UGIFT,
                            person = barn,
                        ),
                    )
            },
        )
    }
    return personopplysningGrunnlag
}

fun PersonopplysningGrunnlag.tilPersonEnkelSøkerOgBarn() = this.søkerOgBarn.map { it.tilPersonEnkel() }
