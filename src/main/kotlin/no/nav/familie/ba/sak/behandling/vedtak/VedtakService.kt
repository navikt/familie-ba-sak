package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.beregning.NyBeregning
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.dokument.DokGenKlient
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class VedtakService(private val behandlingService: BehandlingService,
                    private val behandlingRepository: BehandlingRepository,
                    private val vedtakRepository: VedtakRepository,
                    private val vedtakPersonRepository: VedtakPersonRepository,
                    private val dokGenKlient: DokGenKlient,
                    private val personRepository: PersonRepository,
                    private val fagsakService: FagsakService) {

    @Transactional
    fun opphørVedtak(saksbehandler: String,
                     gjeldendeBehandlingsId: Long,
                     nyBehandlingType: BehandlingType,
                     opphørsdato: LocalDate,
                     postProsessor: (Vedtak) -> Unit): Ressurs<Vedtak> {

        val gjeldendeVedtak = vedtakRepository.findByBehandlingAndAktiv(gjeldendeBehandlingsId)
                              ?: return Ressurs.failure("Fant ikke aktivt vedtak tilknyttet behandling $gjeldendeBehandlingsId")

        val gjeldendeVedtakPerson = vedtakPersonRepository.finnPersonBeregningForVedtak(gjeldendeVedtak.id)
        if (gjeldendeVedtakPerson.isEmpty()) {
            return Ressurs.failure(
                    "Fant ikke vedtak personer tilknyttet behandling $gjeldendeBehandlingsId og vedtak ${gjeldendeVedtak.id}")
        }

        val gjeldendeBehandling = gjeldendeVedtak.behandling
        if (!gjeldendeBehandling.aktiv) {
            return Ressurs.failure("Aktivt vedtak er tilknyttet behandling $gjeldendeBehandlingsId som IKKE er aktivt")
        }

        val nyBehandling = Behandling(fagsak = gjeldendeBehandling.fagsak,
                                      journalpostID = null,
                                      type = nyBehandlingType,
                                      kategori = gjeldendeBehandling.kategori,
                                      underkategori = gjeldendeBehandling.underkategori,
                                      resultat = BehandlingResultat.OPPHØRT)

        // Må flushe denne til databasen for å sørge å opprettholde unikhet på (fagsakid,aktiv)
        behandlingRepository.saveAndFlush(gjeldendeBehandling.also { it.aktiv = false })
        behandlingRepository.save(nyBehandling)

        val nyttVedtak = Vedtak(
                ansvarligSaksbehandler = saksbehandler,
                behandling = nyBehandling,
                vedtaksdato = LocalDate.now(),
                forrigeVedtakId = gjeldendeVedtak.id,
                opphørsdato = opphørsdato
        )

        // Trenger ikke flush her fordi det kreves unikhet på (behandlingid,aktiv) og det er ny behandlingsid
        vedtakRepository.save(gjeldendeVedtak.also { it.aktiv = false })
        vedtakRepository.save(nyttVedtak)

        postProsessor(nyttVedtak)

        return Ressurs.success(nyttVedtak)
    }

    @Transactional
    fun lagreEllerOppdaterVedtakForAktivBehandling(behandling: Behandling,
                                                   personopplysningGrunnlag: PersonopplysningGrunnlag,
                                                   restSamletVilkårResultat: List<RestVilkårResultat>,
                                                   ansvarligSaksbehandler: String): Vedtak {
        val forrigeVedtak = hentForrigeVedtak(behandling = behandling)
        val vedtak = Vedtak(
                behandling = behandling,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                vedtaksdato = LocalDate.now(),
                forrigeVedtakId = forrigeVedtak?.id,
                opphørsdato = if (behandling.resultat == BehandlingResultat.OPPHØRT) LocalDate.now().førsteDagINesteMåned() else null,
                stønadBrevMarkdown = if (behandling.resultat != BehandlingResultat.INNVILGET) Result.runCatching {
                    dokGenKlient.hentStønadBrevMarkdown(behandling,
                                                        ansvarligSaksbehandler)
                }
                        .fold(
                                onSuccess = { it },
                                onFailure = { e ->
                                    error("Klart ikke å opprette vedtak på grunn av feil fra dokumentgenerering.")
                                }
                        ) else ""
        )

        return lagreOgDeaktiverGammel(vedtak)
    }


    @Transactional
    fun oppdaterAktivVedtakMedBeregning(vedtak: Vedtak,
                                        personopplysningGrunnlag: PersonopplysningGrunnlag,
                                        nyBeregning: NyBeregning)
            : Ressurs<RestFagsak> {
        nyBeregning.barnasBeregning.map {
            val person =
                    personRepository.findByPersonIdentAndPersonopplysningGrunnlag(PersonIdent(it.ident),
                                                                                  personopplysningGrunnlag.id)
                    ?: error("Barnet du prøver å registrere på vedtaket er ikke tilknyttet behandlingen.")

            if (it.stønadFom.isBefore(person.fødselsdato)) {
                error("Ugyldig fra og med dato for barn med fødselsdato ${person.fødselsdato}")
            }

            val sikkerStønadFom = it.stønadFom.withDayOfMonth(1)
            val sikkerStønadTom = person.fødselsdato.plusYears(18).sisteDagIForrigeMåned()

            if (sikkerStønadTom.isBefore(sikkerStønadFom)) {
                error("Stønadens fra-og-med-dato (${sikkerStønadFom}) er etter til-og-med-dato (${sikkerStønadTom}). ")
            }

            vedtakPersonRepository.save(
                    VedtakPerson(
                            person = person,
                            vedtak = vedtak,
                            beløp = it.beløp,
                            stønadFom = sikkerStønadFom,
                            stønadTom = sikkerStønadTom,
                            type = it.ytelsetype
                    )
            )
        }

        lagreOgDeaktiverGammel(vedtak.copy(
                stønadBrevMarkdown = Result.runCatching {
                    dokGenKlient.hentStønadBrevMarkdown(behandling = vedtak.behandling,
                                                        ansvarligSaksbehandler = vedtak.ansvarligSaksbehandler)
                }
                        .fold(
                                onSuccess = { it },
                                onFailure = { e ->
                                    return Ressurs.failure("Klart ikke å opprette vedtak på grunn av feil fra dokumentgenerering.",
                                                           e)
                                }
                        )
        ))

        return fagsakService.hentRestFagsak(vedtak.behandling.fagsak.id)
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
            vedtakRepository.save(aktivVedtak.also { it.aktiv = false })
        }

        LOG.info("${SikkerhetContext.hentSaksbehandler()} oppretter vedtak $vedtak")
        return vedtakRepository.save(vedtak)
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}


