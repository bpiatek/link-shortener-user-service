CREATE TABLE email_verifications (
     id                  BIGSERIAL PRIMARY KEY,
     user_id             BIGINT NOT NULL UNIQUE,
     token_hash          VARCHAR(255) NOT NULL,
     expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
     created_at          TIMESTAMP WITH TIME ZONE NOT NULL,

     CONSTRAINT fk_user
         FOREIGN KEY(user_id)
             REFERENCES users(id)
);

CREATE INDEX idx_email_verifications_on_token_hash ON email_verifications (token_hash);
