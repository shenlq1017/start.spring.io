import BodyClassName from 'react-body-classname'
import FileSaver from 'file-saver'
import get from 'lodash/get'
import React, {
  Suspense,
  lazy,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react'
import { toast } from 'react-toastify'

import useHash from './utils/Hash'
import useWindowsUtils from './utils/WindowsUtils'
import { AppContext } from './reducer/App'
import { DependencyDialog } from './common/dependency'
import { Fields, Loading } from './common/builder'
import { Form } from './common/form'
import { Header, SideLeft, SideRight } from './common/layout'
import { InitializrContext } from './reducer/Initializr'
import { getConfig, getInfo, getProject } from './utils/ApiUtils'

const Explore = lazy(() => import('./common/explore/Explore'))
const Share = lazy(() => import('./common/share/Share'))
const History = lazy(() => import('./common/history/History'))
const HotKeys = lazy(() => import('./common/builder/HotKeys'))
const Favorite = lazy(() => import('./common/favorite/Favorite'))

const EXPLORE_DEBOUNCE_MS = 450

export default function Application() {
  const {
    complete,
    dispatch,
    theme,
    share: shareOpen,
    explore: exploreOpen,
    history: historyOpen,
    favorite: favoriteOpen,
    favoriteAdd: favoriteAddOpen,
    favoriteOptions,
    list,
    dependencies,
  } = useContext(AppContext)
  const {
    values,
    share,
    dispatch: dispatchInitializr,
  } = useContext(InitializrContext)

  const [blob, setBlob] = useState(null)
  const [generating, setGenerating] = useState(false)

  const buttonExplore = useRef(null)
  const buttonDependency = useRef(null)
  const buttonSubmit = useRef(null)
  const exploreRequestId = useRef(0)

  const windowsUtils = useWindowsUtils()
  useHash()

  useEffect(() => {
    if (windowsUtils.origin) {
      const url = `${windowsUtils.origin}/metadata/client`
      getInfo(url).then(jsonConfig => {
        const response = getConfig(jsonConfig)
        dispatchInitializr({ type: 'COMPLETE', payload: { ...response } })
        dispatch({ type: 'COMPLETE', payload: response })
      })
    }
  }, [dispatch, dispatchInitializr, windowsUtils.origin])

  const onEscape = () => {
    setBlob(null)
    dispatch({
      type: 'UPDATE',
      payload: {
        list: false,
        share: false,
        explore: false,
        nav: false,
        history: favoriteOptions.back === 'history',
        favorite: favoriteOptions.back === 'favorite',
        favoriteAdd: false,
      },
    })
  }

  const fetchProjectBlob = useCallback(async () => {
    const url = `${windowsUtils.origin}/starter.zip`
    const requestId = ++exploreRequestId.current
    try {
      const project = await getProject(url, values, get(dependencies, 'list'))
      if (requestId === exploreRequestId.current) {
        setBlob(project)
      }
      return project
    } catch (err) {
      if (requestId === exploreRequestId.current) {
        toast.error(
          err || `Could not connect to server. Please check your network.`
        )
      }
      throw err
    }
  }, [windowsUtils.origin, values, dependencies])

  const onSubmit = async () => {
    if (generating || list) {
      return
    }
    setGenerating(true)
    const url = `${windowsUtils.origin}/starter.zip`
    const project = await getProject(
      url,
      values,
      get(dependencies, 'list')
    ).catch(err => {
      toast.error(
        err || `Could not connect to server. Please check your network.`
      )
    })
    setGenerating(false)
    if (project) {
      FileSaver.saveAs(project, `${get(values, 'meta.artifact')}.zip`)
      dispatch({ type: 'ADD_HISTORY', payload: share })
    }
  }

  const onExplore = async () => {
    dispatch({ type: 'UPDATE', payload: { explore: true, list: false } })
    setBlob(null)
    try {
      await fetchProjectBlob()
    } catch (e) {
      onEscape()
    }
  }

  // Debounced Explore refresh when architecture / template / entities change
  const architecture = get(values, 'architecture')
  const template = get(values, 'template')
  const entities = get(values, 'entities')
  const entitiesSig = JSON.stringify(
    (entities || []).map(e => ({
      name: e.name,
      table: e.table,
      db: e.db,
      orm: e.orm,
      fields: e.fields,
      apis: e.apis,
    }))
  )
  const exploreSignature = `${architecture}|${template || ''}|${entitiesSig}`
  const prevExploreSignature = useRef(exploreSignature)
  useEffect(() => {
    if (!exploreOpen || !complete) {
      prevExploreSignature.current = exploreSignature
      return undefined
    }
    if (prevExploreSignature.current === exploreSignature) {
      return undefined
    }
    prevExploreSignature.current = exploreSignature
    setBlob(null)
    const timer = setTimeout(() => {
      fetchProjectBlob().catch(() => {
        /* toast already shown */
      })
    }, EXPLORE_DEBOUNCE_MS)
    return () => {
      clearTimeout(timer)
    }
  }, [exploreOpen, complete, exploreSignature, fetchProjectBlob])

  const onShare = () => {
    dispatch({ type: 'UPDATE', payload: { share: true } })
  }

  const onFavoriteAdd = () => {
    dispatch({ type: 'UPDATE', payload: { favoriteAdd: true } })
  }

  return (
    <>
      <BodyClassName className={theme} />
      <Suspense fallback=''>
        <HotKeys
          onSubmit={() => {
            if (get(buttonSubmit, 'current')) {
              buttonSubmit.current.click()
            }
          }}
          onExplore={() => {
            if (get(buttonExplore, 'current')) {
              buttonExplore.current.click()
            }
          }}
          onDependency={event => {
            if (get(buttonDependency, 'current')) {
              buttonDependency.current.click()
            }
            event.preventDefault()
          }}
          onEscape={onEscape}
        />
      </Suspense>
      <SideLeft />
      <div id='main'>
        <Header />
        <hr className='divider' />
        <Form onSubmit={onSubmit}>
          {!complete ? (
            <Loading />
          ) : (
            <>
              <Fields
                onSubmit={onSubmit}
                onShare={onShare}
                onExplore={onExplore}
                onFavoriteAdd={onFavoriteAdd}
                refExplore={buttonExplore}
                refSubmit={buttonSubmit}
                refDependency={buttonDependency}
                generating={generating}
              />
              <DependencyDialog onClose={onEscape} />
            </>
          )}
        </Form>
      </div>
      <SideRight />
      <Suspense fallback=''>
        <Share open={shareOpen || false} shareUrl={share} onClose={onEscape} />
        <Explore
          projectName={`${get(values, 'meta.artifact')}.zip`}
          blob={blob}
          open={exploreOpen || false}
          onClose={onEscape}
        />
        <History open={historyOpen || false} onClose={onEscape} />
        <Favorite
          add={favoriteAddOpen || false}
          open={favoriteOpen || false}
          onClose={onEscape}
        />
      </Suspense>
    </>
  )
}
