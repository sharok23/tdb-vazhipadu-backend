CREATE INDEX idx_order_line_offering_order ON order_line (offering_id, order_id);
CREATE INDEX idx_order_line_order ON order_line (order_id);
