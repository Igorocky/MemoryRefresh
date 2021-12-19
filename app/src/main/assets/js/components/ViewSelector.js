"use strict";

const VIEW_NAME_ATTR = '_view'
function createQueryObjectForView(viewName, params) {
    return {[VIEW_NAME_ATTR]:viewName, ...(hasValue(params)?params:{})}
}

const HOME_PAGE_VIEW = 'homePage'
const DEBUG_VIEW = 'debug'
const TAGS_VIEW = 'tags'
const BACKUPS_VIEW = 'backups'
const HTTP_SERVER_VIEW = 'httpserver'
const SEARCH_NOTES_VIEW = 'searchNotes'
const ADD_NOTES_VIEW = 'addNotes'
const VIEWS = {}
function addView({name, component}) {
    VIEWS[name] = {
        name,
        render({query, openView, setPageTitle}) {
            return re(component,{openView, setPageTitle, query})
        }
    }
}
addView({name: HOME_PAGE_VIEW, component: HomePage})
addView({name: DEBUG_VIEW, component: DebugPage})
addView({name: BACKUPS_VIEW, component: BackupsView})
addView({name: HTTP_SERVER_VIEW, component: HttpServerView})
addView({name: TAGS_VIEW, component: TagsView})
addView({name: SEARCH_NOTES_VIEW, component: SearchNotesView})
addView({name: ADD_NOTES_VIEW, component: AddNotesView})

const ViewSelector = ({}) => {
    const [currentViewUrl, setCurrentViewUrl] = useState(null)
    const [environmentName, setEnvironmentName] = useState(null)
    const [pageTitle, setPageTitle] = useState(null)
    const [showMoreControlButtons, setShowMoreControlButtons] = useState(false)

    const query = parseSearchParams(currentViewUrl)

    useEffect(() => {
        updatePageTitle()
    }, [environmentName, pageTitle])

    useEffect(() => {
        openView(TAGS_VIEW)
    }, [])

    function updatePageTitle() {
        document.title = `${environmentName == 'PROD' ? '' : '{' + environmentName + '} - '}${pageTitle}`
    }

    function openView(viewName,params) {
        setCurrentViewUrl(window.location.pathname + '?' + new URLSearchParams(createQueryObjectForView(viewName,params)).toString())
    }

    function getSelectedView() {
        return VIEWS[query[VIEW_NAME_ATTR]]
    }

    function renderSelectedView() {
        const selectedView = getSelectedView()
        if (selectedView) {
            return selectedView.render({
                query,
                openView,
                setPageTitle: str => setPageTitle(str),
            })
        }
    }

    function renderControlButtons() {
        const selectedViewName = getSelectedView()?.name
        const bgColor = viewName => viewName == selectedViewName ? '#00ff72' : undefined
        const additionalButtons = [
            [
                {key:BACKUPS_VIEW, viewName:BACKUPS_VIEW, iconName:"backup_table"},
                {key:HTTP_SERVER_VIEW, viewName:HTTP_SERVER_VIEW, iconName:"devices"},
            ]
        ]
        const buttons = [[
            {key:TAGS_VIEW, viewName:TAGS_VIEW, iconName:"sell"},
            {key:SEARCH_NOTES_VIEW, viewName:SEARCH_NOTES_VIEW, iconName:"search"},
            {key:ADD_NOTES_VIEW, viewName:ADD_NOTES_VIEW, iconName:"add"},
            // {key:DEBUG_VIEW, viewName:DEBUG_VIEW, iconName:"adb"},
            getOpenedViewButton(),
            {key:'more', iconName:"more_horiz", onClick: () => setShowMoreControlButtons(old => !old)},
        ].filter(e=>hasValue(e))]

        if (showMoreControlButtons) {
            buttons.push(...additionalButtons)
        }

        function getOpenedViewButton() {
            const currViewName = query[VIEW_NAME_ATTR]
            for (let i = 0; i < additionalButtons.length; i++) {
                for (let j = 0; j < additionalButtons[i].length; j++) {
                    if (additionalButtons[i][j].viewName == currViewName) {
                        return additionalButtons[i][j]
                    }
                }
            }
            return null
        }

        function openViewInternal(viewName,params) {
            setShowMoreControlButtons(false)
            openView(viewName)
        }

        return re(KeyPad, {
            componentKey: "controlButtons",
            keys: buttons.map(r => r.map(b => ({...b,onClick: b.onClick??(() => openViewInternal(b.viewName)), style:{backgroundColor:bgColor(b.viewName)}}))),
            variant: "outlined",
        })
    }

    if (currentViewUrl) {
        return RE.Container.col.top.left({}, {},
            renderControlButtons(),
            renderSelectedView()
        )
    } else {
        const newViewUrl = window.location.pathname + window.location.search
        setCurrentViewUrl(newViewUrl)
        return "Starting..."
    }
}