import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import { AiPropertyOption } from '../models/ai-reference.model';

@Injectable({
  providedIn: 'root'
})
export class AiReferenceDataService {
  constructor(private readonly apiService: ApiService) {
  }

  loadProperties(): Observable<AiPropertyOption[]> {
    return this.apiService.get<PageResponse<AiPropertyOption>>('/properties', {
      status: 'ACTIVE',
      page: 0,
      size: 200,
      sort: 'name,asc'
    }).pipe(map((response) => response.content));
  }
}
