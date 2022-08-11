package no.nav.familie.ba.sak.kjerne.logg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Identkonverterer
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.foedselsnummer.FoedselsNr
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class LoggService(
    private val loggRepository: LoggRepository,
    private val rolleConfig: RolleConfig
) {

    private val metrikkPerLoggType: Map<LoggType, Counter> = LoggType.values().associateWith {
        Metrics.counter(
            "behandling.logg",
            "type",
            it.name,
            "beskrivelse",
            it.visningsnavn
        )
    }

    fun opprettBehandlendeEnhetEndret(
        behandling: Behandling,
        fraEnhet: Arbeidsfordelingsenhet,
        tilEnhet: ArbeidsfordelingPåBehandling,
        manuellOppdatering: Boolean,
        begrunnelse: String
    ) {
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLENDE_ENHET_ENDRET,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = "Behandlende enhet ${if (manuellOppdatering) "manuelt" else "automatisk"} endret fra " +
                    "${fraEnhet.enhetId} ${fraEnhet.enhetNavn} til ${tilEnhet.behandlendeEnhetId} ${tilEnhet.behandlendeEnhetNavn}." +
                    if (begrunnelse.isNotBlank()) "\n\n$begrunnelse" else ""
            )
        )
    }

    fun opprettMottattDokument(behandling: Behandling, tekst: String, mottattDato: LocalDateTime) {
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.DOKUMENT_MOTTATT,
                tittel = "Dokument mottatt ${mottattDato.toLocalDate().tilKortString()}",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = tekst
            )
        )
    }

    fun opprettRegistrerInstitusjonLogg(behandling: Behandling) {
        val tittel = "institusjon ble registrert"
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.INSTITUSJON_REGISTRERT,
                tittel = tittel,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = ""
            )
        )
    }

    fun opprettRegistrerVergeLogg(behandling: Behandling) {
        val tittel = "verge ble registrert"
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.VERGE_REGISTRERT,
                tittel = tittel,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = ""
            )
        )
    }

    fun opprettRegistrertSøknadLogg(behandling: Behandling, søknadFinnesFraFør: Boolean) {
        val tittel = if (!søknadFinnesFraFør) "Søknaden ble registrert" else "Søknaden ble endret"
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.SØKNAD_REGISTRERT,
                tittel = tittel,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = ""
            )
        )
    }

    fun opprettEndretBehandlingstema(
        behandling: Behandling,
        forrigeUnderkategori: BehandlingUnderkategori,
        forrigeKategori: BehandlingKategori,
        nyUnderkategori: BehandlingUnderkategori,
        nyKategori: BehandlingKategori
    ) {
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLINGSTYPE_ENDRET,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = "Behandlingstema er manuelt endret fra ${
                tilBehandlingstema(
                    underkategori = forrigeUnderkategori,
                    kategori = forrigeKategori
                )
                } til ${tilBehandlingstema(underkategori = nyUnderkategori, kategori = nyKategori)}"
            )
        )
    }

    fun opprettVilkårsvurderingLogg(
        behandling: Behandling,
        forrigeBehandlingsresultat: Behandlingsresultat,
        nyttBehandlingsresultat: Behandlingsresultat
    ): Logg? {

        val tekst = when {
            forrigeBehandlingsresultat == Behandlingsresultat.IKKE_VURDERT -> {
                "Resultat ble ${nyttBehandlingsresultat.displayName.lowercase()}"
            }

            forrigeBehandlingsresultat != nyttBehandlingsresultat -> {
                "Resultat gikk fra ${forrigeBehandlingsresultat.displayName.lowercase()} til ${nyttBehandlingsresultat.displayName.lowercase()}"
            }

            else -> return null
        }

        return lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.VILKÅRSVURDERING,
                tittel = if (forrigeBehandlingsresultat != Behandlingsresultat.IKKE_VURDERT) "Vilkårsvurdering endret" else "Vilkårsvurdering gjennomført",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = tekst
            )
        )
    }

    fun opprettAutovedtakTilManuellBehandling(behandling: Behandling, tekst: String) {
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.AUTOVEDTAK_TIL_MANUELL_BEHANDLING,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = tekst
            )
        )
    }

    private fun opprettLivshendelseLogg(behandling: BehandlingLoggRequest, tittel: String) {
        lagre(
            Logg(
                behandlingId = behandling.behandling.id,
                type = LoggType.LIVSHENDELSE,
                tittel = tittel,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = "Gjelder barn ${fødselsdatoer(behandling)}"
            )
        )
    }

    private fun fødselsdatoer(behandling: BehandlingLoggRequest) = Utils.slåSammen(
        behandling.barnasIdenter
            .filter { Identkonverterer.er11Siffer(it) }
            .distinct()
            .map { FoedselsNr(it) }
            .map { it.foedselsdato }
            .map { it.tilKortString() }
    )

    fun opprettBehandlingLogg(behandlingLogg: BehandlingLoggRequest) {
        val behandling = behandlingLogg.behandling
        if (behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            opprettLivshendelseLogg(behandling = behandlingLogg, tittel = "Mottok fødselshendelse")
        } else if (behandling.skalBehandlesAutomatisk && behandling.erSmåbarnstillegg()) {
            opprettLivshendelseLogg(behandling = behandlingLogg, tittel = "Mottok overgansstønadshendelse")
        }

        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLING_OPPRETTET,
                tittel = "${behandling.type.visningsnavn} opprettet",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = ""
            )
        )
    }

    fun opprettSendTilBeslutterLogg(behandling: Behandling) {
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = if (behandling.erManuellMigrering()) LoggType.SEND_TIL_SYSTEM else LoggType.SEND_TIL_BESLUTTER,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                )
            )
        )
    }

    fun opprettBeslutningOmVedtakLogg(behandling: Behandling, beslutning: Beslutning, begrunnelse: String? = null) {
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = if (behandling.erManuellMigrering()) LoggType.MIGRERING_BEKREFTET else LoggType.GODKJENNE_VEDTAK,
                tittel = if (beslutning.erGodkjent()) {
                    if (behandling.erManuellMigrering()) "Migrering bekreftet" else "Vedtak godkjent"
                } else "Vedtak underkjent",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.BESLUTTER),
                tekst = if (!beslutning.erGodkjent()) "Begrunnelse: $begrunnelse" else "",
                opprettetAv = if (behandling.erManuellMigrering()) SikkerhetContext.SYSTEM_NAVN else
                    SikkerhetContext.hentSaksbehandlerNavn()
            )
        )
    }

    fun opprettDistribuertBrevLogg(behandlingId: Long, tekst: String, rolle: BehandlerRolle) {
        lagre(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.DISTRIBUERE_BREV,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, rolle),
                tekst = tekst
            )
        )
    }

    fun opprettBrevIkkeDistribuertUkjentAdresseLogg(behandlingId: Long, brevnavn: String) {
        lagre(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.BREV_IKKE_DISTRIBUERT,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SYSTEM),
                tekst = brevnavn
            )
        )
    }

    fun opprettBrevIkkeDistribuertUkjentDødsboadresseLogg(behandlingId: Long, brevnavn: String) {
        lagre(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.BREV_IKKE_DISTRIBUERT_UKJENT_DØDSBO,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SYSTEM),
                tekst = brevnavn
            )
        )
    }

    fun opprettFerdigstillBehandling(behandling: Behandling) {
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.FERDIGSTILLE_BEHANDLING,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SYSTEM),
            )
        )
    }

    fun opprettHenleggBehandling(behandling: Behandling, årsak: String, begrunnelse: String) {
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.HENLEGG_BEHANDLING,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = "$årsak: $begrunnelse"
            )
        )
    }

    fun opprettBarnLagtTilLogg(behandling: Behandling, barn: Person) {
        val beskrivelse =
            "${barn.navn.uppercase()} (${barn.hentAlder()} år) | ${Identkonverterer.formaterIdent(barn.aktør.aktivFødselsnummer())} lagt til"
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BARN_LAGT_TIL,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = beskrivelse
            )
        )
    }

    fun opprettSettPåVentLogg(behandling: Behandling, årsak: String) {
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLIG_SATT_PÅ_VENT,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = "Årsak: $årsak"
            )
        )
    }

    fun opprettOppdaterVentingLogg(behandling: Behandling, endretÅrsak: String?, endretFrist: LocalDate?) {
        val tekst = if (endretFrist != null && endretÅrsak != null) {
            "Frist og årsak er endret til \"${endretÅrsak}\" og ${endretFrist.tilKortString()}"
        } else if (endretFrist != null) {
            "Frist er endret til ${endretFrist.tilKortString()}"
        } else if (endretÅrsak != null) {
            "Årsak er endret til \"${endretÅrsak}\""
        } else {
            return
        }

        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.VENTENDE_BEHANDLING_ENDRET,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = tekst
            )
        )
    }

    fun opprettEtterbetalingKorrigeringLogg(behandling: Behandling, tittel: String) {
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.ETTERBETALING_KORRIGERT,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tittel = tittel,
                tekst = ""
            )
        )
    }

    fun gjenopptaBehandlingLogg(behandling: Behandling) {
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLIG_GJENOPPTATT,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                )
            )
        )
    }

    fun lagre(logg: Logg): Logg {
        metrikkPerLoggType[logg.type]?.increment()

        return loggRepository.save(logg)
    }

    fun hentLoggForBehandling(behandlingId: Long): List<Logg> {
        return loggRepository.hentLoggForBehandling(behandlingId)
    }

    companion object {

        private fun tilBehandlingstema(underkategori: BehandlingUnderkategori, kategori: BehandlingKategori): String {
            return "${kategori.visningsnavn}  ${underkategori.visningsnavn.lowercase()}"
        }
    }
}

enum class RegistrerVergeLoggType {
    VERGE_REGISTRERT,
    INSTITUSJON_REGISTRERT,
}
