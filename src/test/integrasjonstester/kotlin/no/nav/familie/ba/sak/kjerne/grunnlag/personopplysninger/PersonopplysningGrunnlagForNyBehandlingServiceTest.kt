package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.every
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.PersonopplysningGrunnlagForNyBehandlingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PersonopplysningGrunnlagForNyBehandlingServiceTest(

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @Autowired
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,

    @Autowired
    private val featureToggleService: FeatureToggleService,

    @Autowired
    private val personopplysningGrunnlagForNyBehandlingService: PersonopplysningGrunnlagForNyBehandlingService,
) : AbstractSpringIntegrationTest() {

    @Test
    fun `opprettKopiEllerNyttPersonopplysningGrunnlag - skal opprette nytt PersonopplysningGrunnlag som kopi av personopplysningsgrunnlag fra forrige behandling ved satsendring`() {
        val morId = randomFnr()
        val barnId = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        val førsteBehandling =
            behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        every {
            featureToggleService.isEnabled(
                FeatureToggleConfig.SATSENDRING_KOPIER_GRUNNLAG_FRA_FORRIGE_BEHANDLING,
                false,
            )
        } returns true

        personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
            førsteBehandling,
            null,
            morId,
            listOf(barnId),
        )

        val grunnlagFraFørsteBehandling =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = førsteBehandling.id)

        tilkjentYtelseRepository.saveAndFlush(lagInitiellTilkjentYtelse(førsteBehandling, ""))

        avsluttOgLagreBehandling(førsteBehandling)

        assertThat(grunnlagFraFørsteBehandling!!.personer.size).isEqualTo(2)
        assertThat(grunnlagFraFørsteBehandling.personer.any { it.aktør.aktivFødselsnummer() == morId })
        assertThat(grunnlagFraFørsteBehandling.personer.any { it.aktør.aktivFødselsnummer() == barnId })

        val satsendring = lagBehandling(
            fagsak,
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.SATSENDRING,
        )

        val satsendringBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            satsendring,
        )

        personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
            satsendringBehandling,
            førsteBehandling,
            morId,
            listOf(barnId),
        )

        val grunnlagFraSatsendringBehandling =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = satsendringBehandling.id)

        assertThat(grunnlagFraSatsendringBehandling!!.personer.size).isEqualTo(2)
        assertThat(grunnlagFraSatsendringBehandling.personer.any { it.aktør.aktivFødselsnummer() == morId })
        assertThat(grunnlagFraSatsendringBehandling.personer.any { it.aktør.aktivFødselsnummer() == barnId })
        assertThat(grunnlagFraSatsendringBehandling.id)
            .isNotEqualTo(grunnlagFraFørsteBehandling.id)
        validerAtPersonerIGrunnlagErLike(grunnlagFraFørsteBehandling, grunnlagFraSatsendringBehandling)
    }

    private fun avsluttOgLagreBehandling(behandling: Behandling) {
        behandling.status = BehandlingStatus.AVSLUTTET
        behandling.leggTilBehandlingStegTilstand(StegType.BEHANDLING_AVSLUTTET)
        behandlingHentOgPersisterService.lagreEllerOppdater(behandling)
    }

    companion object {
        fun validerAtPersonerIGrunnlagErLike(
            personopplysningGrunnlagFørsteBehandling: PersonopplysningGrunnlag,
            personopplysningGrunnlagSatsendringBehandling: PersonopplysningGrunnlag,
        ) {
            personopplysningGrunnlagFørsteBehandling.personer.fold(mutableListOf<Pair<Person, Person>>()) { acc, person ->
                acc.add(
                    Pair(
                        person,
                        personopplysningGrunnlagSatsendringBehandling.personer.first { it.aktør.aktivFødselsnummer() == person.aktør.aktivFødselsnummer() },
                    ),
                )
                acc
            }.forEach {
                validerAtSubEntiteterAvPersonErLike(
                    it.first.bostedsadresser,
                    it.second.bostedsadresser,
                    it.first.bostedsadresser.firstOrNull()?.person,
                    it.second.bostedsadresser.firstOrNull()?.person,
                )
                validerAtSubEntiteterAvPersonErLike(
                    it.first.sivilstander,
                    it.second.sivilstander,
                    it.first.sivilstander.firstOrNull()?.person,
                    it.second.sivilstander.firstOrNull()?.person,
                )

                assertThat(it.first.sivilstander).containsExactlyInAnyOrderElementsOf(it.second.sivilstander)

                validerAtSubEntiteterAvPersonErLike(
                    it.first.statsborgerskap,
                    it.second.statsborgerskap,
                    it.first.statsborgerskap.firstOrNull()?.person,
                    it.second.statsborgerskap.firstOrNull()?.person,
                )

                validerAtSubEntiteterAvPersonErLike(
                    it.first.opphold,
                    it.second.opphold,
                    it.first.opphold.firstOrNull()?.person,
                    it.second.opphold.firstOrNull()?.person,
                )

                validerAtSubEntiteterAvPersonErLike(
                    it.first.arbeidsforhold,
                    it.second.arbeidsforhold,
                    it.first.arbeidsforhold.firstOrNull()?.person,
                    it.second.arbeidsforhold.firstOrNull()?.person,
                )

                validerAtSubEntiteterAvPersonErLike(
                    listOf(it.first.dødsfall),
                    listOf(it.second.dødsfall),
                    it.first.dødsfall?.person,
                    it.second.dødsfall?.person,
                )
            }
        }

        fun validerAtSubEntiteterAvPersonErLike(
            forrige: List<Any?>,
            kopiert: List<Any?>,
            forrigePerson: Person?,
            kopiertPerson: Person?,
        ) {
            assertThat(kopiert).containsExactlyInAnyOrderElementsOf(forrige)

            if (kopiertPerson != null) {
                assertThat(kopiertPerson).isEqualTo(forrigePerson)
                assertThat(kopiertPerson.id).isNotEqualTo(forrigePerson?.id)
            }
        }
    }
}
