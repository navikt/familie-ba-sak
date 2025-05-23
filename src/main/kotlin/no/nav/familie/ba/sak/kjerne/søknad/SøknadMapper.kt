package no.nav.familie.ba.sak.kjerne.søknad

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknad
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype
import org.springframework.stereotype.Component

interface SøknadMapper {
    val søknadVersjon: Int

    fun mapTilSøknad(versjonertBarnetrygdSøknad: VersjonertBarnetrygdSøknad): Søknad

    companion object {
        fun Søknadstype.tilBehandlingUnderkategori(): BehandlingUnderkategori =
            when (this) {
                Søknadstype.UTVIDET -> BehandlingUnderkategori.UTVIDET
                Søknadstype.ORDINÆR -> BehandlingUnderkategori.ORDINÆR
                else -> throw IllegalArgumentException("Søknadstype i Søknad må være satt for innsendte søknader: $this")
            }
    }

    @Component
    class Lookup(
        private val søknadMappere: List<SøknadMapper>,
    ) {
        fun hentMapperForSøknadVersjon(søknadVersjon: Int) = søknadMappere.single { it.søknadVersjon == søknadVersjon }
    }
}
