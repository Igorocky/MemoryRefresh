"use strict";

const DELAY_DUR_REGEX = /^(\d+)([smhdM])?$/
const DELAY_COEF_REGEX = /^x(\d+(?:\.\d+)?)$/

const DelayCmp = ({
                      actualDelay, initialDelay, setDelayRef, delayResultRef,
                      coefs, coefsOnChange,
                      delayTextFieldRef, delayTextFieldId, delayTextFieldTabIndex, updateDelayRequestIsInProgress,
                      onSubmit, onF9}) => {

    // actualDelay = '15d 21h'
    // initialDelay = '3h'
    // onSubmit = newDelay => console.log('submit:delay', newDelay)
    // coefs = ['','x2.0','','']

    const [delay, setDelay] = useState(initialDelay)
    if (setDelayRef) {
        setDelayRef.current = newDelay => setDelay(newDelay)
    }

    const {renderMessagePopup, showDialog} = useMessagePopup()

    function parseDelayDurStr(text) {
        const delayParseRes = DELAY_DUR_REGEX.exec(text)
        return {delayAmount: delayParseRes?.[1], delayUnit: delayParseRes?.[2]}
    }

    function parseDelayCoefStr(text) {
        const delayParseRes = DELAY_COEF_REGEX.exec(text)
        return {delayCoef: delayParseRes?.[1]}
    }

    const {delayUnit: initialDelayUnit} = parseDelayDurStr(initialDelay)
    const {delayAmount, delayUnit:delayUnitOrig} = parseDelayDurStr(delay)
    const delayUnit = delayUnitOrig??initialDelayUnit
    const {delayCoef} = parseDelayCoefStr(delay)

    function getResult() {
        if (hasValue(delayCoef)) {
            return delay
        } else if (hasValue(delayAmount)) {
            return delayAmount + delayUnit
        }
    }

    const result = getResult()
    if (delayResultRef) {
        delayResultRef.current = result
    }

    async function openSettings() {
        const newCoefs = await showDialog({
            title: 'Delay coefficients:',
            contentRenderer: resolve => re(DelayCoefsSettingsCmp,{
                coefs,
                onOk: newCoefs => resolve(newCoefs),
                onCancel: () => resolve(null)
            })
        })
        if (hasValue(newCoefs)) {
            coefsOnChange(newCoefs)
        }
    }

    function toggleCoef(idx) {
        const coef = coefs[idx]
        delay !== coef ? setDelay(coef) : setDelay(initialDelay)
    }

    function renderCoefs() {
        return re(KeyPad, {
            componentKey: "controlButtons",
            buttonWidth:'55px',
            keys: [[
                ...coefs.map((coef,idx) => ({
                    onClick: () => {
                        if (coef !== '') {
                            toggleCoef(idx)
                        }
                    },
                    style:{backgroundColor: coef !== '' && delay === coef ? '#00d0ff' : undefined},
                    symbol:coef,
                })),
                {
                    onClick: openSettings,
                    style:{},
                    iconName:'settings',
                }
            ]],
            variant: "outlined",
        })
    }

    function delayFormOnKeyDown(event) {
        const keyCode = event.keyCode
        if (keyCode === F9_KEY_CODE) {
            event.preventDefault();
            onF9?.()
        } else {
            const newDelay = getDelayCoefByKeyCode({event, coefs, currDelay:delay, initialDelay})
            if (hasValue(newDelay)) {
                setDelay(newDelay)
            }
        }
    }

    return RE.Container.col.top.left({},{style:{marginBottom:'15px'}},
        renderCoefs(),
        re(DelayForm, {
            actualDelay,
            initialDelay,
            delay,
            delayOnChange: newDelay => setDelay(newDelay),
            delayTextFieldRef, delayTextFieldId, delayTextFieldTabIndex, updateDelayRequestIsInProgress,
            onSubmit: () => {
                if (hasValue(result)) {
                    onSubmit?.(result)
                }
            },
            onKeyDown: delayFormOnKeyDown,
            result,
        }),
        renderMessagePopup()
    )
}
