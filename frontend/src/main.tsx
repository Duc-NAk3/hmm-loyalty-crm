import { App as AntApp, ConfigProvider } from 'antd'
import viVN from 'antd/locale/vi_VN'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './index.css'

dayjs.locale('vi')

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfigProvider
      locale={viVN}
      theme={{
        token: { colorPrimary: '#b45309', borderRadius: 8, fontSize: 14 },
        components: {
          Layout: { siderBg: '#3b2a12', triggerBg: '#2a1d0c', headerBg: '#3b2a12' },
          Menu: { darkItemBg: '#3b2a12', darkItemSelectedBg: '#b45309' },
        },
      }}
    >
      <AntApp>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </AntApp>
    </ConfigProvider>
  </StrictMode>,
)
