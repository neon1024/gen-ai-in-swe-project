import { useCallback, useState, type ReactNode } from 'react'
import { ConfirmDialog } from './confirm-dialog'

export interface ConfirmOptions {
  title: string
  message: ReactNode
  confirmLabel: string
  cancelLabel?: string
  destructive?: boolean
}

/**
 * Promise-based confirmation for important actions. Render `dialog` once in the page, then
 * `if (!(await confirm({...}))) return`.
 */
export function useConfirm() {
  const [request, setRequest] = useState<(ConfirmOptions & { resolve: (ok: boolean) => void }) | null>(null)

  const confirm = useCallback(
    (options: ConfirmOptions) => new Promise<boolean>((resolve) => setRequest({ ...options, resolve })),
    [],
  )
  const close = useCallback((ok: boolean) => {
    setRequest((current) => {
      current?.resolve(ok)
      return null
    })
  }, [])
  const cancel = useCallback(() => close(false), [close])

  const dialog = request ? (
    <ConfirmDialog
      title={request.title}
      confirmLabel={request.confirmLabel}
      cancelLabel={request.cancelLabel}
      destructive={request.destructive}
      onConfirm={() => close(true)}
      onCancel={cancel}
    >
      {request.message}
    </ConfirmDialog>
  ) : null

  return { confirm, dialog }
}
