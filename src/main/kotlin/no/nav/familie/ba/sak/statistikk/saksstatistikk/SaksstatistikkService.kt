package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import no.nav.familie.ba.sak.integrasjoner.journalføring.JournalføringService
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpostType
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.HENLAGT_FEILAKTIG_OPPRETTET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.HENLAGT_SØKNAD_TRUKKET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.eksterne.kontrakter.saksstatistikk.AktørDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.ResultatBegrunnelseDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@Service
class SaksstatistikkService(
    private val behandlingService: BehandlingService,
    private val journalføringRepository: JournalføringRepository,
    private val journalføringService: JournalføringService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val fagsakService: FagsakService,
    private val personopplysningerService: PersonopplysningerService,
    private val persongrunnlagService: PersongrunnlagService,
    private val vedtaksperiodeService: VedtaksperiodeService,
) {

    fun mapTilBehandlingDVH(behandlingId: Long): BehandlingDVH? {
        val behandling = behandlingService.hent(behandlingId)
        val forrigeBehandlingId = behandlingService.hentForrigeBehandlingSomErVedtatt(behandling)
            .takeIf { erRevurderingEllerTekniskBehandling(behandling) }?.id

        val datoMottatt = when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.SØKNAD -> {
                val journalpost = journalføringRepository
                    .findByBehandlingId(behandlingId)
                    .filter { it.type == DbJournalpostType.I }
                journalpost.map { journalføringService.hentJournalpost(it.journalpostId) }
                    .filter { it.tittel != null && it.tittel!!.contains("søknad", ignoreCase = true) }
                    .mapNotNull { it.datoMottatt }
                    .minOrNull() ?: behandling.opprettetTidspunkt
            }
            else -> behandling.opprettetTidspunkt
        }

        val behandlendeEnhetsKode =
            arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId).behandlendeEnhetId
        val ansvarligEnhetKode = arbeidsfordelingService.hentArbeidsfordelingsenhet(behandling).enhetId

        val aktivtVedtak = vedtakService.hentAktivForBehandling(behandlingId)
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId)

        val now = ZonedDateTime.now()
        return BehandlingDVH(
            funksjonellTid = now,
            tekniskTid = now,
            mottattDato = datoMottatt.atZone(TIMEZONE),
            registrertDato = datoMottatt.atZone(TIMEZONE),
            behandlingId = behandling.id.toString(),
            funksjonellId = UUID.randomUUID().toString(),
            sakId = behandling.fagsak.id.toString(),
            behandlingType = behandling.type.name,
            behandlingStatus = behandling.status.name,
            behandlingKategori = behandling.underkategori.name, // Gjøres pga. tilpasning til DVH-modell
            behandlingAarsak = behandling.opprettetÅrsak.name,
            automatiskBehandlet = behandling.skalBehandlesAutomatisk,
            utenlandstilsnitt = behandling.kategori.name, // Gjøres pga. tilpasning til DVH-modell
            ansvarligEnhetKode = ansvarligEnhetKode,
            behandlendeEnhetKode = behandlendeEnhetsKode,
            ansvarligEnhetType = "NORG",
            behandlendeEnhetType = "NORG",
            totrinnsbehandling = !behandling.skalBehandlesAutomatisk,
            avsender = "familie-ba-sak",
            versjon = hentPropertyFraMaven("familie.kontrakter.saksstatistikk") ?: "2",
            // Ikke påkrevde felt
            vedtaksDato = aktivtVedtak?.vedtaksdato?.toLocalDate(),
            relatertBehandlingId = forrigeBehandlingId?.toString(),
            vedtakId = aktivtVedtak?.id?.toString(),
            resultat = behandling.resultat.name,
            behandlingTypeBeskrivelse = behandling.type.visningsnavn,
            resultatBegrunnelser = behandling.resultatBegrunnelser(),
            behandlingOpprettetAv = behandling.opprettetAv,
            behandlingOpprettetType = "saksbehandlerId",
            behandlingOpprettetTypeBeskrivelse = "saksbehandlerId. VL ved automatisk behandling",
            beslutter = totrinnskontroll?.beslutterId,
            saksbehandler = totrinnskontroll?.saksbehandlerId
        )
    }

    fun mapTilSakDvh(sakId: Long): SakDVH? {
        val fagsak = fagsakService.hentPåFagsakId(sakId)
        val aktivBehandling = behandlingService.hentAktivForFagsak(fagsakId = fagsak.id)

        var landkodeSøker: String = PersonopplysningerService.UKJENT_LANDKODE

        val deltagere = if (aktivBehandling != null) {
            val personer = persongrunnlagService.hentAktiv(behandlingId = aktivBehandling.id)?.søkerOgBarn ?: emptySet()
            personer.map {
                if (it.type == PersonType.SØKER) {
                    landkodeSøker = hentLandkode(it)
                }
                AktørDVH(
                    it.aktør.aktørId.toLong(),
                    it.type.name
                )
            }
        } else {

            landkodeSøker = hentLandkode(fagsak.aktør)
            listOf(AktørDVH(fagsak.aktør.aktørId.toLong(), PersonType.SØKER.name))
        }

        return SakDVH(
            funksjonellTid = ZonedDateTime.now(),
            tekniskTid = ZonedDateTime.now(),
            opprettetDato = LocalDate.now(),
            funksjonellId = UUID.randomUUID().toString(),
            sakId = sakId.toString(),
            aktorId = fagsak.aktør.aktørId.toLong(),
            aktorer = deltagere,
            sakStatus = fagsak.status.name,
            avsender = "familie-ba-sak",
            versjon = hentPropertyFraMaven("familie.kontrakter.saksstatistikk") ?: "2",
            bostedsland = landkodeSøker,
        )
    }

    private fun hentLandkode(person: Person): String {
        return if (person.bostedsadresser.isNotEmpty()) "NO" else {
            personopplysningerService.hentLandkodeUtenlandskBostedsadresse(
                person.aktør
            )
        }
    }

    private fun hentLandkode(aktør: Aktør): String {
        val personInfo = personopplysningerService.hentPersoninfoEnkel(aktør)

        return if (personInfo.bostedsadresser.isNotEmpty()) "NO" else {
            personopplysningerService.hentLandkodeUtenlandskBostedsadresse(aktør)
        }
    }

    private fun erRevurderingEllerTekniskBehandling(behandling: Behandling) =
        behandling.type == BehandlingType.REVURDERING || behandling.type == BehandlingType.TEKNISK_OPPHØR || behandling.type == BehandlingType.TEKNISK_ENDRING

    private fun Behandling.resultatBegrunnelser(): List<ResultatBegrunnelseDVH> {
        return when (resultat) {
            HENLAGT_SØKNAD_TRUKKET, HENLAGT_FEILAKTIG_OPPRETTET -> emptyList()
            else -> vedtakService.hentAktivForBehandling(behandlingId = id)
                ?.hentResultatBegrunnelserFraVedtaksbegrunnelser()
                ?: emptyList()
        }
    }

    private fun Vedtak.hentResultatBegrunnelserFraVedtaksbegrunnelser(): List<ResultatBegrunnelseDVH> {
        return vedtaksperiodeService.hentPersisterteVedtaksperioder(this)
            .flatMap { vedtaksperiode ->
                vedtaksperiode.begrunnelser
                    .map {
                        ResultatBegrunnelseDVH(
                            fom = vedtaksperiode.fom,
                            tom = vedtaksperiode.tom,
                            type = it.vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType.name,
                            vedtakBegrunnelse = it.vedtakBegrunnelseSpesifikasjon.name,
                        )
                    }
            }
    }

    companion object {

        val TIMEZONE: ZoneId = ZoneId.systemDefault()
    }
}
