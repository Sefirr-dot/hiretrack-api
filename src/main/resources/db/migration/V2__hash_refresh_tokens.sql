-- Refresh tokens are now opaque values stored as SHA-256 hashes.
-- Tokens issued by previous versions (raw JWTs) can no longer be matched, so drop them.
DELETE FROM refresh_tokens;

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_documents_application_id ON documents(application_id);
