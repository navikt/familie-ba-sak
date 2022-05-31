package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import java.time.YearMonth

data class MinimertKompetanse(
    val fom: YearMonth,
    val tom: YearMonth,
    val søkersAktivitet: SøkersAktivitet,
    val annenForeldersAktivitet: AnnenForeldersAktivitet,
    val annenForeldersAktivitetsland: String,
    val barnetsBostedsland: String,
    val resultat: KompetanseResultat,
    val person: MinimertRestPerson
)

fun Kompetanse.tilMinimertKompetanse(personopplysningGrunnlag: PersonopplysningGrunnlag): List<MinimertKompetanse> {

    if (this.fom == null || this.tom == null || søkersAktivitet == null || annenForeldersAktivitet == null || annenForeldersAktivitetsland == null || barnetsBostedsland == null || resultat == null) {
        throw Feil("Kompetanse mangler verdier")
    }

    return this.barnAktører.map { aktør ->

        val fødselsdato = personopplysningGrunnlag.barna.find { it.aktør == aktør }?.fødselsdato
            ?: throw Feil("Fant ikke aktør i personopplysninggrunnlaget")

        MinimertKompetanse(
            fom = this.fom,
            tom = this.tom,
            søkersAktivitet = this.søkersAktivitet,
            annenForeldersAktivitet = this.annenForeldersAktivitet,
            annenForeldersAktivitetsland = this.annenForeldersAktivitetsland,
            barnetsBostedsland = this.barnetsBostedsland,
            resultat = this.resultat,
            person = MinimertRestPerson(
                personIdent = aktør.aktivFødselsnummer(),
                fødselsdato = fødselsdato,
                type = PersonType.BARN,
            )
        )
    }
}
