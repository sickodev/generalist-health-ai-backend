package com.generalisthealthai.rcm.medprompt.seed;

import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import com.generalisthealthai.rcm.infrastructure.persistence.RcmExemplarRepository;
import com.generalisthealthai.rcm.medprompt.embedding.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExemplarSeedService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExemplarSeedService.class);

    private final RcmExemplarRepository exemplarRepository;
    private final EmbeddingService embeddingService;

    public ExemplarSeedService(RcmExemplarRepository exemplarRepository, EmbeddingService embeddingService) {
        this.exemplarRepository = exemplarRepository;
        this.embeddingService = embeddingService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedExemplarsIfEmpty();
    }

    public synchronized void seedExemplarsIfEmpty() {
        if (exemplarRepository.count() > 0) {
            log.info("RcmExemplar database already contains {} exemplars, skipping seed", exemplarRepository.count());
            return;
        }

        log.info("Seeding initial Medprompt RCM historical exemplars with validated Chain-of-Thought reasoning...");

        List<RcmExemplar> seeds = List.of(
                createExemplar(
                        "BCBS",
                        "93000",
                        "R07.9",
                        "Electrocardiogram routine ECG with interpretation and report",
                        ReviewDecision.PAID,
                        null,
                        "1. Patient presented with acute chest pain (R07.9). " +
                        "2. Under BCBS Medical Policy #104, an electrocardiogram (CPT 93000) is medically necessary for evaluating acute chest pain. " +
                        "3. Place of service is emergency/office setting which is covered. " +
                        "4. Conclusion: Claim aligns with coverage policy. Payment outcome: PAID."
                ),
                createExemplar(
                        "BCBS",
                        "93000",
                        "M54.5",
                        "Electrocardiogram routine ECG with interpretation and report",
                        ReviewDecision.DENIED,
                        "CO-50",
                        "1. Claim submitted for CPT 93000 (Electrocardiogram) with primary diagnosis M54.5 (Low back pain). " +
                        "2. BCBS Medical Policy #104 restricts EKG coverage to cardiac symptoms, pre-operative clearance, or cardiovascular disease management. " +
                        "3. Low back pain (M54.5) lacks documented clinical linkage or medical necessity for diagnostic EKG. " +
                        "4. Conclusion: Claim violates medical necessity guidelines. Decision: DENIED with CARC CO-50."
                ),
                createExemplar(
                        "UnitedHealthcare",
                        "73721",
                        "M25.561",
                        "Magnetic resonance imaging (MRI) of lower extremity other than joint without contrast material",
                        ReviewDecision.DENIED,
                        "CO-197",
                        "1. Target procedure is advanced diagnostic imaging CPT 73721 (MRI Knee) for knee pain (M25.561). " +
                        "2. Under UnitedHealthcare Commercial Radiology Policy RAD012, all outpatient advanced imaging procedures require prior authorization prior to rendering service. " +
                        "3. The claim was submitted without an approved prior authorization number on file. " +
                        "4. Conclusion: Service rendered without required pre-certification. Decision: DENIED with CARC CO-197."
                ),
                createExemplar(
                        "UnitedHealthcare",
                        "99214",
                        "M54.5",
                        "Office or other outpatient visit for evaluation and management of established patient, 30-39 min",
                        ReviewDecision.PAID,
                        null,
                        "1. Established patient office evaluation for chronic low back pain (M54.5). " +
                        "2. Moderate level of Medical Decision Making (MDM) documented corresponding to CPT 99214. " +
                        "3. Prior authorization is not required for standard evaluation and management office visits. " +
                        "4. Conclusion: Compliant with outpatient E/M coding guidelines. Decision: PAID."
                ),
                createExemplar(
                        "Aetna",
                        "97110",
                        "M54.5",
                        "Therapeutic procedure, 1 or more areas, each 15 minutes; therapeutic exercises",
                        ReviewDecision.DENIED,
                        "CO-16",
                        "1. Physical therapy session billed under CPT 97110 for lumbar condition M54.5. " +
                        "2. Aetna Clinical Policy Bulletin #0250 mandates a revised plan of care and authorization certification after 12 visits. " +
                        "3. Supporting progress notes and therapy plan was omitted from the submission. " +
                        "4. Conclusion: Lack of documentation for continued therapy justification. Decision: DENIED with CARC CO-16."
                ),
                createExemplar(
                        "Cigna",
                        "45378",
                        "Z12.11",
                        "Colonoscopy, flexible; diagnostic, including collection of specimen(s) by brushing or washing",
                        ReviewDecision.PAID,
                        null,
                        "1. Preventative routine screening colonoscopy for patient aged 45+ under ICD-10 Z12.11. " +
                        "2. ACA preventative care mandates 100% coverage without cost sharing for in-network screening colonoscopies. " +
                        "3. Conclusion: Fully covered preventative screening benefit. Decision: PAID."
                )
        );

        exemplarRepository.saveAll(seeds);
        log.info("Successfully seeded {} RCM Medprompt exemplars into the vector repository", seeds.size());
    }

    private RcmExemplar createExemplar(
            String payerId,
            String cptCode,
            String icd10Code,
            String serviceDescription,
            ReviewDecision decision,
            String denialCode,
            String rationale) {

        String summaryText = String.format("Payer: %s | CPT: %s | Diagnosis: %s | Description: %s | Decision: %s",
                payerId, cptCode, icd10Code, serviceDescription, decision);

        float[] embedding = embeddingService.embed(summaryText);

        RcmExemplar exemplar = RcmExemplar.builder()
                .payerId(payerId)
                .cptCode(cptCode)
                .icd10Code(icd10Code)
                .serviceDescription(serviceDescription)
                .groundTruthDecision(decision)
                .denialCode(denialCode)
                .validatedCoTRationale(rationale)
                .metadataJson("{\"source\":\"seed_v1\"}")
                .build();

        exemplar.setEmbeddingArray(embedding);
        return exemplar;
    }
}
