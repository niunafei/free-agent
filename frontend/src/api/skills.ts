import { get, post, patch } from './request'
import type { SkillDefinition } from '@/types'

export const skillsApi = {
  list: () => get<SkillDefinition[]>('/skills'),
  importSkill: (sourcePath: string) => post<SkillDefinition>('/skills/import', { sourcePath }),
  update: (id: number, data: { enabled?: boolean }) => patch<SkillDefinition>(`/skills/${id}`, data)
}