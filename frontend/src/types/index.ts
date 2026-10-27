export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface UserInfo {
  userId: number
  username: string
  displayName: string
  email?: string
  phone?: string
}

export interface Conversation {
  id: number
  userId: number
  title: string | null
  selectedModelId: number | null
  workingDir: string | null
  authorizedPaths: string | null
  contextBudget: number
  contextStrategy: string
  keepRecentTurn: number
  summaryId: number | null
  archivedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface ConversationMessage {
  id: number
  conversationId: number
  role: string
  content: string | null
  charCount: number
  tokenEstimate: number
  pinned: boolean
  summarized: boolean
  sourceType: string | null
  status: string | null
  createdAt: string
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  list: T[]
}

export interface ModelProfile {
  id: number
  provider: string
  displayName: string
  accountAlias: string
  adapterType: string
  baseUrl: string | null
  profileKey: string
  enabled: boolean
  status: string
  lastCheckedAt: string | null
  lastErrorCode: string | null
  lastErrorMsg: string | null
  createdAt: string
  updatedAt: string
}

export interface AgentTask {
  id: number
  title: string
  selectedModelId: number | null
  conversationId: number | null
  skillId: number | null
  status: string
  currentStepId: string | null
  planVersion: number
  startedAt: string | null
  finishedAt: string | null
  errorCode: string | null
  errorMsg: string | null
  resultJson: string | null
  createdAt: string
  updatedAt: string
}

export interface ToolDefinition {
  id: number
  toolId: string
  name: string
  description: string | null
  category: string | null
  inputSchemaJson: string | null
  outputSchemaJson: string | null
  riskLevel: string
  enabled: boolean
  timeoutMs: number
}

export interface McpServerConfig {
  id: number
  name: string
  transportType: string
  endpointOrCommandRef: string | null
  enabled: boolean
  connectionStatus: string
  trustLevel: string
  lastCheckedAt: string | null
}

export interface McpCapability {
  id: number
  serverId: number
  capabilityType: string
  capabilityName: string
  description: string | null
  schemaJson: string | null
  enabled: boolean
  riskLevel: string
}

export interface SkillDefinition {
  id: number
  skillId: string
  name: string
  version: string
  description: string | null
  sourcePath: string
  contentHash: string | null
  enabled: boolean
  validationStatus: string
}

export interface TaskEvent {
  eventId: number
  taskId: number
  sequence: number
  timestamp: string
  eventType: string
  level: string
  stepId: string | null
  status: string | null
  message: string | null
  payloadJson: string | null
  durationMs: number | null
  modelId: number | null
  toolId: string | null
  requiresApproval: boolean
}

export interface ContextStats {
  messageCount: number
  totalChars: number
  totalTokenEstimate: number
  contextBudget: number
  contextStrategy: string
  keepRecentTurn: number
  budgetUsage: number
}
export interface LoginStatusResult {
  status: string
  message: string
  detail: string | null
}