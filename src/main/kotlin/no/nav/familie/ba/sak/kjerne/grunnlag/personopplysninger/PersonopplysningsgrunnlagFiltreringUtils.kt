package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap

object PersonopplysningsgrunnlagFiltreringUtils {
    fun List<Bostedsadresse>.filtrerBortBostedsadresserFørEldsteBarn(
        personOpplysningGrunnlag: PersonopplysningGrunnlag,
        filtrerAdresser: Boolean,
    ): List<Bostedsadresse> {
        if (!filtrerAdresser) return this

        val eldsteBarnsFødselsdato = personOpplysningGrunnlag.barna.minOfOrNull { it.fødselsdato } ?: return this

        val filtrerteAdresser = this.filter { it.gyldigTilOgMed?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }

        return filtrerteAdresser
    }

    fun List<Oppholdsadresse>.filtrerBortOppholdsadresserFørEldsteBarn(
        personOpplysningGrunnlag: PersonopplysningGrunnlag,
        filtrerAdresser: Boolean,
    ): List<Oppholdsadresse> {
        if (!filtrerAdresser) return this

        val eldsteBarnsFødselsdato = personOpplysningGrunnlag.barna.minOfOrNull { it.fødselsdato } ?: return this

        val filtrerteAdresser = this.filter { it.gyldigTilOgMed?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }

        return filtrerteAdresser
    }

    fun List<DeltBosted>.filtrerBortDeltBostedForSøker(
        personType: PersonType,
        filtrerAdresser: Boolean,
    ): List<DeltBosted> {
        if (!filtrerAdresser) return this

        return if (personType == PersonType.SØKER) {
            emptyList()
        } else {
            this
        }
    }

    fun List<Statsborgerskap>.filtrerBortStatsbrogerskapFørEldsteBarn(
        personOpplysningGrunnlag: PersonopplysningGrunnlag,
        filtrerStatsborgerskap: Boolean,
    ): List<Statsborgerskap> {
        if (!filtrerStatsborgerskap) return this

        val eldsteBarnsFødselsdato = personOpplysningGrunnlag.barna.minOfOrNull { it.fødselsdato } ?: return this

        return this.filter { it.gyldigTilOgMed?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }
    }
}
