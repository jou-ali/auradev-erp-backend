-- Align fixed-length CHAR columns with Hibernate String mappings (VARCHAR).

ALTER TABLE bill_sequences
    ALTER COLUMN fy TYPE VARCHAR(4);

ALTER TABLE purchase_sequences
    ALTER COLUMN fy TYPE VARCHAR(4);

ALTER TABLE credit_note_sequences
    ALTER COLUMN fy TYPE VARCHAR(4);

ALTER TABLE tenants
    ALTER COLUMN state_code TYPE VARCHAR(2);

ALTER TABLE customers
    ALTER COLUMN state_code TYPE VARCHAR(2);

ALTER TABLE suppliers
    ALTER COLUMN state_code TYPE VARCHAR(2);

ALTER TABLE bills
    ALTER COLUMN place_of_supply_state TYPE VARCHAR(2);
