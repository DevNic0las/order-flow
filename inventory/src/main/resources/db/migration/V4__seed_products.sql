INSERT INTO inventory.tb_inventory (product_name, quantity)
VALUES
    ('Teclado mecânico', 50),
    ('Mouse sem fio', 30),
    ('Monitor 27"', 15),
    ('Headset gamer', 8),
    ('Webcam Full HD', 25)
    ON CONFLICT DO NOTHING;