import { CatalogConfig, INVENTORY_ITEM_TYPES } from './models/catalog.model';

const baseFields = [
  {
    key: 'name',
    labelKey: 'catalogs.fields.name',
    type: 'text' as const,
    required: true,
    maxLength: 150,
    table: true,
    primary: true
  },
  {
    key: 'description',
    labelKey: 'catalogs.fields.description',
    type: 'textarea' as const,
    rows: 3,
    table: true
  }
];

const inventoryItemTypeOptions = INVENTORY_ITEM_TYPES.map((type) => ({
  value: type,
  labelKey: `inventoryItems.type.${type}`
}));

export const CATALOG_CONFIGS: CatalogConfig[] = [
  {
    key: 'cities',
    titleKey: 'catalogs.items.cities.title',
    descriptionKey: 'catalogs.items.cities.description',
    endpoint: '/catalogs/cities',
    fields: [
      {
        key: 'name',
        labelKey: 'catalogs.fields.name',
        type: 'text',
        required: true,
        maxLength: 150,
        table: true,
        primary: true
      },
      {
        key: 'country',
        labelKey: 'catalogs.fields.country',
        type: 'text',
        maxLength: 100,
        table: true
      }
    ]
  },
  {
    key: 'brands',
    titleKey: 'catalogs.items.brands.title',
    descriptionKey: 'catalogs.items.brands.description',
    endpoint: '/catalogs/brands',
    fields: baseFields
  },
  {
    key: 'inventory-items',
    titleKey: 'catalogs.items.inventoryItems.title',
    descriptionKey: 'catalogs.items.inventoryItems.description',
    endpoint: '/inventory-items',
    defaultSort: 'name,asc',
    fields: [
      ...baseFields,
      {
        key: 'brandId',
        labelKey: 'purchases.items.brand',
        type: 'select',
        table: true,
        options: []
      },
      {
        key: 'unit',
        labelKey: 'catalogs.fields.unit',
        type: 'text',
        maxLength: 50,
        table: true
      },
      {
        key: 'itemType',
        labelKey: 'catalogs.fields.itemType',
        type: 'select',
        required: true,
        table: true,
        options: inventoryItemTypeOptions
      },
      {
        key: 'internalCode',
        labelKey: 'catalogs.fields.internalCode',
        type: 'text',
        maxLength: 100,
        table: true
      },
      {
        key: 'barcode',
        labelKey: 'catalogs.fields.barcode',
        type: 'text',
        maxLength: 100,
        table: true
      },
      {
        key: 'availableForMaintenance',
        labelKey: 'catalogs.fields.availableForMaintenance',
        type: 'checkbox',
        table: true
      },
      {
        key: 'availableForReservations',
        labelKey: 'catalogs.fields.availableForReservations',
        type: 'checkbox',
        table: true
      },
      {
        key: 'availableForPurchases',
        labelKey: 'catalogs.fields.availableForPurchases',
        type: 'checkbox',
        table: true
      }
    ]
  },
  {
    key: 'suppliers',
    titleKey: 'catalogs.items.suppliers.title',
    descriptionKey: 'catalogs.items.suppliers.description',
    endpoint: '/catalogs/suppliers',
    fields: [
      {
        key: 'name',
        labelKey: 'catalogs.fields.name',
        type: 'text',
        required: true,
        maxLength: 150,
        table: true,
        primary: true
      },
      {
        key: 'phone',
        labelKey: 'catalogs.fields.phone',
        type: 'text',
        maxLength: 50,
        table: true
      },
      {
        key: 'email',
        labelKey: 'catalogs.fields.email',
        type: 'email',
        maxLength: 150,
        table: true
      },
      {
        key: 'website',
        labelKey: 'catalogs.fields.website',
        type: 'url'
      },
      {
        key: 'notes',
        labelKey: 'catalogs.fields.notes',
        type: 'textarea',
        rows: 3
      }
    ]
  },
  {
    key: 'payment-categories',
    titleKey: 'catalogs.items.paymentCategories.title',
    descriptionKey: 'catalogs.items.paymentCategories.description',
    endpoint: '/catalogs/payment-categories',
    fields: baseFields
  },
  {
    key: 'maintenance-categories',
    titleKey: 'catalogs.items.maintenanceCategories.title',
    descriptionKey: 'catalogs.items.maintenanceCategories.description',
    endpoint: '/catalogs/maintenance-categories',
    fields: baseFields
  },
  {
    key: 'maintenance-types',
    titleKey: 'catalogs.items.maintenanceTypes.title',
    descriptionKey: 'catalogs.items.maintenanceTypes.description',
    endpoint: '/catalogs/maintenance-types',
    fields: baseFields
  },
  {
    key: 'maintenance-people',
    titleKey: 'catalogs.items.maintenancePeople.title',
    descriptionKey: 'catalogs.items.maintenancePeople.description',
    endpoint: '/catalogs/maintenance-people',
    fields: [
      {
        key: 'fullName',
        labelKey: 'catalogs.fields.fullName',
        type: 'text',
        required: true,
        maxLength: 150,
        table: true,
        primary: true
      },
      {
        key: 'phone',
        labelKey: 'catalogs.fields.phone',
        type: 'text',
        maxLength: 50,
        table: true
      },
      {
        key: 'email',
        labelKey: 'catalogs.fields.email',
        type: 'email',
        maxLength: 150,
        table: true
      },
      {
        key: 'notes',
        labelKey: 'catalogs.fields.notes',
        type: 'textarea',
        rows: 3
      }
    ]
  },
  {
    key: 'platforms',
    titleKey: 'catalogs.items.platforms.title',
    descriptionKey: 'catalogs.items.platforms.description',
    endpoint: '/catalogs/platforms',
    fields: baseFields
  },
  {
    key: 'task-templates',
    titleKey: 'catalogs.items.taskTemplates.title',
    descriptionKey: 'catalogs.items.taskTemplates.description',
    endpoint: '/catalogs/task-templates',
    fields: baseFields
  }
];
