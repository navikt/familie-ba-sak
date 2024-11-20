package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import org.springframework.stereotype.Service

@Service
class OpprettGrunnlagOgSignaturDataService(
    private val persongrunnlagService: PersongrunnlagService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val saksbehandlerContext: SaksbehandlerContext,
) {
    fun opprett(vedtak: Vedtak): GrunnlagOgSignaturData {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(vedtak.behandling.id)
        val (saksbehandler, beslutter) =
            hentSaksbehandlerOgBeslutter(
                behandling = vedtak.behandling,
                totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(vedtak.behandling.id),
            )
        val enhet = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(vedtak.behandling.id).behandlendeEnhetNavn
        return GrunnlagOgSignaturData(
            grunnlag = personopplysningGrunnlag,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            enhet = enhet,
        )
    }

    fun hentSaksbehandlerOgBeslutter(
        behandling: Behandling,
        totrinnskontroll: Totrinnskontroll?,
    ): Pair<String, String> =
        when {
            behandling.steg <= StegType.SEND_TIL_BESLUTTER || totrinnskontroll == null -> {
                Pair(saksbehandlerContext.hentSaksbehandlerSignaturTilBrev(), "Beslutter")
            }

            totrinnskontroll.erBesluttet() -> {
                Pair(totrinnskontroll.saksbehandler, totrinnskontroll.beslutter!!)
            }

            behandling.steg == StegType.BESLUTTE_VEDTAK -> {
                Pair(
                    totrinnskontroll.saksbehandler,
                    if (totrinnskontroll.saksbehandler == saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()) {
                        "Beslutter"
                    } else {
                        saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()
                    },
                )
            }

            else -> {
                throw Feil("Prøver å hente saksbehandler og beslutters navn for generering av brev i en ukjent tilstand.")
            }
        }
}

class GrunnlagOgSignaturData(
    val grunnlag: PersonopplysningGrunnlag,
    val saksbehandler: String,
    val beslutter: String,
    val enhet: String,
)
