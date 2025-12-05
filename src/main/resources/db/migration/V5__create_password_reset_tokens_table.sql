CREATE TABLE password_reset_tokens (
   id                  BIGSERIAL PRIMARY KEY,
   user_id             BIGINT NOT NULL UNIQUE,
   token_hash          VARCHAR(255) NOT NULL,
   expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
   created_at          TIMESTAMP WITH TIME ZONE NOT NULL,

   CONSTRAINT fk_user_reset
       FOREIGN KEY(user_id)
           REFERENCES users(id)
);

CREATE INDEX idx_password_reset_tokens_on_token_hash ON password_reset_tokens (token_hash);