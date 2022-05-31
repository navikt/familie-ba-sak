package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingMigreringUtils
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.gjelderAlltidFraBarnetsFødselsdato
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VilkårsvurderingForNyBehandlingService(
    private val vilkårService: VilkårService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val behandlingService: BehandlingService,
    private val persongrunnlagService: PersongrunnlagService
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
                vilkårService.initierVilkårsvurderingForBehandling(
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
            personResultater = lagVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                this,
                forrigeBehandlingSomErVedtatt,
                nyMigreringsdato
            )
        }
        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
    }

    fun genererVilkårsvurderingForHelmanuellMigrering(
        behandling: Behandling,
        nyMigreringsdato: LocalDate
    ): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling).apply {
            personResultater = lagVilkårsvurderingForHelmanuellMigrering(this, nyMigreringsdato)
        }
        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
    }

    private fun lagVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
        vilkårsvurdering: Vilkårsvurdering,
        forrigeBehandlingSomErVedtatt: Behandling,
        nyMigreringsdato: LocalDate
    ): Set<PersonResultat> {
        val forrigeBehandlingsvilkårsvurdering = vilkårsvurderingService
            .hentAktivForBehandling(forrigeBehandlingSomErVedtatt.id)
            ?: throw Feil(
                "Kan ikke kopiere vilkårsvurdering fra forrige behandling ${forrigeBehandlingSomErVedtatt.id}" +
                    "til behandling ${vilkårsvurdering.behandling.id}"
            )
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id)

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val vilkårTyperForPerson = forrigeBehandlingsvilkårsvurdering.personResultater
                .single { it.aktør == person.aktør }.vilkårResultater
                .filter { it.resultat == Resultat.OPPFYLT }
                .map { it.vilkårType }

            val vilkårResultater = vilkårTyperForPerson.map { vilkår ->
                val fom = VilkårsvurderingMigreringUtils.utledPeriodeFom(
                    forrigeBehandlingsvilkårsvurdering,
                    vilkår,
                    person,
                    nyMigreringsdato
                )

                val tom: LocalDate? =
                    VilkårsvurderingMigreringUtils.utledPeriodeTom(
                        forrigeBehandlingsvilkårsvurdering,
                        vilkår,
                        person,
                        fom
                    )

                val begrunnelse = "Migrering"

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = false,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = vilkår,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = begrunnelse,
                    behandlingId = personResultat.vilkårsvurdering.behandling.id
                )
            }.toSortedSet(VilkårResultat.VilkårResultatComparator)

            val manglendePerioder = VilkårsvurderingMigreringUtils.kopiManglendePerioderFraForrigeVilkårsvurdering(
                vilkårResultater,
                forrigeBehandlingsvilkårsvurdering, person
            )
            vilkårResultater.addAll(manglendePerioder.map { it.kopierMedParent(personResultat) }.toSet())
            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    private fun lagVilkårsvurderingForHelmanuellMigrering(
        vilkårsvurdering: Vilkårsvurdering,
        nyMigreringsdato: LocalDate
    ): Set<PersonResultat> {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id)

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val vilkårTyperForPerson = Vilkår.hentVilkårFor(person.type)
            val vilkårResultater = vilkårTyperForPerson.map { vilkår ->
                val fom = when {
                    vilkår.gjelderAlltidFraBarnetsFødselsdato() -> person.fødselsdato
                    nyMigreringsdato.isBefore(person.fødselsdato) -> person.fødselsdato
                    else -> nyMigreringsdato
                }

                val tom: LocalDate? = when (vilkår) {
                    Vilkår.UNDER_18_ÅR -> person.fødselsdato.plusYears(18)
                        .minusDays(1)
                    else -> null
                }

                val begrunnelse = "Migrering"

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = false,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = vilkår,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = begrunnelse,
                    behandlingId = personResultat.vilkårsvurdering.behandling.id
                )
            }.toSortedSet(VilkårResultat.VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
