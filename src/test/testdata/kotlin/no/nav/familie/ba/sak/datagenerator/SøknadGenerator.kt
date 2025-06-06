package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.tilDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.søknad.Barn
import no.nav.familie.ba.sak.kjerne.søknad.Søker
import no.nav.familie.ba.sak.kjerne.søknad.Søknad

fun lagSøknadDTO(
    søkerIdent: String,
    barnasIdenter: List<String>,
    underkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
): SøknadDTO =
    SøknadDTO(
        underkategori = underkategori.tilDto(),
        søkerMedOpplysninger =
            SøkerMedOpplysninger(
                ident = søkerIdent,
            ),
        barnaMedOpplysninger =
            barnasIdenter.map {
                BarnMedOpplysninger(
                    ident = it,
                )
            },
        endringAvOpplysningerBegrunnelse = "",
    )

fun lagSøknad(
    søkerPlanleggerÅBoINorge12Mnd: Boolean = true,
    barneIdenterTilPlanleggerBoINorge12Mnd: Map<String, Boolean> = mapOf(randomFnr() to true),
): Søknad =
    Søknad(
        søker = Søker(fnr = randomFnr(), planleggerÅBoINorge12Mnd = søkerPlanleggerÅBoINorge12Mnd),
        barn = barneIdenterTilPlanleggerBoINorge12Mnd.map { (fnr, planleggerÅBoINorge12Mnd) -> Barn(fnr, planleggerÅBoINorge12Mnd) },
        behandlingKategori = BehandlingKategori.NASJONAL,
        behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
        målform = Målform.NB,
    )
