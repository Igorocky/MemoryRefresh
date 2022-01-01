"use strict";

function useMessagePopup() {
    const [dialogOpened, setDialogOpened] = useState(false)
    const [title, setTitle] = useState(null)
    const [text, setText] = useState(null)
    const [contentRenderer, setContentRenderer] = useState(null)
    const [cancelBtnText, setCancelBtnText] = useState(null)
    const [onCancel, setOnCancel] = useState(null)
    const [okBtnText, setOkBtnText] = useState(null)
    const [okBtnColor, setOkBtnColor] = useState(null)
    const [onOk, setOnOk] = useState(null)
    const [showProgress, setShowProgress] = useState(false)
    const [additionalActionsRenderer, setAdditionalActionsRenderer] = useState(null)

    function renderOkButton() {
        if (hasValue(okBtnText)) {
            return RE.div({style:{position: 'relative'}},
                RE.Button({variant: 'contained', color: okBtnColor??'primary', disabled: showProgress, onClick: onOk}, okBtnText),
                showProgress?RE.CircularProgress({size:24, style: inButtonCircularProgressStyle}):null
            )
        }
    }

    function renderCancelButton() {
        if (hasValue(cancelBtnText)) {
            return RE.Button({onClick: onCancel}, cancelBtnText)
        }
    }

    function renderActionButtons() {
        return RE.Fragment({},
            additionalActionsRenderer?.(),
            renderCancelButton(),
            renderOkButton()
        )
    }

    function renderMessagePopup() {
        if (dialogOpened) {
            return RE.Dialog({open:true},
                RE.If(hasValue(title), () => RE.DialogTitle({}, title)),
                RE.If(hasValue(contentRenderer), () => RE.DialogContent({}, contentRenderer())),
                RE.If(hasValue(text), () => RE.DialogContent({}, RE.Typography({}, text))),
                RE.DialogActions({}, renderActionButtons())
            )
        }
    }

    async function confirmAction({text, cancelBtnText = 'cancel', okBtnText = 'ok', okBtnColor}) {
        return new Promise(resolve => {
            setDialogOpened(true)
            setTitle(null)
            setContentRenderer(null)
            setText(text)
            setCancelBtnText(cancelBtnText)
            setOnCancel(() => () => {
                setDialogOpened(false)
                resolve(false)
            })
            setOkBtnText(okBtnText)
            setOkBtnColor(okBtnColor)
            setOnOk(() => () => {
                setDialogOpened(false)
                resolve(true)
            })
            setShowProgress(false)
            setAdditionalActionsRenderer(null)
        })
    }

    async function showMessage({title, text, okBtnText = 'ok', additionalActionsRenderer = null, hideOkBtn = false}) {
        return new Promise(resolve => {
            setDialogOpened(true)
            setTitle(null)
            setContentRenderer(null)
            setText(text)
            setCancelBtnText(null)
            setOnCancel(null)
            setOkBtnText(okBtnText)
            setOkBtnColor(null)
            setOnOk(() => () => {
                setDialogOpened(false)
                resolve(true)
            })
            setShowProgress(false)
            setAdditionalActionsRenderer(() => additionalActionsRenderer)
        })
    }

    async function showDialog({title, contentRenderer, cancelBtnText, cancelBtnResult = null}) {
        return new Promise(resolve => {
            setDialogOpened(true)
            setTitle(title)
            setContentRenderer(() => () => {
                return contentRenderer(dialogResult => {
                    setDialogOpened(false)
                    resolve(dialogResult)
                })
            })
            setText(null)
            setCancelBtnText(cancelBtnText)
            setOnCancel(() => () => {
                setDialogOpened(false)
                resolve(cancelBtnResult)
            })
            setOkBtnText(null)
            setOkBtnColor(null)
            setOnOk(null)
            setShowProgress(false)
            setAdditionalActionsRenderer(() => null)
        })
    }

    function showMessageWithProgress({text, okBtnText = 'ok'}) {
        setDialogOpened(true)
        setTitle(null)
        setContentRenderer(null)
        setText(text)
        setCancelBtnText(null)
        setOnCancel(null)
        setOkBtnText(okBtnText)
        setOkBtnColor(null)
        setOnOk(() => () => null)
        setShowProgress(true)
        setAdditionalActionsRenderer(null)
        return () => setDialogOpened(false)
    }

    function showError({code, msg}) {
        return showMessage({text: `Error [${code}] - ${msg}`})
    }

    return {renderMessagePopup, confirmAction, showMessage, showError, showMessageWithProgress, showDialog}
}
