import { createContext, useContext, type ButtonHTMLAttributes, type HTMLAttributes, type ReactNode } from 'react'
import { cn } from '../../lib/utils'

const TabsContext = createContext<{ value: string; onValueChange: (value: string) => void }>({ value: '', onValueChange: () => undefined })

export function Tabs({ value, onValueChange, className, children }: { value: string; onValueChange: (value: string) => void; className?: string; children: ReactNode }) {
  return <TabsContext.Provider value={{ value, onValueChange }}><div className={cn(className)}>{children}</div></TabsContext.Provider>
}
export function TabsList({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('inline-flex h-10 items-center justify-center rounded-lg bg-muted p-1 text-muted-foreground', className)} {...props} />
}
export function TabsTrigger({ value, className, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { value: string }) {
  const tabs = useContext(TabsContext)
  return <button type="button" aria-selected={tabs.value === value} onClick={() => tabs.onValueChange(value)} className={cn('inline-flex items-center justify-center rounded-md px-3 py-1.5 text-sm font-medium transition-all aria-selected:bg-background aria-selected:text-foreground aria-selected:shadow-sm', className)} {...props} />
}
