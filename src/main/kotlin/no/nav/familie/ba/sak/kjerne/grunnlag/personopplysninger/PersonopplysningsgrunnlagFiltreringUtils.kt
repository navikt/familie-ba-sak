package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import java.time.LocalDate

object PersonopplysningsgrunnlagFiltreringUtils {
    fun List<Bostedsadresse>.filtrerBortBostedsadresserFørEldsteBarn(
        eldsteBarnsFødselsdato: LocalDate,
        filtrerAdresser: Boolean,
    ): List<Bostedsadresse> {
        if (!filtrerAdresser) return this

        return this.filter { it.gyldigTilOgMed?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }
    }

    fun List<Oppholdsadresse>.filtrerBortOppholdsadresserFørEldsteBarn(
        eldsteBarnsFødselsdato: LocalDate,
        filtrerAdresser: Boolean,
    ): List<Oppholdsadresse> {
        if (!filtrerAdresser) return this

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
        eldsteBarnsFødselsdato: LocalDate,
        filtrerStatsborgerskap: Boolean,
    ): List<Statsborgerskap> {
        if (!filtrerStatsborgerskap) return this

        return this.filter { it.gyldigTilOgMed?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }
    }

    fun List<Opphold>.filtrerBortOppholdFørEldsteBarn(
        eldsteBarnsFødselsdato: LocalDate,
        filtrerOpphold: Boolean,
    ): List<Opphold> {
        if (!filtrerOpphold) return this

        return this.filter { it.oppholdTil?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }
    }

    fun List<Sivilstand>.filtrerBortIkkeRelevanteSivilstander(
        filtrerSivilstand: Boolean,
        behandlingKategori: BehandlingKategori,
        behandlingUnderkategori: BehandlingUnderkategori,
        personType: PersonType,
    ): List<Sivilstand> {
        if (!filtrerSivilstand) return this

        return if (behandlingUnderkategori == BehandlingUnderkategori.ORDINÆR && personType != PersonType.BARN && behandlingKategori == BehandlingKategori.NASJONAL) {
            emptyList()
        } else {
            this
        }
    }
}
