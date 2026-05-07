package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.erFomEtterTom
import no.nav.familie.ba.sak.kjerne.personident.Aktør
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
    ): List<Bostedsadresse> = this.filter { it.gyldigTilOgMed?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }

    fun List<Oppholdsadresse>.filtrerBortOppholdsadresserFørEldsteBarn(
        eldsteBarnsFødselsdato: LocalDate,
    ): List<Oppholdsadresse> = this.filter { it.gyldigTilOgMed?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }

    fun List<DeltBosted>.filtrerBortDeltBostedForSøker(
        personType: PersonType,
    ): List<DeltBosted> =
        if (personType == PersonType.SØKER) {
            emptyList()
        } else {
            this
        }

    fun List<Statsborgerskap>.filtrerBortStatsborgerskapFørEldsteBarn(
        eldsteBarnsFødselsdato: LocalDate,
    ): List<Statsborgerskap> = this.filter { it.gyldigTilOgMed?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }

    fun List<Statsborgerskap>.filtrerBortUgyldigeStatsborgerskap(aktør: Aktør): List<Statsborgerskap> {
        val (ugyldigeStatsborgerskap, gyldigeStatsborgerskap) = this.partition { it.erFomEtterTom() }

        if (ugyldigeStatsborgerskap.isNotEmpty()) {
            secureLogger.warn(
                "Filtrerer bort ${ugyldigeStatsborgerskap.size} statsborgerskap fra PDL med fom etter gyldigTilOgMed: $ugyldigeStatsborgerskap - aktør ${aktør.aktørId}",
            )
        }

        return gyldigeStatsborgerskap
    }

    fun List<Opphold>.filtrerBortOppholdFørEldsteBarn(
        eldsteBarnsFødselsdato: LocalDate,
    ): List<Opphold> = this.filter { it.oppholdTil?.isSameOrAfter(eldsteBarnsFødselsdato) ?: true }

    fun List<Sivilstand>.filtrerBortIkkeRelevanteSivilstander(
        behandlingKategori: BehandlingKategori,
        behandlingUnderkategori: BehandlingUnderkategori,
        personType: PersonType,
    ): List<Sivilstand> =
        if (behandlingUnderkategori == BehandlingUnderkategori.ORDINÆR && personType != PersonType.BARN && behandlingKategori == BehandlingKategori.NASJONAL) {
            emptyList()
        } else {
            this
        }
}
