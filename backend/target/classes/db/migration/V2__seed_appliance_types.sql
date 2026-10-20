INSERT INTO appliance_type (id, code, name, active, display_order)
VALUES
    ('00000000-0000-4000-8000-000000000001', 'AIR_CONDITIONER', '에어컨', true, 1),
    ('00000000-0000-4000-8000-000000000002', 'REFRIGERATOR', '냉장고', true, 2),
    ('00000000-0000-4000-8000-000000000003', 'WASHING_MACHINE', '세탁기', true, 3),
    ('00000000-0000-4000-8000-000000000004', 'TV', 'TV', true, 4)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    active = EXCLUDED.active,
    display_order = EXCLUDED.display_order,
    updated_at = now();
