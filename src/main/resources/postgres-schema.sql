-- PostgreSQL + pgvector Schema for Generalist Health AI RCM MVP

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- Audit Jobs Table
CREATE TABLE IF NOT EXISTS audit_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    audit_type VARCHAR(64) NOT NULL,
    payer_id VARCHAR(64),
    patient_id VARCHAR(64),
    cpt_codes TEXT,
    icd10_codes TEXT,
    raw_payload TEXT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_audit_jobs_status ON audit_jobs(status);
CREATE INDEX IF NOT EXISTS idx_audit_jobs_payer ON audit_jobs(payer_id);
CREATE INDEX IF NOT EXISTS idx_audit_jobs_patient ON audit_jobs(patient_id);

-- Audit Reports Table
CREATE TABLE IF NOT EXISTS audit_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL UNIQUE REFERENCES audit_jobs(id) ON DELETE CASCADE,
    decision VARCHAR(32) NOT NULL,
    denial_risk VARCHAR(32) NOT NULL,
    risk_score DOUBLE PRECISION NOT NULL,
    predicted_denial_codes TEXT,
    audit_rationale TEXT,
    appeal_letter_draft TEXT,
    confidence_score DOUBLE PRECISION,
    patient_responsibility_amount NUMERIC(12, 2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_reports_job_id ON audit_reports(job_id);
CREATE INDEX IF NOT EXISTS idx_audit_reports_decision ON audit_reports(decision);
CREATE INDEX IF NOT EXISTS idx_audit_reports_denial_risk ON audit_reports(denial_risk);

-- RCM Historical Exemplars (Medprompt Vector Store)
CREATE TABLE IF NOT EXISTS rcm_exemplars (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payer_id VARCHAR(64),
    cpt_code VARCHAR(32),
    icd10_code VARCHAR(32),
    service_description VARCHAR(512),
    ground_truth_decision VARCHAR(32) NOT NULL,
    denial_code VARCHAR(32),
    validated_cot_rationale TEXT,
    embedding vector(768),
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rcm_exemplars_payer_cpt ON rcm_exemplars(payer_id, cpt_code);
CREATE INDEX IF NOT EXISTS idx_rcm_exemplars_decision ON rcm_exemplars(ground_truth_decision);

-- Cosine Distance HNSW Index for fast vector similarity search (768-dim)
CREATE INDEX IF NOT EXISTS idx_rcm_exemplars_embedding_hnsw 
ON rcm_exemplars USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
