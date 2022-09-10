"use strict";

const MessagePopupState = {
    stateId: 'stateId',
    title: 'title',
    text: 'text',
    fullScreen: 'fullScreen',
    contentRenderer: 'contentRenderer',
    cancelBtnText: 'cancelBtnText',
    onCancel: 'onCancel',
    okBtnText: 'okBtnText',
    okBtnColor: 'okBtnColor',
    onOk: 'onOk',
    onClose: 'onClose',
    showProgress: 'showProgress',
    additionalActionsRenderer: 'additionalActionsRenderer',

    new(init) {
        return createObj(init)
    }
}

function useMessagePopup() {

    const s = MessagePopupState
    const [states, setStates] = useState([])
    const [stateCnt, setStateCnt] = useState(0)

    function renderOkButton({st}) {
        if (hasValue(st[s.okBtnText])) {
            return RE.div({style:{position: 'relative'}},
                RE.Button(
                    {
                        variant: 'contained',
                        color: st[s.okBtnColor] ?? 'primary',
                        disabled: st[s.showProgress],
                        onClick: st[s.onOk]
                    },
                    st[s.okBtnText]
                ),
                st[s.showProgress]?RE.CircularProgress({size:24, style: inButtonCircularProgressStyle}):null
            )
        }
    }

    function renderCancelButton({st}) {
        if (hasValue(st[s.cancelBtnText])) {
            return RE.Button({onClick: st[s.onCancel]}, st[s.cancelBtnText])
        }
    }

    function renderActionButtons({st}) {
        return RE.Fragment({},
            st[s.additionalActionsRenderer]?.(),
            renderCancelButton({st}),
            renderOkButton({st})
        )
    }

    function renderMessagePopupForState({st}) {
        return RE.Dialog({open:true, fullScreen:st[s.fullScreen], key: st[s.stateId]},
            RE.If(hasValue(st[s.title]), () => RE.DialogTitle({},
                RE.If(hasValue(st[s.onClose]), () => iconButton({iconName:'close', onClick: st[s.onClose],})),
                st[s.title]
            )),
            RE.If(hasValue(st[s.contentRenderer]), () => RE.DialogContent({}, st[s.contentRenderer]())),
            RE.If(hasValue(st[s.text]), () => RE.DialogContent({}, RE.Typography({}, st[s.text]))),
            RE.DialogActions({}, renderActionButtons({st}))
        )
    }

    function openPopup(state) {
        setStates(prev => [
            ...prev,
            state
        ])
    }

    function closePopup({stateId}) {
        setStates(prev => prev.filter(st => st[s.stateId] !== stateId))
    }

    // public ----------------------------------------

    function renderMessagePopup() {
        return states.map(st => renderMessagePopupForState({st}))
    }

    function confirmAction({text, cancelBtnText = 'cancel', okBtnText = 'ok', okBtnColor}) {
        return new Promise(resolve => {
            const stateId = stateCnt
            setStateCnt(prev => prev+1)
            openPopup(s.new({
                [s.stateId]: stateId,
                [s.title]: null,
                [s.contentRenderer]: null,
                [s.text]: text,
                [s.fullScreen]: false,
                [s.cancelBtnText]: cancelBtnText,
                [s.onCancel]: () => {
                    closePopup({stateId})
                    resolve(false)
                },
                [s.okBtnText]: okBtnText,
                [s.okBtnColor]: okBtnColor,
                [s.onOk]: () => {
                    closePopup({stateId})
                    resolve(true)
                },
                [s.onClose]: null,
                [s.showProgress]: false,
                [s.additionalActionsRenderer]: null,
            }))
        })
    }

    async function showMessage({title, text, okBtnText = 'ok', additionalActionsRenderer = null, hideOkBtn = false}) {
        return new Promise(resolve => {
            const stateId = stateCnt
            setStateCnt(prev => prev+1)

            openPopup(s.new({
                [s.stateId]: stateId,
                [s.title]: title,
                [s.contentRenderer]: null,
                [s.text]: text,
                [s.fullScreen]: false,
                [s.cancelBtnText]: null,
                [s.onCancel]: null,
                [s.okBtnText]: okBtnText,
                [s.okBtnColor]: null,
                [s.onOk]: () => {
                    closePopup({stateId})
                    resolve(true)
                },
                [s.onClose]: null,
                [s.showProgress]: false,
                [s.additionalActionsRenderer]: additionalActionsRenderer,
            }))
        })
    }

    async function showDialog({title, fullScreen = false, contentRenderer, cancelBtnText, cancelBtnResult = null, okBtnText = null, okBtnColor = null, okBtnResult = null, additionalActionsRenderer = null, onClose}) {
        return new Promise(resolve => {
            const stateId = stateCnt
            setStateCnt(prev => prev+1)

            openPopup(s.new({
                type:'showDialog',
                args: arguments[0],
                [s.stateId]: stateId,
                [s.title]: title,
                [s.contentRenderer]: () => {
                    return contentRenderer(dialogResult => {
                        closePopup({stateId})
                        resolve(dialogResult)
                    })
                },
                [s.text]: null,
                [s.fullScreen]: fullScreen,
                [s.cancelBtnText]: cancelBtnText,
                [s.onCancel]: () => {
                    closePopup({stateId})
                    resolve(cancelBtnResult)
                },
                [s.okBtnText]: okBtnText,
                [s.okBtnColor]: okBtnColor,
                [s.onOk]: () => {
                    closePopup({stateId})
                    resolve(okBtnResult)
                },
                [s.onClose]: onClose,
                [s.showProgress]: false,
                [s.additionalActionsRenderer]: null,
            }))
        })
    }

    function showMessageWithProgress({text, okBtnText = 'ok'}) {
        const stateId = stateCnt
        setStateCnt(prev => prev+1)

        openPopup(s.new({
            [s.stateId]: stateId,
            [s.title]: null,
            [s.contentRenderer]: null,
            [s.text]: text,
            [s.fullScreen]: false,
            [s.cancelBtnText]: null,
            [s.onCancel]: null,
            [s.okBtnText]: okBtnText,
            [s.okBtnColor]: null,
            [s.onOk]: () => null,
            [s.onClose]: null,
            [s.showProgress]: true,
            [s.additionalActionsRenderer]: null,
        }))
        return () => closePopup({stateId})
    }

    function showError({code, msg}) {
        return showMessage({text: `Error [${code}] - ${msg}`})
    }

    return {
        renderMessagePopup: useFuncRef(renderMessagePopup),
        confirmAction: useFuncRef(confirmAction),
        showMessage: useFuncRef(showMessage),
        showError: useFuncRef(showError),
        showMessageWithProgress: useFuncRef(showMessageWithProgress),
        showDialog: useFuncRef(showDialog),
    }
}
