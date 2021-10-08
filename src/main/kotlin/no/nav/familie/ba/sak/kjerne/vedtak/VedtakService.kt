package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.dokument.DokumentService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class VedtakService(
    private val behandlingService: BehandlingService,
    private val loggService: LoggService,
    private val vedtakRepository: VedtakRepository,
    private val dokumentService: DokumentService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val tilbakekrevingService: TilbakekrevingService,
) {

    fun opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling: Behandling): Vedtak {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)
        loggService.opprettBeslutningOmVedtakLogg(behandling, Beslutning.GODKJENT)

        val vedtak = hentAktivForBehandling(behandlingId = behandling.id)
            ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")
        return oppdaterVedtakMedStønadsbrev(vedtak = vedtak)
    }

    fun hent(vedtakId: Long): Vedtak {
        return vedtakRepository.getById(vedtakId)
    }

    fun hentAktivForBehandling(behandlingId: Long): Vedtak? {
        return vedtakRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun hentAktivForBehandlingThrows(behandlingId: Long): Vedtak {
        return vedtakRepository.findByBehandlingAndAktiv(behandlingId)
            ?: throw Feil("Finner ikke aktivt vedtak på behandling $behandlingId")
    }

    fun oppdater(vedtak: Vedtak): Vedtak {

        return if (vedtakRepository.findByIdOrNull(vedtak.id) != null) {
            vedtakRepository.saveAndFlush(vedtak)
        } else {
            error("Forsøker å oppdatere et vedtak som ikke er lagret")
        }
    }

    fun oppdaterVedtakMedStønadsbrev(vedtak: Vedtak): Vedtak {
        val skalSendesBrev =
            !vedtak.behandling.erTekniskOpphør() &&
                vedtak.behandling.opprettetÅrsak != BehandlingÅrsak.SATSENDRING
        return if (skalSendesBrev) {
            val brev = dokumentService.genererBrevForVedtak(vedtak)
            vedtakRepository.save(vedtak.also { it.stønadBrevPdF = brev })
        } else {
            vedtak
        }
    }

    /**
     * Oppdater vedtaksdato og brev.
     * Vi oppdaterer brevet for å garantere å få riktig beslutter og vedtaksdato.
     */
    fun oppdaterVedtaksdatoOgBrev(vedtak: Vedtak) {
        vedtak.vedtaksdato = LocalDateTime.now()
        oppdaterVedtakMedStønadsbrev(vedtak)

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} beslutter vedtak $vedtak")
    }

    /**
     * Når et vilkår vurderes (endres) vil vi resette steget og slette data som blir generert senere i løypa
     */
    @Transactional
    fun resettStegVedEndringPåVilkår(behandlingId: Long) {
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandlingId,
            steg = StegType.VILKÅRSVURDERING
        )
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandlingId)
    }

    /**
     * Når en andel vurderes (endres) vil vi resette steget og slette data som blir generert senere i løypa
     */
    @Transactional
    fun resettStegVedEndringPåEndredeUtbetalingsperioder(behandlingId: Long) {
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandlingId,
            steg = StegType.BEHANDLINGSRESULTAT
        )
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandlingId)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(VedtakService::class.java)

        data class BrevtekstParametre(
            val gjelderSøker: Boolean = false,
            val barnasFødselsdatoer: List<LocalDate> = emptyList(),
            val månedOgÅrBegrunnelsenGjelderFor: String = "",
            val målform: Målform
        )

        val BrevParameterComparator =
            compareBy<Map.Entry<VedtakBegrunnelseSpesifikasjon, BrevtekstParametre>>(
                { !it.value.gjelderSøker },
                { it.value.barnasFødselsdatoer.isNotEmpty() }
            )
    }
}
