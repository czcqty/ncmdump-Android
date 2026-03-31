import { createRoot } from 'react-dom/client'
import React, { useEffect, useState } from 'react'
import './main.css'
import App from './App'
import { FluentProvider, webLightTheme, webDarkTheme } from '@fluentui/react-components'

const Root: React.FC = () => {
  const [isDarkMode, setIsDarkMode] = useState(
    window.matchMedia('(prefers-color-scheme: dark)').matches
  )

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')

    const handleChange = (e: MediaQueryListEvent) => {
      setIsDarkMode(e.matches)
    }

    // 监听系统主题变化
    mediaQuery.addEventListener('change', handleChange)

    // 清理监听器
    return () => {
      mediaQuery.removeEventListener('change', handleChange)
    }
  }, [])

  return (
    <FluentProvider theme={isDarkMode ? webDarkTheme : webLightTheme}>
      <App />
    </FluentProvider>
  )
}

createRoot(document.getElementById('root')!).render(<Root />)
