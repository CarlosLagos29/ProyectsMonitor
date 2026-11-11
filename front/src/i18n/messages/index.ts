import checkReason from './checkReason'
import common from './common'
import errors from './errors'
import home from './home'
import login from './login'
import monitorDetail from './monitorDetail'
import notFound from './notFound'
import status from './status'

export const messages = {
  en: {
    common: common.en,
    status: status.en,
    checkReason: checkReason.en,
    errors: errors.en,
    login: login.en,
    home: home.en,
    monitorDetail: monitorDetail.en,
    notFound: notFound.en,
  },
  es: {
    common: common.es,
    status: status.es,
    checkReason: checkReason.es,
    errors: errors.es,
    login: login.es,
    home: home.es,
    monitorDetail: monitorDetail.es,
    notFound: notFound.es,
  },
}

export type MessageSchema = typeof messages.en
