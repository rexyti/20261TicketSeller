ALTER TABLE cortesias
    DROP COLUMN destinatario,
    ADD COLUMN destinatario_id UUID,
    ADD COLUMN email_destinatario VARCHAR(255);
