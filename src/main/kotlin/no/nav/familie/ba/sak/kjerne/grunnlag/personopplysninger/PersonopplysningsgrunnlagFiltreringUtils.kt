package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap

object PersonopplysningsgrunnlagFiltreringUtils {
    fun List<Bostedsadresse>.filtrerBortBostedsadresserFørEldsteBarn(
        personOpplysningGrunnlag: PersonopplysningGrunnlag,
        filtrerAdresser: Boolean,
    ): List<Bostedsadresse> {
        if (!filtrerAdresser) return this

        val eldsteBarnsFødselsdato = personOpplysningGrunnlag.barna.minOfOrNull { it.fødselsdato } ?: return this

        return this.filter { it.gyldigTilOgMed?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }
    }

    fun List<Oppholdsadresse>.filtrerBortOppholdsadresserFørEldsteBarn(
        personOpplysningGrunnlag: PersonopplysningGrunnlag,
        filtrerAdresser: Boolean,
    ): List<Oppholdsadresse> {
        if (!filtrerAdresser) return this

        val eldsteBarnsFødselsdato = personOpplysningGrunnlag.barna.minOfOrNull { it.fødselsdato } ?: return this

        return this.filter { it.gyldigTilOgMed?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }
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

    fun List<Statsborgerskap>.filtrerBortStatsborgerskapFørEldsteBarn(
        personOpplysningGrunnlag: PersonopplysningGrunnlag,
        filtrerStatsborgerskap: Boolean,
    ): List<Statsborgerskap> {
        if (!filtrerStatsborgerskap) return this

        val eldsteBarnsFødselsdato = personOpplysningGrunnlag.barna.minOfOrNull { it.fødselsdato } ?: return this

        return this.filter { it.gyldigTilOgMed?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }
    }

    fun List<Opphold>.filtrerBortOppholdFørEldsteBarn(
        personOpplysningGrunnlag: PersonopplysningGrunnlag,
        filtrerOpphold: Boolean,
    ): List<Opphold> {
        if (!filtrerOpphold) return this

        val eldsteBarnsFødselsdato = personOpplysningGrunnlag.barna.minOfOrNull { it.fødselsdato } ?: return this

        return this.filter { it.oppholdTil?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }
    }

    fun List<Sivilstand>.filtrerBortIkkeRelevanteSivilstand(
        personOpplysningGrunnlag: PersonopplysningGrunnlag,
        filtrerSivilstand: Boolean,
        underkategori: BehandlingUnderkategori,
        personType: PersonType,
    ): List<Sivilstand> {
        if (!filtrerSivilstand) return this

        if (personType == PersonType.BARN) return this

        if (underkategori == BehandlingUnderkategori.ORDINÆR) return emptyList()

        val eldsteBarnsFødselsdato = personOpplysningGrunnlag.barna.minOfOrNull { it.fødselsdato } ?: return this

        val sortertSivilstand = this.sortedBy { it.gyldigFraOgMed }

        return sortertSivilstand
            .windowed(size = 2, step = 1, partialWindows = true)
            .mapIndexedNotNull { _, window ->
                val denne = window[0]
                val neste = window.getOrNull(1)

                if (neste != null &&
                    (neste.gyldigFraOgMed?.isBefore(eldsteBarnsFødselsdato) == true)
                ) {
                    null
                } else {
                    denne
                }
            }
    }
}
