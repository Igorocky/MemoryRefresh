"use strict";

const HttpServerView = ({}) => {
    const {renderMessagePopup, showError, showMessage, showMessageWithProgress} = useMessagePopup()
    const [httpServerState, setHttpServerState] = useState(null)

    useEffect(async () => {
        reloadServerState()
    }, [])

    async function reloadServerState() {
        const serverStateResp = await be.getHttpServerState()
        setHttpServerState(serverStateResp.data)
    }

    async function startHttpServer() {
        const resp = await be.startHttpServer()
        if (resp.err) {
            await showError(resp.err)
        }
        reloadServerState()
    }

    async function stopHttpServer() {
        await be.stopHttpServer()
        reloadServerState()
    }

    async function saveSettings(newSettings) {
        await be.saveHttpServerSettings({...httpServerState.settings, ...newSettings})
        reloadServerState()
    }

    function renderTextSetting({title, attrName, editable = true,validator, isPassword = false}) {
        return re(TextParamView,{
            paramName:title,
            paramValue: httpServerState.settings[attrName],
            onSave: newValue => saveSettings({[attrName]: newValue}),
            editable,
            validator,
            isPassword
        })
    }

    function renderButtons() {
        const startStopButton = httpServerState.isRunning
            ? RE.Button({variant: 'contained', color: 'secondary', onClick: stopHttpServer}, "stop https server")
            : RE.Button({variant: 'contained', color: 'primary', onClick: startHttpServer}, "start https server")
        return RE.Container.row.left.center({}, {},
            iconButton({iconName:'refresh', onClick: reloadServerState}),
            startStopButton
        )
    }

    function renderUrl() {
        if (httpServerState.isRunning) {
            return 'Running on ' + httpServerState.url
        }
    }

    if (!httpServerState) {
        return "Loading..."
    } else {
        return RE.Container.col.top.left({},{style: {margin:'10px'}},
            renderButtons(),
            renderUrl(),
            renderTextSetting({title:'Key store file',attrName:'keyStoreName',editable:false}),
            renderTextSetting({title:'Key store password',attrName:'keyStorePassword',isPassword:true}),
            renderTextSetting({title:'Key alias',attrName:'keyAlias'}),
            renderTextSetting({title:'Private key password',attrName:'privateKeyPassword',isPassword:true}),
            renderTextSetting({title:'Port',attrName:'port',validator: str => str.match(/^\d+$/)?true:false}),
            renderTextSetting({title:'Server password',attrName:'serverPassword',isPassword:true}),
            renderMessagePopup(),
        )
    }
}