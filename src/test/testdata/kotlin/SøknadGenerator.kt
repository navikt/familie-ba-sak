import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.tilDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori

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
