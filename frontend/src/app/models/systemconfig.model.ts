import { AutoIdEntity, BaseSearchObject } from './common.model';
import { ConfigDataType } from './enums';

/** SystemConfigDto (6.4) — mirrors SystemConfigEntity (6.5). */
export interface SystemConfigDto extends AutoIdEntity {
  configKey: string;
  configValue: string;
  dataType: ConfigDataType;
  category: string;
  description?: string;
  isEditable: boolean;
}

/** SystemConfigUpdateRequest (6.10). */
export interface SystemConfigUpdateRequest {
  configValue: string;
  description?: string;
}

/** SystemConfigSearchObject (6.8) filter fields, combine with BaseSearchObject. */
export interface SystemConfigSearchObject extends BaseSearchObject {
  category?: string;
  dataType?: ConfigDataType;
}