import { DatePipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { ProductBoxFacesModalComponent } from '../../components/product-box-faces-modal/product-box-faces-modal.component';
import { ProductBoxModelFormModalComponent } from '../../components/product-box-model-form-modal/product-box-model-form-modal.component';
import {
  PRODUCT_BOX_FACE_NAMES,
  ProductBoxInventoryItemOption,
  ProductBoxModel,
  ProductBoxModelRequest,
  ProductBoxModelSummary
} from '../../models/product-box-model.model';
import { ProductBoxModelService } from '../../services/product-box-model.service';
import { ProductBoxReferenceDataService } from '../../services/product-box-reference-data.service';

type FormMode = 'create' | 'edit';

@Component({
  selector: 'app-product-box-models-page',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    NgClass,
    TranslatePipe,
    ConfirmModalComponent,
    ProductBoxModelFormModalComponent,
    ProductBoxFacesModalComponent
  ],
  templateUrl: './product-box-models-page.component.html'
})
export class ProductBoxModelsPageComponent implements OnInit {
  private readonly productBoxModelService = inject(ProductBoxModelService);
  private readonly referenceDataService = inject(ProductBoxReferenceDataService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly loadingReferences = signal(false);
  readonly models = signal<ProductBoxModelSummary[]>([]);
  readonly inventoryItems = signal<ProductBoxInventoryItemOption[]>([]);
  readonly selectedModel = signal<ProductBoxModel | null>(null);
  readonly modelForFaces = signal<ProductBoxModel | null>(null);
  readonly modelToDelete = signal<ProductBoxModelSummary | null>(null);
  readonly formVisible = signal(false);
  readonly formMode = signal<FormMode>('create');
  readonly search = signal('');
  readonly inventoryItemId = signal('');
  readonly purchaseItemId = signal('');
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly pageLabel = computed(() => {
    if (this.totalElements() === 0) {
      return this.languageService.instant('productBoxModels.pagination.noItems');
    }

    return this.languageService.instant('productBoxModels.pagination.pageOf', {
      page: this.page() + 1,
      totalPages: this.totalPages()
    });
  });

  readonly deleteMessage = computed(() => {
    const model = this.modelToDelete();
    if (!model) {
      return '';
    }

    return this.languageService.instant('productBoxModels.confirmDeleteMessage', { name: model.name });
  });

  ngOnInit(): void {
    this.loadReferences();
    this.loadModels();
  }

  loadReferences(): void {
    this.loadingReferences.set(true);
    this.referenceDataService.loadInventoryItems().subscribe({
      next: (inventoryItems) => {
        this.inventoryItems.set(inventoryItems);
        this.loadingReferences.set(false);
      },
      error: (error: unknown) => {
        this.loadingReferences.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.messages.referencesError')));
      }
    });
  }

  loadModels(): void {
    this.loading.set(true);
    this.productBoxModelService
      .findAll({
        inventoryItemId: this.inventoryItemId() || undefined,
        purchaseItemId: this.purchaseItemId().trim() || undefined,
        search: this.search().trim() || undefined,
        page: this.page(),
        size: this.size(),
        sort: 'createdAt,desc'
      })
      .subscribe({
        next: (response: PageResponse<ProductBoxModelSummary>) => {
          this.applyPage(response);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.messages.loadError')));
        }
      });
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadModels();
  }

  clearFilters(): void {
    this.search.set('');
    this.inventoryItemId.set('');
    this.purchaseItemId.set('');
    this.page.set(0);
    this.loadModels();
  }

  previousPage(): void {
    if (this.first()) {
      return;
    }

    this.page.update((value) => value - 1);
    this.loadModels();
  }

  nextPage(): void {
    if (this.last()) {
      return;
    }

    this.page.update((value) => value + 1);
    this.loadModels();
  }

  changePageSize(value: string | number): void {
    this.size.set(Number(value));
    this.page.set(0);
    this.loadModels();
  }

  openCreateForm(): void {
    this.formMode.set('create');
    this.selectedModel.set(null);
    this.formVisible.set(true);
  }

  openEditForm(modelId: string): void {
    this.loading.set(true);
    this.productBoxModelService.findById(modelId).subscribe({
      next: (model) => {
        this.selectedModel.set(model);
        this.formMode.set('edit');
        this.formVisible.set(true);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.messages.detailError')));
      }
    });
  }

  openFaces(modelId: string): void {
    this.loading.set(true);
    this.productBoxModelService.findById(modelId).subscribe({
      next: (model) => {
        this.modelForFaces.set(model);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.messages.detailError')));
      }
    });
  }

  closeFaces(): void {
    this.modelForFaces.set(null);
  }

  closeForm(): void {
    if (this.saving()) {
      return;
    }

    this.formVisible.set(false);
    this.selectedModel.set(null);
    this.formMode.set('create');
  }

  saveModel(request: ProductBoxModelRequest): void {
    this.saving.set(true);
    const selectedModel = this.selectedModel();
    const saveRequest = this.formMode() === 'edit' && selectedModel
      ? this.productBoxModelService.update(selectedModel.id, request)
      : this.productBoxModelService.create(request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          this.formMode() === 'edit'
            ? this.languageService.instant('productBoxModels.messages.updated')
            : this.languageService.instant('productBoxModels.messages.created')
        );
        this.closeForm();
        this.loadModels();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.messages.saveError')));
      }
    });
  }

  requestDelete(model: ProductBoxModelSummary): void {
    this.modelToDelete.set(model);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.modelToDelete.set(null);
  }

  confirmDelete(): void {
    const model = this.modelToDelete();
    if (!model) {
      return;
    }

    this.deletingId.set(model.id);
    this.productBoxModelService.delete(model.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.modelToDelete.set(null);
        this.toastService.success(this.languageService.instant('productBoxModels.messages.deleted'));
        this.loadModels();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.messages.deleteError')));
      }
    });
  }

  onFacesChanged(): void {
    this.loadModels();
  }

  inventoryItemLabel(item: ProductBoxInventoryItemOption): string {
    return item.fullName || [item.name, item.brandName].filter(Boolean).join(' - ') || item.id;
  }

  modelAssociationLabel(model: ProductBoxModelSummary): string {
    if (model.inventoryItemName) {
      return model.inventoryItemName;
    }

    if (model.purchaseItemName) {
      return model.purchaseItemName;
    }

    if (model.purchaseItemId) {
      return model.purchaseItemId;
    }

    return this.languageService.instant('productBoxModels.table.standalone');
  }

  dimensionsLabel(model: ProductBoxModelSummary): string {
    return `${model.width} × ${model.height} × ${model.depth} ${model.unit}`;
  }

  completedFacesLabel(model: ProductBoxModelSummary): string {
    const faces = model.faces ?? {};
    const count = PRODUCT_BOX_FACE_NAMES.filter((faceName) => !!faces[faceName]).length;
    return this.languageService.instant('productBoxModels.table.facesCount', { count, total: PRODUCT_BOX_FACE_NAMES.length });
  }

  private applyPage(response: PageResponse<ProductBoxModelSummary>): void {
    this.models.set(response.content);
    this.page.set(response.page);
    this.size.set(response.size);
    this.totalElements.set(response.totalElements);
    this.totalPages.set(response.totalPages);
    this.first.set(response.first);
    this.last.set(response.last);
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
