package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.dataGenerator.behandling.kjørStegprosessForBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VilkårsvurderingFlyttResultaterTest(
    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val vedtakService: VedtakService,

    @Autowired
    private val stegService: StegService,

    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,

    @Autowired
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,

    @Autowired
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService
) : AbstractSpringIntegrationTest() {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal ikke endre på forrige behandling sin vilkårsvurdering ved flytting av resultater`() {
        val søker = randomFnr()
        val barn1 = ClientMocks.barnFnr[0]
        val barn2 = ClientMocks.barnFnr[1]

        // Lager førstegangsbehandling med utvidet vilkåret avslått
        val vilkårsvurderingMedUtvidetAvslått = Vilkårsvurdering(behandling = lagBehandling())
        val søkerPersonResultat = lagPersonResultat(
            vilkårsvurdering = vilkårsvurderingMedUtvidetAvslått,
            aktør = personidentService.hentAktør(søker),
            periodeFom = LocalDate.now().minusMonths(8),
            periodeTom = LocalDate.now().plusYears(2),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER,
            resultat = Resultat.OPPFYLT
        )

        søkerPersonResultat.addVilkårResultat(
            lagVilkårResultat(
                personResultat = søkerPersonResultat,
                vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(8),
                periodeTom = LocalDate.now().plusYears(2),
                behandlingId = vilkårsvurderingMedUtvidetAvslått.behandling.id
            )
        )

        val forrigeBehandlingPersonResultater = setOf(
            søkerPersonResultat,
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurderingMedUtvidetAvslått,
                aktør = personidentService.hentAktør(barn1),
                periodeFom = LocalDate.now().minusMonths(8),
                periodeTom = LocalDate.now().plusYears(2),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                resultat = Resultat.OPPFYLT
            ),
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurderingMedUtvidetAvslått,
                aktør = personidentService.hentAktør(barn2),
                periodeFom = LocalDate.now().minusMonths(8),
                periodeTom = LocalDate.now().plusYears(2),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                resultat = Resultat.OPPFYLT
            )
        )

        vilkårsvurderingMedUtvidetAvslått.personResultater = forrigeBehandlingPersonResultater

        val b = kjørStegprosessForBehandling(
            søkerFnr = søker,
            barnasIdenter = listOf(barn1, barn2),
            underkategori = BehandlingUnderkategori.UTVIDET,
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            overstyrendeVilkårsvurdering = vilkårsvurderingMedUtvidetAvslått,
            behandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING,
            vedtakService = vedtakService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            endretUtbetalingAndelService = endretUtbetalingAndelService,
            fagsakService = fagsakService,
            persongrunnlagService = persongrunnlagService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository
        )

        val vilkårsvurderingFraForrigeBehandlingFørNyRevurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = b.id)

        // Lager revurdering når utvidet ikke løper, så underkategorien er ordinær
        val siste = kjørStegprosessForBehandling(
            tilSteg = StegType.REGISTRERE_PERSONGRUNNLAG,
            søkerFnr = søker,
            barnasIdenter = listOf(barn1, barn2),
            underkategori = BehandlingUnderkategori.ORDINÆR,
            behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            overstyrendeVilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling()),
            behandlingstype = BehandlingType.REVURDERING,
            vedtakService = vedtakService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            endretUtbetalingAndelService = endretUtbetalingAndelService,
            fagsakService = fagsakService,
            persongrunnlagService = persongrunnlagService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository
        )

        // Sjekker at vilkårsvurderingen fra forrige behandling ikke er endret
        val vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = b.id)

        val søkersVilkår =
            vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering?.personResultater?.find { it.erSøkersResultater() }?.vilkårResultater
        Assertions.assertEquals(
            3,
            søkersVilkår?.size
        )
        Assertions.assertEquals(søkerPersonResultat.vilkårResultater, søkersVilkår)

        Assertions.assertEquals(
            5,
            vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering?.personResultater?.find { it.aktør.aktivFødselsnummer() == barn1 }?.vilkårResultater?.size
        )
        Assertions.assertEquals(
            vilkårsvurderingMedUtvidetAvslått.personResultater.find { it.aktør.aktivFødselsnummer() == barn1 }?.vilkårResultater,
            vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering?.personResultater?.find { it.aktør.aktivFødselsnummer() == barn1 }?.vilkårResultater
        )

        Assertions.assertEquals(
            5,
            vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering?.personResultater?.find { it.aktør.aktivFødselsnummer() == barn2 }?.vilkårResultater?.size
        )
        Assertions.assertEquals(
            vilkårsvurderingMedUtvidetAvslått.personResultater.find { it.aktør.aktivFødselsnummer() == barn2 }?.vilkårResultater,
            vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering?.personResultater?.find { it.aktør.aktivFødselsnummer() == barn2 }?.vilkårResultater
        )

        Assertions.assertEquals(
            vilkårsvurderingFraForrigeBehandlingFørNyRevurdering?.id,
            vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering?.id
        )
    }
}
