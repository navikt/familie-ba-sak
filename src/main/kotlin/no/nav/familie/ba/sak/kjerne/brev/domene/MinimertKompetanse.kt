package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson

data class MinimertKompetanse(
    val søkersAktivitet: SøkersAktivitet,
    val annenForeldersAktivitet: AnnenForeldersAktivitet,
    val annenForeldersAktivitetslandNavn: LandNavn,
    val barnetsBostedslandNavn: LandNavn,
    val resultat: KompetanseResultat,
    val personer: List<MinimertRestPerson>
)

data class LandNavn(val navn: String)

fun Kompetanse.tilMinimertKompetanse(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    hentLand: (String) -> String,
): MinimertKompetanse {

    this.validerFelterErSatt()

    return MinimertKompetanse(
        søkersAktivitet = this.søkersAktivitet!!,
        annenForeldersAktivitet = this.annenForeldersAktivitet!!,
        annenForeldersAktivitetslandNavn = LandNavn(hentLand(this.annenForeldersAktivitetsland!!)),
        barnetsBostedslandNavn = LandNavn(hentLand(this.barnetsBostedsland!!)),
        resultat = this.resultat!!,
        personer = this.barnAktører.map { aktør ->
            val fødselsdato = personopplysningGrunnlag.barna.find { it.aktør == aktør }?.fødselsdato
                ?: throw Feil("Fant ikke aktør i personopplysninggrunnlaget")
            MinimertRestPerson(
                personIdent = aktør.aktivFødselsnummer(),
                fødselsdato = fødselsdato,
                type = PersonType.BARN,
            )
        }
    )
}
