import { useState } from 'react'

function getTheme() {
  const theme = localStorage.getItem('springtheme')
  if (theme === 'light' || theme === 'dark') {
    return theme
  }
  // Design system: Dark Mode OLED first (MASTER.md). Fall back to system
  // preference only when the user has never chosen a theme.
  if (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches) {
    return 'light'
  }
  return 'dark'
}

export default function useTheme() {
  const [darkTheme] = useState(getTheme())
  return darkTheme
}
