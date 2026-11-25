export function formatCam(cam: number | null): string {
  return cam === null ? 'CAM ??' : `CAM ${String(cam).padStart(2, '0')}`
}
