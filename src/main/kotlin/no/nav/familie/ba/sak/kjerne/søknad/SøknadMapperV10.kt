package no.nav.familie.ba.sak.kjerne.søknad

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.søknad.SøknadMapper.Companion.tilBehandlingUnderkategori
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknad
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknadV10
import no.nav.familie.kontrakter.ba.søknad.v9.SøknadsFeltId
import no.nav.familie.kontrakter.ba.søknad.v9.hentVerdiForSøknadsfelt
import no.nav.familie.kontrakter.ba.søknad.v9.tilBoolskSvar
import org.springframework.stereotype.Component

@Component
class SøknadMapperV10 : SøknadMapper {
    override val søknadVersjon: Int = 10

    override fun mapTilSøknad(versjonertBarnetrygdSøknad: VersjonertBarnetrygdSøknad): Søknad {
        if (versjonertBarnetrygdSøknad !is VersjonertBarnetrygdSøknadV10) {
            throw IllegalArgumentException("Kan ikke mappe søknad av type ${versjonertBarnetrygdSøknad::class.simpleName} til versjon 10")
        }

        return Søknad(
            søker =
                Søker(
                    fnr = versjonertBarnetrygdSøknad.barnetrygdSøknad.søker.fnr,
                    planleggerÅBoINorge12Mnd =
                        versjonertBarnetrygdSøknad.barnetrygdSøknad.søker.spørsmål
                            .hentVerdiForSøknadsfelt(SøknadsFeltId.PLANLEGGER_Å_BO_I_NORGE_12_MND_SØKER)
                            .tilBoolskSvar(),
                ),
            barn =
                versjonertBarnetrygdSøknad.barnetrygdSøknad.barn
                    .map {
                        Barn(
                            fnr = it.fnr,
                            planleggerÅBoINorge12Mnd = it.spørsmål.hentVerdiForSøknadsfelt(SøknadsFeltId.PLANLEGGER_Å_BO_I_NORGE_12_MND_BARN).tilBoolskSvar(),
                        )
                    },
            behandlingUnderkategori = versjonertBarnetrygdSøknad.barnetrygdSøknad.søknadstype.tilBehandlingUnderkategori(),
            behandlingKategori =
                versjonertBarnetrygdSøknad.barnetrygdSøknad.antallEøsSteg
                    .takeIf { it > 0 }
                    ?.let { BehandlingKategori.EØS }
                    ?: BehandlingKategori.NASJONAL,
            målform =
                when (versjonertBarnetrygdSøknad.barnetrygdSøknad.originalSpråk) {
                    "nb" -> Målform.NB
                    "nn" -> Målform.NN
                    else -> Målform.NB
                },
        )
    }
}
