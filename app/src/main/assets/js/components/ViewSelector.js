"use strict";

const VIEW_NAME_ATTR = '_view'
function createQueryObjectForView(viewName, params) {
    return {[VIEW_NAME_ATTR]:viewName, ...(hasValue(params)?params:{})}
}

const BACKUPS_VIEW = 'BACKUPS_VIEW'
const HTTP_SERVER_VIEW = 'HTTP_SERVER_VIEW'

const TAGS_VIEW = 'TAGS_VIEW'
const CARDS_SEARCH_VIEW = 'CARDS_SEARCH_VIEW'
const CREATE_CARD_VIEW = 'CREATE_CARD_VIEW'
const REPEAT_CARDS_VIEW = 'REPEAT_CARDS_VIEW'
const FAST_REPEAT_CARDS_VIEW = 'FAST_REPEAT_CARDS_VIEW'

const VIEWS = {}
function addView({name, component, params}) {
    VIEWS[name] = {
        name,
        render({openView, setPageTitle, query, controlsContainer}) {
            return re(component,{...(params??{}), openView, setPageTitle, query, controlsContainer})
        }
    }
}
addView({name: BACKUPS_VIEW, component: BackupsView})
addView({name: HTTP_SERVER_VIEW, component: HttpServerView})

addView({name: TAGS_VIEW, component: TagsView})
addView({name: CARDS_SEARCH_VIEW, component: CardsSearchView})
addView({name: CREATE_CARD_VIEW, component: CreateCardView})
addView({name: REPEAT_CARDS_VIEW, component: RepeatCardsView, params: {key:1, cycledMode:false}})
addView({name: FAST_REPEAT_CARDS_VIEW, component: RepeatCardsView, params: {key:2, cycledMode:true}})

const ViewSelector = ({}) => {
    const [currentViewUrl, setCurrentViewUrl] = useState(null)
    const [pageTitle, setPageTitle] = useState("MemoryRefresh")
    const [showMoreControlButtons, setShowMoreControlButtons] = useState(false)

    const controlsContainer = useRef(null)

    const query = parseSearchParams(currentViewUrl)

    useEffect(() => {
        updatePageTitle()
    }, [pageTitle])

    useEffect(() => {
        openView(REPEAT_CARDS_VIEW)
    }, [])

    function updatePageTitle() {
        document.title = pageTitle
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
                controlsContainer: showMoreControlButtons ? null : controlsContainer
            })
        }
    }

    function renderControlButtons() {
        const selectedViewName = getSelectedView()?.name
        const bgColor = viewName => viewName == selectedViewName ? '#00d0ff' : undefined
        const additionalButtons = [
            [
                {key:FAST_REPEAT_CARDS_VIEW, viewName:FAST_REPEAT_CARDS_VIEW, iconName:'speed'},
                {key:TAGS_VIEW, viewName:TAGS_VIEW, iconName:'sell'},
                {key:CARDS_SEARCH_VIEW, viewName:CARDS_SEARCH_VIEW, iconName:'search'},
                {key:BACKUPS_VIEW, viewName:BACKUPS_VIEW, iconName:'archive'},
                {key:HTTP_SERVER_VIEW, viewName:HTTP_SERVER_VIEW, iconName:'reset_tv'},
            ]
        ]
        const buttons = [[
            {key:REPEAT_CARDS_VIEW, viewName:REPEAT_CARDS_VIEW, iconName:"published_with_changes"},
            {key:CREATE_CARD_VIEW, viewName:CREATE_CARD_VIEW, iconName:"add"},
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
            openView(viewName,params)
        }

        return RE.Container.row.left.center({ref: controlsContainer},{},
            re(KeyPad, {
                componentKey: "controlButtons",
                keys: buttons.map(r => r.map(b => ({...b,onClick: b.onClick??(() => openViewInternal(b.viewName)), style:{backgroundColor:bgColor(b.viewName)}}))),
                variant: "outlined",
            })
        )
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