-- V8__add_idx_pedido_cliente.sql
-- Agrega indice explicito sobre pedidos.cliente_id.
-- PostgreSQL no crea indice automaticamente para FKs; este indice
-- mejora el rendimiento de queries que filtran por cliente.

CREATE INDEX IF NOT EXISTS idx_pedido_cliente ON pedidos(cliente_id);