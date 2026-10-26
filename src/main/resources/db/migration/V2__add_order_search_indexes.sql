CREATE INDEX idx_orders_temple_date ON orders (temple_id, booking_date, id);
CREATE INDEX idx_orders_temple_phone ON orders (temple_id, phone, id);
CREATE INDEX idx_orders_temple_status ON orders (temple_id, status, id);
