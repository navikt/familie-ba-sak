package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.FagsakRepository
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.NyttVedtak
import no.nav.familie.ba.sak.behandling.restDomene.RestBehandling
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.toRestBehandlingVedtak
import no.nav.familie.ba.sak.behandling.restDomene.toRestFagsak
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakt.Ressurs
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class FagsakService(
        private val behandlingslagerService: BehandlingslagerService,
        private val fagsakRepository: FagsakRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository) {

    fun hentRestFagsak(fagsakId: Long?): Ressurs<RestFagsak> {
        val fagsak = fagsakRepository.finnFagsak(fagsakId)
                ?: return Ressurs.failure("Fant ikke fagsak med fagsakId: $fagsakId")

        val behandlinger = behandlingslagerService.hentBehandlinger(fagsak.id)

        val restBehandlinger: List<RestBehandling> = behandlinger.map {
            val personopplysningGrunnlag = it?.id?.let { it1 -> personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it1) }
            val barnasFødselsnummer = personopplysningGrunnlag?.barna?.map { barn -> barn.personIdent?.ident }
            val vedtakForBehandling = behandlingslagerService.hentVedtakForBehandling( it?.id ).map { behandlingVedtak -> behandlingVedtak?.toRestBehandlingVedtak() }

            RestBehandling(
                    aktiv = it?.aktiv ?: false,
                    behandlingId = it?.id,
                    barnasFødselsnummer = barnasFødselsnummer,
                    vedtakForBehandling = vedtakForBehandling
            )
        }

        return Ressurs.success(data = fagsak.toRestFagsak(restBehandlinger))
    }

    fun hentFagsakForPersonident(personIdent: PersonIdent): Fagsak? {
        return fagsakRepository.finnFagsakForPersonIdent(personIdent)
    }

    fun lagreFagsak(fagsak: Fagsak) {
        fagsakRepository.save(fagsak)
    }

    fun nyttVedtakForAktivBehandling(fagsakId: Long, nyttVedtak: NyttVedtak, ansvarligSaksbehandler: String): BehandlingVedtak {
        val behandling = behandlingslagerService.hentBehandlingHvisEksisterer(fagsakId)
                ?: throw Error("Fant ikke behandling på fagsak $fagsakId")

        val tidligsteStønadFom: LocalDate? = nyttVedtak.barnasBeregning.map { barnBeregning -> barnBeregning.stønadFom }.min()
        val eldsteBarn: LocalDate? = LocalDate.now() // Her må vi ha fødselsdato for barn

        if (tidligsteStønadFom == null || eldsteBarn == null) {
            throw Error("Fant ikke barn i listen over beregninger")
        } else {
            val behandlingVedtak = BehandlingVedtak(
                    behandling = behandling,
                    ansvarligSaksbehandler = ansvarligSaksbehandler,
                    vedtaksdato = LocalDate.now(),
                    stønadFom = tidligsteStønadFom,
                    stønadTom = eldsteBarn.plusYears(18),
                    stønadBrevMarkdown = "" // TODO hent markdown fra dokgen
            )

            behandlingslagerService.lagreBehandlingVedtak(behandlingVedtak)
            return behandlingVedtak
        }
    }
}