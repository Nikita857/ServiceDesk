-- V2__add_unique_constraint_to_user_roles.sql
ALTER TABLE user_roles
    ADD CONSTRAINT uk_user_id_role UNIQUE (user_id, role);