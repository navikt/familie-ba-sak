package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.internal.TestVerktøyService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.brev.domene.tilBrev
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak.SammensattKontrollsakService
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class DokumentGenereringService(
    private val persongrunnlagService: PersongrunnlagService,
    private val brevService: BrevService,
    private val brevKlient: BrevKlient,
    private val integrasjonClient: IntegrasjonClient,
    private val saksbehandlerContext: SaksbehandlerContext,
    private val sammensattKontrollsakService: SammensattKontrollsakService,
    @Lazy private val testVerktøyService: TestVerktøyService,
    private val personopplysningerService: PersonopplysningerService,
    private val organisasjonService: OrganisasjonService,
) {
    fun genererBrevForVedtak(vedtak: Vedtak): ByteArray {
        try {
            if (!vedtak.behandling.skalBehandlesAutomatisk && vedtak.behandling.steg > StegType.BESLUTTE_VEDTAK) {
                throw FunksjonellFeil("Ikke tillatt å generere brev etter at behandlingen er sendt fra beslutter")
            }

            val sammensattKontrollsak = sammensattKontrollsakService.finnSammensattKontrollsak(vedtak.behandling.id)

            val målform = persongrunnlagService.hentSøkersMålform(vedtak.behandling.id)
            val vedtaksbrev =
                when {
                    sammensattKontrollsak != null -> brevService.hentSammensattKontrollsakBrevdata(vedtak, sammensattKontrollsak)
                    vedtak.behandling.opprettetÅrsak == BehandlingÅrsak.DØDSFALL_BRUKER -> brevService.hentDødsfallbrevData(vedtak)
                    vedtak.behandling.opprettetÅrsak == BehandlingÅrsak.KORREKSJON_VEDTAKSBREV -> brevService.hentKorreksjonbrevData(vedtak)
                    else -> brevService.hentVedtaksbrevData(vedtak)
                }
            return brevKlient.genererBrev(målform.tilSanityFormat(), vedtaksbrev)
        } catch (feil: Throwable) {
            if (feil is FunksjonellFeil) throw feil

            secureLogger.info("Feil ved dokumentgenerering. Genererer hentBegrunnelsetest \n ${testVerktøyService.hentBegrunnelsetest(vedtak.behandling.id)}")
            secureLogger.info("Feil ved dokumentgenerering. Genererer hentVedtaksperioderTest \n ${testVerktøyService.hentVedtaksperioderTest(vedtak.behandling.id)}")

            throw Feil(
                message = "Klarte ikke generere vedtaksbrev på behandling ${vedtak.behandling}: ${feil.message}",
                frontendFeilmelding = "Det har skjedd en feil, og brevet er ikke sendt. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = feil,
            )
        }
    }

    fun genererManueltBrev(
        manueltBrevRequest: ManueltBrevRequest,
        fagsak: Fagsak,
        erForhåndsvisning: Boolean = false,
    ): ByteArray {
        val navnIBrevHeader = finnSøkerEllerInstitusjonsNavn(fagsak)
        try {
            val brev: Brev =
                manueltBrevRequest.tilBrev(
                    navnIBrevHeader,
                    saksbehandlerContext.hentSaksbehandlerSignaturTilBrev(),
                ) { integrasjonClient.hentLandkoderISO2() }
            return brevKlient.genererBrev(
                målform = manueltBrevRequest.mottakerMålform.tilSanityFormat(),
                brev = brev,
            )
        } catch (exception: Exception) {
            if (exception is Feil || exception is FunksjonellFeil) {
                throw exception
            }

            throw Feil(
                message = "Klarte ikke generere brev for ${manueltBrevRequest.brevmal}. ${exception.message}",
                frontendFeilmelding = "${if (erForhåndsvisning) "Det har skjedd en feil" else "Det har skjedd en feil, og brevet er ikke sendt"}. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = exception,
            )
        }
    }

    private fun finnSøkerEllerInstitusjonsNavn(fagsak: Fagsak): String {
        return if (fagsak.type == FagsakType.INSTITUSJON) {
            val orgnummer =
                fagsak.institusjon?.orgNummer
                    ?: throw FunksjonellFeil("Mangler påkrevd variabel orgnummer for institusjon")
            organisasjonService.hentOrganisasjon(orgnummer).navn
        } else {
            personopplysningerService.hentPersoninfoEnkel(fagsak.aktør).navn
                ?: throw Feil("Klarte ikke hente navn på fagsak.aktør fra pdl")
        }
    }
}
