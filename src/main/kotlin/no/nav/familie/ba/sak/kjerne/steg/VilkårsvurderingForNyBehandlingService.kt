package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.VilkårsvurderingForNyBehandlingUtils.førstegangskjøringAvVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.steg.VilkårsvurderingForNyBehandlingUtils.genererInitiellVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.steg.VilkårsvurderingForNyBehandlingUtils.genererVilkårsvurderingFraForrigeVedtattBehandling
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingMetrics
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VilkårsvurderingForNyBehandlingService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val behandlingService: BehandlingService,
    private val persongrunnlagService: PersongrunnlagService,
    private val personidentService: PersonidentService,
    private val behandlingstemaService: BehandlingstemaService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val vilkårsvurderingMetrics: VilkårsvurderingMetrics
) {

    fun opprettVilkårsvurderingUtenomHovedflyt(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling?,
        nyMigreringsdato: LocalDate? = null
    ) {
        when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.ENDRE_MIGRERINGSDATO -> {
                genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                    behandling = behandling,
                    forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt
                        ?: throw Feil("Kan ikke opprette behandling med årsak 'Endre migreringsdato' hvis det ikke finnes en tidligere behandling som er iverksatt"),
                    nyMigreringsdato = nyMigreringsdato
                        ?: throw Feil("Kan ikke opprette behandling med årsak 'Endre migreringsdato' uten en migreringsdato")
                )
                // Lagre ned migreringsdato
                behandlingService.lagreNedMigreringsdato(nyMigreringsdato, behandling)
            }
            BehandlingÅrsak.HELMANUELL_MIGRERING -> {
                genererVilkårsvurderingForHelmanuellMigrering(
                    behandling = behandling,
                    nyMigreringsdato = nyMigreringsdato
                        ?: throw Feil("Kan ikke opprette behandling med årsak 'Helmanuell migrering' uten en migreringsdato")
                )
                // Lagre ned migreringsdato
                behandlingService.lagreNedMigreringsdato(nyMigreringsdato, behandling)
            }
            !in listOf(BehandlingÅrsak.SØKNAD, BehandlingÅrsak.FØDSELSHENDELSE) -> {
                initierVilkårsvurderingForBehandling(
                    behandling = behandling,
                    bekreftEndringerViaFrontend = true,
                    forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt
                )
            }
            else -> logger.info(
                "Perioder i vilkårsvurdering generer ikke automatisk for " +
                    behandling.opprettetÅrsak.visningsnavn
            )
        }
    }

    fun genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling,
        nyMigreringsdato: LocalDate
    ): Vilkårsvurdering {

        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling).apply {
            personResultater =
                VilkårsvurderingForNyBehandlingUtils.lagPersonResultaterForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                    vilkårsvurdering = this,
                    nyMigreringsdato = nyMigreringsdato,
                    forrigeBehandlingVilkårsvurdering = hentForrigeBehandlingVilkårsvurdering(
                        forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt,
                        nyBehandlingId = behandling.id
                    ),
                    personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)
                )
        }
        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
    }

    private fun hentForrigeBehandlingVilkårsvurdering(
        forrigeBehandlingSomErVedtatt: Behandling,
        nyBehandlingId: Long
    ) = (
        vilkårsvurderingService
            .hentAktivForBehandling(forrigeBehandlingSomErVedtatt.id)
            ?: throw Feil(
                "Kan ikke kopiere vilkårsvurdering fra forrige behandling ${forrigeBehandlingSomErVedtatt.id}" +
                    "til behandling $nyBehandlingId"
            )
        )

    fun genererVilkårsvurderingForHelmanuellMigrering(
        behandling: Behandling,
        nyMigreringsdato: LocalDate
    ): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling).apply {
            personResultater = VilkårsvurderingForNyBehandlingUtils.lagPersonResultaterForHelmanuellMigrering(
                vilkårsvurdering = this,
                nyMigreringsdato = nyMigreringsdato,
                personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)
            )
        }
        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
    }

    fun initierVilkårsvurderingForBehandling(
        behandling: Behandling,
        bekreftEndringerViaFrontend: Boolean,
        forrigeBehandlingSomErVedtatt: Behandling? = null
    ): Vilkårsvurdering {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)

        if (behandling.skalBehandlesAutomatisk && personopplysningGrunnlag.barna.isEmpty()) {
            throw IllegalStateException("PersonopplysningGrunnlag for fødselshendelse skal inneholde minst ett barn")
        }

        val aktivVilkårsvurdering = hentVilkårsvurdering(behandling.id)
        val barnaAktørSomAlleredeErVurdert = aktivVilkårsvurdering?.personResultater?.mapNotNull {
            personopplysningGrunnlag.barna.firstOrNull { barn -> barn.aktør == it.aktør }
        }?.filter { it.type == PersonType.BARN }?.map { it.aktør } ?: emptyList()

        val initiellVilkårsvurdering =
            genererInitiellVilkårsvurdering(
                behandling = behandling,
                barnaAktørSomAlleredeErVurdert = barnaAktørSomAlleredeErVurdert,
                personopplysningGrunnlag = personopplysningGrunnlag
            )

        if (førstegangskjøringAvVilkårsvurdering(aktivVilkårsvurdering) && behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            vilkårsvurderingMetrics.tellMetrikker(initiellVilkårsvurdering)
        }

        val løpendeUnderkategori = behandlingstemaService.hentLøpendeUnderkategori(behandling.fagsak.id)

        return if (forrigeBehandlingSomErVedtatt != null && aktivVilkårsvurdering == null) {
            val vilkårsvurdering = genererVilkårsvurderingFraForrigeVedtattBehandling(
                initiellVilkårsvurdering = initiellVilkårsvurdering,
                forrigeBehandlingVilkårsvurdering = hentVilkårsvurderingThrows(forrigeBehandlingSomErVedtatt.id),
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                løpendeUnderkategori = løpendeUnderkategori
            )
            endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
                behandling,
                forrigeBehandlingSomErVedtatt
            )
            vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
        } else if (aktivVilkårsvurdering != null) {
            val (initieltSomErOppdatert, aktivtSomErRedusert) = VilkårsvurderingUtils.flyttResultaterTilInitielt(
                initiellVilkårsvurdering = initiellVilkårsvurdering,
                aktivVilkårsvurdering = aktivVilkårsvurdering,
                løpendeUnderkategori = løpendeUnderkategori,
                forrigeBehandlingVilkårsvurdering = if (forrigeBehandlingSomErVedtatt != null) hentVilkårsvurdering(
                    forrigeBehandlingSomErVedtatt.id
                ) else null
            )

            if (aktivtSomErRedusert.personResultater.isNotEmpty() && !bekreftEndringerViaFrontend) {
                throw FunksjonellFeil(
                    melding = "Saksbehandler forsøker å fjerne vilkår fra vilkårsvurdering",
                    frontendFeilmelding = VilkårsvurderingUtils.lagFjernAdvarsel(aktivtSomErRedusert.personResultater)
                )
            }
            vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = initieltSomErOppdatert)
        } else {
            vilkårsvurderingService.lagreInitielt(initiellVilkårsvurdering)
        }
    }

    fun hentVilkårsvurdering(behandlingId: Long): Vilkårsvurdering? = vilkårsvurderingService.hentAktivForBehandling(
        behandlingId = behandlingId
    )

    fun hentVilkårsvurderingThrows(behandlingId: Long): Vilkårsvurdering =
        hentVilkårsvurdering(behandlingId) ?: throw Feil(
            message = "Fant ikke aktiv vilkårsvurdering for behandling $behandlingId",
            frontendFeilmelding = "Fant ikke aktiv vilkårsvurdering for behandling."
        )

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
