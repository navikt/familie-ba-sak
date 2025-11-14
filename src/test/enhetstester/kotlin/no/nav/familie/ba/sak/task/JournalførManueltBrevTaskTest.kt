package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.kjerne.brev.DokumentGenereringService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Bruker
import no.nav.familie.ba.sak.kjerne.brev.mottaker.FullmektigEllerVerge
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Institusjon
import no.nav.familie.ba.sak.kjerne.brev.mottaker.ManuellAdresseInfo
import no.nav.familie.ba.sak.kjerne.brev.mottaker.tilAvsenderMottaker
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.task.dto.JournalførManueltBrevDTO
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.jboss.logging.MDC
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID

class JournalførManueltBrevTaskTest {
    @Nested
    inner class DoTask {
        private val utgåendeJournalføringService = mockk<UtgåendeJournalføringService>()
        private val dokumentGenereringService = mockk<DokumentGenereringService>()
        private val taskRepositoryWrapper = mockk<TaskRepositoryWrapper>()
        private val fagsakService = mockk<FagsakService>()

        private val journalførManueltBrevTask =
            JournalførManueltBrevTask(
                utgåendeJournalføringService = utgåendeJournalføringService,
                dokumentGenereringService = dokumentGenereringService,
                taskRepository = taskRepositoryWrapper,
                fagsakService = fagsakService,
            )

        @ParameterizedTest
        @EnumSource(
            value = Brevmal::class,
            names = [
                "INFORMASJONSBREV_DELT_BOSTED",
                "HENLEGGE_TRUKKET_SØKNAD",
                "HENLEGGE_TRUKKET_SØKNAD_INSTITUSJON",
                "SVARTIDSBREV",
                "SVARTIDSBREV_INSTITUSJON",
                "FORLENGET_SVARTIDSBREV",
                "FORLENGET_SVARTIDSBREV_INSTITUSJON",
                "TILBAKEKREVINGSVEDTAK_MOTREGNING",
                "INFORMASJONSBREV_FØDSEL_VERGEMÅL",
                "INFORMASJONSBREV_FØDSEL_MINDREÅRIG",
                "INFORMASJONSBREV_KAN_SØKE",
                "INFORMASJONSBREV_FØDSEL_GENERELL",
                "INFORMASJONSBREV_KAN_SØKE_EØS",
                "INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_GJORT_VEDTAK_TIL_ANNEN_FORELDER",
                "INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER",
                "INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_ÅRLIG_KONTROLL",
                "INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HENTER_IKKE_REGISTEROPPLYSNINGER",
                "INFORMASJONSBREV_KAN_HA_RETT_TIL_PENGESTØTTE_FRA_NAV",
                "INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE",
                "INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE_INSTITUSJON",
                "UTBETALING_ETTER_KA_VEDTAK",
            ],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `doTask - skal opprette journalpost uten forside og opprette oppgave for distribusjon for brevmaler`(brevmal: Brevmal) {
            // Arrange
            val fagsak = lagFagsak()
            val manueltBrevRequest = ManueltBrevRequest(brevmal = brevmal, enhet = Enhet(enhetId = "1234", enhetNavn = "Testenhet"))
            val task = JournalførManueltBrevTask.opprettTask(behandlingId = null, fagsakId = fagsak.id, manuellBrevRequest = manueltBrevRequest, mottakerInfo = Bruker)

            every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak

            val fnrSlot = slot<String>()
            val fagsakIdSlot = slot<String>()
            val journalførendeEnhetSlot = slot<String>()
            val brevSlot = slot<List<Dokument>>()
            val førstesideSlot = slot<Førsteside?>()
            val avsenderMottakerSlot = slot<AvsenderMottaker?>()

            every {
                utgåendeJournalføringService.journalførDokument(
                    fnr = capture(fnrSlot),
                    fagsakId = capture(fagsakIdSlot),
                    journalførendeEnhet = capture(journalførendeEnhetSlot),
                    brev = capture(brevSlot),
                    førsteside = captureNullable(førstesideSlot),
                    avsenderMottaker = captureNullable(avsenderMottakerSlot),
                    eksternReferanseId = any(),
                )
            } returns "journalpostId"

            every { dokumentGenereringService.genererManueltBrev(any(), any(), any()) } returns ByteArray(0)

            val taskSlot = slot<Task>()

            every { taskRepositoryWrapper.save(capture(taskSlot)) } returns mockk()

            // Act
            journalførManueltBrevTask.doTask(task)

            // Assert
            val capturedFnr = fnrSlot.captured
            val capturedFagsakId = fagsakIdSlot.captured
            val capturedJournalførendeEnhetId = journalførendeEnhetSlot.captured
            val capturedBrev = brevSlot.captured
            val capturedFørsteside = førstesideSlot.captured
            val capturedAvsenderMottaker = avsenderMottakerSlot.captured

            assertThat(capturedFnr).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(capturedFagsakId).isEqualTo(fagsak.id.toString())
            assertThat(capturedJournalførendeEnhetId).isEqualTo(manueltBrevRequest.enhet?.enhetId)
            assertThat(capturedBrev).isNotNull
            assertThat(capturedFørsteside).isNull()
            assertThat(capturedAvsenderMottaker).isNull()

            val capturedTask = taskSlot.captured
            assertThat(capturedTask.type).isEqualTo(DistribuerDokumentTask.TASK_STEP_TYPE)
            val distribuerDokumentDTO = objectMapper.readValue(capturedTask.payload, DistribuerDokumentDTO::class.java)
            assertThat(distribuerDokumentDTO.journalpostId).isEqualTo("journalpostId")
            assertThat(distribuerDokumentDTO.brevmal).isEqualTo(brevmal)
            assertThat(distribuerDokumentDTO.fagsakId).isEqualTo(fagsak.id)
            assertThat(distribuerDokumentDTO.manuellAdresseInfo).isNull()
        }

        @ParameterizedTest
        @EnumSource(
            value = Brevmal::class,
            names = [
                "INNHENTE_OPPLYSNINGER",
                "INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED",
                "INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT",
                "INNHENTE_OPPLYSNINGER_INSTITUSJON",
                "INFORMASJONSBREV_TIL_FORELDER_MED_SELVSTENDIG_RETT_VI_HAR_FÅTT_F016_KAN_SØKE_OM_BARNETRYGD",
                "VARSEL_OM_REVURDERING",
                "VARSEL_OM_REVURDERING_INSTITUSJON",
                "VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14",
                "VARSEL_OM_REVURDERING_SAMBOER",
                "VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED",
                "VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS",
                "VARSEL_OM_ÅRLIG_REVURDERING_EØS",
                "VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER",
                "VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT",
            ],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `doTask - skal opprette journalpost med forside og opprette oppgave for distribusjon for brevmaler`(brevmal: Brevmal) {
            // Arrange
            val fagsak = lagFagsak()
            val manueltBrevRequest = ManueltBrevRequest(brevmal = brevmal, enhet = Enhet(enhetId = "1234", enhetNavn = "Testenhet"))
            val task = JournalførManueltBrevTask.opprettTask(behandlingId = null, fagsakId = fagsak.id, manuellBrevRequest = manueltBrevRequest, mottakerInfo = Bruker)

            every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak

            val fnrSlot = slot<String>()
            val fagsakIdSlot = slot<String>()
            val journalførendeEnhetSlot = slot<String>()
            val brevSlot = slot<List<Dokument>>()
            val førstesideSlot = slot<Førsteside?>()
            val avsenderMottakerSlot = slot<AvsenderMottaker?>()

            every {
                utgåendeJournalføringService.journalførDokument(
                    fnr = capture(fnrSlot),
                    fagsakId = capture(fagsakIdSlot),
                    journalførendeEnhet = capture(journalførendeEnhetSlot),
                    brev = capture(brevSlot),
                    førsteside = captureNullable(førstesideSlot),
                    avsenderMottaker = captureNullable(avsenderMottakerSlot),
                    eksternReferanseId = any(),
                )
            } returns "journalpostId"

            every { dokumentGenereringService.genererManueltBrev(any(), any(), any()) } returns ByteArray(0)

            val taskSlot = slot<Task>()

            every { taskRepositoryWrapper.save(capture(taskSlot)) } returns mockk()

            // Act
            journalførManueltBrevTask.doTask(task)

            // Assert
            val capturedFnr = fnrSlot.captured
            val capturedFagsakId = fagsakIdSlot.captured
            val capturedJournalførendeEnhetId = journalførendeEnhetSlot.captured
            val capturedBrev = brevSlot.captured
            val capturedFørsteside = førstesideSlot.captured
            val capturedAvsenderMottaker = avsenderMottakerSlot.captured

            assertThat(capturedFnr).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(capturedFagsakId).isEqualTo(fagsak.id.toString())
            assertThat(capturedJournalførendeEnhetId).isEqualTo(manueltBrevRequest.enhet?.enhetId)
            assertThat(capturedBrev).isNotNull
            assertThat(capturedFørsteside).isNotNull
            assertThat(capturedAvsenderMottaker).isNull()

            val capturedTask = taskSlot.captured
            assertThat(capturedTask.type).isEqualTo(DistribuerDokumentTask.TASK_STEP_TYPE)
            val distribuerDokumentDTO = objectMapper.readValue(capturedTask.payload, DistribuerDokumentDTO::class.java)
            assertThat(distribuerDokumentDTO.journalpostId).isEqualTo("journalpostId")
            assertThat(distribuerDokumentDTO.brevmal).isEqualTo(brevmal)
            assertThat(distribuerDokumentDTO.fagsakId).isEqualTo(fagsak.id)
            assertThat(distribuerDokumentDTO.manuellAdresseInfo).isNull()
        }

        @ParameterizedTest
        @EnumSource(
            value = Brevmal::class,
            names = [
                "INFORMASJONSBREV_DELT_BOSTED",
                "HENLEGGE_TRUKKET_SØKNAD",
                "HENLEGGE_TRUKKET_SØKNAD_INSTITUSJON",
                "SVARTIDSBREV",
                "SVARTIDSBREV_INSTITUSJON",
                "FORLENGET_SVARTIDSBREV",
                "FORLENGET_SVARTIDSBREV_INSTITUSJON",
                "TILBAKEKREVINGSVEDTAK_MOTREGNING",
                "INFORMASJONSBREV_FØDSEL_VERGEMÅL",
                "INFORMASJONSBREV_FØDSEL_MINDREÅRIG",
                "INFORMASJONSBREV_KAN_SØKE",
                "INFORMASJONSBREV_FØDSEL_GENERELL",
                "INFORMASJONSBREV_KAN_SØKE_EØS",
                "INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_GJORT_VEDTAK_TIL_ANNEN_FORELDER",
                "INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER",
                "INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_ÅRLIG_KONTROLL",
                "INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HENTER_IKKE_REGISTEROPPLYSNINGER",
                "INFORMASJONSBREV_KAN_HA_RETT_TIL_PENGESTØTTE_FRA_NAV",
                "INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE",
                "INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE_INSTITUSJON",
                "UTBETALING_ETTER_KA_VEDTAK",
                "INNHENTE_OPPLYSNINGER",
                "INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED",
                "INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT",
                "INNHENTE_OPPLYSNINGER_INSTITUSJON",
                "INFORMASJONSBREV_TIL_FORELDER_MED_SELVSTENDIG_RETT_VI_HAR_FÅTT_F016_KAN_SØKE_OM_BARNETRYGD",
                "VARSEL_OM_REVURDERING",
                "VARSEL_OM_REVURDERING_INSTITUSJON",
                "VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14",
                "VARSEL_OM_REVURDERING_SAMBOER",
                "VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED",
                "VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS",
                "VARSEL_OM_ÅRLIG_REVURDERING_EØS",
                "VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER",
                "VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT",
            ],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `doTask - skal kaste feil dersom brevmal ikke er støttet`(brevmal: Brevmal) {
            // Arrange
            val fagsak = lagFagsak()
            val manueltBrevRequest = ManueltBrevRequest(brevmal = brevmal, enhet = Enhet(enhetId = "1234", enhetNavn = "Testenhet"))
            val task = JournalførManueltBrevTask.opprettTask(behandlingId = null, fagsakId = fagsak.id, manuellBrevRequest = manueltBrevRequest, mottakerInfo = Bruker)

            every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak
            every { dokumentGenereringService.genererManueltBrev(any(), any(), any()) } returns ByteArray(0)

            // Act & Assert
            val ikkeStøttetBrevmalFeil = assertThrows<Feil> { journalførManueltBrevTask.doTask(task) }
            assertThat(ikkeStøttetBrevmalFeil.message).isEqualTo("$brevmal støtter ikke generering av forside")
        }

        @Test
        fun `doTask - skal sette avsenderMottaker på Journalpost og manuellAdresseInfo på DistribuerDokumentTask når mottaker er FullmektigEllerVerge`() {
            // Arrange
            val fagsak = lagFagsak()
            val manueltBrevRequest = ManueltBrevRequest(brevmal = Brevmal.SVARTIDSBREV, enhet = Enhet(enhetId = "1234", enhetNavn = "Testenhet"))
            val mottakerInfo = FullmektigEllerVerge(navn = "Fullmektig", manuellAdresseInfo = ManuellAdresseInfo("Adresseveien 1", postnummer = "1234", poststed = "Test", landkode = "NO"))
            val task = JournalførManueltBrevTask.opprettTask(behandlingId = null, fagsakId = fagsak.id, manuellBrevRequest = manueltBrevRequest, mottakerInfo = mottakerInfo)

            every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak

            val avsenderMottakerSlot = slot<AvsenderMottaker?>()

            every {
                utgåendeJournalføringService.journalførDokument(
                    fnr = any(),
                    fagsakId = any(),
                    journalførendeEnhet = any(),
                    brev = any(),
                    førsteside = any(),
                    avsenderMottaker = captureNullable(avsenderMottakerSlot),
                    eksternReferanseId = any(),
                )
            } returns "journalpostId"

            every { dokumentGenereringService.genererManueltBrev(any(), any(), any()) } returns ByteArray(0)

            val taskSlot = slot<Task>()

            every { taskRepositoryWrapper.save(capture(taskSlot)) } returns mockk()
            // Act
            journalførManueltBrevTask.doTask(task)

            // Assert
            val capturedAvsenderMottaker = avsenderMottakerSlot.captured

            assertThat(capturedAvsenderMottaker).isNotNull
            assertThat(capturedAvsenderMottaker!!.id).isNull()
            assertThat(capturedAvsenderMottaker.navn).isEqualTo(mottakerInfo.navn)

            val capturedTask = taskSlot.captured
            assertThat(capturedTask.type).isEqualTo(DistribuerDokumentTask.TASK_STEP_TYPE)
            val distribuerDokumentDTO = objectMapper.readValue(capturedTask.payload, DistribuerDokumentDTO::class.java)
            assertThat(distribuerDokumentDTO.manuellAdresseInfo).isNotNull
            assertThat(distribuerDokumentDTO.manuellAdresseInfo).isEqualTo(mottakerInfo.manuellAdresseInfo)
        }

        @Test
        fun `doTask - skal sette default journalførende enhet når enhet er null`() {
            // Arrange
            val fagsak = lagFagsak()
            val manueltBrevRequest = ManueltBrevRequest(brevmal = Brevmal.SVARTIDSBREV, enhet = null)
            val mottakerInfo = Institusjon(orgNummer = "999888777", navn = "Institusjon AS")
            val task = JournalførManueltBrevTask.opprettTask(behandlingId = null, fagsakId = fagsak.id, manuellBrevRequest = manueltBrevRequest, mottakerInfo = mottakerInfo)

            every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak

            val journalførendeEnhetSlot = slot<String>()

            every {
                utgåendeJournalføringService.journalførDokument(
                    fnr = any(),
                    fagsakId = any(),
                    journalførendeEnhet = capture(journalførendeEnhetSlot),
                    brev = any(),
                    førsteside = any(),
                    avsenderMottaker = any(),
                    eksternReferanseId = any(),
                )
            } returns "journalpostId"

            every { dokumentGenereringService.genererManueltBrev(any(), any(), any()) } returns ByteArray(0)

            every { taskRepositoryWrapper.save(any()) } returns mockk()

            // Act
            journalførManueltBrevTask.doTask(task)

            // Assert
            val capturedJournalførendeEnhetId = journalførendeEnhetSlot.captured

            assertThat(capturedJournalførendeEnhetId).isEqualTo(DEFAULT_JOURNALFØRENDE_ENHET)
        }
    }

    @Nested
    inner class OpprettTask {
        @Test
        fun `skal opprette task`() {
            // Arrange
            val fagsakId = 321L
            val behandlingId = 123L
            val manueltBrevDto = ManueltBrevRequest(brevmal = Brevmal.SVARTIDSBREV)
            val mottakerInfo = Bruker
            val callId = UUID.randomUUID()
            MDC.clear()
            MDC.put(MDCConstants.MDC_CALL_ID, callId)

            // Act
            val task =
                JournalførManueltBrevTask.opprettTask(
                    behandlingId = behandlingId,
                    fagsakId = fagsakId,
                    manuellBrevRequest = manueltBrevDto,
                    mottakerInfo = mottakerInfo,
                )

            // Assert
            assertThat(task.type).isEqualTo(JournalførManueltBrevTask.TASK_STEP_TYPE)
            assertThat(task.payload).isNotNull()
            val journalførManueltBrevDTO = objectMapper.readValue(task.payload, JournalførManueltBrevDTO::class.java)
            assertThat(journalførManueltBrevDTO.fagsakId).isEqualTo(fagsakId)
            assertThat(journalførManueltBrevDTO.behandlingId).isEqualTo(behandlingId)
            assertThat(journalførManueltBrevDTO.manuellBrevRequest).isEqualTo(manueltBrevDto)
            assertThat(journalførManueltBrevDTO.mottaker.avsenderMottaker).isEqualTo(mottakerInfo.tilAvsenderMottaker())
            assertThat(journalførManueltBrevDTO.mottaker.manuellAdresseInfo).isEqualTo(mottakerInfo.manuellAdresseInfo)
            assertThat(journalførManueltBrevDTO.eksternReferanseId).isEqualTo("${fagsakId}_${behandlingId}_$callId")
            assertThat(task.metadata["fagsakId"]).isEqualTo(fagsakId.toString())
            assertThat(task.metadata["behandlingId"]).isEqualTo(behandlingId.toString())
            assertThat(task.metadata["brevmal"]).isEqualTo(manueltBrevDto.brevmal.name)
            assertThat(task.metadata["mottakerType"]).isEqualTo(Bruker::class.simpleName)
            MDC.clear()
        }
    }
}
