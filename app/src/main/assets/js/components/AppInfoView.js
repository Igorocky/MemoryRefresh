"use strict";

const AppInfoView = ({}) => {
    const {renderMessagePopup, showError} = useMessagePopup()

    const [appInfo, setAppInfo] = useState(null)
    const [errorLoadingAppInfo, setErrorLoadingAppInfo] = useState(null)

    useEffect(async () => {
        const res = await be.getAppInfo()
        if (res.err) {
            setErrorLoadingAppInfo(res.err)
            showError(res.err)
        } else {
            setAppInfo(res.data)
        }
    }, [])

    function renderPageContent() {
        if (hasValue(errorLoadingAppInfo)) {
            return RE.div({}, `Error: [${errorLoadingAppInfo.code}] - ${errorLoadingAppInfo.msg}`)
        } else if (hasNoValue(appInfo)) {
            return RE.div({}, `Loading...`)
        } else {
            return RE.Container.col.top.left({},{},
                RE.div({}, `Version: ${appInfo.version}`),
            )
        }
    }

    return RE.Fragment({},
        renderPageContent(),
        renderMessagePopup()
    )
}