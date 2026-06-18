ALTER TABLE inventory_items
    ADD COLUMN IF NOT EXISTS brand_id UUID NULL;

ALTER TABLE inventory_items
    DROP CONSTRAINT IF EXISTS fk_inventory_items_brand;

ALTER TABLE inventory_items
    ADD CONSTRAINT fk_inventory_items_brand
        FOREIGN KEY (brand_id) REFERENCES brands(id);

CREATE INDEX IF NOT EXISTS idx_inventory_items_brand
    ON inventory_items(brand_id);

UPDATE inventory_items inventory_item
SET brand_id = migrated_brand.brand_id
FROM (
    SELECT DISTINCT ON (purchase_item.inventory_item_id)
           purchase_item.inventory_item_id,
           purchase_item.brand_id
    FROM purchase_items purchase_item
    JOIN brands brand
      ON brand.id = purchase_item.brand_id
     AND brand.organization_id = purchase_item.organization_id
     AND brand.deleted_at IS NULL
    WHERE purchase_item.inventory_item_id IS NOT NULL
      AND purchase_item.brand_id IS NOT NULL
    ORDER BY purchase_item.inventory_item_id, purchase_item.created_at DESC
) migrated_brand
WHERE inventory_item.id = migrated_brand.inventory_item_id
  AND inventory_item.brand_id IS NULL;

ALTER TABLE purchase_items
    DROP CONSTRAINT IF EXISTS fk_purchase_items_brand;

DROP INDEX IF EXISTS idx_purchase_items_brand;

ALTER TABLE purchase_items
    DROP COLUMN IF EXISTS brand_id;
