-- Align PostgreSQL movement_type enum label with Java enum name customer_return
ALTER TYPE movement_type RENAME VALUE 'return' TO 'customer_return';
