package no.nav.familie.ba.sak.saksstatistikk

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType.*
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.journalføring.JournalføringService
import no.nav.familie.ba.sak.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.nare.Resultat.NEI
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_NAVN
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.eksterne.kontrakter.saksstatistikk.AktørDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.ResultatBegrunnelseDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime


@Service
class SaksstatistikkService(private val behandlingService: BehandlingService,
                            private val behandlingResultatService: BehandlingResultatService,
                            private val journalføringRepository: JournalføringRepository,
                            private val journalføringService: JournalføringService,
                            private val arbeidsfordelingService: ArbeidsfordelingService,
                            private val totrinnskontrollService: TotrinnskontrollService,
                            private val vedtakService: VedtakService,
                            private val fagsakService: FagsakService,
                            private val personopplysningerService: PersonopplysningerService,
                            private val persongrunnlagService: PersongrunnlagService,
                            private val featureToggleService: FeatureToggleService) {

    fun mapTilBehandlingDVH(behandlingId: Long, forrigeBehandlingId: Long? = null): BehandlingDVH? {
        val behandling = behandlingService.hent(behandlingId)

        if (behandling.skalBehandlesAutomatisk && fødselshendelseSkalRullesTilbake()) return null

        val behandlingResultat = behandlingResultatService.hentAktivForBehandling(behandlingId)

        val datoMottatt = when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.SØKNAD -> {
                val journalpost = journalføringRepository.findByBehandlingId(behandlingId)
                journalpost.mapNotNull { journalføringService.hentJournalpost(it.journalpostId).data }
                        .filter { it.journalposttype == Journalposttype.I }
                        .filter { it.tittel != null && it.tittel!!.contains("søknad", ignoreCase = true) }
                        .mapNotNull { it.datoMottatt }
                        .minOrNull() ?: behandling.opprettetTidspunkt
            }
            else -> behandling.opprettetTidspunkt
        }

        val behandlendeEnhetsKode = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId).behandlendeEnhetId
        val ansvarligEnhetKode = arbeidsfordelingService.hentArbeidsfordelingsenhet(behandling).enhetId

        val aktivtVedtak = vedtakService.hentAktivForBehandling(behandlingId)
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId)

        val now = ZonedDateTime.now()
        return BehandlingDVH(funksjonellTid = now,
                             tekniskTid = now, // TODO burde denne vært satt til opprettetTidspunkt/endretTidspunkt?
                             mottattDato = datoMottatt.atZone(TIMEZONE),
                             registrertDato = datoMottatt.atZone(TIMEZONE),
                             behandlingId = behandling.id.toString(),
                             sakId = behandling.fagsak.id.toString(),
                             behandlingType = behandling.type.name,
                             behandlingStatus = behandling.status.name,
                             behandlingKategori = behandling.kategori.name,
                             behandlingUnderkategori = behandling.underkategori.name,
                             utenlandstilsnitt = "NASJONAL",
                             ansvarligEnhetKode = ansvarligEnhetKode,
                             behandlendeEnhetKode = behandlendeEnhetsKode,
                             ansvarligEnhetType = "NORG",
                             behandlendeEnhetType = "NORG",
                             totrinnsbehandling = totrinnskontroll?.saksbehandler != SYSTEM_NAVN,
                             avsender = "familie-ba-sak",
                             versjon = hentPropertyFraMaven("familie.kontrakter.saksstatistikk") ?: "2",
                // Ikke påkrevde felt
                             vedtaksDato = aktivtVedtak?.vedtaksdato,
                             relatertBehandlingId = forrigeBehandlingId?.toString(),
                             vedtakId = aktivtVedtak?.id?.toString(),
                             resultat = behandlingResultat?.samletResultat?.name,
                             behandlingTypeBeskrivelse = behandling.type.visningsnavn,
                             resultatBegrunnelser = behandlingResultat?.samletResultatBegrunnelser() ?: emptyList(),
                             behandlingOpprettetAv = behandling.opprettetAv,
                             behandlingOpprettetType = "saksbehandlerId",
                             behandlingOpprettetTypeBeskrivelse = "saksbehandlerId. VL ved automatisk behandling",
                             beslutter = totrinnskontroll?.beslutter,
                             saksbehandler = totrinnskontroll?.saksbehandler
        )
    }

    fun mapTilSakDvh(sakId: Long): SakDVH? {
        val fagsak = fagsakService.hentRestFagsak(sakId).getDataOrThrow()
        val aktivBehandling = behandlingService.hentAktivForFagsak(fagsakId = fagsak.id)
        //Skipper saker som har automatisk behandling
        val skalBehandleAutomatisk = aktivBehandling?.skalBehandlesAutomatisk ?: false
        if (skalBehandleAutomatisk && fødselshendelseSkalRullesTilbake()) return null

        val søkersAktørId = personopplysningerService.hentAktivAktørId(Ident(fagsak.søkerFødselsnummer))

        val deltagere = if (aktivBehandling != null) {
            val personer = persongrunnlagService.hentAktiv(aktivBehandling.id)?.personer ?: emptySet()
            personer.map {
                AktørDVH(personopplysningerService.hentAktivAktørId(Ident(it.personIdent.ident)).id.toLong(),
                         it.type.name)
            }
        } else {
            listOf(AktørDVH(personopplysningerService.hentAktivAktørId(Ident(fagsak.søkerFødselsnummer)).id.toLong(),
                            PersonType.SØKER.name))
        }

        return SakDVH(
                funksjonellTid = ZonedDateTime.now(),
                tekniskTid = ZonedDateTime.now(),
                opprettetDato = LocalDate.now(),
                sakId = sakId.toString(),
                aktorId = søkersAktørId.id.toLong(),
                aktorer = deltagere,
                sakStatus = fagsak.status.name,
                avsender = "familie-ba-sak",
                versjon = hentPropertyFraMaven("familie.kontrakter.saksstatistikk") ?: "2",
        )
    }

    private fun BehandlingResultat.samletResultatBegrunnelser(): List<ResultatBegrunnelseDVH> {
        return when (samletResultat) {
            IKKE_VURDERT -> emptyList()
            AVSLÅTT -> finnÅrsakerTilAvslag()
            DELVIS_INNVILGET -> TODO()
            HENLAGT -> TODO()
            OPPHØRT -> TODO()
            INNVILGET -> listOf(ResultatBegrunnelseDVH("Alle vilkår er oppfylt",
                                                       "Vilkår vurdert for søker: ${Vilkår.hentVilkårFor(PersonType.SØKER)}\n" +
                                                       "Vilkår vurdert for barn: ${
                                                           Vilkår.hentVilkårFor(PersonType.BARN).toMutableList().apply {
                                                               if (behandling.skalBehandlesAutomatisk) this.remove(Vilkår.LOVLIG_OPPHOLD)
                                                           }
                                                       }"))
        }
    }

    private fun BehandlingResultat.finnÅrsakerTilAvslag(): List<ResultatBegrunnelseDVH> {
        val søker = persongrunnlagService.hentSøker(behandling)?.personIdent?.ident
        val barna = persongrunnlagService.hentBarna(behandling).map { it.personIdent.ident }

        val søkerResultatNei = personResultater.find { it.personIdent == søker }
                ?.vilkårResultater?.filter { it.resultat == NEI }

        if (!søkerResultatNei.isNullOrEmpty()) {
            return søkerResultatNei.map { ResultatBegrunnelseDVH("${it.vilkårType.name} ikke oppfylt for søker") }
        } else {

            val negativeVilkårResultater = Vilkår.values().map { it to mutableListOf<String>() }.toMap()

            personResultater.filter { barna.contains(it.personIdent) }
                    .forEach { personResultat ->
                        personResultat.vilkårResultater.filter { it.resultat == NEI }
                                .forEach { vilkårResultatNei ->
                                    negativeVilkårResultater[vilkårResultatNei.vilkårType]!!.add(personResultat.personIdent)
                                }
                    }

            return negativeVilkårResultater.filterValues { it.isNotEmpty() }.map { (negativtVilkår, personer) ->
                ResultatBegrunnelseDVH("${negativtVilkår.name} ikke oppfylt for barn ${Utils.slåSammen(personer)}")
            }
        }
    }

    private fun fødselshendelseSkalRullesTilbake(): Boolean =
            featureToggleService.isEnabled("familie-ba-sak.rollback-automatisk-regelkjoring", defaultValue = true)

    companion object {

        val TIMEZONE = ZoneId.of("Europe/Paris")
    }
}