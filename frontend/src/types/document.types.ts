export type DocumentCategory = 'POLICIES' | 'FINANCIAL' | 'ENGINEERING' | 'LEGAL' | 'HR' | 'GENERAL';

export interface DocumentItem {
  id: string;
  tenantId: string;
  title: string;
  category: DocumentCategory;
  sourceType: string;
  sourceUri: string;
  mimeType: string;
  fileSizeBytes: number;
  checksum: string;
  securityClassification: string;
  allowedRoles: string[];
  allowedDepartments: string[];
  tags: string[];
  status: 'PENDING' | 'PARSING' | 'EMBEDDING' | 'INDEXED' | 'FAILED';
  uploaderName: string;
  createdAt: string;
}
