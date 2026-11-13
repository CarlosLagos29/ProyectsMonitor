import type { CheckResultDto } from './dto/check.dto'
import type { MonitorDto, MonitorListResponseDto } from './dto/monitor.dto'
import { API_PREFIX, http } from './http'
import { toCheckResult } from './mappers/check.mapper'
import {
  toCreateMonitorRequest,
  toMonitor,
  toMonitorListQuery,
  toMonitorPage,
  toUpdateMonitorRequest,
} from './mappers/monitor.mapper'
import type {
  CheckResult,
  Monitor,
  MonitorChanges,
  MonitorDraft,
  MonitorListParams,
  MonitorPage,
} from './api.types'

const MONITORS_PATH = `${API_PREFIX}/monitors`

function monitorPath(id: string) {
  return `${MONITORS_PATH}/${encodeURIComponent(id)}`
}

export async function listMonitors(params: MonitorListParams = {}): Promise<MonitorPage> {
  const { data } = await http.get<MonitorListResponseDto>(MONITORS_PATH, {
    params: toMonitorListQuery(params),
  })
  return toMonitorPage(data)
}

export async function getMonitor(id: string): Promise<Monitor> {
  const { data } = await http.get<MonitorDto>(monitorPath(id))
  return toMonitor(data)
}

export async function createMonitor(draft: MonitorDraft, canSetPublic: boolean): Promise<Monitor> {
  const body = toCreateMonitorRequest(draft, canSetPublic)
  const { data } = await http.post<MonitorDto>(MONITORS_PATH, body)
  return toMonitor(data)
}

export async function updateMonitor(
  id: string,
  changes: MonitorChanges,
  canSetPublic: boolean,
): Promise<Monitor> {
  const body = toUpdateMonitorRequest(changes, canSetPublic)
  const { data } = await http.patch<MonitorDto>(monitorPath(id), body)
  return toMonitor(data)
}

export async function deleteMonitor(id: string): Promise<void> {
  await http.delete(monitorPath(id))
}

export async function checkMonitor(id: string): Promise<CheckResult> {
  const { data } = await http.post<CheckResultDto>(`${monitorPath(id)}/check`)
  return toCheckResult(data)
}
