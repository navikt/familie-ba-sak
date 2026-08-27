package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrBostedsadresse.Companion.fregManglendeFlytteDato
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.harUgyldigPeriode
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
    ): List<Bostedsadresse> = filtrerBortAdresserSomOpphørerFør(cutoffDato = eldsteBarnsFødselsdato) { Adresse.opprettFra(it) }

    fun List<Oppholdsadresse>.filtrerBortOppholdsadresserFørEldsteBarn(
        eldsteBarnsFødselsdato: LocalDate,
    ): List<Oppholdsadresse> = filtrerBortAdresserSomOpphørerFør(cutoffDato = eldsteBarnsFødselsdato) { Adresse.opprettFra(it) }

    /**
     * Historiske adresser fra PDL mangler ofte til-og-med-dato og opphører implisitt når neste adresse begynner.
     * En adresse uten til-og-med regnes derfor som opphørt før cutoff-datoen dersom en annen adresse har fra-og-med som er
     * senere enn dens egen, men ikke etter cutoff-datoen – uavhengig av rekkefølgen i listen. Adresser med lik fra-og-med
     * avslutter ikke hverandre, og adresser uten fra-og-med beholdes alltid.
     *
     * Adresser med ugyldig periode og adresser med manglende flyttedato fra Freg (0001-01-01) avslutter ikke andre adresser
     *
     */
    private fun <T> List<T>.filtrerBortAdresserSomOpphørerFør(
        cutoffDato: LocalDate,
        tilAdresse: (T) -> Adresse,
    ): List<T> {
        val adresser = map { it to tilAdresse(it) }
        val fomDatoerSomAvslutterAndreAdresser =
            adresser
                .map { (_, adresse) -> adresse }
                .filterNot { it.harUgyldigPeriode() }
                .mapNotNull { it.fomUtenManglendeFlyttedato() }

        return adresser
            .filter { (_, adresse) ->
                val fom = adresse.fomUtenManglendeFlyttedato()
                val tom = adresse.gyldigTilOgMed
                when {
                    tom != null -> tom.isSameOrAfter(cutoffDato)
                    fom != null -> fomDatoerSomAvslutterAndreAdresser.none { it.isAfter(fom) && it.isSameOrBefore(cutoffDato) }
                    else -> true
                }
            }.map { (original, _) -> original }
    }

    private fun Adresse.fomUtenManglendeFlyttedato(): LocalDate? = gyldigFraOgMed?.takeUnless { it == fregManglendeFlytteDato }

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
