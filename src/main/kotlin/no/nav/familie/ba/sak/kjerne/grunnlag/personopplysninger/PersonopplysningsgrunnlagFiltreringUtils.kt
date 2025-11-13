package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse

object PersonopplysningsgrunnlagFiltreringUtils {
    fun List<Bostedsadresse>.filtrerBortBostedsadresserFørEldsteBarn(
        personOpplysningGrunnlag: PersonopplysningGrunnlag,
        featureToggleService: FeatureToggleService,
    ): List<Bostedsadresse> {
        if (!featureToggleService.isEnabled(FeatureToggle.FILTRER_ADRESSE_PÅ_CUTOFF)) return this

        val eldsteBarnsFødselsdato = personOpplysningGrunnlag.barna.minOfOrNull { it.fødselsdato } ?: return this

        val filtrerteAdresser = this.filter { it.gyldigTilOgMed?.isAfter(eldsteBarnsFødselsdato) ?: true }

        return filtrerteAdresser
    }

    fun List<Oppholdsadresse>.filtrerBortOppholdsadresserFørEldsteBarn(
        personOpplysningGrunnlag: PersonopplysningGrunnlag,
        featureToggleService: FeatureToggleService,
    ): List<Oppholdsadresse> {
        if (!featureToggleService.isEnabled(FeatureToggle.FILTRER_ADRESSE_PÅ_CUTOFF)) return this

        val eldsteBarnsFødselsdato = personOpplysningGrunnlag.barna.minOfOrNull { it.fødselsdato } ?: return this

        val filtrerteAdresser = this.filter { it.gyldigTilOgMed?.isAfter(eldsteBarnsFødselsdato) ?: true }

        return filtrerteAdresser
    }

    fun List<DeltBosted>.filtrerBortDeltBostedførEldsteBarn(
        personOpplysningGrunnlag: PersonopplysningGrunnlag,
        featureToggleService: FeatureToggleService,
    ): List<DeltBosted> {
        if (!featureToggleService.isEnabled(FeatureToggle.FILTRER_ADRESSE_PÅ_CUTOFF)) return this

        val eldsteBarnsFødselsdato = personOpplysningGrunnlag.barna.minOfOrNull { it.fødselsdato } ?: return this

        val filtrerteAdresser = this.filter { it.sluttdatoForKontrakt?.isAfter(eldsteBarnsFødselsdato) ?: true }

        return filtrerteAdresser
    }
}
