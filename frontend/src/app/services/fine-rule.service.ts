import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { BaseCrudService } from '../core/services/base-crud.service';

// Adjust these import paths to match your actual model locations.
import { FineRuleDto } from '../models/finerule.model';
import { FineRuleCreateRequest } from '../models/finerule.model';
import { FineRuleUpdateRequest } from '../models/finerule.model';
import { FineRuleSearchObject } from '../models/finerule.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/fine-rules';

/**
 * ADMIN only. No extra endpoints beyond the standard four (search,
 * findById, create, update) and no delete — fine rules are deactivated
 * via update (isActive: false) rather than removed.
 */
@Injectable({ providedIn: 'root' })
export class FineRuleService extends BaseCrudService<
  FineRuleDto,
  FineRuleCreateRequest,
  FineRuleUpdateRequest,
  FineRuleSearchObject,
  number
> {
  constructor(http: HttpClient) {
    super(http, API_BASE);
  }
}
