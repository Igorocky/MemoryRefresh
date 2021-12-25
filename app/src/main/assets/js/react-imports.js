'use strict';

const re = React.createElement
const useState = React.useState
const useEffect = React.useEffect
const useMemo = React.useMemo
const useCallback = React.useCallback
const useRef = React.useRef
const useReducer = React.useReducer
const Fragment = React.Fragment

function reFactory(elemType) {
    return (props, ...children) => re(elemType, props, ...children)
}

const MaterialUI = window['MaterialUI']
const MuiColors = MaterialUI.colors

const DIRECTION = {row: "row", column: "column",}
const JUSTIFY = {flexStart: "flex-start", center: "center", flexEnd: "flex-end", spaceBetween: "space-between", spaceAround: "space-around",}
const ALIGN_ITEMS = {flexStart: "flex-start", center: "center", flexEnd: "flex-end", stretch: "stretch", spaceAround: "baseline",}

function gridFactory(direction, justify, alignItems) {
    return (props, childProps, ...children) => re(MaterialUI.Grid, {container:true, direction:direction,
            justifyContent:justify, alignItems:alignItems, ...props},
        React.Children.map(children, child => {
            return re(MaterialUI.Grid, {item:true, ...childProps}, child)
        })
    )
}

const RE = {
    div: reFactory('div'),
    span: reFactory('span'),
    table: reFactory('table'),
    tbody: reFactory('tbody'),
    tr: reFactory('tr'),
    td: reFactory('td'),
    AppBar: reFactory(MaterialUI.AppBar),
    Button: reFactory(MaterialUI.Button),
    ButtonGroup: reFactory(MaterialUI.ButtonGroup),
    Breadcrumbs: reFactory(MaterialUI.Breadcrumbs),
    CircularProgress: reFactory(MaterialUI.CircularProgress),
    Checkbox: reFactory(MaterialUI.Checkbox),
    Chip: reFactory(MaterialUI.Chip),
    Dialog: reFactory(MaterialUI.Dialog),
    DialogContent: reFactory(MaterialUI.DialogContent),
    DialogTitle: reFactory(MaterialUI.DialogTitle),
    DialogActions: reFactory(MaterialUI.DialogActions),
    FormControlLabel: reFactory(MaterialUI.FormControlLabel),
    FormControl: reFactory(MaterialUI.FormControl),
    FormLabel: reFactory(MaterialUI.FormLabel),
    FormGroup: reFactory(MaterialUI.FormGroup),
    Grid: reFactory(MaterialUI.Grid),
    Icon: reFactory(MaterialUI.Icon),
    IconButton: reFactory(MaterialUI.IconButton),
    List: reFactory(MaterialUI.List),
    ListItem: reFactory(MaterialUI.ListItem),
    ListItemText: reFactory(MaterialUI.ListItemText),
    Modal: reFactory(MaterialUI.Modal),
    MenuItem: reFactory(MaterialUI.MenuItem),
    InputLabel: reFactory(MaterialUI.InputLabel),
    Paper: reFactory(MaterialUI.Paper),
    Portal: reFactory(MaterialUI.Portal),
    RadioGroup: reFactory(MaterialUI.RadioGroup),
    Radio : reFactory(MaterialUI.Radio),
    Slider: reFactory(MaterialUI.Slider),
    Select: reFactory(MaterialUI.Select),
    Typography: reFactory(MaterialUI.Typography),
    TextField: reFactory(MaterialUI.TextField),
    Toolbar: reFactory(MaterialUI.Toolbar),
    img: reFactory('img'),
    If: (condition, elemsProvider) => condition?elemsProvider():null,
    IfNot: (condition, elemsProvider) => !condition?elemsProvider():null,
    Fragment: reFactory(React.Fragment),
    Container: {
        row: {
            left: {
                top: gridFactory(DIRECTION.row, JUSTIFY.flexStart, ALIGN_ITEMS.flexStart),
                center: gridFactory(DIRECTION.row, JUSTIFY.flexStart, ALIGN_ITEMS.center),
                bottom: gridFactory(DIRECTION.row, JUSTIFY.flexStart, ALIGN_ITEMS.flexEnd),
            },
            center: {
                top: gridFactory(DIRECTION.row, JUSTIFY.center, ALIGN_ITEMS.flexStart),
                center: gridFactory(DIRECTION.row, JUSTIFY.center, ALIGN_ITEMS.center),
            },
            right: {
                top: gridFactory(DIRECTION.row, JUSTIFY.flexEnd, ALIGN_ITEMS.flexStart),
                center: gridFactory(DIRECTION.row, JUSTIFY.flexEnd, ALIGN_ITEMS.center),
            },
        },
        col: {
            top: {
                left: gridFactory(DIRECTION.column, JUSTIFY.flexStart, ALIGN_ITEMS.flexStart),
                center: gridFactory(DIRECTION.column, JUSTIFY.flexStart, ALIGN_ITEMS.center),
                right: gridFactory(DIRECTION.column, JUSTIFY.flexStart, ALIGN_ITEMS.flexEnd),
            }
        }
    },
}

function useStateFromLocalStorage({key, validator}) {
    const [value, setValue] = useState(() => validator(readFromLocalStorage(key, undefined)))

    function setValueInternal(newValue) {
        newValue = validator(newValue)
        saveToLocalStorage(key, newValue)
        setValue(newValue)
    }

    return [
        value,
        newValue => {
            if (typeof newValue === 'function') {
                setValueInternal(newValue(value))
            } else {
                setValueInternal(newValue)
            }
        }
    ]
}

function useStateFromLocalStorageNumber({key, min, max, minIsDefault, maxIsDefault, defaultValue, nullable}) {
    function getDefaultValue() {
        if (typeof defaultValue === 'function') {
            return defaultValue()
        } else if (minIsDefault) {
            return min
        } else if (maxIsDefault) {
            return max
        } else if (hasValue(defaultValue) || nullable && defaultValue === null) {
            return defaultValue
        } else if (nullable) {
            return null
        } else if (hasValue(min)) {
            return min
        } else if (hasValue(max)) {
            return max
        } else {
            throw new Error('Cannot determine default value for ' + key)
        }
    }

    return useStateFromLocalStorage({
        key,
        validator: value => {
            if (value === undefined) {
                return getDefaultValue()
            } else if (value === null) {
                if (nullable) {
                    return null
                } else {
                    return getDefaultValue()
                }
            } else if (!(typeof value === 'number')) {
                return getDefaultValue()
            } else {
                if (hasValue(min) && value < min || hasValue(max) && max < value) {
                    return getDefaultValue()
                } else {
                    return value
                }
            }
        }
    })
}

function useStateFromLocalStorageString({key, defaultValue, nullable}) {
    function getDefaultValue() {
        if (typeof defaultValue === 'function') {
            return defaultValue()
        } else if (hasValue(defaultValue) || nullable && defaultValue === null) {
            return defaultValue
        } else if (nullable) {
            return null
        } else {
            throw new Error('Cannot determine default value for ' + key)
        }
    }

    return useStateFromLocalStorage({
        key,
        validator: value => {
            if (value === undefined) {
                return getDefaultValue()
            } else if (value === null) {
                if (nullable) {
                    return null
                } else {
                    return getDefaultValue()
                }
            } else if (!(typeof value === 'string')) {
                return getDefaultValue()
            } else {
                return value
            }
        }
    })
}

function useStateFromLocalStorageBoolean({key, defaultValue, nullable}) {
    function getDefaultValue() {
        if (typeof defaultValue === 'function') {
            return defaultValue()
        } else if (hasValue(defaultValue) || nullable && defaultValue === null) {
            return defaultValue
        } else if (nullable) {
            return null
        } else {
            throw new Error('Cannot determine default value for ' + key)
        }
    }

    return useStateFromLocalStorage({
        key,
        validator: value => {
            if (value === undefined) {
                return getDefaultValue()
            } else if (value === null) {
                if (nullable) {
                    return null
                } else {
                    return getDefaultValue()
                }
            } else if (!(typeof value === 'boolean')) {
                return getDefaultValue()
            } else {
                return value
            }
        }
    })
}

function iconButton({iconName,onClick,disabled,iconStyle}) {
    return RE.IconButton(
        {
            onClick: e => {
                e.stopPropagation();
                onClick?.()
            },
            disabled
        },
        RE.Icon({style:{color:'black', ...(iconStyle??{})}}, iconName)
    )
}

const inButtonCircularProgressStyle = {
    color: MuiColors.green[500],
    position: 'absolute',
    top: '50%',
    left: '50%',
    marginTop: -12,
    marginLeft: -12,
}

function useMessagePopup() {
    const [dialogOpened, setDialogOpened] = useState(false)
    const [text, setText] = useState(null)
    const [cancelBtnText, setCancelBtnText] = useState(null)
    const [onCancel, setOnCancel] = useState(null)
    const [okBtnText, setOkBtnText] = useState(null)
    const [onOk, setOnOk] = useState(null)
    const [showProgress, setShowProgress] = useState(false)
    const [additionalActionsRenderer, setAdditionalActionsRenderer] = useState(null)

    function renderOkButton() {
        if (okBtnText) {
            return RE.div({style:{position: 'relative'}},
                RE.Button({variant: 'contained', color: 'primary', disabled: showProgress, onClick: onOk}, okBtnText),
                showProgress?RE.CircularProgress({size:24, style: inButtonCircularProgressStyle}):null
            )
        }
    }

    function renderCancelButton() {
        if (cancelBtnText) {
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
                RE.DialogContent({}, RE.Typography({}, text)),
                RE.DialogActions({}, renderActionButtons())
            )
        }
    }

    async function confirmAction({text, cancelBtnText = 'cancel', okBtnText = 'ok'}) {
        return new Promise(resolve => {
            setDialogOpened(true)
            setText(text)
            setCancelBtnText(cancelBtnText)
            setOnCancel(() => () => {
                setDialogOpened(false)
                resolve(false)
            })
            setOkBtnText(okBtnText)
            setOnOk(() => () => {
                setDialogOpened(false)
                resolve(true)
            })
            setShowProgress(false)
            setAdditionalActionsRenderer(null)
        })
    }

    async function showMessage({text, okBtnText = 'ok', additionalActionsRenderer = null}) {
        return new Promise(resolve => {
            setDialogOpened(true)
            setText(text)
            setCancelBtnText(null)
            setOnCancel(null)
            setOkBtnText(okBtnText)
            setOnOk(() => () => {
                setDialogOpened(false)
                resolve(true)
            })
            setShowProgress(false)
            setAdditionalActionsRenderer(() => additionalActionsRenderer)
        })
    }

    function showMessageWithProgress({text, okBtnText = 'ok'}) {
        setDialogOpened(true)
        setText(text)
        setCancelBtnText(null)
        setOnCancel(null)
        setOkBtnText(okBtnText)
        setOnOk(() => () => null)
        setShowProgress(true)
        setAdditionalActionsRenderer(null)
        return () => setDialogOpened(false)
    }

    function showError({code, msg}) {
        return showMessage({text: `Error [${code}] - ${msg}`})
    }

    return {renderMessagePopup, confirmAction, showMessage, showError, showMessageWithProgress}
}

/**
 * @param url:string
 */
function parseSearchParams(url) {
    if (hasNoValue(url)) {
        return {}
    }
    const startIdx = url.indexOf('?')
    const params = new URLSearchParams(startIdx >= 0 ? url.substring(startIdx+1,url.length) : '')
    const result = {}
    for(const [key, value] of params.entries()) {
        result[key] = value
    }
    return result
}