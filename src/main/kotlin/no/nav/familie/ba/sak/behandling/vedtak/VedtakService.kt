package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestStønadBrevBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.toRestStønadBrevBegrunnelse
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils.midlertidigUtledBehandlingResultatType
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_NAVN
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class VedtakService(private val arbeidsfordelingService: ArbeidsfordelingService,
                    private val behandlingService: BehandlingService,
                    private val behandlingRepository: BehandlingRepository,
                    private val behandlingResultatService: BehandlingResultatService,
                    private val loggService: LoggService,
                    private val vedtakRepository: VedtakRepository,
                    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
                    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
                    private val dokumentService: DokumentService,
                    private val totrinnskontrollService: TotrinnskontrollService) {

    @Transactional
    fun opphørVedtak(saksbehandler: String,
                     gjeldendeBehandlingsId: Long,
                     nyBehandlingType: BehandlingType,
                     opphørsdato: LocalDate,
                     postProsessor: (Vedtak) -> Unit): Ressurs<Vedtak> {

        val gjeldendeVedtak = vedtakRepository.findByBehandlingAndAktiv(gjeldendeBehandlingsId)
                              ?: return Ressurs.failure("Fant ikke aktivt vedtak tilknyttet behandling $gjeldendeBehandlingsId")

        val gjeldendeAndelerTilkjentYtelse =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(gjeldendeBehandlingsId))
        if (gjeldendeAndelerTilkjentYtelse.isEmpty()) {
            return Ressurs.failure(
                    "Fant ikke andeler tilkjent ytelse tilknyttet behandling $gjeldendeBehandlingsId")
        }

        val gjeldendeBehandling = gjeldendeVedtak.behandling
        if (!gjeldendeBehandling.aktiv) {
            return Ressurs.failure("Aktivt vedtak er tilknyttet behandling $gjeldendeBehandlingsId som IKKE er aktivt")
        }

        val nyBehandling = Behandling(fagsak = gjeldendeBehandling.fagsak,
                                      type = nyBehandlingType,
                                      kategori = gjeldendeBehandling.kategori,
                                      underkategori = gjeldendeBehandling.underkategori,
                                      opprinnelse = BehandlingOpprinnelse.MANUELL)

        // Må flushe denne til databasen for å sørge å opprettholde unikhet på (fagsakid,aktiv)
        behandlingRepository.saveAndFlush(gjeldendeBehandling.also { it.aktiv = false })
        behandlingRepository.save(nyBehandling)
        loggService.opprettBehandlingLogg(nyBehandling)

        val nyttVedtak = Vedtak(
                behandling = nyBehandling,
                vedtaksdato = LocalDate.now(),
                forrigeVedtakId = gjeldendeVedtak.id,
                opphørsdato = opphørsdato
        )

        // Trenger ikke flush her fordi det kreves unikhet på (behandlingid,aktiv) og det er ny behandlingsid
        vedtakRepository.save(gjeldendeVedtak.also { it.aktiv = false })
        vedtakRepository.save(nyttVedtak)

        val nyTilkjentYtelse = TilkjentYtelse(
                behandling = nyBehandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now()
        )
        tilkjentYtelseRepository.save(nyTilkjentYtelse)

        totrinnskontrollService.opprettEllerHentTotrinnskontroll(nyBehandling, saksbehandler)
        totrinnskontrollService.besluttTotrinnskontroll(nyBehandling, SYSTEM_NAVN, Beslutning.GODKJENT)

        behandlingRepository.save(nyBehandling.also { it.steg = StegType.FERDIGSTILLE_BEHANDLING })

        postProsessor(nyttVedtak)

        return Ressurs.success(nyttVedtak)
    }

    @Transactional
    fun lagreEllerOppdaterVedtakForAktivBehandling(behandling: Behandling,
                                                   personopplysningGrunnlag: PersonopplysningGrunnlag): Vedtak {
        val forrigeVedtak = hentForrigeVedtak(behandling = behandling)

        // TODO: Midlertidig fiks før støtte for delvis innvilget
        val behandlingResultatType = midlertidigUtledBehandlingResultatType(
                hentetBehandlingResultatType = behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandling.id))
        //val behandlingResultatType = behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandling.id)

        val vedtak = Vedtak(
                behandling = behandling,
                forrigeVedtakId = forrigeVedtak?.id,
                ansvarligEnhet = arbeidsfordelingService.bestemBehandlendeEnhet(behandling),
                opphørsdato = if (behandlingResultatType == BehandlingResultatType.OPPHØRT) LocalDate.now()
                        .førsteDagINesteMåned() else null
        )

        return lagreOgDeaktiverGammel(vedtak)
    }


    @Transactional
    fun oppdaterVedtakMedStønadsbrev(vedtak: Vedtak) {
        vedtak.stønadBrevPdF = dokumentService.genererBrevForVedtak(vedtak)

        lagreOgDeaktiverGammel(vedtak)
    }

    @Transactional
    fun leggTilStønadBrevBegrunnelse(restStønadBrevBegrunnelse: RestStønadBrevBegrunnelse, fagsakId: Long): List<RestStønadBrevBegrunnelse> {
        val behandling: Behandling = behandlingService.hentAktivForFagsak(fagsakId)
                                     ?: throw Feil(message = "Finner ikke aktiv behandling på fagsak")

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
                     ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        val begrunnelse =
                StønadBrevBegrunnelse(vedtak = vedtak,
                                      fom = restStønadBrevBegrunnelse.fom,
                                      tom = restStønadBrevBegrunnelse.tom,
                                      begrunnelse = restStønadBrevBegrunnelse.begrunnelse,
                                      årsak = restStønadBrevBegrunnelse.årsak)

        vedtak.addStønadBrevBegrunnelse(begrunnelse)

        lagreEllerOppdater(vedtak)

        return vedtak.stønadBrevBegrunnelser.map{
            it.toRestStønadBrevBegrunnelse();
        }
    }

    fun hentForrigeVedtak(behandling: Behandling): Vedtak? {
        val behandlinger = behandlingService.hentBehandlinger(behandling.fagsak.id)


        return when (val forrigeBehandling = behandlinger.filter { it.id != behandling.id }.maxBy { it.opprettetTidspunkt }) {
            null -> null
            else -> hentAktivForBehandling(behandlingId = forrigeBehandling.id)
        }
    }

    fun hent(vedtakId: Long): Vedtak {
        return vedtakRepository.getOne(vedtakId)
    }

    fun hentAktivForBehandling(behandlingId: Long): Vedtak? {
        return vedtakRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun lagreOgDeaktiverGammel(vedtak: Vedtak): Vedtak {
        val aktivVedtak = hentAktivForBehandling(vedtak.behandling.id)

        if (aktivVedtak != null && aktivVedtak.id != vedtak.id) {
            vedtakRepository.saveAndFlush(aktivVedtak.also { it.aktiv = false })
        }

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vedtak $vedtak")
        return vedtakRepository.save(vedtak)
    }

    fun lagreEllerOppdater(vedtak: Vedtak): Vedtak {
        return vedtakRepository.save(vedtak)
    }

    fun besluttVedtak(vedtak: Vedtak) {
        vedtak.vedtaksdato = LocalDate.now()
        oppdaterVedtakMedStønadsbrev(vedtak)

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} beslutter vedtak $vedtak")
        lagreEllerOppdater(vedtak)
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}


