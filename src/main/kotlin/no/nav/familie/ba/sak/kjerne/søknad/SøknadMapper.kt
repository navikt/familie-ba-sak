package no.nav.familie.ba.sak.kjerne.søknad

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknad
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype
import no.nav.familie.kontrakter.ba.søknad.v8.AndreForelder
import no.nav.familie.kontrakter.ba.søknad.v9.bokmålsverdi
import no.nav.familie.kontrakter.ba.søknad.v9.tilBoolskSvar
import no.nav.familie.kontrakter.felles.søknad.BaSøknaddokumentasjon
import no.nav.familie.kontrakter.felles.søknad.MissingVersionException
import no.nav.familie.kontrakter.felles.søknad.Søknadsfelt
import org.springframework.stereotype.Component

interface SøknadMapper {
    val søknadVersjon: Int

    fun mapTilSøknad(versjonertBarnetrygdSøknad: VersjonertBarnetrygdSøknad): Søknad

    companion object {
        // SøknadsFeltId i kontrakten har ingen verdi for fosterbarn, så feltet slås opp på nøkkelen sin
        private const val ER_FOSTERBARN = "erFosterbarn"

        fun Søknadstype.tilBehandlingUnderkategori(): BehandlingUnderkategori =
            when (this) {
                Søknadstype.UTVIDET -> BehandlingUnderkategori.UTVIDET
                Søknadstype.ORDINÆR -> BehandlingUnderkategori.ORDINÆR
                else -> throw IllegalArgumentException("Søknadstype i Søknad må være satt for innsendte søknader: $this")
            }

        fun Map<String, Søknadsfelt<Any>>.erFosterbarn(): Boolean = this[ER_FOSTERBARN]?.bokmålsverdi().tilBoolskSvar()

        fun AndreForelder?.harKryssetForDeltBosted(): Boolean = this?.skriftligAvtaleOmDeltBosted?.bokmålsverdi().tilBoolskSvar()

        fun List<BaSøknaddokumentasjon>.inneholderVedlegg(): Boolean = this.any { it.opplastedeVedlegg.isNotEmpty() }
    }

    @Component
    class Lookup(
        private val søknadMappere: List<SøknadMapper>,
    ) {
        fun hentSøknadMapperForVersjon(søknadVersjon: Int) = søknadMappere.singleOrNull { it.søknadVersjon == søknadVersjon } ?: throw MissingVersionException("Mangler SøknadMapper for versjon $søknadVersjon")
    }
}
